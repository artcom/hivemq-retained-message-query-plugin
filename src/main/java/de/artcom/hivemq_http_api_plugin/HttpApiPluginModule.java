package de.artcom.hivemq_http_api_plugin;

import com.hivemq.spi.HiveMQPluginModule;
import com.hivemq.spi.PluginEntryPoint;
import com.hivemq.spi.plugin.meta.Information;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Information(name = "HiveMQ HTTP API Plugin", author = "ART+COM AG", version = "0.4.3-SNAPSHOT")
public class HttpApiPluginModule extends HiveMQPluginModule {

    @Override
    protected void configurePlugin() {
        bind(ExecutorService.class).toInstance(Executors.newSingleThreadExecutor());
    }

    @Override
    protected Class<? extends PluginEntryPoint> entryPointClass() {
        return HttpApiMainClass.class;
    }
}
