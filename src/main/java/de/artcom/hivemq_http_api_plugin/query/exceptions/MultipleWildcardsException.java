package de.artcom.hivemq_http_api_plugin.query.exceptions;

public class MultipleWildcardsException extends QueryException {
    public MultipleWildcardsException(String topic) {
        super(topic);
    }
}
