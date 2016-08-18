package de.artcom.hivemq_http_api_plugin.query;

import de.artcom.hivemq_http_api_plugin.RetainedTopicTree;
import de.artcom.hivemq_http_api_plugin.query.results.Result;
import de.artcom.hivemq_http_api_plugin.query.results.ResultList;
import de.artcom.hivemq_http_api_plugin.query.results.Topic;
import de.artcom.hivemq_http_api_plugin.query.results.TopicNotFoundError;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class Processor {
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
        List<RetainedTopicTree.Node> nodes = retainedTopicTree.getWildcardTopics(query.topic);
        List<Result> results = new ArrayList<>();

        for (RetainedTopicTree.Node node : nodes) {
            results.add(createResult(node, query.depth));
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
