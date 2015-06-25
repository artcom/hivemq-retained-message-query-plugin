package de.artcom.hivemq_http_api_plugin.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.Response.Status.OK;

public class QueryResultList implements QueryResult {

    private final List<QueryResult> results;

    public QueryResultList(List<QueryResult> results) {
        this.results = results;
    }

    @Override
    public Response.Status getStatus() {
        return OK;
    }

    @Override
    public String toJSON(ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(results);
    }
}
