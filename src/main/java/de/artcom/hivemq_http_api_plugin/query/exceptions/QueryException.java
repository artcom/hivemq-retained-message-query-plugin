package de.artcom.hivemq_http_api_plugin.query.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.artcom.hivemq_http_api_plugin.query.QueryResponse;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

public class QueryException extends Exception {
    public final String topic;

    QueryException() {
        topic = null;
    }

    QueryException(String topic) {
        this.topic = topic;
    }

    public QueryResponse toQueryResponse(ObjectMapper objectMapper) {
        return QueryResponse.error(HTTP_INTERNAL_ERROR, "Internal error", objectMapper);
    }
}
