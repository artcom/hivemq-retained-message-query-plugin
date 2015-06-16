package de.artcom.hivemq_http_api_plugin;

import com.dcsquare.hivemq.spi.callback.CallbackPriority;
import com.dcsquare.hivemq.spi.callback.events.broker.OnBrokerStart;
import com.dcsquare.hivemq.spi.callback.events.broker.OnBrokerStop;
import com.dcsquare.hivemq.spi.callback.exception.BrokerUnableToStartException;
import com.dcsquare.hivemq.spi.services.RESTService;
import com.dcsquare.hivemq.spi.services.rest.RESTConfig;
import de.artcom.hivemq_http_api_plugin.resources.QueryResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class HttpApiService implements OnBrokerStart, OnBrokerStop {

    private static final Logger LOG = LoggerFactory.getLogger(HttpApiService.class);
    private final RESTService restService;

    @Inject
    public HttpApiService(RESTService restService) {
        this.restService = restService;
    }

    @Override
    public void onBrokerStart() throws BrokerUnableToStartException {
        RESTConfig config = new RESTConfig();
        config.addResource(QueryResource.class);

        try {
            restService.start(config);
        } catch (Exception e) {
            LOG.error("Cannot start HTTP API service", e);
            throw new BrokerUnableToStartException(e);
        }
    }

    @Override
    public void onBrokerStop() {
        restService.stop();
    }

    @Override
    public int priority() {
        return CallbackPriority.MEDIUM;
    }
}
