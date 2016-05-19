package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

public class QueryResponse {
    public final JsonNode body;
    public final int status;

    public static QueryResponse success(List<JsonNode> bodies, ObjectMapper objectMapper) {
        ArrayNode arrayNode = objectMapper.getNodeFactory().arrayNode().addAll(bodies);
        return success(arrayNode);
    }

    public static QueryResponse success(JsonNode body) {
        return new QueryResponse(body, HTTP_OK);
    }

    public static QueryResponse error(int status, String message, ObjectMapper objectMapper) {
        return error(status, message, null, objectMapper);
    }

    public static QueryResponse error(int status, String message, String topic, ObjectMapper objectMapper) {
        JsonNodeFactory nodeFactory = objectMapper.getNodeFactory();

        ObjectNode error = nodeFactory.objectNode();
        error.set("error", nodeFactory.numberNode(status));
        error.set("message", nodeFactory.textNode(message));

        if (topic != null) {
            error.set("topic", nodeFactory.textNode(topic));
        }

        return new QueryResponse(error, status);
    }

    private QueryResponse(JsonNode body, int status) {
        this.body = body;
        this.status = status;
    }
}
