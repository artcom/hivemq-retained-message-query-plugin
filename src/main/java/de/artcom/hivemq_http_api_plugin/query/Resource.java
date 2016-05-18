package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import static java.net.HttpURLConnection.HTTP_OK;

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
        try {
            JsonNode result = computeResult(body);
            return createResponse(HTTP_OK, result);
        } catch (IOException ignored) {
            JsonNode errorObject = createErrorObject(HTTP_BAD_REQUEST, "The request body must be a JSON object");
            return createResponse(HTTP_BAD_REQUEST, errorObject);
        }
    }

    private JsonNode computeResult(String body) throws IOException {
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
        }
    }

    abstract Query parseQuery(JsonNode json) throws ParameterException;

    abstract JsonNode formatResult(QueryResult result);

    abstract JsonNode formatException(QueryException exception);

    private JsonNode singleQuery(Query query) {
        try {
            query.validate();
            if (query.isWildcardQuery()) {
                ArrayNode array = objectMapper.getNodeFactory().arrayNode();

                for (QueryResult result : queryProcessor.processWildcardQuery(query)) {
                    array.add(formatResult(result));
                }

                return array;
            } else {
                QueryResult result = queryProcessor.processSingleQuery(query);
                return formatResult(result);
            }
        } catch (QueryException exception) {
            return formatException(exception);
        }
    }

    private JsonNode batchQuery(List<Query> queries) {
        List<JsonNode> results = Lists.transform(queries, this::singleQuery);
        return objectMapper.getNodeFactory().arrayNode().addAll(results);
    }

    JsonNode createErrorObject(int status, String message) {
        return createErrorObject(status, message, null);
    }

    JsonNode createErrorObject(int status, String message, String topic) {
        JsonNodeFactory nodeFactory = objectMapper.getNodeFactory();

        ObjectNode error = nodeFactory.objectNode();
        error.set("error", nodeFactory.numberNode(status));
        error.set("message", nodeFactory.textNode(message));

        if (topic != null) {
            error.set("topic", nodeFactory.textNode(topic));
        }

        return error;
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
