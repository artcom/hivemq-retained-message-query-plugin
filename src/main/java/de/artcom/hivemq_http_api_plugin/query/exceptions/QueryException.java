package de.artcom.hivemq_http_api_plugin.query.exceptions;

public class QueryException extends Exception {
    public final String topic;

    QueryException() {
        topic = null;
    }

    QueryException(String topic) {
        this.topic = topic;
    }
}
