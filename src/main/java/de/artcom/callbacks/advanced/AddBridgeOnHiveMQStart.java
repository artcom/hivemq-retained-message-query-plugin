/*
 * Copyright 2013 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.artcom.callbacks.advanced;

import com.dcsquare.hivemq.spi.bridge.Address;
import com.dcsquare.hivemq.spi.bridge.Bridge;
import com.dcsquare.hivemq.spi.bridge.StartType;
import com.dcsquare.hivemq.spi.bridge.TopicPattern;
import com.dcsquare.hivemq.spi.callback.CallbackPriority;
import com.dcsquare.hivemq.spi.callback.events.broker.OnBrokerStart;
import com.dcsquare.hivemq.spi.callback.exception.BrokerUnableToStartException;
import com.dcsquare.hivemq.spi.message.QoS;
import com.dcsquare.hivemq.spi.services.BridgeManagerService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the {@link com.dcsquare.hivemq.spi.callback.events.broker.OnBrokerStart} callback, which is invoked when HiveMQ is
 * starting. It can be used to execute custom plugin or system initialization stuff.
 *
 * It uses the callback to start up a bridge connection at the startup of the broker.
 *
 * @author Christian Goetz
 */
public class AddBridgeOnHiveMQStart implements OnBrokerStart {

    private final BridgeManagerService bridgeManagerService;
    Logger log = LoggerFactory.getLogger(AddBridgeOnHiveMQStart.class);

    @Inject
    public AddBridgeOnHiveMQStart(final BridgeManagerService bridgeManagerService) {
        this.bridgeManagerService = bridgeManagerService;
    }

    /**
     * This method is called from HiveMQ, and the custom behaviour has to be implemented in here.
     * If some preconditions are not met to successfully operate, a {@link com.dcsquare.hivemq.spi.callback.exception.BrokerUnableToStartException}
     * should be thrown.
     *
     * @throws com.dcsquare.hivemq.spi.callback.exception.BrokerUnableToStartException If the exception is thrown, HiveMQ will be stopped.
     */
    @Override
    public void onBrokerStart() throws BrokerUnableToStartException {
        log.info("Adding Bridge to MQTT Dashboard");
        final Bridge bridge = createBridge();

        // Start bridge with Bridge Manager Service dynamically
        final ListenableFuture<Void> future = bridgeManagerService.startBridge(bridge);

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                log.info("Bridge started successfully");
            }

            @Override
            public void onFailure(Throwable t) {
                log.info("Bridge failed to start");
            }
        });

    }

    private Bridge createBridge() {
        final Bridge exampleBridge = new Bridge();
        exampleBridge.setConnectionName("BridgeConnection");
        exampleBridge.setClientId("bridgeClient");
        exampleBridge.setStartType(StartType.MANUAL);
        exampleBridge.setTryPrivate(true);

        exampleBridge.setCleanSession(true);

        final List<TopicPattern> topicPatterns = new ArrayList<TopicPattern>();
        final TopicPattern pattern = new TopicPattern();
        pattern.setLocalPrefix("");
        pattern.setRemotePrefix("");
        pattern.setPattern("#");
        pattern.setQoS(QoS.AT_LEAST_ONCE);
        pattern.setType(TopicPattern.Type.OUT);
        topicPatterns.add(pattern);
        exampleBridge.setTopicPatterns(topicPatterns);

        final List<Address> addresses = new ArrayList<Address>();
        addresses.add(new Address("broker.mqttdashboard.com", 1883));
        exampleBridge.setAddresses(addresses);

        exampleBridge.setKeepAlive(60);
        exampleBridge.setRestartTimeout(10);

        exampleBridge.setIdleTimeout(30);
        exampleBridge.setThreshold(10);

        exampleBridge.setNotificationsEnabled(false);

        exampleBridge.setRoundRobin(false);

        return exampleBridge;
    }


    /**
     * The priority is used when more than one OnConnectCallback is implemented to determine the order.
     * If there is only one callback, which implements a certain interface, the priority has no effect.
     *
     * @return callback priority
     */
    @Override
    public int priority() {
        return CallbackPriority.MEDIUM;
    }


}
