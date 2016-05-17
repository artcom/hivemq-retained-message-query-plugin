package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Path("/query")
public class QueryResource extends Resource {
    @Inject
    QueryResource(QueryProcessor queryProcessor) {
        super(queryProcessor);
    }

    @Override
    Query parseQuery(JsonNode json) {
        try {
            Query query = new Query();
            query.topic = json.get("topic").textValue();

            if (json.has("depth")) {
                query.depth = json.get("depth").asInt();
            }

            if (json.has("flatten")) {
                query.flatten = json.get("flatten").asBoolean();
            }

            return query;
        } catch (NullPointerException ignored) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    int getStatus(IQueryResult result) {
        return result.getStatus();
    }

    @Override
    String getPayload(IQueryResult result) {
        return result.toJson(objectMapper).toString();
    }
}
