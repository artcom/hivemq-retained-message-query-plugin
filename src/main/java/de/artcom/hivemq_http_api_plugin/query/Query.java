package de.artcom.hivemq_http_api_plugin.query;

import de.artcom.hivemq_http_api_plugin.query.exceptions.LeadingSlashException;
import de.artcom.hivemq_http_api_plugin.query.exceptions.MultipleWildcardsException;
import de.artcom.hivemq_http_api_plugin.query.exceptions.TrailingSlashException;
import org.apache.commons.lang3.StringUtils;

class Query {
    public String topic;
    public int depth;
    public boolean flatten;

    boolean isWildcardQuery() {
        return topic.contains("+");
    }

    void validate() throws LeadingSlashException, TrailingSlashException, MultipleWildcardsException {
        if (topic.startsWith("/")) {
            throw new LeadingSlashException(topic);
        }

        if (topic.endsWith("/")) {
            throw new TrailingSlashException(topic);
        }

        if (StringUtils.countMatches(topic, '+') > 1) {
            throw new MultipleWildcardsException(topic);
        }
    }
}
