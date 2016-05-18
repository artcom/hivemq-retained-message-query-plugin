package de.artcom.hivemq_http_api_plugin.query.exceptions;

public class TrailingSlashException extends QueryException {
    public TrailingSlashException(String topic) {
        super(topic);
    }
}
