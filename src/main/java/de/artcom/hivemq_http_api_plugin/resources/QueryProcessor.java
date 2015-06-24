package de.artcom.hivemq_http_api_plugin.resources;

import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
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
            .omitEmptyStrings()
            .trimResults(CharMatcher.is('/'));

    private static final Joiner PATH_JOINER = Joiner.on('/').skipNulls();

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
        if (query.topic.contains("+")) {
            return processWildcardQuery(query);
        } else {
            return processSingleQuery(query);
        }
    }

    private QueryResult processWildcardQuery(Query query) {
        List<String> parts = Lists.newArrayList(WILDCARD_TOPIC_SPLITTER.split(query.topic));

        if (parts.isEmpty() || parts.size() > 2) {
            return new QueryResultError(query.topic, BAD_REQUEST);
        }

        String prefix = parts.get(0);
        String suffix = null;
        ImmutableList<String> subPath = ImmutableList.of();

        if (parts.size() > 1) {
            suffix = parts.get(1);
            subPath = ImmutableList.copyOf(suffix.split("/"));
        }

        List<QueryResult> results = new ArrayList<QueryResult>();
        RetainedTopicTree.Node node = retainedTopicTree.getTopic(prefix);

        if (node != null) {
            for (Map.Entry<String, RetainedTopicTree.Node> entry : node.getChildren().entrySet()) {
                String childName = entry.getKey();
                RetainedTopicTree.Node childNode = entry.getValue();

                RetainedTopicTree.Node match = childNode.getSubNode(subPath);

                if (match != null) {
                    results.add(createResult(match, PATH_JOINER.join(prefix, childName, suffix), query.depth));
                }
            }
        }

        return new QueryResultList(results);
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
