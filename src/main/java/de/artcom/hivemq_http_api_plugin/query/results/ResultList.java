package de.artcom.hivemq_http_api_plugin.query.results;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

public class ResultList extends ArrayList<Result> implements Result {
    public ResultList(List<Result> results) {
        super(results);
    }

    @Override
    public int getStatus() {
        return HTTP_OK;
    }

    @Override
    public ImmutableList<Result> flatten() {
        ImmutableList.Builder<Result> builder = ImmutableList.builder();
        forEach((result) -> builder.addAll(result.flatten()));
        return builder.build();
    }
}
