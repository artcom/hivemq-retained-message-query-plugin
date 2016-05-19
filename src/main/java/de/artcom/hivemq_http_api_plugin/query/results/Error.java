package de.artcom.hivemq_http_api_plugin.query.results;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class Error implements Result {
    private final int error;

    public Error(int error) {
        this.error = error;
    }

    @Override
    public int getStatus() {
        return error;
    }

    public ImmutableList<Result> flatten() {
        return ImmutableList.of(this);
    }
}
