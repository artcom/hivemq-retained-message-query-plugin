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

package de.artcom.hivemq_http_api_plugin;

import com.dcsquare.hivemq.spi.PluginEntryPoint;
import com.dcsquare.hivemq.spi.callback.registry.CallbackRegistry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class HttpApiMainClass extends PluginEntryPoint {

    private final HttpApiService httpApiService;
    private final RetainedTopicTree retainedTopicTree;

    @Inject
    public HttpApiMainClass(HttpApiService httpApiService, RetainedTopicTree retainedTopicTree) {
        this.httpApiService = httpApiService;
        this.retainedTopicTree = retainedTopicTree;
    }

    @PostConstruct
    public void postConstruct() {
        CallbackRegistry callbackRegistry = getCallbackRegistry();
        callbackRegistry.addCallback(httpApiService);
        callbackRegistry.addCallback(retainedTopicTree);
    }
}
