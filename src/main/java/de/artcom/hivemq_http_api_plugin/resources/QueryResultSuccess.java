package de.artcom.hivemq_http_api_plugin.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;

public class QueryResultSuccess implements QueryResult {
    private final String topic;
    private final Optional<String> payload;

    public QueryResultSuccess(String topic, Optional<String> payload) {
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

    public Optional<String> getPayload() {
        return payload;
    }
}
