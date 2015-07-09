package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface QueryResult {
    int getStatus();
    String toJSON(ObjectMapper objectMapper) throws JsonProcessingException;
}
