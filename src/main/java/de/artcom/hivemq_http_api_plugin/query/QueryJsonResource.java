package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Lists;

import javax.inject.Inject;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("/json")
public class QueryJsonResource {
    private final ObjectMapper objectMapper;
    private final QueryProcessor queryProcessor;

    @Inject
    public QueryJsonResource(QueryProcessor queryProcessor) {
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
                IQuery query = objectMapper.readValue(body, QueryJson.class);
                result = singleQuery(query);
            } else if (json.isArray()) {
                List<IQuery> queries = objectMapper.readValue(body, new TypeReference<List<QueryJson>>() {});
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

    private IQueryResult singleQuery(IQuery query) {
        return queryProcessor.process(query);
    }

    private IQueryResult batchQuery(List<IQuery> queries) {
        List<IQueryResult> results = Lists.transform(queries, this::singleQuery);

        return new QueryResultList(results);
    }

    private Response createResponse(IQueryResult result) {
        try {
            JsonNode json = result.toPlainJson(objectMapper);
            JsonNode jsonContainer = json.isContainerNode() ? json : objectMapper.getNodeFactory().objectNode();
            return Response
                    .status(HTTP_OK)
                    .entity(jsonContainer.toString())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                    .header("Access-Control-Allow-Methods", "POST")
                    .build();
        } catch (IOException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
