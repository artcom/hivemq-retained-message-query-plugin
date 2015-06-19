package de.artcom.hivemq_http_api_plugin;

import com.dcsquare.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.dcsquare.hivemq.spi.callback.exception.OnPublishReceivedException;
import com.dcsquare.hivemq.spi.message.PUBLISH;
import com.dcsquare.hivemq.spi.message.RetainedMessage;
import com.dcsquare.hivemq.spi.security.ClientData;
import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class RetainedTopicTree implements OnPublishReceivedCallback {
    private final Node root = new Node();

    @Inject
    public RetainedTopicTree(RetainedMessageStore retainedMessageStore) {
        Set<RetainedMessage> messages = retainedMessageStore.getRetainedMessages();
        for (RetainedMessage message : messages) {
            addTopic(message.getTopic(), message.getMessage());
        }
    }

    public Node createTopic(String topic) {
        return root.getOrCreateTopic(topic, true);
    }

    public Node getTopic(String topic) {
        return root.getOrCreateTopic(topic, false);
    }

    private void addTopic(String topic, byte[] payload) {
        Node node = createTopic(topic);
        node.setPayload(payload);
    }

    private void removeTopic(String topic) {

    }

    @Override
    public void onPublishReceived(PUBLISH publish, ClientData clientData) throws OnPublishReceivedException {
        if (publish.isRetain()) {
            String topic = publish.getTopic();
            byte[] payload = publish.getPayload();

            if (payload == null) {
                removeTopic(topic);
            } else {
                addTopic(topic, payload);
            }
        }
    }

    @Override
    public int priority() {
        return 0;
    }

    public static class Node {
        private Optional<byte[]> payload = Optional.absent();
        private Map<String, Node> children = new HashMap<String, Node>();

        private Node getOrCreateTopic(String topic, boolean create) {
            ImmutableList<String> path = ImmutableList.copyOf(topic.split("/"));
            return getOrCreatePath(path, create);
        }

        private Node getOrCreatePath(ImmutableList<String> path, boolean create) {
            if (path.size() == 0) {
                return this;
            } else {
                String name = path.get(0);

                if (create) {
                    createChild(name);
                }

                Node child = getChild(name);

                if (child == null) {
                    return null;
                }

                return child.getOrCreatePath(path.subList(1, path.size()), create);
            }
        }

        private void createChild(String name) {
            if (!children.containsKey(name)) {
                children.put(name, new Node());
            }
        }

        private Node getChild(String name) {
            return children.get(name);
        }

        public Optional<byte[]> getPayload() {
            return payload;
        }

        public void setPayload(byte[] payload) {
            this.payload = Optional.of(payload);
        }
    }
}
