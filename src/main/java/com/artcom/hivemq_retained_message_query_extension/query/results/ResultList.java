package com.artcom.hivemq_retained_message_query_extension.query.results;

import java.util.ArrayList;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_OK;

public class ResultList extends ArrayList<Result> implements Result {
    @Override
    public int getStatus() {
        return HTTP_OK;
    }

    @Override
    public Stream<Result> flatten() {
        return stream().flatMap(Result::flatten);
    }
}
