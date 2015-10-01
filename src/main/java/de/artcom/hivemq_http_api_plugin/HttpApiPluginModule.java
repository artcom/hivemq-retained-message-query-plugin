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

import com.hivemq.spi.HiveMQPluginModule;
import com.hivemq.spi.PluginEntryPoint;
import com.hivemq.spi.plugin.meta.Information;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Information(name = "HiveMQ HTTP API Plugin", author = "ART+COM AG", version = "0.4.0-SNAPSHOT")
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
