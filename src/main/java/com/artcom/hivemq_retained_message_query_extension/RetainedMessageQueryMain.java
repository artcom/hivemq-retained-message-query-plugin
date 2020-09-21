package com.artcom.hivemq_retained_message_query_extension;

import com.artcom.hivemq_retained_message_query_extension.query.QueryHandler;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.*;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetainedMessageQueryMain implements ExtensionMain {
    private static final @NotNull Logger log = LoggerFactory.getLogger(RetainedMessageQueryMain.class);
    Server server;

    @Override
    public void extensionStart(@NotNull ExtensionStartInput extensionStartInput, @NotNull ExtensionStartOutput extensionStartOutput) {
        try {
            RetainedMessageTree retainedMessageTree = new RetainedMessageTree();
            startServer(retainedMessageTree);
            registerPublishInterceptor(retainedMessageTree);

            final ExtensionInformation extensionInformation = extensionStartInput.getExtensionInformation();
            log.info("Started " + extensionInformation.getName() + ":" + extensionInformation.getVersion());
        } catch (Exception e) {
            log.error("Exception thrown at extension start: ", e);
        }
    }

    private void startServer(RetainedMessageTree retainedMessageTree) {
        try {
            server = new Server(4567);
            server.setHandler(new QueryHandler(retainedMessageTree));
            server.start();
        } catch (Exception e) {
            log.error("Exception thrown while starting HTTP server: ", e);
        }
    }

    private void stopServer() {
        server.setStopAtShutdown(true);
    }

    private void registerPublishInterceptor(RetainedMessageTree retainedMessageTree) {
        final ClientInitializer initializer = (initializerInput, clientContext) -> clientContext.addPublishInboundInterceptor(retainedMessageTree);
        Services.initializerRegistry().setClientInitializer(initializer);
    }

    @Override
    public void extensionStop(@NotNull ExtensionStopInput extensionStopInput, @NotNull ExtensionStopOutput extensionStopOutput) {
        stopServer();

        final ExtensionInformation extensionInformation = extensionStopInput.getExtensionInformation();
        log.info("Stopped " + extensionInformation.getName() + ":" + extensionInformation.getVersion());
    }
}
