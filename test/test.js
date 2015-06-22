"use strict";

const chai = require("chai");
const mqtt = require("mqtt");
const Promise = require("bluebird");
const request = require("request-promise");
const _ = require("lodash");

chai.use(require("chai-as-promised"));
const expect = chai.expect;

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

  function publish(topic, value) {
    return client.publishAsync(topic, value, { retain: true, qos: 2 });
  }

  before(function(done) {
    client = Promise.promisifyAll(mqtt.connect(`mqtt://${BROKER_URL}`));
    client.on("connect", done);
  });

  beforeEach(function() {
    this.prefix = `test/hivemq-api-${Date.now()}`;
    this.missingTopic = `${this.prefix}/does-not-exist`;
    this.data = [
      { topic: `${this.prefix}/topic1`, payload: "foo" },
      { topic: `${this.prefix}/topic2`, payload: "bar" }
    ];

    return Promise.all(_.map(this.data, ({ topic, payload }) =>
      publish(topic, payload)
    ));
  });

  afterEach(function() {
    return Promise.all(_.map(this.data, ({ topic }) =>
      publish(topic, null)
    ));
  });

  after(function() {
    client.end();
  });

  describe("Single Queries", function() {
    it("should return the payload of a topic", function() {
      const { topic, payload } = this.data[0];
      const query = singleQuery(topic);
      return expect(query).to.eventually.deep.equal({ topic, payload });
    });

    it("should return error for inexistent topic", function() {
      return singleQuery(this.missingTopic).then(() => {
        throw new chai.AssertionError("Promise was expected to be rejected.");
      }, (error) => {
        expect(error.response.statusCode).to.equal(404);
        expect(error.response.body).to.deep.equal({
          topic: this.missingTopic,
          error: "NOT_FOUND"
        });
      });
    });

    it("should return error for unpublished topic", function() {
      const { topic } = this.data[0];

      return publish(topic, null).then(() => {
        return singleQuery(topic);
      }).then(() => {
        throw new chai.AssertionError("Promise was expected to be rejected.");
      }, (error) => {
        expect(error.response.statusCode).to.equal(404);
        expect(error.response.body).to.deep.equal({
          topic: topic,
          error: "NOT_FOUND"
        });
      });
    });

    describe("with Depth Parameter", function() {
      it("should return the payload of immediate children", function() {
        return singleQuery(this.prefix, 1).then((result) => {
          expect(result).to.contain.all.keys(["topic", "children"]);
          expect(result.topic).to.equal(this.prefix);
          expect(result.children).to.have.length(2);
          expect(result.children).to.include(this.data[0]);
          expect(result.children).to.include(this.data[1]);
        });
      });

      it("should return the payload of deeper children", function() {
        const parent = this.data[1];
        const child = { topic: parent.topic + "/deepTopic", payload: "baz" };
        return publish(child.topic, child.payload).then(() => {
          return singleQuery(this.prefix, 2);
        }).then((result) => {
          expect(result).to.contain.all.keys(["topic", "children"]);
          expect(result.topic).to.equal(this.prefix);
          expect(result.children).to.have.length(2);
          expect(result.children).to.include(this.data[0]);
          expect(result.children).to.include(Object.assign({}, this.data[1], {
            children: [child]
          }));
        });
      });
    });
  });

  describe("Batch Queries", function() {
    it("should return the values of multiple topics", function() {
      const topics = _.map(this.data, "topic");
      const query = batchQuery(topics);
      return expect(query).to.eventually.deep.equal(this.data);
    });

    it("should return values and errors for multiple topics", function() {
      const topics = [this.data[0].topic, this.missingTopic];
      const query = batchQuery(topics);
      return expect(query).to.eventually.deep.equal([
        this.data[0],
        { topic: this.missingTopic, error: "NOT_FOUND" }
      ]);
    });
  });
});
