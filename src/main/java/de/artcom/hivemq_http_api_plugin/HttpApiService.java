package de.artcom.hivemq_http_api_plugin;

import com.dcsquare.hivemq.spi.callback.CallbackPriority;
import com.dcsquare.hivemq.spi.callback.events.broker.OnBrokerStart;
import com.dcsquare.hivemq.spi.callback.events.broker.OnBrokerStop;
import com.dcsquare.hivemq.spi.callback.exception.BrokerUnableToStartException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Lists;
import de.artcom.hivemq_http_api_plugin.query.*;
import spark.Spark;

import javax.inject.Inject;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

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
            JsonNode json = objectMapper.readTree(body);

            if (json.isObject()) {
                Query query = objectMapper.readValue(body, Query.class);
                QueryResult result = singleQuery(query);
                response.status(result.getStatus());
                return result.toJSON(objectMapper);
            } else if (json.isArray()) {
                List<Query> queries = objectMapper.readValue(body, new TypeReference<List<Query>>() {});
                QueryResult result = batchQuery(queries);
                response.status(result.getStatus());
                return result.toJSON(objectMapper);
            }

            response.status(HTTP_BAD_REQUEST);
            return null;
        });

        Spark.exception(JsonMappingException.class, (mappingException, request, response) -> {
            QueryResult result = new QueryResultError(HTTP_BAD_REQUEST, ErrorMessage.JSON_FORMAT);

            try {
                response.status(HTTP_BAD_REQUEST);
                response.body(result.toJSON(objectMapper));
            } catch (JsonProcessingException processingException) {
                response.status(HTTP_INTERNAL_ERROR);
                response.body(null);
            }
        });

        Spark.after((request, response) -> {
            response.type("application/json; charset=utf-8");
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
}
