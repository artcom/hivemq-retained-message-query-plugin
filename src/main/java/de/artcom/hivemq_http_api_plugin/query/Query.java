package de.artcom.hivemq_http_api_plugin.query;

import de.artcom.hivemq_http_api_plugin.query.results.Error;
import de.artcom.hivemq_http_api_plugin.query.results.LeadingSlashError;
import de.artcom.hivemq_http_api_plugin.query.results.TrailingSlashError;

class Query {
    public String topic;
    public int depth;
    public boolean flatten;

    boolean isWildcardQuery() {
        return topic.contains("+");
    }

    Error validate() {
        if (topic.startsWith("/")) {
            return new LeadingSlashError(topic);
        }

        if (topic.endsWith("/")) {
            return new TrailingSlashError(topic);
        }

        return null;
    }
}
