package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import de.artcom.hivemq_http_api_plugin.query.exceptions.*;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@Path("/json")
public class QueryJsonResource extends Resource {
    @Inject
    public QueryJsonResource(QueryProcessor queryProcessor) {
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
            query.depth = -1;
            return query;
        } catch (NullPointerException ignored) {
            throw new ParameterException();
        }
    }

    @Override
    JsonNode formatResult(QueryResult result) {
        ObjectNode object = objectMapper.getNodeFactory().objectNode();
        List<QueryResult> children = result.getChildren();

        if (children != null) {
            children.forEach((child) -> addResultToObject(child, object));
        }

        return object;
    }

    private void addResultToObject(QueryResult child, ObjectNode object) {
        String[] topicNames = child.getTopic().split("/");
        String topicName = topicNames[topicNames.length - 1];

        try {
            object.set(topicName, resultToJson(child));
        } catch (IOException ignored) {
        }
    }

    private JsonNode resultToJson(QueryResult result) throws IOException {
        List<QueryResult> children = result.getChildren();

        if (children != null) {
            ObjectNode object = objectMapper.getNodeFactory().objectNode();
            children.forEach((child) -> addResultToObject(child, object));
            return object;
        } else {
            return objectMapper.readTree(result.getPayload());
        }
    }

    @Override
    JsonNode formatException(QueryException exception) {
        return objectMapper.getNodeFactory().objectNode();
    }
}
