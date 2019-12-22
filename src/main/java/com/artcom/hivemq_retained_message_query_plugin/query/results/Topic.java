package com.artcom.hivemq_retained_message_query_plugin.query.results;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_OK;

public class Topic implements Result {
    @Nullable
    private final String topic;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String payload;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<Topic> children;

    public Topic(@Nullable String topic, @Nullable String payload, @Nullable List<Topic> children) {
        this.topic = topic;
        this.payload = payload;
        this.children = children;
    }

    @Override
    public int getStatus() {
        return HTTP_OK;
    }

    public Stream<Result> flatten() {
        Stream<Topic> childrenStream = children == null ? Stream.empty() : children.stream();

        return Stream.concat(
                Stream.of(new Topic(topic, payload, null)),
                childrenStream.flatMap(Topic::flatten));
    }
}
