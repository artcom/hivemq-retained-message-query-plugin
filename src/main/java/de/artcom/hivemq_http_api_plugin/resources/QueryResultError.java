package de.artcom.hivemq_http_api_plugin.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response;

public class QueryResultError implements QueryResult {
    private final String topic;
    private final Response.Status error;

    public QueryResultError(String topic, Response.Status error) {
        this.topic = topic;
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

    public String getTopic() {
        return topic;
    }

    public Response.Status getError() {
        return error;
    }
}
