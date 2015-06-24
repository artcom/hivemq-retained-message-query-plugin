package de.artcom.hivemq_http_api_plugin.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

import javax.ws.rs.core.Response;

import java.util.List;

import static javax.ws.rs.core.Response.Status.OK;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResultSuccess implements QueryResult {
    private final String topic;
    private final Optional<String> payload;
    private final Optional<List<QueryResultSuccess>> children;

    public QueryResultSuccess(String topic, Optional<String> payload, Optional<List<QueryResultSuccess>> children) {
        this.topic = topic;
        this.payload = payload;
        this.children = children;
    }

    @Override @JsonIgnore
    public Response.Status getStatus() {
        return OK;
    }

    @Override
    public String toJSON(ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }

    public String getTopic() {
        return topic;
    }

    public Optional<String> getPayload() {
        return payload;
    }

    public Optional<List<QueryResultSuccess>> getChildren() {
        return children;
    };
}
