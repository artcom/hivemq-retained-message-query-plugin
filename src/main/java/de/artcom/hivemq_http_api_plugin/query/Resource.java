package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.query.results.Error;
import de.artcom.hivemq_http_api_plugin.query.results.ParameterError;
import de.artcom.hivemq_http_api_plugin.query.results.Result;
import de.artcom.hivemq_http_api_plugin.query.results.ResultList;

import javax.inject.Inject;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@Path("/query")
public class Resource {
    private final ObjectMapper objectMapper;
    private final Processor processor;

    @Inject
    Resource(Processor processor) {
        objectMapper = new ObjectMapper();
        objectMapper.setVisibilityChecker(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(ANY)
                .withGetterVisibility(NONE)
                .withSetterVisibility(NONE)
                .withCreatorVisibility(NONE));

        this.processor = processor;
    }

    @OPTIONS
    public static javax.ws.rs.core.Response options() {
        return javax.ws.rs.core.Response.ok("")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "POST")
                .build();
    }

    @POST
    public javax.ws.rs.core.Response post(String body) throws JsonProcessingException {
        Result result = computeResult(body);

        return javax.ws.rs.core.Response
                .status(result.getStatus())
                .entity(objectMapper.writeValueAsString(result))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "POST")
                .build();
    }

    private Result computeResult(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);

            if (json.isArray()) {
                return processBatchQuery(Lists.newArrayList(json.elements()));
            } else if (json.isObject()) {
                return processSingleQuery(json);
            }
        } catch (IOException ignored) {
        }

        return new ParameterError();
    }

    private Result processBatchQuery(List<JsonNode> queryJsons) {
        return queryJsons.stream()
                .map(this::processSingleQuery)
                .collect(Collectors.toCollection(ResultList::new));
    }

    private Result processSingleQuery(JsonNode queryJson) {
        Query query;
        try {
            query = objectMapper.treeToValue(queryJson, Query.class);
        } catch (JsonProcessingException e) {
            return new ParameterError();
        }

        Error error = query.validate();
        if (error != null) {
            return error;
        }

        Result result = processor.processQuery(query);
        if (query.flatten) {
            return result.flatten().collect(Collectors.toCollection(ResultList::new));
        } else {
            return result;
        }
    }
}
