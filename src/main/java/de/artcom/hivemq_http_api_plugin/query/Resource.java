package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.query.exceptions.ParameterException;
import de.artcom.hivemq_http_api_plugin.query.exceptions.QueryException;

import javax.inject.Inject;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

@Path("/query")
public class Resource {
    private final ObjectMapper objectMapper;
    private final Processor processor;

    @Inject
    Resource(Processor processor) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());

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
    public javax.ws.rs.core.Response post(String body) {
        Response response = computeReponse(body);

        return javax.ws.rs.core.Response
                .status(response.status)
                .entity(response.body)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "POST")
                .build();
    }

    private Response computeReponse(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);

            if (json.isArray()) {
                return processBatchQuery(Lists.newArrayList(json.elements()));
            } else if (json.isObject()) {
                return processSingleQuery(json);
            }

            return formatException(new ParameterException());
        } catch (IOException ignored) {
            return Response.error(HTTP_BAD_REQUEST, "The request body must be a JSON object", objectMapper);
        }
    }

    private Response processBatchQuery(List<JsonNode> queryJsons) {
        List<JsonNode> responseBodies = Lists.transform(queryJsons, (queryJson) -> processSingleQuery(queryJson).body);
        return Response.success(responseBodies, objectMapper);
    }

    private Response processSingleQuery(JsonNode queryJson) {
        try {
            Query query = parseQuery(queryJson);
            query.validate();

            if (query.isWildcardQuery()) {
                List<Result> results = processor.processWildcardQuery(query);

                if (query.flatten) {
                    Iterable<Result> flatResults = Iterables.concat(
                            Iterables.transform(results, Result::flatten));
                    return formatResults(Lists.newArrayList(flatResults));
                } else {
                    return formatResults(results);
                }
            } else {
                Result result = processor.processSingleQuery(query);

                if (query.flatten) {
                    return formatResults(result.flatten());
                } else {
                    return formatResult(result);
                }
            }
        } catch (QueryException exception) {
            return formatException(exception);
        }
    }

    private Query parseQuery(JsonNode json) throws ParameterException {
        try {
            return objectMapper.treeToValue(json, Query.class);
        } catch (JsonProcessingException e) {
            throw new ParameterException();
        }
    }

    private Response formatResults(List<Result> results) {
        Iterable<JsonNode> responseBodies = Lists.transform(results, (result) -> formatResult(result).body);
        return Response.success(responseBodies, objectMapper);
    }

    private Response formatResult(Result result) {
        return Response.success(objectMapper.valueToTree(result));
    }

    private Response formatException(QueryException exception) {
        return exception.toQueryResponse(objectMapper);
    }
}
