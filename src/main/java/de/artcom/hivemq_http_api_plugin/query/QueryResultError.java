package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResultError implements QueryResult {
    private final Optional<String> topic;
    private final int error;
    private final Optional<String> message;

    public QueryResultError(int error, String message) {
        this(null, error, message);
    }

    public QueryResultError(String topic, int error) {
        this(topic, error, null);
    }

    public QueryResultError(String topic, int error, String message) {
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
