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

package de.artcom.plugin;

import de.artcom.callbacks.*;
import de.artcom.callbacks.advanced.*;
import com.dcsquare.hivemq.spi.PluginEntryPoint;
import com.dcsquare.hivemq.spi.callback.registry.CallbackRegistry;
import com.dcsquare.hivemq.spi.message.QoS;
import com.dcsquare.hivemq.spi.message.RetainedMessage;
import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * This is the main class of the plugin, which is instanciated during the HiveMQ start up process.
 */
public class HelloWorldMainClass extends PluginEntryPoint {

    Logger log = LoggerFactory.getLogger(HelloWorldMainClass.class);

    private final Configuration configuration;

    private final RetainedMessageStore retainedMessageStore;

    private final ClientConnect clientConnect;
    private final PublishReceived publishReceived;
    private final SimpleScheduledCallback simpleScheduledCallback;
    private final ScheduledClearRetainedCallback scheduledClearRetainedCallback;
    private final AddSubscriptionOnClientConnect addSubscriptionOnClientConnect;
    private final SendListOfAllClientsOnPublish sendListOfAllClientsOnPublish;
    private final AddBridgeOnHiveMQStart addBridgeOnHiveMQStart;
    private final LogBrokerUptime logBrokerUptime;

    /**
     * @param configuration Injected configuration, which is declared in the {@link HelloWorldPluginModule}.
     */

    @Inject
    public HelloWorldMainClass(Configuration configuration, final RetainedMessageStore retainedMessageStore,
                               final ClientConnect clientConnect, final PublishReceived publishReceived,
                               final SimpleScheduledCallback simpleScheduledCallback,
                               final ScheduledClearRetainedCallback scheduledClearRetainedCallback,
                               final AddSubscriptionOnClientConnect addSubscriptionOnClientConnect,
                               final SendListOfAllClientsOnPublish sendListOfAllClientsOnPublish,
                               final AddBridgeOnHiveMQStart addBridgeOnHiveMQStart,
                               final LogBrokerUptime logBrokerUptime) {
        this.configuration = configuration;
        this.retainedMessageStore = retainedMessageStore;
        this.clientConnect = clientConnect;
        this.publishReceived = publishReceived;
        this.simpleScheduledCallback = simpleScheduledCallback;
        this.scheduledClearRetainedCallback = scheduledClearRetainedCallback;
        this.addSubscriptionOnClientConnect = addSubscriptionOnClientConnect;
        this.sendListOfAllClientsOnPublish = sendListOfAllClientsOnPublish;
        this.addBridgeOnHiveMQStart = addBridgeOnHiveMQStart;
        this.logBrokerUptime = logBrokerUptime;
    }

    /**
     * This method is executed after the instanciation of the whole class. It is used to initialize
     * the implemented callbacks and make them known to the HiveMQ core.
     */
    @PostConstruct
    public void postConstruct() {

        CallbackRegistry callbackRegistry = getCallbackRegistry();

        callbackRegistry.addCallback(new HiveMQStart());
        callbackRegistry.addCallback(clientConnect);
        callbackRegistry.addCallback(new ClientDisconnect());
        callbackRegistry.addCallback(publishReceived);
        callbackRegistry.addCallback(simpleScheduledCallback);
        callbackRegistry.addCallback(scheduledClearRetainedCallback);
        callbackRegistry.addCallback(addSubscriptionOnClientConnect);
        callbackRegistry.addCallback(sendListOfAllClientsOnPublish);
        callbackRegistry.addCallback(addBridgeOnHiveMQStart);
        callbackRegistry.addCallback(logBrokerUptime);

        log.info("Plugin configuration property: {}", configuration.getString("myProperty"));

        addRetainedMessage("/default", "Hello World.");
    }

    /**
     * Programmatically add a new Retained Message.
     */
    public void addRetainedMessage(String topic, String message) {

        if (!retainedMessageStore.contains(new RetainedMessage(topic, new byte[]{}, QoS.valueOf(0))))
            retainedMessageStore.addOrReplace(new RetainedMessage(topic, message.getBytes(), QoS.valueOf(1)));
    }
}
