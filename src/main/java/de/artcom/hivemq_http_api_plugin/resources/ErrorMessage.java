package de.artcom.hivemq_http_api_plugin.resources;

public class ErrorMessage {
    public static final String JSON_FORMAT = "The response body must be a JSON object with a 'topic' and optional 'depth' property, or a JSON array of such objects.";
    public static final String MULTIPLE_WILDCARDS = "The topic cannot contain more than one wildcard.";
    public static final String TRAILING_SLASH = "The topic cannot end with a slash.";
    public static final String NEGATIVE_DEPTH = "The depth parameter cannot be negative.";
}
