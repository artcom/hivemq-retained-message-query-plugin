package com.artcom.hivemq_retained_message_query_extension;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.parameters.*;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class RetainedMessageTree implements PublishInboundInterceptor, ClientLifecycleEventListener {
    private static final @NotNull Logger log = LoggerFactory.getLogger(RetainedMessageTree.class);
    
    private final Node root = Node.rootNode();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private final HashMap<String, RetainedLastWill> retainedLastWills = new HashMap<String, RetainedLastWill>();

    public CompletableFuture<Void> init() {
        //noinspection OptionalGetWithoutIsPresent
        return Services.retainedMessageStore().iterateAllRetainedMessages(
                (context, retainedPublish) -> addNode(retainedPublish.getTopic(), retainedPublish.getPayload().get()));
    }

    public Stream<Node> getNodes(@NotNull String topic) {
        lock.readLock().lock();

        try {
            return root.getNodes(topic);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void addNode(@NotNull String topic, @NotNull ByteBuffer payload) {
        lock.writeLock().lock();

        try {
            log.debug("Adding node: " + topic);
            Node node = root.createNode(topic);
            node.payload = StandardCharsets.UTF_8.decode(payload).toString();
            log.debug("Adding node done: " + topic);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeNode(@NotNull String topic) {
        lock.writeLock().lock();

        try {
            log.debug("Removing node: " + topic);
            root.removeNode(topic);
            log.debug("Removing node done: " + topic);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void onInboundPublish(@NotNull PublishInboundInput publishInboundInput, @NotNull PublishInboundOutput publishInboundOutput) {
        PublishPacket packet = publishInboundInput.getPublishPacket();

        if (packet.getRetain()) {
            log.debug("Inbound publish received for topic: " + packet.getTopic());

            handlePublish(packet.getTopic(),packet.getPayload());
        }
    }

    private void handlePublish(String topic, Optional<ByteBuffer> payload) {
        Services.extensionExecutorService().submit(() -> {
                    log.debug("Handling inbound publish: " + topic + " " + payload.isPresent());
                    if (payload.isPresent() && payload.get().limit() > 0) {
                        addNode(topic, payload.get());
                    } else {
                        removeNode(topic);
                    }
                }
        );
    }

    @Override
    public void onMqttConnectionStart(@NotNull ConnectionStartInput input) {
        log.debug("onMqttConnectionStart");
        log.debug("onMqttConnectionStart: " + input.getClientInformation().getClientId());

        if(input.getConnectPacket().getWillPublish().isPresent()) {
            WillPublishPacket wpp = input.getConnectPacket().getWillPublish().get();
            if(wpp.getRetain()) {
                log.debug("Storing last will for client: " + input.getClientInformation().getClientId());
                retainedLastWills.put(
                        input.getConnectPacket().getClientId(),
                        new RetainedLastWill(wpp.getTopic(), wpp.getPayload())
                );
            } else {
                log.debug("Last will not retained for client: " + input.getClientInformation().getClientId());
            }
        } else {
            log.debug("No last will for client: " + input.getClientInformation().getClientId());
        }
    }

    @Override
    public void onConnectionLost(@NotNull final ConnectionLostInput input) {
        log.debug("Client connection lost: " + input.getClientInformation().getClientId() + " " + input.getReasonCode());

        if(retainedLastWills.containsKey(input.getClientInformation().getClientId())) {
            log.debug("Applying last will for client: " + input.getClientInformation().getClientId());

            RetainedLastWill will = retainedLastWills.get(input.getClientInformation().getClientId());
            handlePublish(will.topic, will.payload);
            retainedLastWills.remove(input.getClientInformation().getClientId());
        }
    }

    @Override
    public void onAuthenticationSuccessful(@NotNull AuthenticationSuccessfulInput authenticationSuccessfulInput) {
        // NOOP
    }

    public void onDisconnect(@NotNull DisconnectEventInput input) {
        // NOOP
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

        private static Node forPath(@NotNull ImmutableList<String> path) {
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

        private Stream<Node> getNodes(@NotNull String topic) {
            return getNodes(toPath(topic));
        }

        private Stream<Node> getNodes(@NotNull ImmutableList<String> path) {
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

        private Node createNode(@NotNull String topic) {
            return createNode(toPath(topic), 0);
        }

        private Node createNode(@NotNull ImmutableList<String> path, int index) {
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

        private void removeNode(@NotNull String topic) {
            removeNode(toPath(topic));
        }

        private boolean removeNode(@NotNull ImmutableList<String> path) {
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

        private static @Nullable String fromPath(@NotNull ImmutableList<String> path) {
            if (path.isEmpty()) {
                return null;
            }

            return String.join("/", path);
        }
    }

    static public class RetainedLastWill {
        public RetainedLastWill(String topic, Optional<ByteBuffer> payload) {
            this.topic = topic;
            this.payload = payload;
        }

        public String topic;
        public Optional<ByteBuffer> payload;
    }
}
