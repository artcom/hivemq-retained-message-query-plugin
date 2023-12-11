package com.artcom.hivemq_retained_message_query_extension;

import com.artcom.hivemq_retained_message_query_extension.query.QueryHandler;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.parameter.*;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.admin.LifecycleStage;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extension.sdk.api.services.general.IterationContext;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.session.SessionInformation;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RetainedMessageQueryMain implements ExtensionMain {
    private static final @NotNull Logger log = LoggerFactory.getLogger(RetainedMessageQueryMain.class);
    HttpServer server;

    @Override
    public void extensionStart(@NotNull ExtensionStartInput extensionStartInput, @NotNull ExtensionStartOutput extensionStartOutput) {
        log.info("Extension \"{}\": Scheduling initialization", extensionStartInput.getExtensionInformation().getName());

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
        log.info("Extension \"{}\": Initializing", extensionInformation.getName());

        final boolean cors = getCorsConfig();

        log.info("Extension \"{}\": CORS headers {}", extensionInformation.getName(), (cors ? "enabled" : "disabled"));

        RetainedMessageTree retainedMessageTree = new RetainedMessageTree();
        retainedMessageTree.init().whenComplete((ignored, throwable) -> {
            try {
                if (throwable != null) {
                    throw throwable;
                }

                startServer(retainedMessageTree, extensionInformation.getName(), cors);
                registerRetainedMessageTree(retainedMessageTree, extensionInformation.getName());
                if (System.getenv("QUERY_PLUGIN_DISCONNECT_CLIENTS") != null && "true".equals(System.getenv("QUERY_PLUGIN_DISCONNECT_CLIENTS").toLowerCase())) {
                    disconnectAllClients(extensionInformation.getName());
                }

                log.info("Extension \"{}\": Started successfully", extensionInformation.getName());
            } catch (Throwable e) {
                log.error("Extension \"{}\": Exception during initialization\n", extensionInformation.getName(), e);
            }
        });
    }

    private void startServer(@NotNull RetainedMessageTree retainedMessageTree, String extensionName, boolean cors) throws Exception {
        final int port = System.getenv("QUERY_PLUGIN_PORT") != null ? Integer.parseInt(System.getenv("QUERY_PLUGIN_PORT")) : 8080;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        log.info("Extension \"{}\": Server listening on port {}", extensionName, port);
        HttpContext context = server.createContext("/query");
        context.setHandler(new QueryHandler(retainedMessageTree, cors));
        server.start();
    }

    private void stopServer() {
        server.stop(1);
    }

    private void registerRetainedMessageTree(@NotNull RetainedMessageTree retainedMessageTree, String extensionName) {
        log.debug("Extension \"{}\": Registering Retained Message Tree", extensionName);
        Services.eventRegistry().setClientLifecycleEventListener(input -> {
            return retainedMessageTree;
        });

        final ClientInitializer initializer = (initializerInput, clientContext) -> {
            log.debug("Extension \"{}\":Adding a PublishInboundInterceptor to client id {}", extensionName, initializerInput.getClientInformation().getClientId());
            clientContext.addPublishInboundInterceptor(retainedMessageTree);
        };

        Services.initializerRegistry().setClientInitializer(initializer);
    }

    private void disconnectAllClients(String extensionName) {
        final ClientService clientService = Services.clientService();

        CompletableFuture<Void> iterationFuture = clientService.iterateAllClients(
            new IterationCallback<SessionInformation>() {
                @Override
                public void iterate(IterationContext context, SessionInformation sessionInformation) {
                    if (sessionInformation.isConnected()) {
                        log.debug("Extension \"{}\": Disconnecting client {}", extensionName, sessionInformation.getClientIdentifier());
                        clientService.disconnectClient(sessionInformation.getClientIdentifier(), false, DisconnectReasonCode.ADMINISTRATIVE_ACTION, "Retained message query extension initialization");
                    }
                }
        });
        iterationFuture.whenComplete((ignored, throwable) -> {
            if (throwable == null) {
                log.info("Extension \"{}\": Disconnected all clients", extensionName);
            } else {
                log.error("Extension \"{}\": Exception while disconnecting all clients", extensionName, throwable);
            }
        });
    };

    @Override
    public void extensionStop(@NotNull ExtensionStopInput extensionStopInput, @NotNull ExtensionStopOutput extensionStopOutput) {
        stopServer();

        final ExtensionInformation extensionInformation = extensionStopInput.getExtensionInformation();
        log.info("Extension \"" + extensionInformation.getName()  + "\": Stopped successfully");
    }

    private boolean getCorsConfig() {
        try {
            if (System.getenv("QUERY_PLUGIN_CORS") != null) {
                return "true".equals(System.getenv("QUERY_PLUGIN_CORS").toLowerCase());
            }

            InputSource inputXML = new InputSource(new FileReader(new File("conf/config.xml")));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xPath = xPathfactory.newXPath();
            String result = xPath.evaluate("/hivemq/retained-message-query-extension/cors-header/text()", inputXML);
            return "true".equals(result.toLowerCase());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
