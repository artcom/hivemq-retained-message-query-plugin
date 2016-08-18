package de.artcom.hivemq_http_api_plugin.query.results;

import java.util.stream.Stream;

public interface Result {
    public int getStatus();
    public abstract Stream<Result> flatten();
}
