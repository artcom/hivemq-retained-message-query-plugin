package de.artcom.hivemq_http_api_plugin;

import com.google.common.collect.ImmutableList;
import com.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.hivemq.spi.callback.exception.OnPublishReceivedException;
import com.hivemq.spi.message.PUBLISH;
import com.hivemq.spi.message.RetainedMessage;
import com.hivemq.spi.security.ClientData;
import com.hivemq.spi.services.RetainedMessageStore;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

@Singleton
public class RetainedTopicTree implements OnPublishReceivedCallback {
    private final Node root = Node.rootNode();
    private final ExecutorService executorService;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    @Inject
    public RetainedTopicTree(ExecutorService executorService, RetainedMessageStore retainedMessageStore) {
        this.executorService = executorService;

        Set<RetainedMessage> messages = retainedMessageStore.getRetainedMessages();
        for (RetainedMessage message : messages) {
            addTopic(message.getTopic(), message.getMessage());
        }
    }

    public Node getTopic(String topic) {
        lock.readLock().lock();

        try {
            return root.getTopic(topic);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Stream<Node> getWildcardTopics(String topic) {
        lock.readLock().lock();

        try {
            return root.getWildcardTopics(topic);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void addTopic(String topic, byte[] payload) {
        lock.writeLock().lock();

        try {
            Node node = root.createTopic(topic);
            node.payload = new String(payload, Charset.forName("UTF-8"));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeTopic(String topic) {
        lock.writeLock().lock();

        try {
            root.removeTopic(topic);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void onPublishReceived(final PUBLISH publish, ClientData clientData) throws OnPublishReceivedException {
        if (publish.isRetain()) {
            executorService.submit(() -> {
                        String topic = publish.getTopic();
                        byte[] payload = publish.getPayload();

                        if (payload.length == 0) {
                            removeTopic(topic);
                        } else {
                            addTopic(topic, payload);
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
        @Nullable
        public final String topic;
        @Nullable
        public String payload;
        private final TreeMap<String, Node> children = new TreeMap<>();

        private static Node rootNode() {
            return new Node(null);
        }

        private static Node forPath(ImmutableList<String> path) {
            return new Node(fromPath(path));
        }

        private Node(@Nullable String topic) {
            this.topic = topic;
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public Stream<Node> getChildren() {
            return children.values().stream();
        }

        private Node getTopic(String topic) {
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

        private Stream<Node> getWildcardTopics(String topic) {
            return getWildcardPathes(toPath(topic));
        }

        private Stream<Node> getWildcardPathes(ImmutableList<String> path) {
            if (path.isEmpty()) {
                return Stream.of(this);
            }

            String name = path.get(0);

            if ("+".equals(name)) {
                return getChildren().flatMap(child -> child.getWildcardPathes(path.subList(1, path.size())));
            } else {
                Node child = children.get(name);

                if (child == null) {
                    return Stream.empty();
                }

                return child.getWildcardPathes(path.subList(1, path.size()));
            }
        }

        private Node createTopic(String topic) {
            return createPath(toPath(topic), 0);
        }

        private Node createPath(ImmutableList<String> path, int index) {
            if (index >= path.size()) {
                return this;
            }

            String name = path.get(index);

            if (!children.containsKey(name)) {
                children.put(name, Node.forPath(path.subList(0, index + 1)));
            }

            Node child = children.get(name);

            return child.createPath(path, index + 1);
        }

        private void removeTopic(String topic) {
            removePath(toPath(topic));
        }

        private boolean removePath(ImmutableList<String> path) {
            if (path.isEmpty()) {
                payload = null;
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

            return remove && children.isEmpty() && payload == null;
        }

        private static ImmutableList<String> toPath(String topic) {
            return ImmutableList.copyOf(topic.split("/"));
        }

        private static String fromPath(ImmutableList<String> path) {
            return String.join("/", path);
        }
    }
}
