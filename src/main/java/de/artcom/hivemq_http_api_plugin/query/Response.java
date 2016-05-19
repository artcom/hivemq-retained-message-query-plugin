package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import static java.net.HttpURLConnection.HTTP_OK;

public class Response {
    public final JsonNode body;
    public final int status;

    public static Response success(Iterable<JsonNode> bodies, ObjectMapper objectMapper) {
        ArrayNode arrayNode = objectMapper.getNodeFactory().arrayNode().addAll(Lists.newArrayList(bodies));
        return success(arrayNode);
    }

    public static Response success(JsonNode body) {
        return new Response(body, HTTP_OK);
    }

    public static Response error(int status, String message, ObjectMapper objectMapper) {
        return error(status, message, null, objectMapper);
    }

    public static Response error(int status, String message, String topic, ObjectMapper objectMapper) {
        JsonNodeFactory nodeFactory = objectMapper.getNodeFactory();

        ObjectNode error = nodeFactory.objectNode();
        error.set("error", nodeFactory.numberNode(status));
        error.set("message", nodeFactory.textNode(message));

        if (topic != null) {
            error.set("topic", nodeFactory.textNode(topic));
        }

        return new Response(error, status);
    }

    private Response(JsonNode body, int status) {
        this.body = body;
        this.status = status;
    }
}
