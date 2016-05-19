package de.artcom.hivemq_http_api_plugin.query.results;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class TrailingSlashError extends Error {
    private final String topic;
    private final String message = "The topic cannot end with a slash.";

    public TrailingSlashError(String topic) {
        super(HTTP_BAD_REQUEST);
        this.topic = topic;
    }
}
