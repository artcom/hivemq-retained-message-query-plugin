package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

@JsonInclude(JsonInclude.Include.NON_NULL)
class QueryResultSuccess implements IQueryResult {
    private final String topic;
    private final String payload;
    private final List<QueryResultSuccess> children;

    QueryResultSuccess(String topic, String payload, List<QueryResultSuccess> children) {
        this.topic = topic;
        this.payload = payload;
        this.children = children;
    }

    @Override
    @JsonIgnore
    public int getStatus() {
        return HTTP_OK;
    }

    @Override
    public String toJSON(ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }

    @Override

    @Override
    public ImmutableList<IQueryResult> flatten() {
        ImmutableList.Builder<IQueryResult> builder = ImmutableList.<IQueryResult>builder();
        builder.add(new QueryResultSuccess(topic, payload, null));

        if (children != null) {
            for (IQueryResult child : children) {
                builder.addAll(child.flatten());
            }
        }

        return builder.build();
    }

    public String getTopic() {
        return topic;
    }

    public Optional<String> getPayload() {
        return payload;
    }

    public Optional<List<QueryResultSuccess>> getChildren() {
        return children;
    }
}
