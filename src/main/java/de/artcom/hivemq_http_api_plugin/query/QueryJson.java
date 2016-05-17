package de.artcom.hivemq_http_api_plugin.query;

class QueryJson implements IQuery {
    public String topic;

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public int getDepth() {
        return -1;
    }

    @Override
    public boolean getFlatten() {
        return false;
    }
}
