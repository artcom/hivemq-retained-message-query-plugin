package de.artcom.hivemq_http_api_plugin.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import static javax.ws.rs.core.Response.Status.*;

public class QueryResource {

    private final ObjectMapper objectMapper;
    private final QueryProcessor queryProcessor;

    @Inject
    public QueryResource(QueryProcessor queryProcessor) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());

        this.queryProcessor = queryProcessor;
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
                return createBatchResponse(batchQuery(queries));
            }
        } catch (IOException e) {
        }

        return Response.status(BAD_REQUEST).build();
    }

    private QueryResult singleQuery(Query query) {
        return queryProcessor.process(query);
    }

    private List<QueryResult> batchQuery(List<Query> queries) {
        return Lists.transform(queries, new Function<Query, QueryResult>() {
            @Override
            public QueryResult apply(Query query) {
                return singleQuery(query);
            }
        });
    }

    private Response createSingleResponse(QueryResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            return Response.status(result.getStatus()).entity(json).build();
        } catch (JsonProcessingException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response createBatchResponse(List<QueryResult> results) {
        try {
            String json = objectMapper.writeValueAsString(results);
            return Response.status(OK).entity(json).build();
        } catch (JsonProcessingException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

}
