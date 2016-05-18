package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.query.exceptions.ParameterException;
import de.artcom.hivemq_http_api_plugin.query.exceptions.QueryException;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
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

            if (json.isObject()) {
                Query query = parseQuery(json);
                return singleQuery(query);
            } else if (json.isArray()) {
                List<Query> queries = new ArrayList<>();

                for (JsonNode queryJson : json) {
                    queries.add(parseQuery(queryJson));
                }

                return batchQuery(queries);
            }

            throw new ParameterException();
        } catch (ParameterException exception) {
            return formatException(exception);
        } catch (IOException ignored) {
            return QueryResponse.error(HTTP_BAD_REQUEST, "The request body must be a JSON object", objectMapper);
        }
    }

    abstract Query parseQuery(JsonNode json) throws ParameterException;

    abstract QueryResponse formatResult(QueryResult result, Query query);

    abstract QueryResponse formatException(QueryException exception);

    private QueryResponse singleQuery(Query query) {
        try {
            query.validate();
            if (query.isWildcardQuery()) {
                ArrayNode array = objectMapper.getNodeFactory().arrayNode();

                for (QueryResult result : queryProcessor.processWildcardQuery(query)) {
                    QueryResponse queryResponse = formatResult(result, query);
                    array.add(queryResponse.body);
                }

                return QueryResponse.success(array);
            } else {
                QueryResult result = queryProcessor.processSingleQuery(query);
                return formatResult(result, query);
            }
        } catch (QueryException exception) {
            return formatException(exception);
        }
    }

    private QueryResponse batchQuery(List<Query> queries) {
        List<QueryResponse> responses = Lists.transform(queries, this::singleQuery);
        ArrayNode array = objectMapper.getNodeFactory().arrayNode();

        for (QueryResponse response : responses) {
            array.add(response.body);
        }

        return QueryResponse.success(array);
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
}
