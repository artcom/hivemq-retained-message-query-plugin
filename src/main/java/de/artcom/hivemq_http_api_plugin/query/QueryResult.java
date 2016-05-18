package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

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

    @JsonIgnore
    public int getStatus() {
        return HTTP_OK;
    }

    public JsonNode toPlainJson(ObjectMapper mapper) throws IOException {
        if (children != null) {
            ObjectNode result = mapper.getNodeFactory().objectNode();
            children.forEach((child) -> addChildToNode(child, result, mapper));
            return result;
        } else {
            return mapper.readTree(payload);
        }
    }

    private static void addChildToNode(QueryResult child, ObjectNode node, ObjectMapper mapper) {
        try {
            String[] topicNames = child.topic.split("/");
            String topicName = topicNames[topicNames.length - 1];
            node.set(topicName, child.toPlainJson(mapper));
        } catch (IOException ignored) {
        }
    }

    public ImmutableList<QueryResult> flatten() {
        ImmutableList.Builder<QueryResult> builder = ImmutableList.<QueryResult>builder();
        builder.add(new QueryResult(topic, payload, null));

        if (children != null) {
            children.forEach((child) -> builder.addAll(child.flatten()));
        }

        return builder.build();
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public List<QueryResult> getChildren() {
        return children;
    }
}
