package de.artcom.hivemq_http_api_plugin;

import com.hivemq.spi.PluginEntryPoint;
import com.hivemq.spi.callback.registry.CallbackRegistry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

class HttpApiMainClass extends PluginEntryPoint {

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
