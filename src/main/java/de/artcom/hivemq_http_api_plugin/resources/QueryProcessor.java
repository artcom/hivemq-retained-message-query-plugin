package de.artcom.hivemq_http_api_plugin.resources;

import com.dcsquare.hivemq.spi.message.RetainedMessage;
import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import com.google.common.base.Optional;

import javax.inject.Inject;
import java.nio.charset.Charset;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class QueryProcessor {
    private final RetainedMessageStore retainedMessageStore;

    @Inject
    public QueryProcessor(RetainedMessageStore retainedMessageStore) {
        this.retainedMessageStore = retainedMessageStore;
    }

    public QueryResult process(Query query) {
        Optional<RetainedMessage> message = retainedMessageStore.getRetainedMessage(query.topic);

        if (!message.isPresent()) {
            return new QueryResultError(query.topic, NOT_FOUND);
        }

        return new QueryResultSuccess(
                message.get().getTopic(),
                new String(message.get().getMessage(), Charset.forName("UTF-8"))
        );
    }
}
