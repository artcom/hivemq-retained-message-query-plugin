package de.artcom.hivemq_http_api_plugin.query.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.artcom.hivemq_http_api_plugin.query.QueryResponse;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class ParameterException extends QueryException {
    @Override
    public QueryResponse toQueryResponse(ObjectMapper objectMapper) {
        return QueryResponse.error(
                HTTP_BAD_REQUEST,
                "The request body must be a JSON object with a 'topic' and optional 'depth' property, " +
                        "or a JSON array of such objects.",
                objectMapper);

    }
}
