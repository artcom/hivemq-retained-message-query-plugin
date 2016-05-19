package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
class Result {
    private final String topic;
    @Nullable
    private final String payload;
    @Nullable
    private final List<Result> children;

    Result(String topic, @Nullable String payload, @Nullable List<Result> children) {
        this.topic = topic;
        this.payload = payload;
        this.children = children;
    }

    public ImmutableList<Result> flatten() {
        ImmutableList.Builder<Result> builder = ImmutableList.builder();
        builder.add(new Result(topic, payload, null));

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
    public List<Result> getChildren() {
        return children;
    }
}
