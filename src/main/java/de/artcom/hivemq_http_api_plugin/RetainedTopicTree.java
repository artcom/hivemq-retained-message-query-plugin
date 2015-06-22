package de.artcom.hivemq_http_api_plugin;

import com.dcsquare.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.dcsquare.hivemq.spi.callback.exception.OnPublishReceivedException;
import com.dcsquare.hivemq.spi.message.PUBLISH;
import com.dcsquare.hivemq.spi.message.RetainedMessage;
import com.dcsquare.hivemq.spi.security.ClientData;
import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    public Node getTopic(String topic) {
        return root.getOrCreateTopic(topic, false);
    }

    private void addTopic(String topic, byte[] payload) {
        Node node = root.getOrCreateTopic(topic, true);
        node.setPayload(payload);
    }

    private void removeTopic(String topic) {
        root.removeTopic(topic);
    }

    @Override
    public void onPublishReceived(PUBLISH publish, ClientData clientData) throws OnPublishReceivedException {
        if (publish.isRetain()) {
            String topic = publish.getTopic();
            byte[] payload = publish.getPayload();

            if (payload.length == 0) {
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
        private Map<String, Node> children = new TreeMap<String, Node>();

        private Node getOrCreateTopic(String topic, boolean create) {
            ImmutableList<String> path = ImmutableList.copyOf(topic.split("/"));
            return getOrCreatePath(path, create);
        }

        private Node getOrCreatePath(ImmutableList<String> path, boolean create) {
            if (path.size() == 0) {
                return this;
            } else {
                String name = path.get(0);

                if (create && !children.containsKey(name)) {
                    children.put(name, new Node());
                }

                Node child = children.get(name);

                if (child == null) {
                    return null;
                }

                return child.getOrCreatePath(path.subList(1, path.size()), create);
            }
        }

        private void removeTopic(String topic) {
            ImmutableList<String> path = ImmutableList.copyOf(topic.split("/"));
            removeTopic(path);
        }

        private boolean removeTopic(ImmutableList<String> path) {
            if (path.isEmpty()) {
                return children.isEmpty();
            }

            String name = path.get(0);
            Node child = children.get(name);

            if (child == null) {
                return false;
            }

            boolean remove = child.removeTopic(path.subList(1, path.size()));

            if (remove) {
                children.remove(name);
            }

            return remove && children.isEmpty() && !payload.isPresent();
        }

        public Optional<byte[]> getPayload() {
            return payload;
        }

        public void setPayload(byte[] payload) {
            this.payload = Optional.of(payload);
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public ImmutableMap<String, Node> getChildren() {
            return ImmutableMap.copyOf(children);
        }
    }
}
