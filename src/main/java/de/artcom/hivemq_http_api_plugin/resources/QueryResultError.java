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

    public QueryResultError(Response.Status error) {
        this.topic = Optional.absent();
        this.error = error;
    }

    public QueryResultError(String topic, Response.Status error) {
        this.topic = Optional.of(topic);
        this.error = error;
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
}
