package de.artcom.hivemq_http_api_plugin.query.results;

import com.google.common.collect.ImmutableList;

public interface Result {
    public int getStatus();
    public abstract ImmutableList<Result> flatten();
}
