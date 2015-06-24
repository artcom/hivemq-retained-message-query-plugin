package de.artcom.hivemq_http_api_plugin;

import com.dcsquare.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.dcsquare.hivemq.spi.callback.exception.OnPublishReceivedException;
import com.dcsquare.hivemq.spi.message.PUBLISH;
import com.dcsquare.hivemq.spi.message.RetainedMessage;
import com.dcsquare.hivemq.spi.security.ClientData;
import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Singleton
public class RetainedTopicTree implements OnPublishReceivedCallback {
    private final Node root = new Node();
    private final ExecutorService executorService;

    @Inject
    public RetainedTopicTree(ExecutorService executorService, RetainedMessageStore retainedMessageStore) {
        this.executorService = executorService;

        Set<RetainedMessage> messages = retainedMessageStore.getRetainedMessages();
        for (RetainedMessage message : messages) {
            addTopic(message.getTopic(), message.getMessage());
        }
    }

    public Node getTopic(String topic) {
        return root.getTopic(topic);
    }

    private void addTopic(String topic, byte[] payload) {
        Node node = root.createTopic(topic);
        node.setPayload(payload);
    }

    private void removeTopic(String topic) {
        root.removeTopic(topic);
    }

    @Override
    public void onPublishReceived(final PUBLISH publish, ClientData clientData) throws OnPublishReceivedException {
        if (publish.isRetain()) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                        String topic = publish.getTopic();
                        byte[] payload = publish.getPayload();

                        if (payload.length == 0) {
                            removeTopic(topic);
                        } else {
                            addTopic(topic, payload);
                        }
                    }
                }
            );
        }
    }

    @Override
    public int priority() {
        return 0;
    }

    public static class Node {
        private Optional<byte[]> payload = Optional.absent();
        private ConcurrentHashMap<String, Node> children = new ConcurrentHashMap<String, Node>();

        public Optional<byte[]> getPayload() {
            return payload;
        }

        public void setPayload(byte[] payload) {
            this.payload = Optional.of(payload);
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public ImmutableSortedMap<String, Node> getChildren() {
            return ImmutableSortedMap.copyOf(children);
        }

        public Node getTopic(String topic) {
            return getPath(toPath(topic));
        }

        private Node getPath(ImmutableList<String> path) {
            if (path.isEmpty() || path.get(0).isEmpty()) {
                return this;
            }

            String name = path.get(0);
            Node child = children.get(name);

            if (child == null) {
                return null;
            }

            return child.getPath(path.subList(1, path.size()));
        }

        private Node createTopic(String topic) {
            return createPath(toPath(topic));
        }

        private Node createPath(ImmutableList<String> path) {
            if (path.isEmpty()) {
                return this;
            }

            String name = path.get(0);
            children.putIfAbsent(name, new Node());
            Node child = children.get(name);

            return child.createPath(path.subList(1, path.size()));
        }

        private void removeTopic(String topic) {
            removePath(toPath(topic));
        }

        private boolean removePath(ImmutableList<String> path) {
            if (path.isEmpty()) {
                return children.isEmpty();
            }

            String name = path.get(0);
            Node child = children.get(name);

            if (child == null) {
                return false;
            }

            boolean remove = child.removePath(path.subList(1, path.size()));

            if (remove) {
                children.remove(name);
            }

            return remove && children.isEmpty() && !payload.isPresent();
        }

        private ImmutableList<String> toPath(String topic) {
            return ImmutableList.copyOf(topic.split("/"));
        }
    }
}
