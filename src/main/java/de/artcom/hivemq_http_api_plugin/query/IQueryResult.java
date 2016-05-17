package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import java.io.IOException;

interface IQueryResult {
    int getStatus();

    JsonNode toJson(ObjectMapper mapper);

    JsonNode toPlainJson(ObjectMapper mapper) throws IOException;

    ImmutableList<IQueryResult> flatten();
}
