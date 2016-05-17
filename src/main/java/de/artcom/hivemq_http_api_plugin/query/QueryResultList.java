package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

class QueryResultList implements IQueryResult {
    private final Iterable<IQueryResult> results;

    QueryResultList(Iterable<IQueryResult> results) {
        this.results = results;
    }

    @Override
    public int getStatus() {
        return HTTP_OK;
    }

    @Override
    public JsonNode toJson(ObjectMapper mapper) {
        ArrayNode resultArray = mapper.getNodeFactory().arrayNode();

        for (IQueryResult result : results) {
            resultArray.add(result.toJson(mapper));
        }

        return resultArray;
    }

    @Override
    public JsonNode toPlainJson(ObjectMapper mapper) throws IOException {
        ArrayNode resultArray = mapper.getNodeFactory().arrayNode();

        for (IQueryResult result : results) {
            resultArray.add(result.toPlainJson(mapper));
        }

        return resultArray;
    }

    @Override
    public ImmutableList<IQueryResult> flatten() {
        ImmutableList.Builder<IQueryResult> builder = ImmutableList.<IQueryResult>builder();

        for (IQueryResult result : results) {
            builder.addAll(result.flatten());
        }

        return builder.build();
    }
}
