package de.artcom.hivemq_http_api_plugin.query.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.artcom.hivemq_http_api_plugin.query.QueryResponse;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class MultipleWildcardsException extends QueryException {
    public MultipleWildcardsException(String topic) {
        super(topic);
    }

    @Override
    public QueryResponse toQueryResponse(ObjectMapper objectMapper) {
        return QueryResponse.error(
                HTTP_BAD_REQUEST,
                "The topic cannot contain more than one wildcard.",
                topic,
                objectMapper);
    }
}
