package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

public interface QueryResult {
    int getStatus();
    String toJSON(ObjectMapper objectMapper) throws JsonProcessingException;
    ImmutableList<QueryResult> flatten();
}
