package de.artcom.hivemq_http_api_plugin.resources;

import com.google.common.base.*;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.RetainedTopicTree;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class QueryProcessor {

    private static final Splitter WILDCARD_TOPIC_SPLITTER = Splitter
            .on('+')
            .trimResults(CharMatcher.is('/'));

    private static final Function<byte[], String> PAYLOAD_TO_STRING = new Function<byte[], String>() {
        @Override
        public String apply(byte[] bytes) {
            return new String(bytes, Charset.forName("UTF-8"));
        }
    };

    private final RetainedTopicTree retainedTopicTree;

    @Inject
    public QueryProcessor(RetainedTopicTree retainedTopicTree) {
        this.retainedTopicTree = retainedTopicTree;
    }

    public QueryResult process(Query query) {
        if (query.topic.endsWith("/")) {
            return new QueryResultError(query.topic, BAD_REQUEST, ErrorMessage.TRAILING_SLASH);
        } else if (query.depth < 0) {
            return new QueryResultError(query.topic, BAD_REQUEST, ErrorMessage.NEGATIVE_DEPTH);
        }

        if (query.topic.contains("+")) {
            return processWildcardQuery(query);
        } else {
            return processSingleQuery(query);
        }
    }

    private QueryResult processWildcardQuery(Query query) {
        List<String> parts = Lists.newArrayList(WILDCARD_TOPIC_SPLITTER.split(query.topic));

        if (parts.size() > 2) {
            return new QueryResultError(query.topic, BAD_REQUEST, ErrorMessage.MULTIPLE_WILDCARDS);
        }

        String prefix = parts.get(0);
        String suffix = parts.get(1);

        List<QueryResult> results = new ArrayList<QueryResult>();
        RetainedTopicTree.Node node = retainedTopicTree.getTopic(prefix);

        if (node != null) {
            for (Map.Entry<String, RetainedTopicTree.Node> entry : node.getChildren().entrySet()) {
                String childName = entry.getKey();
                RetainedTopicTree.Node childNode = entry.getValue();

                RetainedTopicTree.Node match = childNode.getTopic(suffix);

                if (match != null) {
                    String topic = joinPath(prefix, childName, suffix);
                    results.add(createResult(match, topic, query.depth));
                }
            }
        }

        return new QueryResultList(results);
    }

    private String joinPath(String prefix, String childName, String suffix) {
        String topic = "";

        if (!prefix.isEmpty()) {
            topic += prefix + "/";
        }

        topic += childName;

        if (!suffix.isEmpty()) {
            topic += "/" + suffix;
        }

        return topic;
    }

    private QueryResult processSingleQuery(Query query) {
        RetainedTopicTree.Node node = retainedTopicTree.getTopic(query.topic);

        if (node == null) {
            return new QueryResultError(query.topic, NOT_FOUND);
        }

        return createResult(node, query.topic, query.depth);
    }

    private QueryResultSuccess createResult(RetainedTopicTree.Node node, String topic, int depth) {
        List<QueryResultSuccess> children = null;

        if (depth > 0 && node.hasChildren()) {
            children = new ArrayList<QueryResultSuccess>();

            for (Map.Entry<String, RetainedTopicTree.Node> entry : node.getChildren().entrySet()) {
                String name = entry.getKey();
                RetainedTopicTree.Node child = entry.getValue();
                children.add(createResult(child, topic + "/" + name, depth - 1));
            }
        }

        return new QueryResultSuccess(
                topic,
                node.getPayload().transform(PAYLOAD_TO_STRING),
                Optional.fromNullable(children)
        );
    }
}
