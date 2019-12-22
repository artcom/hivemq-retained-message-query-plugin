package com.artcom.hivemq_retained_message_query_plugin;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.Services;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class RetainedMessageTree implements PublishInboundInterceptor {
    private final Node root = Node.rootNode();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public RetainedMessageTree() {
//        Set<RetainedMessage> messages = Services.retainedMessageStore();
//        for (RetainedMessage message : messages) {
//            addNode(message.getTopic(), message.getMessage());
//        }
    }

    public Stream<Node> getNodes(String topic) {
        lock.readLock().lock();

        try {
            return root.getNodes(topic);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void addNode(String topic, ByteBuffer payload) {
        lock.writeLock().lock();

        try {
            Node node = root.createNode(topic);
            node.payload = StandardCharsets.UTF_8.decode(payload).toString();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeNode(String topic) {
        lock.writeLock().lock();

        try {
            root.removeNode(topic);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void onInboundPublish(@NotNull PublishInboundInput publishInboundInput, @NotNull PublishInboundOutput publishInboundOutput) {
        PublishPacket packet = publishInboundInput.getPublishPacket();

        if (packet.getRetain()) {
            Services.extensionExecutorService().submit(() -> {
                        String topic = packet.getTopic();
                        Optional<ByteBuffer> payload = packet.getPayload();

                        if (payload.isPresent()) {
                            addNode(topic, payload.get());
                        } else {
                            removeNode(topic);
                        }
                    }
            );
        }
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

        private Stream<Node> getNodes(String topic) {
            return getNodes(toPath(topic));
        }

        private Stream<Node> getNodes(ImmutableList<String> path) {
            if (path.isEmpty()) {
                return Stream.of(this);
            }

            String name = path.get(0);

            if ("+".equals(name)) {
                return getChildren().flatMap(child -> child.getNodes(path.subList(1, path.size())));
            } else {
                Node child = children.get(name);

                if (child == null) {
                    return Stream.empty();
                }

                return child.getNodes(path.subList(1, path.size()));
            }
        }

        private Node createNode(String topic) {
            return createNode(toPath(topic), 0);
        }

        private Node createNode(ImmutableList<String> path, int index) {
            if (index >= path.size()) {
                return this;
            }

            String name = path.get(index);

            if (!children.containsKey(name)) {
                children.put(name, Node.forPath(path.subList(0, index + 1)));
            }

            Node child = children.get(name);

            return child.createNode(path, index + 1);
        }

        private void removeNode(String topic) {
            removeNode(toPath(topic));
        }

        private boolean removeNode(ImmutableList<String> path) {
            if (path.isEmpty()) {
                payload = null;
                return children.isEmpty();
            }

            String name = path.get(0);
            Node child = children.get(name);

            if (child == null) {
                return false;
            }

            boolean remove = child.removeNode(path.subList(1, path.size()));

            if (remove) {
                children.remove(name);
            }

            return remove && children.isEmpty() && payload == null;
        }

        private static ImmutableList<String> toPath(@Nullable String topic) {
            if (topic == null) {
                return ImmutableList.of();
            }

            return ImmutableList.copyOf(topic.split("/", -1));
        }

        private static @Nullable String fromPath(ImmutableList<String> path) {
            if (path.isEmpty()) {
                return null;
            }

            return String.join("/", path);
        }
    }
}
