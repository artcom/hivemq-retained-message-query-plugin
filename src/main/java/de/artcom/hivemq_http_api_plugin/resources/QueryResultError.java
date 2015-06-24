package de.artcom.hivemq_http_api_plugin.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

import javax.ws.rs.core.Response;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResultError implements QueryResult {
    private final Optional<String> topic;
    private final Response.Status error;
    private final Optional<String> message;

    public QueryResultError(Response.Status error, String message) {
        this(null, error, message);
    }

    public QueryResultError(String topic, Response.Status error) {
        this(topic, error, null);
    }

    public QueryResultError(String topic, Response.Status error, String message) {
        this.topic = Optional.fromNullable(topic);
        this.error = error;
        this.message = Optional.fromNullable(message);
    }

    @Override @JsonIgnore
    public Response.Status getStatus() {
        return error;
    }

    @Override
    public String toJSON(ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }

    public Optional<String> getTopic() {
        return topic;
    }

    public Response.Status getError() {
        return error;
    }

    public Optional<String> getMessage() {
        return message;
    }
}
