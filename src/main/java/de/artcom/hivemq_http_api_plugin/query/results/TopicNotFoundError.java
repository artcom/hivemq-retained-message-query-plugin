package de.artcom.hivemq_http_api_plugin.query.results;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

public class TopicNotFoundError extends Error {
    private final String topic;

    public TopicNotFoundError(String topic) {
        super(HTTP_NOT_FOUND);
        this.topic = topic;
    }
}
