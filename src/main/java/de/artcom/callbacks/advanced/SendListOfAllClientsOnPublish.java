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
import com.dcsquare.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.dcsquare.hivemq.spi.callback.exception.OnPublishReceivedException;
import com.dcsquare.hivemq.spi.message.PUBLISH;
import com.dcsquare.hivemq.spi.security.ClientData;
import com.dcsquare.hivemq.spi.services.ClientService;
import com.dcsquare.hivemq.spi.services.PublishService;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * This class implements the {@link com.dcsquare.hivemq.spi.callback.events.OnPublishReceivedCallback}, which is triggered everytime
 * a new message is published to the broker. This callback enables a custom handling of a
 * MQTT message, for acme saving to a database.
 * <p/>
 * Addtionally this callback shows how the PublishService can be used to republish messages on other topics.
 *
 * @author Christian Goetz
 */
public class SendListOfAllClientsOnPublish implements OnPublishReceivedCallback {

    Logger logger = LoggerFactory.getLogger(SendListOfAllClientsOnPublish.class);

    private final PublishService publishService;
    private final ClientService clientService;
    private final String allClientsTopic = "broker/all/clients";

    @Inject
    public SendListOfAllClientsOnPublish(final PublishService publishService, final ClientService clientService) {
        this.publishService = publishService;
        this.clientService = clientService;
    }

    /**
     * This method is called from the HiveMQ, when a new MQTT {@link com.dcsquare.hivemq.spi.message.PUBLISH} message arrives
     * at the broker. In this acme the method is just logging each message to the console.
     *
     * @param publish    The publish message send by the client.
     * @param clientData Useful information about the clients authentication state and credentials.
     * @throws com.dcsquare.hivemq.spi.callback.exception.OnPublishReceivedException
     *          When the exception is thrown, the publish is not
     *          accepted and will NOT be delivered to the subscribing clients.
     */
    @Override
    public void onPublishReceived(PUBLISH publish, ClientData clientData) throws OnPublishReceivedException {
        if (publish.getTopic().equals("fetch/all/clients")) {

            String clientID = clientData.getClientId();
            String topic = publish.getTopic();
            String message = new String(publish.getPayload(), Charsets.UTF_8);

            logger.info("Client " + clientID + " sent a message to topic " + topic + ": " + message);

            // Get all clients
            String allClients = getAllClients();
            publish.setPayload(allClients.getBytes(Charsets.UTF_8));

            // This redirects the message with the help of the PublishService

            redirectPublish(allClientsTopic, publish);

            logger.info("Ignoring message and sending list of all clients to topic {}", allClientsTopic);
            throw new OnPublishReceivedException("This message should not be published", false);
        }
    }

    /**
     * This methods iterates over all clients and build a string.
     *
     * @return a comma-separated string of all clients
     */
    private String getAllClients() {
        StringBuilder stringBuilder = new StringBuilder();

        final Set<String> connectedClients = clientService.getConnectedClients();
        for (String connectedClient : connectedClients) {
            stringBuilder.append(connectedClient).append(",");
        }
        return stringBuilder.toString();
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
     * Copy and redirect a PUBLISH message to a new topic
     */
    private void redirectPublish(String newTopic, PUBLISH publish) {
        PUBLISH copy = PUBLISH.copy(publish);
        copy.setTopic(newTopic);
        publishService.publish(copy);
    }
}
