package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.artcom.hivemq_http_api_plugin.query.exceptions.*;

import javax.inject.Inject;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

@Path("/query")
public class QueryResource extends Resource {
    @Inject
    QueryResource(QueryProcessor queryProcessor) {
        super(queryProcessor);
    }

    @OPTIONS
    public Response options() {
        return super.options();
    }

    @POST
    public Response post(String body) {
        return super.post(body);
    }

    @Override
    Query parseQuery(JsonNode json) throws ParameterException {
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
            throw new ParameterException();
        }
    }

    @Override
    QueryResponse formatResult(QueryResult result, Query query) {
        if (query.flatten) {
            return QueryResponse.success(objectMapper.valueToTree(result.flatten()));
        } else {
            return QueryResponse.success(objectMapper.valueToTree(result));
        }
    }

    @Override
    QueryResponse formatException(QueryException exception) {
        return exception.toQueryResponse(objectMapper);
    }
}
