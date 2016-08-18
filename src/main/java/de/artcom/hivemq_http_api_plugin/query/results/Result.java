package de.artcom.hivemq_http_api_plugin.query.results;

import java.util.stream.Stream;

public interface Result {
    int getStatus();
    Stream<Result> flatten();
}
