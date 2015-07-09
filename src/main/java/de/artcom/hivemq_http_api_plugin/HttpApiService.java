package de.artcom.hivemq_http_api_plugin;

import com.dcsquare.hivemq.spi.callback.CallbackPriority;
import com.dcsquare.hivemq.spi.callback.events.broker.OnBrokerStart;
import com.dcsquare.hivemq.spi.callback.events.broker.OnBrokerStop;
import com.dcsquare.hivemq.spi.callback.exception.BrokerUnableToStartException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.query.*;
import spark.Response;
import spark.Spark;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

public class HttpApiService implements OnBrokerStart, OnBrokerStop {

    private final ObjectMapper objectMapper;
    private final QueryProcessor queryProcessor;

    @Inject
    public HttpApiService(QueryProcessor queryProcessor) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());

        this.queryProcessor = queryProcessor;
    }

    @Override
    public void onBrokerStart() throws BrokerUnableToStartException {
        Spark.port(8080);

        Spark.post("/query", (request, response) -> {
            String body = request.body();
            QueryResult result = new QueryResultError(BAD_REQUEST, ErrorMessage.JSON_FORMAT);

            try {
                JsonNode json = objectMapper.readTree(body);

                if (json.isObject()) {
                    Query query = objectMapper.readValue(body, Query.class);
                    result = singleQuery(query);
                } else if (json.isArray()) {
                    List<Query> queries = objectMapper.readValue(body, new TypeReference<List<Query>>() {});
                    result = batchQuery(queries);
                }
            } catch (IOException e) {
            }

            return createResponse(response, result);
        });
    }

    @Override
    public void onBrokerStop() {
        Spark.stop();
    }

    @Override
    public int priority() {
        return CallbackPriority.MEDIUM;
    }

    private QueryResult singleQuery(Query query) {
        return queryProcessor.process(query);
    }

    private QueryResult batchQuery(List<Query> queries) {
        List<QueryResult> results = Lists.transform(queries, this::singleQuery);
        return new QueryResultList(results);
    }

    private Object createResponse(Response response, QueryResult result) {
        try {
            response.status(result.getStatus().getStatusCode());
            response.type("application/json; charset=utf-8");
            return result.toJSON(objectMapper);
        } catch (JsonProcessingException e) {
            response.status(INTERNAL_SERVER_ERROR.getStatusCode());
            return null;
        }
    }
}
