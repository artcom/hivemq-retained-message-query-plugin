package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

import javax.ws.rs.Path;
import java.io.IOException;

import static java.net.HttpURLConnection.HTTP_OK;

@Path("/json")
public class QueryJsonResource extends Resource {
    @Inject
    public QueryJsonResource(QueryProcessor queryProcessor) {
        super(queryProcessor);
    }

    @Override
    Query parseQuery(JsonNode json) {
        try {
            Query query = new Query();
            query.topic = json.get("topic").textValue();
            query.depth = -1;
            return query;
        } catch (NullPointerException ignored) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    int getStatus(IQueryResult result) {
        return HTTP_OK;
    }

    @Override
    String getPayload(IQueryResult result) {
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
