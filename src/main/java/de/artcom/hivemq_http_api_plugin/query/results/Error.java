package de.artcom.hivemq_http_api_plugin.query.results;

import java.util.stream.Stream;

public class Error implements Result {
    private final int error;

    public Error(int error) {
        this.error = error;
    }

    @Override
    public int getStatus() {
        return error;
    }

    public Stream<Result> flatten() {
        return Stream.of(this);
    }
}
