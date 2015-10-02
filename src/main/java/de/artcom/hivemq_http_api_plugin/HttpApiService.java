package de.artcom.hivemq_http_api_plugin;

import com.hivemq.spi.callback.CallbackPriority;
import com.hivemq.spi.callback.events.broker.OnBrokerStart;
import com.hivemq.spi.callback.events.broker.OnBrokerStop;
import com.hivemq.spi.callback.exception.BrokerUnableToStartException;
import com.hivemq.spi.services.rest.RESTService;
import de.artcom.hivemq_http_api_plugin.query.QueryResource;

import javax.inject.Inject;

public class HttpApiService implements OnBrokerStart, OnBrokerStop {

    private final RESTService restService;

    @Inject
    public HttpApiService(RESTService restService) {
        this.restService = restService;
    }

    @Override
    public void onBrokerStart() throws BrokerUnableToStartException {
        restService.addJaxRsResources(QueryResource.class);
    }

    @Override
    public void onBrokerStop() {}

    @Override
    public int priority() {
        return CallbackPriority.MEDIUM;
    }
}
