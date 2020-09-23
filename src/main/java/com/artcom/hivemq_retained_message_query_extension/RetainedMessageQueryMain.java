package com.artcom.hivemq_retained_message_query_extension;

import com.artcom.hivemq_retained_message_query_extension.query.QueryHandler;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.*;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.admin.LifecycleStage;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RetainedMessageQueryMain implements ExtensionMain {
    private static final @NotNull Logger log = LoggerFactory.getLogger(RetainedMessageQueryMain.class);
    Server server;

    @Override
    public void extensionStart(@NotNull ExtensionStartInput extensionStartInput, @NotNull ExtensionStartOutput extensionStartOutput) {
        log.info("Extension \"" + extensionStartInput.getExtensionInformation().getName()  + "\": Scheduling initialization");

        scheduleInitialize(extensionStartInput);
    }

    private void scheduleInitialize(@NotNull ExtensionStartInput extensionStartInput) {
        Services.extensionExecutorService().schedule(() -> {
            if (Services.adminService().getCurrentStage() == LifecycleStage.STARTED_SUCCESSFULLY) {
                initialize(extensionStartInput.getExtensionInformation());
            } else {
                scheduleInitialize(extensionStartInput);
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void initialize(@NotNull ExtensionInformation extensionInformation) {
        log.info("Extension \"" + extensionInformation.getName()  + "\": Initializing");
        
        RetainedMessageTree retainedMessageTree = new RetainedMessageTree();
        retainedMessageTree.init().whenComplete((ignored, throwable) -> {
            try {
                if (throwable != null) {
                    throw throwable;
                }

                startServer(retainedMessageTree);
                registerPublishInterceptor(retainedMessageTree);


                log.info("Extension \"" + extensionInformation.getName()  + "\": Started successfully");
            } catch (Throwable e) {
                log.error("Extension \"" + extensionInformation.getName()  + "\": Exception during initialization\n", e);
            }
        });
    }

    private void startServer(@NotNull RetainedMessageTree retainedMessageTree) throws Exception {
        server = new Server(8080);
        server.setHandler(new QueryHandler(retainedMessageTree));
        server.start();
    }

    private void stopServer() {
        server.setStopAtShutdown(true);
    }

    private void registerPublishInterceptor(@NotNull RetainedMessageTree retainedMessageTree) {
        final ClientInitializer initializer = (initializerInput, clientContext) -> clientContext.addPublishInboundInterceptor(retainedMessageTree);
        Services.initializerRegistry().setClientInitializer(initializer);
    }

    @Override
    public void extensionStop(@NotNull ExtensionStopInput extensionStopInput, @NotNull ExtensionStopOutput extensionStopOutput) {
        stopServer();

        final ExtensionInformation extensionInformation = extensionStopInput.getExtensionInformation();
        log.info("Extension \"" + extensionInformation.getName()  + "\": Stopped successfully");
    }
}
