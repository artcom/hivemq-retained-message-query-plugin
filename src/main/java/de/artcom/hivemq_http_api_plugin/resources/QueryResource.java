package de.artcom.hivemq_http_api_plugin.resources;

import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Path("query")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QueryResource {

    private static final Logger LOG = LoggerFactory.getLogger(QueryResource.class);
    private final RetainedMessageStore retainedMessageStore;

    @Inject
    public QueryResource(RetainedMessageStore retainedMessageStore) {
        this.retainedMessageStore = retainedMessageStore;
    }

    @POST
    public Result query(String body) {
        LOG.info("POST");
        if (body != null) LOG.info(body);
        return new Result("foo", body);
    }

    @XmlRootElement
    public class Query {

        public Query() {}

        @XmlElement(required = true)
        public String topic;

        @XmlElement
        public int depth;
    }

    @XmlRootElement
    public class Result {
        public Result(String topic) {
            this.topic = topic;
        }

        public Result(String topic, Object value) {
            this.topic = topic;
            this.value = value;
        }

        @XmlElement(required = true)
        public String topic;

        @XmlElement
        public Object value;
    }
}
