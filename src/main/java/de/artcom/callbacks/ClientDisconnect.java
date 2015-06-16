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

package de.artcom.callbacks;

import com.dcsquare.hivemq.spi.callback.events.OnDisconnectCallback;
import com.dcsquare.hivemq.spi.security.ClientData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the OnDisconnectCallback, which is invoke everytime a client disconnects.
 * The callback allows to implement custom logic, which should be executed after a disconnect.
 *
 * @author Christian Goetz
 */
public class ClientDisconnect implements OnDisconnectCallback {

    Logger log = LoggerFactory.getLogger(ClientDisconnect.class);

    /**
     * This method is called from the HiveMQ on a client disconnect.
     *
     * @param clientData       Useful information about the clients authentication state and credentials.
     * @param abruptDisconnect When true the connection of the client broke down without a
     *                         {@link com.dcsquare.hivemq.spi.message.DISCONNECT} message and if false then the client
     *                         disconnected properly with a {@link com.dcsquare.hivemq.spi.message.DISCONNECT} message.
     */
    @Override
    public void onDisconnect(ClientData clientData, boolean abruptDisconnect) {
        log.info("Client {} is disconnected", clientData.getClientId());
    }
}
