package de.artcom.hivemq_retained_message_query_plugin;

import com.hivemq.spi.HiveMQPluginModule;
import com.hivemq.spi.PluginEntryPoint;
import com.hivemq.spi.plugin.meta.Information;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Information(name = "HiveMQ Retained Message Query Plugin", author = "ART+COM AG", version = "0.6.1-SNAPSHOT")
public class RetainedMessageQueryPluginModule extends HiveMQPluginModule {

    @Override
    protected void configurePlugin() {
        bind(ExecutorService.class).toInstance(Executors.newSingleThreadExecutor());
    }

    @Override
    protected Class<? extends PluginEntryPoint> entryPointClass() {
        return RetainedMessageQueryMainClass.class;
    }
}
