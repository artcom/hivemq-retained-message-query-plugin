package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
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
    public JsonNode toJson(ObjectMapper mapper) {
        ObjectNode result = mapper.getNodeFactory().objectNode().put("topic", topic);

        if (payload != null) {
            result.put("payload", payload);
        }

        if (children != null) {
            ArrayNode arrayNode = result.putArray("children");
            children.forEach((child) -> arrayNode.add(child.toJson(mapper)));
        }

        return result;
    }

    @Override
    public JsonNode toPlainJson(ObjectMapper mapper) throws IOException {
        if (children != null) {
            ObjectNode result = mapper.getNodeFactory().objectNode();
            children.forEach((child) -> addChildToNode(child, result, mapper));
            return result;
        } else {
            return mapper.readTree(payload);
        }
    }

    private static void addChildToNode(QueryResultSuccess child, ObjectNode node, ObjectMapper mapper) {
        try {
            String[] topicNames = child.topic.split("/");
            String topicName = topicNames[topicNames.length - 1];
            node.set(topicName, child.toPlainJson(mapper));
        } catch (IOException ignored) {
        }
    }

    @Override
    public ImmutableList<IQueryResult> flatten() {
        ImmutableList.Builder<IQueryResult> builder = ImmutableList.<IQueryResult>builder();
        builder.add(new QueryResultSuccess(topic, payload, null));

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

    public List<QueryResultSuccess> getChildren() {
        return children;
    }
}
