package de.artcom.hivemq_http_api_plugin.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;

public class QueryResultSuccess implements QueryResult {
    private final String topic;
    private final String payload;

    public QueryResultSuccess(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
    }

    @JsonIgnore
    public Response.Status getStatus() {
        return OK;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }
}
