package de.artcom.hivemq_http_api_plugin.query;

import de.artcom.hivemq_http_api_plugin.RetainedTopicTree;
import de.artcom.hivemq_http_api_plugin.query.results.Result;
import de.artcom.hivemq_http_api_plugin.query.results.ResultList;
import de.artcom.hivemq_http_api_plugin.query.results.Topic;
import de.artcom.hivemq_http_api_plugin.query.results.TopicNotFoundError;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

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
        RetainedTopicTree.Node node = retainedTopicTree.getNode(query.topic);

        if (node == null) {
            return new TopicNotFoundError(query.topic);
        }

        return createResult(node, query.depth);
    }

    private Result processWildcardQuery(Query query) {
        return retainedTopicTree.getWildcardNodes(query.topic)
                .map(node -> createResult(node, query.depth))
                .collect(Collectors.toCollection(ResultList::new));
    }

    private static Topic createResult(RetainedTopicTree.Node node, int depth) {
        List<Topic> children = null;

        if (depth != 0 && node.hasChildren()) {
            children = node.getChildren()
                    .map(child -> createResult(child, depth - 1))
                    .collect(Collectors.toList());
        }

        return new Topic(
                node.topic,
                node.payload,
                children
        );
    }
}
