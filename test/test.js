"use strict";

const expect = require("chai").expect;
const mqtt = require("mqtt");
const request = require("request-promise");
const _ = require("lodash");

const BROKER_URL = process.env.BROKER || "localhost";

function rawQuery(json) {
  return request.post(`http://${BROKER_URL}:8080/query`, { json });
}

function singleQuery(topic, depth=null) {
  const json = _.omit({ topic, depth }, _.isNull);
  return rawQuery(json);
}

function batchQuery(topics) {
  const json = _.map(topics, (topic) => ({ topic }));
  return rawQuery(json);
}

describe("HTTP API", function() {
  let client;

  before(function(done) {
    client = mqtt.connect(`mqtt://${BROKER_URL}`);
    client.on("connect", done);
  });

  beforeEach(function() {
    this.prefix = `test/hivemq-api-${Date.now()}`;
    this.missingTopic = `${this.prefix}/does-not-exist`;
    this.data = [
      { topic: `${this.prefix}/topic1`, payload: "foo" },
      { topic: `${this.prefix}/topic2`, payload: "bar" }
    ];

    _.forEach(this.data, ({ topic, payload }) => {
      client.publish(topic, payload, { retain: true });
    });
  });

  afterEach(function() {
    _.forEach(this.data, ({ topic }) => {
      client.publish(topic, null, { retain: true });
    });
  });

  after(function() {
    client.end();
  });

  describe("Single Queries", function() {
    it("should return the payload of a topic", function() {
      const { topic, payload } = this.data[0];

      return singleQuery(topic).then((result) => {
        expect(result).to.deep.equal({ topic, payload });
      });
    });

    it("should return error for inexistent topic", function() {
      return singleQuery(this.missingTopic).catch((error) => {
        expect(error.response.statusCode).to.equal(404);
        expect(error.response.body).to.deep.equal({
          topic: this.missingTopic,
          error: "NOT_FOUND"
        });
      });
    });
  });

  describe("Batch Queries", function() {
    it("should return the values of multiple topics", function() {
      const topics = _.map(this.data, "topic");

      return batchQuery(topics).then((results) => {
        expect(results).to.deep.equal(this.data);
      });
    });

    it("should return values and errors for multiple topics", function() {
      return batchQuery([this.data[0].topic, this.missingTopic]).then((results) => {
        expect(results).to.deep.equal([
          this.data[0],
          { topic: this.missingTopic, error: "NOT_FOUND" }
        ]);
      });
    });
  });
});
