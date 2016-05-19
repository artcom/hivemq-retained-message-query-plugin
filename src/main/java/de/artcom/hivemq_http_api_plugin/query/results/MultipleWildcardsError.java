package de.artcom.hivemq_http_api_plugin.query.results;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class MultipleWildcardsError extends Error {
    private final String topic;
    private final String message = "The topic cannot contain more than one wildcard.";

    public MultipleWildcardsError(String topic) {
        super(HTTP_BAD_REQUEST);
        this.topic = topic;
    }
}
