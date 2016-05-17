package de.artcom.hivemq_http_api_plugin.query;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.RetainedTopicTree;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class QueryProcessor {
    private static final Splitter WILDCARD_TOPIC_SPLITTER = Splitter
            .on('+')
            .trimResults(CharMatcher.is('/'));

    private final RetainedTopicTree retainedTopicTree;

    @Inject
    public QueryProcessor(RetainedTopicTree retainedTopicTree) {
        this.retainedTopicTree = retainedTopicTree;
    }

    public IQueryResult process(Query query) {
        if (query.topic.startsWith("/")) {
            return QueryResultError.leadingSlash(query.topic);
        } else if (query.topic.endsWith("/")) {
            return QueryResultError.trailingSlash(query.topic);
        }

        IQueryResult result = query.topic.contains("+")
                ? processWildcardQuery(query)
                : processSingleQuery(query);

        if (query.flatten) {
            return new QueryResultList(result.flatten());
        } else {
            return result;
        }
    }

    private IQueryResult processWildcardQuery(Query query) {
        List<String> parts = Lists.newArrayList(WILDCARD_TOPIC_SPLITTER.split(query.topic));

        if (parts.size() > 2) {
            return QueryResultError.multipleWirdcards(query.topic);
        }

        String prefix = parts.get(0);
        String suffix = parts.get(1);

        List<IQueryResult> results = new ArrayList<>();
        RetainedTopicTree.Node node = retainedTopicTree.getTopic(prefix);

        if (node != null) {
            for (Map.Entry<String, RetainedTopicTree.Node> entry : node.getChildren().entrySet()) {
                String childName = entry.getKey();
                RetainedTopicTree.Node childNode = entry.getValue();

                RetainedTopicTree.Node match = retainedTopicTree.getTopic(suffix, childNode);

                if (match != null) {
                    String topic = joinPath(prefix, childName, suffix);
                    results.add(createResult(match, topic, query.depth));
                }
            }
        }

        return new QueryResultList(results);
    }

    private static String joinPath(String prefix, String childName) {
        return prefix.isEmpty() ? childName : prefix + "/" + childName;
    }

    private static String joinPath(String prefix, String childName, String suffix) {
        String topic = joinPath(prefix, childName);
        return suffix.isEmpty() ? topic : topic + "/" + suffix;
    }

    private IQueryResult processSingleQuery(Query query) {
        RetainedTopicTree.Node node = retainedTopicTree.getTopic(query.topic);

        if (node == null) {
            return QueryResultError.notFound(query.topic);
        }

        return createResult(node, query.topic, query.depth);
    }

    private static QueryResultSuccess createResult(RetainedTopicTree.Node node, String topic, int depth) {
        List<QueryResultSuccess> children = null;

        if (depth != 0 && node.hasChildren()) {
            children = new ArrayList<>();

            for (Map.Entry<String, RetainedTopicTree.Node> entry : node.getChildren().entrySet()) {
                String name = entry.getKey();
                RetainedTopicTree.Node child = entry.getValue();
                children.add(createResult(child, joinPath(topic, name), depth - 1));
            }
        }

        String payload = node.payload != null ? new String(node.payload, Charset.forName("UTF-8")) : null;
        return new QueryResultSuccess(
                topic,
                payload,
                children
        );
    }
}
