package de.artcom.hivemq_http_api_plugin.resources;

import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;

@Path("query")
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
    public String query(String body) {
        try {
            Query query = objectMapper.readValue(body, Query.class);
            return query.topic;
        } catch (IOException e) {
            return "error";
        }
    }

    @XmlRootElement
    public static class Query {

        public Query() {}

        @XmlElement(required = true)
        public String topic;

        @XmlElement
        public int depth;
    }
}
