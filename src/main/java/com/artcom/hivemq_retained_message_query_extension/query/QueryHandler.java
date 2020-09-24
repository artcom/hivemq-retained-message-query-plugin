package com.artcom.hivemq_retained_message_query_extension.query;

import com.artcom.hivemq_retained_message_query_extension.RetainedMessageTree;
import com.artcom.hivemq_retained_message_query_extension.query.results.ParameterError;
import com.artcom.hivemq_retained_message_query_extension.query.results.Result;
import com.artcom.hivemq_retained_message_query_extension.query.results.ResultList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

public class QueryHandler implements HttpHandler {
    private static final @NotNull Logger log = LoggerFactory.getLogger(QueryHandler.class);

    private final ObjectMapper objectMapper;
    private final Processor processor;

    public QueryHandler(RetainedMessageTree retainedMessageTree) {
        objectMapper = new ObjectMapper();
        objectMapper.setVisibilityChecker(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(ANY)
                .withGetterVisibility(NONE)
                .withSetterVisibility(NONE)
                .withCreatorVisibility(NONE));

        this.processor = new Processor(retainedMessageTree);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST") && !exchange.getRequestMethod().equals("OPTIONS")) {
            log.error("Query has unsupported method: " + exchange.getRequestMethod());
            return;
        }

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST");

        if (exchange.getRequestMethod().equals("POST")) {
            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining("\n"));

            Result result = computeResult(body);

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");

            String responseBody = objectMapper.writeValueAsString(result);
            exchange.sendResponseHeaders(result.getStatus(), responseBody.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBody.getBytes());
            os.close();

            log.info("Query '" + body + "' from " + exchange.getRemoteAddress() + " processed with status " + result.getStatus());
        }
    }

    private Result computeResult(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);

            if (json.isArray()) {
                return processBatchQuery(Lists.newArrayList(json.elements()));
            } else if (json.isObject()) {
                return processSingleQuery(json);
            }
        } catch (IOException ignored) {
        }

        return new ParameterError();
    }

    private Result processBatchQuery(List<JsonNode> queryJsons) {
        return queryJsons.stream()
                .map(this::processSingleQuery)
                .collect(Collectors.toCollection(ResultList::new));
    }

    private Result processSingleQuery(JsonNode queryJson) {
        Query query;
        try {
            query = objectMapper.treeToValue(queryJson, Query.class);
        } catch (JsonProcessingException e) {
            return new ParameterError();
        }

        Result result = processor.processQuery(query);
        if (query.flatten) {
            return result.flatten().collect(Collectors.toCollection(ResultList::new));
        } else {
            return result;
        }
    }
}
