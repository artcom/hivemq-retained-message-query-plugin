package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResultSuccess implements QueryResult {
    private final String topic;
    private final Optional<String> payload;
    private final Optional<List<QueryResultSuccess>> children;

    public QueryResultSuccess(String topic, Optional<String> payload, Optional<List<QueryResultSuccess>> children) {
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
    public ImmutableList<QueryResult> flatten() {
        ImmutableList.Builder<QueryResult> builder = ImmutableList.<QueryResult>builder();

        builder.add(new QueryResultSuccess(topic, payload, Optional.absent()));

        for (QueryResult child : children.or(Collections.emptyList())) {
            builder.addAll(child.flatten());
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
