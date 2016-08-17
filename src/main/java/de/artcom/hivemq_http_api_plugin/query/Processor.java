package de.artcom.hivemq_http_api_plugin.query;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.RetainedTopicTree;
import de.artcom.hivemq_http_api_plugin.query.results.Result;
import de.artcom.hivemq_http_api_plugin.query.results.ResultList;
import de.artcom.hivemq_http_api_plugin.query.results.Topic;
import de.artcom.hivemq_http_api_plugin.query.results.TopicNotFoundError;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Processor {
    private static final Splitter WILDCARD_TOPIC_SPLITTER = Splitter
            .on('+')
            .trimResults(CharMatcher.is('/'));

    private final RetainedTopicTree retainedTopicTree;

    @Inject
    public Processor(RetainedTopicTree retainedTopicTree) {
        this.retainedTopicTree = retainedTopicTree;
    }

    Result processQuery(Query query) {
        if (query.isWildcardQuery()) {
            return processWildcardQuery(query);
        } else {
            return processSingleQuery(query);
        }
    }

    private Result processSingleQuery(Query query) {
        RetainedTopicTree.Node node = retainedTopicTree.getTopic(query.topic);

        if (node == null) {
            return new TopicNotFoundError(query.topic);
        }

        return createResult(node, query.topic, query.depth);
    }

    private Result processWildcardQuery(Query query) {
        List<String> parts = Lists.newArrayList(WILDCARD_TOPIC_SPLITTER.split(query.topic));

        String prefix = parts.get(0);
        String suffix = parts.get(1);

        List<Result> results = new ArrayList<>();
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

        return new ResultList(results);
    }

    private static String joinPath(String prefix, String childName) {
        return prefix.isEmpty() ? childName : prefix + "/" + childName;
    }

    private static String joinPath(String prefix, String childName, String suffix) {
        String topic = joinPath(prefix, childName);
        return suffix.isEmpty() ? topic : topic + "/" + suffix;
    }

    private static Topic createResult(RetainedTopicTree.Node node, String topic, int depth) {
        List<Topic> children = null;

        if (depth != 0 && node.hasChildren()) {
            children = new ArrayList<>();

            for (Map.Entry<String, RetainedTopicTree.Node> entry : node.getChildren().entrySet()) {
                String name = entry.getKey();
                RetainedTopicTree.Node child = entry.getValue();
                children.add(createResult(child, joinPath(topic, name), depth - 1));
            }
        }

        return new Topic(
                topic,
                node.payload,
                children
        );
    }
}