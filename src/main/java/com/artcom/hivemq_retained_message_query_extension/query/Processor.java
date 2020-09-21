package com.artcom.hivemq_retained_message_query_extension.query;

import com.artcom.hivemq_retained_message_query_extension.query.results.Result;
import com.artcom.hivemq_retained_message_query_extension.query.results.ResultList;
import com.artcom.hivemq_retained_message_query_extension.query.results.Topic;
import com.artcom.hivemq_retained_message_query_extension.query.results.TopicNotFoundError;
import com.artcom.hivemq_retained_message_query_extension.RetainedMessageTree;

import java.util.List;
import java.util.stream.Collectors;

class Processor {
    private final RetainedMessageTree retainedMessageTree;

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
