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
    this.published = [];

    this.publish = function(topic, value) {
      this.published.push(topic);
      return client.publishAsync(topic, value, { retain: true, qos: 2 });
    };

    return Promise.all(_.map(this.data, ({ topic, payload }) =>
      this.publish(topic, payload)
    ));
  });

  afterEach(function() {
    return Promise.all(_.map(this.published, (topic) =>
      this.publish(topic, null)
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

      return this.publish(topic, null).then(() => {
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
      it("should return empty result for intermediary topic", function() {
        const topic = this.prefix;
        const query = singleQuery(topic, 0);
        return expect(query).to.eventually.deep.equal({ topic });
      });

      it("should return the payload of immediate children", function() {
        const topic = this.prefix;
        const query = singleQuery(topic, 1);
        return expect(query).to.eventually.deep.equal({
          topic,
          children: this.data
        });
      });

      it("should return the payload of deeper children", function() {
        const parent = this.data[1];
        const child = { topic: parent.topic + "/deepTopic", payload: "baz" };

        const query = this.publish(child.topic, child.payload).then(() => {
          return singleQuery(this.prefix, 2);
        });

        return expect(query).to.eventually.deep.equal({
          topic: this.prefix,
          children: [
            this.data[0],
            _.assign({}, this.data[1], {
              children: [child]
            })
          ]
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

  describe("Invalid Queries", function() {
    it("should return an error", function() {
      return rawQuery({ invalid: "query" }).then(() => {
        throw new chai.AssertionError("Promise was expected to be rejected.");
      }, (error) => {
        expect(error.response.statusCode).to.equal(400);
        expect(error.response.body).to.deep.equal({
          error: "BAD_REQUEST"
        });
      });
    });
  });

  describe("Wildcard Queries", function() {
    beforeEach(function() {
      this.child1 = `${this.prefix}/topic1/child`;
      this.child2 = `${this.prefix}/topic2/child`;

      return this.publish(this.child1, "one").then(() => {
        return this.publish(this.child2, "two");
      });
    });

    it("should return all children", function() {
      const query = singleQuery(`${this.prefix}/+`);
      return expect(query).to.eventually.deep.equal(this.data);
    });

    it("should return all matching children", function() {
      const query = singleQuery(`${this.prefix}/+/child`);
      return expect(query).to.eventually.deep.equal([
        { topic: this.child1, payload: "one" },
        { topic: this.child2, payload: "two" }
      ]);
    });

    it("should return all matching deep children", function() {
      this.deepChild = `${this.prefix}/topic2/deep/child`;

      const query = this.publish(this.deepChild, "deep").then(() => {
        return singleQuery(`${this.prefix}/+/deep/child`);
      });

      return expect(query).to.eventually.deep.equal([
        { topic: this.deepChild, payload: "deep" }
      ]);
    });

    it("should support the depth parameter", function() {
      const query = singleQuery(`${this.prefix}/+`, 1);
      return expect(query).to.eventually.deep.equal([
        _.assign({}, this.data[0], {
          children: [{ topic: this.child1, payload: "one" }]
        }),
        _.assign({}, this.data[1], {
          children: [{ topic: this.child2, payload: "two" }]
        })
      ]);
    });
  });
});
