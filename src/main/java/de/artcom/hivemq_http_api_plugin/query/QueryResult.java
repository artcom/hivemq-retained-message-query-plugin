package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response;

public interface QueryResult {
    Response.Status getStatus();
    String toJSON(ObjectMapper objectMapper) throws JsonProcessingException;
}
