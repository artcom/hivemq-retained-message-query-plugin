package de.artcom.hivemq_http_api_plugin.query.exceptions;

public class TopicNotFoundException extends QueryException {
    public TopicNotFoundException(String topic) {
        super(topic);
    }
}
