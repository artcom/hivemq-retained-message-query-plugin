package de.artcom.hivemq_http_api_plugin.query;

import org.jetbrains.annotations.Nullable;

class Query {
    @Nullable
    public String topic;
    public int depth;
    public boolean flatten;

    boolean isWildcardQuery() {
        return !(topic == null) && topic.contains("+");
    }
}
