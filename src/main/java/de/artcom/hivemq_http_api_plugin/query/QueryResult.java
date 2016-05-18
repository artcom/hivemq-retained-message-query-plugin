package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
class QueryResult {
    private final String topic;
    @Nullable
    private final String payload;
    @Nullable
    private final List<QueryResult> children;

    QueryResult(String topic, @Nullable String payload, @Nullable List<QueryResult> children) {
        this.topic = topic;
        this.payload = payload;
        this.children = children;
    }

    public ImmutableList<QueryResult> flatten() {
        ImmutableList.Builder<QueryResult> builder = ImmutableList.builder();
        builder.add(new QueryResult(topic, payload, null));

        if (children != null) {
            children.forEach((child) -> builder.addAll(child.flatten()));
        }

        return builder.build();
    }

    public String getTopic() {
        return topic;
    }

    @Nullable
    public String getPayload() {
        return payload;
    }

    @Nullable
    public List<QueryResult> getChildren() {
        return children;
    }
}
