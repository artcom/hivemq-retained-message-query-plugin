package de.artcom.hivemq_http_api_plugin.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.query.exceptions.ParameterException;
import de.artcom.hivemq_http_api_plugin.query.exceptions.QueryException;

import javax.inject.Inject;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

@Path("/query")
public class QueryResource {
    private final ObjectMapper objectMapper;
    private final QueryProcessor queryProcessor;

    @Inject
    QueryResource(QueryProcessor queryProcessor) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());

        this.queryProcessor = queryProcessor;
    }

    @OPTIONS
    public static Response options() {
        return Response.ok("")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "POST")
                .build();
    }

    @POST
    public Response post(String body) {
        QueryResponse response = computeReponse(body);

        return Response
                .status(response.status)
                .entity(response.body)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "POST")
                .build();
    }

    private QueryResponse computeReponse(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);

            if (json.isArray()) {
                return processBatchQuery(Lists.newArrayList(json.elements()));
            } else if (json.isObject()) {
                return processSingleQuery(json);
            }

            return formatException(new ParameterException());
        } catch (IOException ignored) {
            return QueryResponse.error(HTTP_BAD_REQUEST, "The request body must be a JSON object", objectMapper);
        }
    }

    private QueryResponse processBatchQuery(List<JsonNode> queryJsons) {
        List<JsonNode> responseBodies = Lists.transform(queryJsons, (queryJson) -> processSingleQuery(queryJson).body);
        return QueryResponse.success(responseBodies, objectMapper);
    }

    private QueryResponse processSingleQuery(JsonNode queryJson) {
        try {
            Query query = parseQuery(queryJson);
            query.validate();

            if (query.isWildcardQuery()) {
                List<QueryResult> results = queryProcessor.processWildcardQuery(query);

                if (query.flatten) {
                    Iterable<QueryResult> flatResults = Iterables.concat(
                            Iterables.transform(results, QueryResult::flatten));
                    return formatResults(Lists.newArrayList(flatResults));
                } else {
                    return formatResults(results);
                }
            } else {
                QueryResult result = queryProcessor.processSingleQuery(query);

                if (query.flatten) {
                    return formatResults(result.flatten());
                } else {
                    return formatResult(result);
                }
            }
        } catch (QueryException exception) {
            return formatException(exception);
        }
    }

    private static Query parseQuery(JsonNode json) throws ParameterException {
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

    private QueryResponse formatResults(List<QueryResult> results) {
        Iterable<JsonNode> responseBodies = Lists.transform(results, (result) -> formatResult(result).body);
        return QueryResponse.success(responseBodies, objectMapper);
    }

    private QueryResponse formatResult(QueryResult result) {
        return QueryResponse.success(objectMapper.valueToTree(result));
    }

    private QueryResponse formatException(QueryException exception) {
        return exception.toQueryResponse(objectMapper);
    }
}
