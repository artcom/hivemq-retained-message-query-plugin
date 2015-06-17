package de.artcom.hivemq_http_api_plugin.resources;

import com.dcsquare.hivemq.spi.message.RetainedMessage;
import com.dcsquare.hivemq.spi.services.RetainedMessageStore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static javax.ws.rs.core.Response.Status.*;

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
    public Response post(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);

            if (json.isObject()) {
                Query query = objectMapper.readValue(body, Query.class);
                return createSingleResponse(singleQuery(query));
            } else if (json.isArray()) {
                List<Query> queries = objectMapper.readValue(body, new TypeReference<List<Query>>() {});
                return createBulkResponse(bulkQuery(queries));
            }
        } catch (IOException e) {
        }

        return Response.status(BAD_REQUEST).build();
    }

    private QueryResult singleQuery(Query query) {
        Optional<RetainedMessage> optionalMessage = retainedMessageStore.getRetainedMessage(query.topic);

        if (!optionalMessage.isPresent()) {
            return QueryResult.error(NOT_FOUND);
        }

        RetainedMessage message = optionalMessage.get();
        return QueryResult.success(
                message.getTopic(),
                new String(message.getMessage(), Charset.forName("UTF-8"))
        );
    }

    private List<QueryResult> bulkQuery(List<Query> queries) {
        return Lists.transform(queries, new Function<Query, QueryResult>() {
            @Override
            public QueryResult apply(Query query) {
                return singleQuery(query);
            }
        });
    }

    private Response createSingleResponse(QueryResult result) {
        if (result.isError()) {
            return Response.status(result.error).build();
        } else {
            try {
                String json = objectMapper.writeValueAsString(result);
                return Response.status(OK).entity(json).build();
            } catch (JsonProcessingException e) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    private Response createBulkResponse(List<QueryResult> results) {
        try {
            String json = objectMapper.writeValueAsString(results);
            return Response.status(OK).entity(json).build();
        } catch (JsonProcessingException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    public static class Query {
        public String topic;
        public int depth;
    }

    public static class QueryResult {
        public static QueryResult success(String topic, String payload) {
            QueryResult result = new QueryResult();
            result.topic = topic;
            result.payload = payload;
            result.error = null;
            return result;
        }

        public static QueryResult error(Response.Status error) {
            QueryResult result = new QueryResult();
            result.error = error;
            return result;
        }

        @JsonIgnore
        public boolean isError() {
            return error != null;
        }

        public String topic;
        public String payload;
        public Response.Status error;
    }
}
