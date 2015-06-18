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
  const missingTopic = `${prefix}/does-not-exist`;

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

  const rawQuery = function(json) {
    return request.post(`http://${brokerUrl}:8080/query`, { json });
  };

  const singleQuery = function(topic, depth=null) {
    const json = _.omit({ topic, depth }, _.isNull);
    return rawQuery(json);
  };

  const batchQuery = function(topics) {
    const json = _.map(topics, (topic) => ({ topic }));
    return rawQuery(json);
  };

  describe("Single Queries", function() {
    it("should return the payload of a topic", function() {
      const { topic, payload } = data[0];

      return singleQuery(topic).then((result) => {
        expect(result).to.deep.equal({ topic, payload });
      });
    });

    it("should return error for inexistent topic", function() {
      return singleQuery(missingTopic).catch((error) => {
        expect(error.response.statusCode).to.equal(404);
        expect(error.response.body).to.deep.equal({
          topic: missingTopic,
          error: "NOT_FOUND"
        });
      });
    });
  });

  describe("Batch Queries", function() {
    it("should return the values of multiple topics", function() {
      const topics = _.map(data, "topic");

      return batchQuery(topics).then((results) => {
        expect(results).to.deep.equal(data);
      });
    });

    it("should return values and errors for multiple topics", function() {
      return batchQuery([data[0].topic, missingTopic]).then((results) => {
        expect(results).to.deep.equal([
          data[0],
          { topic: missingTopic, error: "NOT_FOUND" }
        ]);
      });
    });
  });
});
