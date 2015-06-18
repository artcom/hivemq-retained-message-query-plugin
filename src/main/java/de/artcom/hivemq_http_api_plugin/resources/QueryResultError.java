package de.artcom.hivemq_http_api_plugin.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.ws.rs.core.Response;

public class QueryResultError implements QueryResult {
    private final String topic;
    private final Response.Status error;

    public QueryResultError(String topic, Response.Status error) {
        this.topic = topic;
        this.error = error;
    }

    @JsonIgnore
    public Response.Status getStatus() {
        return error;
    }

    public String getTopic() {
        return topic;
    }

    public Response.Status getError() {
        return error;
    }
}
