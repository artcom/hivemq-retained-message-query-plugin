package com.artcom.hivemq_retained_message_query_extension.query.results;

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

    @Override
    public Stream<Result> flatten() {
        return Stream.of(this);
    }
}
