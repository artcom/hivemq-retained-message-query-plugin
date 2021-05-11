package com.artcom.hivemq_retained_message_query_extension;

import com.artcom.hivemq_retained_message_query_extension.query.QueryHandler;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.*;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.admin.LifecycleStage;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class RetainedMessageQueryMain implements ExtensionMain {
    private static final @NotNull Logger log = LoggerFactory.getLogger(RetainedMessageQueryMain.class);
    HttpServer server;

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

        final boolean cors = getCORSConfig();

        log.info("Extension \"" + extensionInformation.getName()  + "\": CORS headers " + (cors ? "enabled" : "disabled"));

        RetainedMessageTree retainedMessageTree = new RetainedMessageTree();
        retainedMessageTree.init().whenComplete((ignored, throwable) -> {
            try {
                if (throwable != null) {
                    throw throwable;
                }

                startServer(retainedMessageTree, cors);
                registerPublishInterceptor(retainedMessageTree);


                log.info("Extension \"" + extensionInformation.getName()  + "\": Started successfully");
            } catch (Throwable e) {
                log.error("Extension \"" + extensionInformation.getName()  + "\": Exception during initialization\n", e);
            }
        });
    }

    private void startServer(@NotNull RetainedMessageTree retainedMessageTree, boolean cors) throws Exception {
        server = HttpServer.create(new InetSocketAddress(8080), 0);
        HttpContext context = server.createContext("/query");
        context.setHandler(new QueryHandler(retainedMessageTree, cors));
        server.start();
    }

    private void stopServer() {
        server.stop(1);
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

    private boolean getCORSConfig() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
