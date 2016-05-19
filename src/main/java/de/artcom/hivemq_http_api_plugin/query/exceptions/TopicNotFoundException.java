package de.artcom.hivemq_http_api_plugin.query.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.artcom.hivemq_http_api_plugin.query.QueryResponse;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

public class TopicNotFoundException extends QueryException {
    public TopicNotFoundException(String topic) {
        super(topic);
    }

    @Override
    public QueryResponse toQueryResponse(ObjectMapper objectMapper) {
        return QueryResponse.error(HTTP_NOT_FOUND, "The topic does not exist.", topic, objectMapper);
    }
}
