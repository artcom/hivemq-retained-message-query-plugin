package de.artcom.hivemq_http_api_plugin.query;

interface IQuery {
    String getTopic();

    int getDepth();

    boolean getFlatten();
}
