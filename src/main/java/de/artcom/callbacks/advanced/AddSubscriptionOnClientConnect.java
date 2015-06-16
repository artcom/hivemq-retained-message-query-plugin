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

import com.dcsquare.hivemq.spi.callback.CallbackPriority;
import com.dcsquare.hivemq.spi.callback.events.OnConnectCallback;
import com.dcsquare.hivemq.spi.callback.exception.RefusedConnectionException;
import com.dcsquare.hivemq.spi.message.CONNECT;
import com.dcsquare.hivemq.spi.message.QoS;
import com.dcsquare.hivemq.spi.message.Topic;
import com.dcsquare.hivemq.spi.security.ClientData;
import com.dcsquare.hivemq.spi.services.SubscriptionStore;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an acme of a callback, which is invoked every time a new client is
 * successfully authenticated. The callback can be used to execute some custom behavior,
 * which is necessary when a new client connects or to implement a custom logic based
 * on the {@link com.dcsquare.hivemq.spi.message.CONNECT} to refuse the connection throwing a
 * {@link com.dcsquare.hivemq.spi.callback.exception.RefusedConnectionException}.
 *
 * The callback adds a individual subscription to devices/{clientId}/sensor for each connecting client.
 *
 * @author Christian Goetz
 */
public class AddSubscriptionOnClientConnect implements OnConnectCallback {

    private final SubscriptionStore subscriptionStore;
    Logger log = LoggerFactory.getLogger(AddSubscriptionOnClientConnect.class);

    @Inject
    public AddSubscriptionOnClientConnect(final SubscriptionStore subscriptionStore) {
        this.subscriptionStore = subscriptionStore;
    }

    /**
     * This is the callback method, which is called by the HiveMQ core, if a client has sent,
     * a {@link com.dcsquare.hivemq.spi.message.CONNECT} Message and was successfully authenticated. In this acme there is only
     * a logging statement, normally the behavior would be implemented in here.
     *
     * @param connect    The {@link com.dcsquare.hivemq.spi.message.CONNECT} message from the client.
     * @param clientData Useful information about the clients authentication state and credentials.
     * @throws com.dcsquare.hivemq.spi.callback.exception.RefusedConnectionException This exception should be thrown, if the client is
     *                                    not allowed to connect.
     */
    @Override
    public void onConnect(CONNECT connect, ClientData clientData) throws RefusedConnectionException {
        final String clientId = clientData.getClientId();

        log.info("Client {} is connecting", clientId);

        // Adding a subscription without automatically for the client
        addClientToTopic(clientId,"devices/"+ clientId +"/sensor");
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

    /**
     * Add a Subscription for a certain client
     */
    private void addClientToTopic(String clientId, String topic) {
        subscriptionStore.addSubscription(clientId, new Topic(topic, QoS.valueOf(0)));
        log.info("Added subscription to {} for client {}", topic, clientId);
    }
}
