package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResultError implements QueryResult {
    private final Optional<String> topic;
    private final int error;
    private final Optional<String> message;

    public static QueryResultError queryFormat() {
        return new QueryResultError(HTTP_BAD_REQUEST, "The response body must be a JSON object with a 'topic' and optional 'depth' property, or a JSON array of such objects.");
    }

    public static QueryResultError trailingSlash(String topic) {
        return new QueryResultError(topic, HTTP_BAD_REQUEST, "The topic cannot end with a slash.");
    }

    public static QueryResultError negativeDepth(String topic) {
        return new QueryResultError(topic, HTTP_BAD_REQUEST, "The depth parameter cannot be negative.");
    }

    public static QueryResultError multipleWirdcards(String topic) {
        return new QueryResultError(topic, HTTP_BAD_REQUEST, "The topic cannot contain more than one wildcard.");
    }

    public static QueryResultError notFound(String topic) {
        return new QueryResultError(topic, HTTP_NOT_FOUND);
    }

    private QueryResultError(int error, String message) {
        this(null, error, message);
    }

    private QueryResultError(String topic, int error) {
        this(topic, error, null);
    }

    private QueryResultError(String topic, int error, String message) {
        this.topic = Optional.fromNullable(topic);
        this.error = error;
        this.message = Optional.fromNullable(message);
    }

    @Override @JsonIgnore
    public int getStatus() {
        return error;
    }

    @Override
    public String toJSON(ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }

    public Optional<String> getTopic() {
        return topic;
    }

    public int getError() {
        return error;
    }

    public Optional<String> getMessage() {
        return message;
    }
}
