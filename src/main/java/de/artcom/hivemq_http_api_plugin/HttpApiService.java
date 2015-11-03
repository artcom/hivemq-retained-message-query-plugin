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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

import de.artcom.hivemq_http_api_plugin.query.Query;
import de.artcom.hivemq_http_api_plugin.query.QueryProcessor;
import de.artcom.hivemq_http_api_plugin.query.QueryResult;
import de.artcom.hivemq_http_api_plugin.query.QueryResultError;
import de.artcom.hivemq_http_api_plugin.query.QueryResultList;
import spark.Spark;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static com.google.common.net.HttpHeaders.ORIGIN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

public class HttpApiService implements OnBrokerStart, OnBrokerStop {

    private final ObjectMapper objectMapper;
    private final QueryProcessor queryProcessor;

    private static final Logger log = LoggerFactory.getLogger(HttpApiService.class);

    @Inject
    public HttpApiService(QueryProcessor queryProcessor) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());

        this.queryProcessor = queryProcessor;
    }

    @Override
    public void onBrokerStart() throws BrokerUnableToStartException {
        Spark.port(8080);

        Spark.options("/*", (request, response) -> {
            String method = request.headers(ACCESS_CONTROL_REQUEST_METHOD);
            if (method != null) {
                response.header(ACCESS_CONTROL_ALLOW_METHODS, method);
            }

            String headers = request.headers(ACCESS_CONTROL_REQUEST_HEADERS);
            if (headers != null) {
                response.header(ACCESS_CONTROL_ALLOW_HEADERS, headers);
            }

            response.status(HTTP_OK);
            return "";
        });

        Spark.before((request, response) -> {
            if (request.headers(ORIGIN) != null) {
                response.header(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            }

            response.type("application/json; charset=utf-8");
        });

        Spark.post("/query", (request, response) -> {
            long startTime = System.nanoTime();

            String body = request.body();
            JsonNode json = objectMapper.readTree(body);
            QueryResult result;

            if (json.isObject()) {
                Query query = objectMapper.readValue(body, Query.class);
                result = singleQuery(query);
            } else if (json.isArray()) {
                List<Query> queries = objectMapper.readValue(body, new TypeReference<List<Query>>() {});
                result = batchQuery(queries);
            } else {
                result = QueryResultError.queryFormat();
            }
            response.status(result.getStatus());

            float responseTime = (System.nanoTime() - startTime) / 1000000.0f;
            logRequest(json.get("topic").asText(), responseTime);

            return result.toJSON(objectMapper);
        });

        Spark.exception(JsonMappingException.class, (mappingException, request, response) -> {
            QueryResultError error = QueryResultError.queryFormat();

            try {
                response.status(error.getStatus());
                response.body(error.toJSON(objectMapper));
            } catch (JsonProcessingException processingException) {
                response.status(HTTP_INTERNAL_ERROR);
                response.body("");
            }

            logError(error.getMessage().get(), request.body());
        });
    }

    private QueryResult singleQuery(Query query) {
        return queryProcessor.process(query);
    }

    private QueryResult batchQuery(List<Query> queries) {
        List<QueryResult> results = Lists.transform(queries, this::singleQuery);
        return new QueryResultList(results);
    }

    private void logRequest(String topic, float responseTime) {
        ObjectNode logNode = objectMapper.createObjectNode();
        logNode.put("name", "hivemq-http-api-plugin");
        logNode.put("topic", topic);
        logNode.put("responseTime", responseTime);
        log.info(logNode.toString());
    }

    private void logError(String errorMessage, String requestBody) {
        ObjectNode logNode = objectMapper.createObjectNode();
        logNode.put("name", "hivemq-http-api-plugin");
        logNode.put("error", errorMessage);
        logNode.put("request", requestBody);
        log.error(logNode.toString());
    }

    @Override
    public void onBrokerStop() {
        Spark.stop();
    }

    @Override
    public int priority() {
        return CallbackPriority.MEDIUM;
    }
}
