package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.hivemq.spi.annotations.Nullable;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

@JsonInclude(JsonInclude.Include.NON_NULL)
class QueryResultError implements IQueryResult {
    private final String topic;
    private final int error;
    private final String message;

    static QueryResultError queryFormat() {
        return new QueryResultError(HTTP_BAD_REQUEST, null, "The request body must be a JSON object with a 'topic' and optional 'depth' property, or a JSON array of such objects.");
    }

    static QueryResultError leadingSlash(String topic) {
        return new QueryResultError(HTTP_BAD_REQUEST, topic, "The topic cannot start with a slash.");
    }

    static QueryResultError trailingSlash(String topic) {
        return new QueryResultError(HTTP_BAD_REQUEST, topic, "The topic cannot end with a slash.");
    }

    static QueryResultError multipleWirdcards(String topic) {
        return new QueryResultError(HTTP_BAD_REQUEST, topic, "The topic cannot contain more than one wildcard.");
    }

    static QueryResultError notFound(String topic) {
        return new QueryResultError(HTTP_NOT_FOUND, topic, null);
    }

    private QueryResultError(int error, @Nullable String topic, @Nullable String message) {
        this.topic = topic;
        this.error = error;
        this.message = message;
    }

    @Override
    @JsonIgnore
    public int getStatus() {
        return error;
    }

    @Override
    public JsonNode toJson(ObjectMapper mapper) {
        ObjectNode result = mapper.getNodeFactory().objectNode().put("error", error);

        if (topic != null) {
            result.put("topic", topic);
        }

        if (message != null) {
            result.put("message", message);
        }

        return result;
    }

    @Override
    public JsonNode toPlainJson(ObjectMapper mapper) throws JsonProcessingException {
        return mapper.getNodeFactory().objectNode();
    }

    @Override
    public ImmutableList<IQueryResult> flatten() {
        return ImmutableList.of(this);
    }
}
