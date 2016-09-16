package de.artcom.hivemq_retained_message_query_plugin.query;

import de.artcom.hivemq_retained_message_query_plugin.RetainedMessageTree;
import de.artcom.hivemq_retained_message_query_plugin.query.results.Result;
import de.artcom.hivemq_retained_message_query_plugin.query.results.ResultList;
import de.artcom.hivemq_retained_message_query_plugin.query.results.Topic;
import de.artcom.hivemq_retained_message_query_plugin.query.results.TopicNotFoundError;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

class Processor {
    private final RetainedMessageTree retainedMessageTree;

    @Inject
    public Processor(RetainedMessageTree retainedMessageTree) {
        this.retainedMessageTree = retainedMessageTree;
    }

    Result processQuery(Query query) {
        if (query.isWildcardQuery()) {
            return processWildcardQuery(query);
        } else {
            return processSingleQuery(query);
        }
    }

    private Result processSingleQuery(Query query) {
        return retainedMessageTree.getNodes(query.topic)
                .findFirst()
                .map(node -> (Result)createResult(node, query.depth))
                .orElse(new TopicNotFoundError(query.topic));
    }

    private ResultList processWildcardQuery(Query query) {
        return retainedMessageTree.getNodes(query.topic)
                .map(node -> createResult(node, query.depth))
                .collect(Collectors.toCollection(ResultList::new));
    }

    private static Topic createResult(RetainedMessageTree.Node node, int depth) {
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
