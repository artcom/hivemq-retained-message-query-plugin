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

        return createResult(node, query.depth);
    }

    private Result processWildcardQuery(Query query) {
        List<String> parts = Lists.newArrayList(WILDCARD_TOPIC_SPLITTER.split(query.topic));

        String prefix = parts.get(0);
        String suffix = parts.get(1);

        List<Result> results = new ArrayList<>();
        RetainedTopicTree.Node node = retainedTopicTree.getTopic(prefix);

        if (node != null) {
            for (RetainedTopicTree.Node child : node.getChildren().values()) {
                RetainedTopicTree.Node match = retainedTopicTree.getTopic(suffix, child);

                if (match != null) {
                    results.add(createResult(match, query.depth));
                }
            }
        }

        return new ResultList(results);
    }

    private static Topic createResult(RetainedTopicTree.Node node, int depth) {
        List<Topic> children = null;

        if (depth != 0 && node.hasChildren()) {
            children = new ArrayList<>();

            for (RetainedTopicTree.Node child : node.getChildren().values()) {
                children.add(createResult(child, depth - 1));
            }
        }

        return new Topic(
                node.topic,
                node.payload,
                children
        );
    }
}
