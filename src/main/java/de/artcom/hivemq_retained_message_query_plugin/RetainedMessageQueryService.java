package de.artcom.hivemq_retained_message_query_plugin;

import com.hivemq.spi.callback.CallbackPriority;
import com.hivemq.spi.callback.events.broker.OnBrokerStart;
import com.hivemq.spi.callback.exception.BrokerUnableToStartException;
import com.hivemq.spi.services.rest.RESTService;
import de.artcom.hivemq_retained_message_query_plugin.query.Resource;

import javax.inject.Inject;

class RetainedMessageQueryService implements OnBrokerStart {

    private final RESTService restService;

    @Inject
    public RetainedMessageQueryService(RESTService restService) {
        this.restService = restService;
    }

    @Override
    public void onBrokerStart() throws BrokerUnableToStartException {
        restService.addJaxRsResources(Resource.class);
    }

    @Override
    public int priority() {
        return CallbackPriority.MEDIUM;
    }
}
