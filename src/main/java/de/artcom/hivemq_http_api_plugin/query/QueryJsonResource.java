package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.net.HttpURLConnection.HTTP_OK;

@Path("/json")
public class QueryJsonResource extends QueryResource {
    @Inject
    public QueryJsonResource(QueryProcessor queryProcessor) {
        super(queryProcessor);
    }

    @POST
    public Response post(String body) {
        IQueryResult result = QueryResultError.queryFormat();

        try {
            JsonNode json = objectMapper.readTree(body);

            if (json.isObject()) {
                Query query = extractQuery(json);
                result = singleQuery(query);
            } else if (json.isArray()) {
                List<Query> queries = StreamSupport.stream(json.spliterator(), false)
                        .map(QueryJsonResource::extractQuery)
                        .collect(Collectors.toList());
                result = batchQuery(queries);
            }
        } catch (IOException ignored) {
        }

        return createResponse(HTTP_OK, createPayload(result));
    }

    private static Query extractQuery(JsonNode json) {
        Query query = new Query();
        query.topic = json.get("topic").textValue();
        query.depth = -1;
        query.flatten = false;
        return query;
    }

    private String createPayload(IQueryResult result) {
        JsonNode json = objectMapper.getNodeFactory().objectNode();

        try {
            JsonNode plainJson = result.toPlainJson(objectMapper);
            if (plainJson.isContainerNode()) {
                json = plainJson;
            }
        } catch (IOException ignored) {
        }

        return json.toString();
    }
}
