package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

public class QueryResultList implements QueryResult {
    public static final Joiner COMMA_JOINER = Joiner.on(',');
    private final List<QueryResult> results;

    public QueryResultList(List<QueryResult> results) {
        this.results = results;
    }

    @Override
    public int getStatus() {
        return HTTP_OK;
    }

    @Override
    public String toJSON(ObjectMapper objectMapper) throws JsonProcessingException {
        List<String> jsonResults = new ArrayList<>();

        for (QueryResult result : results) {
            jsonResults.add(result.toJSON(objectMapper));
        }

        return "[" + COMMA_JOINER.join(jsonResults) + "]";
    }
}
