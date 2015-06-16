"use strict";

const expect = require("chai").expect();
const mqtt = require("mqtt");
const request = require("request-promise");
const _ = require("lodash");

describe("Query API", function() {
  const brokerUrl = process.env.BROKER || "localhost";
  const prefix = `test/hivemq-api-${Date.now()}`;
  const topics = {
    [`${prefix}/topic1`]: "foo",
    [`${prefix}/topic2`]: "bar"
  };

  let client;

  before(function(done) {
    client = mqtt.connect(`mqtt://${brokerUrl}`);
    client.on("connect", done);
  });

  beforeEach(function() {
    _.forEach(topics, (value, topic) => {
      client.publish(topic, value, { retain: true });
    });
  });

  afterEach(function() {
    _.forEach(topics, (value, topic) => {
      client.publish(topic, null, { retain: true });
    });
  });

  after(function() {
    client.end();
  });

  const sendQuery = function(query) {
    return request.post(`http://${brokerUrl}/${query}`, { json: query });
  };

  it("should return the value of a topic", function() {
    const [topic, value] = _(topics).pairs().first();
    return sendQuery({ topic }).then((result) => {
      expect(result).to.equal({ topic, value });
    });
  });
});
