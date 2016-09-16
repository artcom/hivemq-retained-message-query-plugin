package de.artcom.hivemq_retained_message_query_plugin.query.results;

import java.util.stream.Stream;

public interface Result {
    int getStatus();
    Stream<Result> flatten();
}
