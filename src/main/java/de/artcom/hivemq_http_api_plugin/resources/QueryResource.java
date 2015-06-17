package de.artcom.hivemq_http_api_plugin.resources;

import com.dcsquare.hivemq.spi.message.RetainedMessage;
import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.Charset;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

public class QueryResource {

    private static final Logger LOG = LoggerFactory.getLogger(QueryResource.class);

    private final ObjectMapper objectMapper;
    private final RetainedMessageStore retainedMessageStore;

    @Inject
    public QueryResource(RetainedMessageStore retainedMessageStore) {
        this.objectMapper = new ObjectMapper();
        this.retainedMessageStore = retainedMessageStore;
    }

    @POST
    @Path("query")
    public Response query(String body) {
        try {
            Query query = objectMapper.readValue(body, Query.class);
            Optional<QueryResult> result = doQuery(query.topic);

            if (!result.isPresent()) {
                return Response.status(NOT_FOUND).build();
            }

            String json = objectMapper.writeValueAsString(result.get());
            return Response.status(OK).entity(json).build();
        } catch (IOException e) {
            return Response.status(BAD_REQUEST).build();
        }
    }

    private Optional<QueryResult> doQuery(String topic) {
        Optional<RetainedMessage> optionalMessage = retainedMessageStore.getRetainedMessage(topic);

        if (!optionalMessage.isPresent()) {
            return Optional.absent();
        }

        RetainedMessage message = optionalMessage.get();
        QueryResult result = new QueryResult();
        result.topic = message.getTopic();
        result.value = new String(message.getMessage(), Charset.forName("UTF-8"));
        return Optional.of(result);
    }

    public static class Query {
        public String topic;
        public int depth;
    }

    public static class QueryResult {
        public String topic;
        public String value;
    }
}
