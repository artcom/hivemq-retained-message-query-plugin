package com.artcom.hivemq_retained_message_query_plugin.query;

import com.artcom.hivemq_retained_message_query_plugin.RetainedMessageQueryMain;
import com.artcom.hivemq_retained_message_query_plugin.query.results.ParameterError;
import com.artcom.hivemq_retained_message_query_plugin.query.results.Result;
import com.artcom.hivemq_retained_message_query_plugin.query.results.ResultList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.artcom.hivemq_retained_message_query_plugin.RetainedMessageTree;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

public class QueryHandler extends AbstractHandler {
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
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        if(!request.getPathInfo().equals("/query")) {
            return;
        }

        if(!request.getMethod().equals("POST") && !request.getMethod().equals("OPTIONS")) {
            log.error("Query has unsupported method: " + request.getMethod());
            return;
        }

        httpServletResponse.addHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        httpServletResponse.addHeader("Access-Control-Allow-Methods", "POST");
        request.setHandled(true);

        if(request.getMethod().equals("POST")) {
            String body = request.getReader().lines().collect(Collectors.joining("\n"));
            Result result = computeResult(body);
            objectMapper.writeValue(httpServletResponse.getWriter(), result);
            httpServletResponse.setStatus(result.getStatus());
            httpServletResponse.addHeader("Content-Type", "application/json; charset=utf-8");

            log.info("Query '" + body + "' from " + request.getRemoteAddr() + " processed with status " + httpServletResponse.getStatus());
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
