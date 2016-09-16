package de.artcom.hivemq_retained_message_query_plugin;

import com.hivemq.spi.PluginEntryPoint;
import com.hivemq.spi.callback.registry.CallbackRegistry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

class RetainedMessageQueryMainClass extends PluginEntryPoint {

    private final RetainedMessageQueryService retainedMessageQueryService;
    private final RetainedMessageTree retainedMessageTree;

    @Inject
    public RetainedMessageQueryMainClass(RetainedMessageQueryService retainedMessageQueryService, RetainedMessageTree retainedMessageTree) {
        this.retainedMessageQueryService = retainedMessageQueryService;
        this.retainedMessageTree = retainedMessageTree;
    }

    @PostConstruct
    public void postConstruct() {
        CallbackRegistry callbackRegistry = getCallbackRegistry();
        callbackRegistry.addCallback(retainedMessageQueryService);
        callbackRegistry.addCallback(retainedMessageTree);
    }
}
