package de.artcom.hivemq_http_api_plugin.query.results;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_OK;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Topic implements Result {
    private final String topic;
    @Nullable
    private final String payload;
    @Nullable
    private final List<Topic> children;

    public Topic(String topic, @Nullable String payload, @Nullable List<Topic> children) {
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

    public String getTopic() {
        return topic;
    }

    @Nullable
    public String getPayload() {
        return payload;
    }

    @Nullable
    public List<Topic> getChildren() {
        return children;
    }
}
