package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
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
    JsonNode formatResult(QueryResult result) {
        return objectMapper.valueToTree(result);
    }

    @Override
    JsonNode formatException(QueryException exception) {
        if (exception instanceof LeadingSlashException) {
            return createErrorObject(HTTP_BAD_REQUEST, "The topic cannot start with a slash.");
        } else if (exception instanceof TrailingSlashException) {
            return createErrorObject(HTTP_BAD_REQUEST, "The topic cannot end with a slash.");
        } else if (exception instanceof MultipleWildcardsException) {
            return createErrorObject(HTTP_BAD_REQUEST, "The topic cannot contain more than one wildcard.");
        } else if (exception instanceof ParameterException) {
            return createErrorObject(HTTP_BAD_REQUEST, "The request body must be a JSON object with a 'topic'" +
                    " and optional 'depth' property, or a JSON array of such objects.");
        } else if(exception instanceof TopicNotFoundException) {
            return createErrorObject(HTTP_NOT_FOUND, "The topic does not exist.");
        }

        return createErrorObject(HTTP_INTERNAL_ERROR, "Internal error");
    }
}
