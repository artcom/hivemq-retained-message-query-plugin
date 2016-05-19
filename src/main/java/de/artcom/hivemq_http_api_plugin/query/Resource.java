package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.query.exceptions.ParameterException;
import de.artcom.hivemq_http_api_plugin.query.exceptions.QueryException;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

abstract class Resource {
    final ObjectMapper objectMapper;
    private final QueryProcessor queryProcessor;

    @Inject
    Resource(QueryProcessor queryProcessor) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());

        this.queryProcessor = queryProcessor;
    }

    public Response options() {
        return Response.ok("")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "POST")
                .build();
    }

    public Response post(String body) {
        QueryResponse response = computeReponse(body);
        return createResponse(response.status, response.body);
    }

    private QueryResponse computeReponse(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);

            if (json.isArray()) {
                return processBatchQuery(Lists.newArrayList(json.elements()));
            } else if (json.isObject()) {
                return processSingleQuery(json);
            }

            return formatException(new ParameterException());
        } catch (IOException ignored) {
            return QueryResponse.error(HTTP_BAD_REQUEST, "The request body must be a JSON object", objectMapper);
        }
    }

    private QueryResponse processBatchQuery(List<JsonNode> queryJsons) {
        List<JsonNode> responseBodies = Lists.transform(queryJsons, (queryJson) -> processSingleQuery(queryJson).body);
        return QueryResponse.success(responseBodies, objectMapper);
    }

    private QueryResponse processSingleQuery(JsonNode queryJson) {
        try {
            Query query = parseQuery(queryJson);
            query.validate();

            if (query.isWildcardQuery()) {
                List<QueryResult> results = queryProcessor.processWildcardQuery(query);
                List<JsonNode> responseBodies = Lists.transform(results, (result) -> formatResult(result, query).body);
                return QueryResponse.success(responseBodies, objectMapper);
            } else {
                QueryResult result = queryProcessor.processSingleQuery(query);
                return formatResult(result, query);
            }
        } catch (QueryException exception) {
            return formatException(exception);
        }
    }

    private static Response createResponse(int status, JsonNode body) {
        return Response
                .status(status)
                .entity(body)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "POST")
                .build();
    }

    abstract Query parseQuery(JsonNode json) throws ParameterException;

    abstract QueryResponse formatResult(QueryResult result, Query query);

    abstract QueryResponse formatException(QueryException exception);
}
