"use strict";

const expect = require("chai").expect;
const mqtt = require("mqtt");
const request = require("request-promise");
const _ = require("lodash");

describe("HTTP API", function() {
  const brokerUrl = process.env.BROKER || "localhost";
  const prefix = `test/hivemq-api-${Date.now()}`;
  const data = [
    { topic: `${prefix}/topic1`, payload: "foo" },
    { topic: `${prefix}/topic2`, payload: "bar" }
  ];

  let client;

  before(function(done) {
    client = mqtt.connect(`mqtt://${brokerUrl}`);
    client.on("connect", done);
  });

  beforeEach(function() {
    _.forEach(data, ({ topic, payload }) => {
      client.publish(topic, payload, { retain: true });
    });
  });

  afterEach(function() {
    _.forEach(data, ({ topic }) => {
      client.publish(topic, null, { retain: true });
    });
  });

  after(function() {
    client.end();
  });

  const sendQuery = function(topic) {
    const query = _.isArray(topic) ?
                  _.map(topic, (t) => ({ topic: t })) :
                  { topic };
    return request.post(`http://${brokerUrl}:8080/query`, { json: query });
  };

  it("should return the payload of a topic", function() {
    const { topic, payload } = data[0];

    return sendQuery(topic).then((result) => {
      expect(result).to.deep.equal({ topic, payload });
    });
  });

  it("should return error for inexistent topic", function() {
    const topic = `${prefix}/does-not-exist`;

    return sendQuery(topic).catch((error) => {
      expect(error.response.statusCode).to.equal(404);
      expect(error.response.body).to.deep.equal({
        topic,
        error: "NOT_FOUND"
      });
    });
  });

  it("should return the values of multiple topics", function() {
    const topics = _.map(data, "topic");

    return sendQuery(topics).then((results) => {
      expect(results).to.deep.equal(data);
    });
  });
});
