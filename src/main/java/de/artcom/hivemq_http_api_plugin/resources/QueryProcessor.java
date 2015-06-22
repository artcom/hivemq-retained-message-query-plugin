package de.artcom.hivemq_http_api_plugin.resources;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import de.artcom.hivemq_http_api_plugin.RetainedTopicTree;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class QueryProcessor {
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
