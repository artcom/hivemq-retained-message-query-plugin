package de.artcom.hivemq_http_api_plugin.query.results;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class LeadingSlashError extends Error {
    private final String topic;
    private final String message = "The topic cannot start with a slash.";

    public LeadingSlashError(String topic) {
        super(HTTP_BAD_REQUEST);
        this.topic = topic;
    }
}
