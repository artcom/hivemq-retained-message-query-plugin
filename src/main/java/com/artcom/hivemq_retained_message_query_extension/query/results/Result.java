package com.artcom.hivemq_retained_message_query_extension.query.results;

import java.util.stream.Stream;

public interface Result {
    int getStatus();
    Stream<Result> flatten();
}
