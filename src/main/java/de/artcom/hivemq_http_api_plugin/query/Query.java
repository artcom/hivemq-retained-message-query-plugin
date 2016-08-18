package de.artcom.hivemq_http_api_plugin.query;

import de.artcom.hivemq_http_api_plugin.query.results.Error;
import de.artcom.hivemq_http_api_plugin.query.results.LeadingSlashError;
import de.artcom.hivemq_http_api_plugin.query.results.TrailingSlashError;
import org.jetbrains.annotations.Nullable;

class Query {
    @Nullable
    public String topic;
    public int depth;
    public boolean flatten;

    boolean isWildcardQuery() {
        return !(topic == null) && topic.contains("+");
    }

    Error validate() {
        if (topic != null && topic.startsWith("/")) {
            return new LeadingSlashError(topic);
        }

        if (topic != null && topic.endsWith("/")) {
            return new TrailingSlashError(topic);
        }

        return null;
    }
}
