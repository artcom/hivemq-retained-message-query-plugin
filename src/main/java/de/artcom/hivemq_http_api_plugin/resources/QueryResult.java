package de.artcom.hivemq_http_api_plugin.resources;

import javax.ws.rs.core.Response;

public interface QueryResult {
    Response.Status getStatus();
}
