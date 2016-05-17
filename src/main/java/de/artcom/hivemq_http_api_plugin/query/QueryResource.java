package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Lists;

import javax.inject.Inject;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@Path("/query")
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
    public Response post(String body) {
        IQueryResult result = QueryResultError.queryFormat();

        try {
            JsonNode json = objectMapper.readTree(body);

            if (json.isObject()) {
                Query query = objectMapper.readValue(body, Query.class);
                result = singleQuery(query);
            } else if (json.isArray()) {
                List<Query> queries = objectMapper.readValue(body, new TypeReference<List<Query>>() {});
                result = batchQuery(queries);
            }
        } catch (IOException ignored) {
        }

        return createResponse(result);
    }

    @OPTIONS
    public static Response options() {
        return Response.ok("")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "POST")
                .build();
    }

    private IQueryResult singleQuery(Query query) {
        return queryProcessor.process(query);
    }

    private IQueryResult batchQuery(List<Query> queries) {
        List<IQueryResult> results = Lists.transform(queries, this::singleQuery);
        return new QueryResultList(results);
    }

    private Response createResponse(IQueryResult result) {
        JsonNode json = result.toJson(objectMapper);
        return Response
                .status(result.getStatus())
                .entity(json.toString())
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "POST")
                .build();
    }
}
