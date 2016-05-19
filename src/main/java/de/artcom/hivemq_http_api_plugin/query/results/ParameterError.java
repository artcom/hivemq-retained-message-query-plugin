package de.artcom.hivemq_http_api_plugin.query.results;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class ParameterError extends Error {
    private final String message = "The request body must be a JSON object with a 'topic' " +
            "and optional 'depth' property, or a JSON array of such objects.";

    public ParameterError() {
        super(HTTP_BAD_REQUEST);
    }
}
