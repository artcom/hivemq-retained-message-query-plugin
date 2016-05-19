package de.artcom.hivemq_http_api_plugin.query.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.artcom.hivemq_http_api_plugin.query.Response;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

public class QueryException extends Exception {
    public final String topic;

    QueryException() {
        topic = null;
    }

    QueryException(String topic) {
        this.topic = topic;
    }

    public Response toQueryResponse(ObjectMapper objectMapper) {
        return Response.error(HTTP_INTERNAL_ERROR, "Internal error", objectMapper);
    }
}
