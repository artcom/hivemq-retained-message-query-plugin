"use strict";

const chai = require("chai");
const mqtt = require("mqtt");
const Promise = require("bluebird");
const request = require("request-promise");
const _ = require("lodash");

chai.use(require("chai-as-promised"));
const expect = chai.expect;

const BROKER_URL = process.env.BROKER || "localhost";

function postQuery(json) {
  return request.post(`http://${BROKER_URL}:8080/query`, { json });
}

describe("HTTP API", function() {
  let client;

  before(function(done) {
    client = Promise.promisifyAll(mqtt.connect(`mqtt://${BROKER_URL}`));
    client.on("connect", done);
  });

  beforeEach(function() {
    this.published = [];
    this.publish = function(topic, value) {
      this.published.push(topic);
      return client.publishAsync(topic, value, { retain: true, qos: 2 });
    };

    this.topic = "hivemq-api-" + Date.now();
    this.prefix = "test/" + this.topic;
    return this.publish(this.prefix + "/topic1", "foo").then(() => {
      return this.publish(this.prefix + "/topic2", "bar");
    });
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
      const query = postQuery({ topic: this.prefix + "/topic1" });
      return expect(query).to.eventually.deep.equal({
        topic: this.prefix + "/topic1",
        payload: "foo"
      });
    });

    it("should return error for inexistent topic", function() {
      return postQuery({ topic: this.prefix + "/does-not-exist" }).then(() => {
        throw new chai.AssertionError("Promise was expected to be rejected.");
      }, (error) => {
        expect(error.response.statusCode).to.equal(404);
        expect(error.response.body).to.deep.equal({
          topic: this.prefix + "/does-not-exist",
          error: "NOT_FOUND"
        });
      });
    });

    it("should return error for unpublished topic", function() {
      return this.publish(this.prefix + "/topic1", null).then(() => {
        return postQuery({ topic: this.prefix + "/topic1" });
      }).then(() => {
        throw new chai.AssertionError("Promise was expected to be rejected.");
      }, (error) => {
        expect(error.response.statusCode).to.equal(404);
        expect(error.response.body).to.deep.equal({
          topic: this.prefix + "/topic1",
          error: "NOT_FOUND"
        });
      });
    });

    describe("with Depth Parameter", function() {
      it("should return empty result for intermediary topic", function() {
        const query = postQuery({ topic: this.prefix, depth: 0 });
        return expect(query).to.eventually.deep.equal({
          topic: this.prefix
        });
      });

      it("should return the payload of immediate children", function() {
        const query = postQuery({ topic: this.prefix, depth: 1 });
        return expect(query).to.eventually.deep.equal({
          topic: this.prefix,
          children: [
            { topic: this.prefix + "/topic1", payload: "foo" },
            { topic: this.prefix + "/topic2", payload: "bar" }
          ]
        });
      });

      it("should return the payload of deeper children", function() {
        const query = this.publish(
          this.prefix + "/topic2/deepTopic", "baz"
        ).then(() => {
          return postQuery({ topic: this.prefix, depth: 2 });
        });

        return expect(query).to.eventually.deep.equal({
          topic: this.prefix,
          children: [
            {
              topic: this.prefix + "/topic1",
              payload: "foo"
            },
            {
              topic: this.prefix + "/topic2",
              payload: "bar",
              children: [
                {
                  topic: this.prefix + "/topic2/deepTopic",
                  payload: "baz"
                }
              ]
            }
          ]
        });
      });
    });
  });

  describe("Batch Queries", function() {
    it("should return the values of multiple topics", function() {
      const query = postQuery([
        { topic: this.prefix + "/topic1" },
        { topic: this.prefix + "/topic2" }
      ]);

      return expect(query).to.eventually.deep.equal([
        { topic: this.prefix + "/topic1", payload: "foo" },
        { topic: this.prefix + "/topic2", payload: "bar" }
      ]);
    });

    it("should return values and errors for multiple topics", function() {
      const query = postQuery([
        { topic: this.prefix + "/topic1" },
        { topic: this.prefix + "/does-not-exist" }
      ]);

      return expect(query).to.eventually.deep.equal([
        { topic: this.prefix + "/topic1", payload: "foo" },
        { topic: this.prefix + "/does-not-exist", error: "NOT_FOUND" }
      ]);
    });

    it("should support different depth parameters", function() {
      const query = this.publish(this.prefix + "/topic1/child", "one").then(() => {
        return this.publish(this.prefix + "/topic2/child", "two");
      }).then(() => {
        return postQuery([
          { topic: this.prefix + "/topic1" },
          { topic: this.prefix + "/topic2", depth: 1 }
        ]);
      });

      return expect(query).to.eventually.deep.equal([
        {
          topic: this.prefix + "/topic1",
          payload: "foo"
        },
        {
          topic: this.prefix + "/topic2",
          payload: "bar",
          children: [
            {
              topic: this.prefix + "/topic2/child",
              payload: "two"
            }
          ]
        }
      ]);
    });
  });

  describe("Invalid Queries", function() {
    it("should return an error when topic is missing", function() {
      return postQuery({ invalid: "query" }).then(() => {
        throw new chai.AssertionError("Promise was expected to be rejected.");
      }, (error) => {
        expect(error.response.statusCode).to.equal(400);
        expect(error.response.body).to.deep.equal({
          error: "BAD_REQUEST",
          message: "The response body must be a JSON object with a 'topic' and optional 'depth' property, or a JSON array of such objects."
        });
      });
    });

    it("should return an error when topic has trailing slash", function() {
      return postQuery({ topic: "trailing/slash/" }).then(() => {
        throw new chai.AssertionError("Promise was expected to be rejected.");
      }, (error) => {
        expect(error.response.statusCode).to.equal(400);
        expect(error.response.body).to.deep.equal({
          topic: "trailing/slash/",
          error: "BAD_REQUEST",
          message: "The topic cannot end with a slash."
        });
      });
    });

    it("should return an error when using multiple wildcards", function() {
      return postQuery({ topic: "using/+/multiple/+/wildcards" }).then(() => {
        throw new chai.AssertionError("Promise was expected to be rejected.");
      }, (error) => {
        expect(error.response.statusCode).to.equal(400);
        expect(error.response.body).to.deep.equal({
          topic: "using/+/multiple/+/wildcards",
          error: "BAD_REQUEST",
          message: "The topic cannot contain more than one wildcard."
        });
      });
    });
  });

  describe("Wildcard Queries", function() {
    beforeEach(function() {
      return this.publish(this.prefix + "/topic1/child", "one").then(() => {
        return this.publish(this.prefix + "/topic2/child", "two");
      });
    });

    it("should return all children", function() {
      const query = postQuery({ topic: this.prefix + "/+" });
      return expect(query).to.eventually.deep.equal([
        { topic: this.prefix + "/topic1", payload: "foo" },
        { topic: this.prefix + "/topic2", payload: "bar" }
      ]);
    });

    it("should return all matching children", function() {
      const query = postQuery({ topic: this.prefix + "/+/child" });
      return expect(query).to.eventually.deep.equal([
        { topic: this.prefix + "/topic1/child", payload: "one" },
        { topic: this.prefix + "/topic2/child", payload: "two" }
      ]);
    });

    it("should return all matching deep children", function() {
      const query = this.publish(
        this.prefix + "/topic2/deep/child", "deep"
      ).then(() => {
        return postQuery({ topic: this.prefix + "/+/deep/child" });
      });

      return expect(query).to.eventually.deep.equal([
        { topic: this.prefix + "/topic2/deep/child", payload: "deep" }
      ]);
    });

    it("should support the depth parameter", function() {
      const query = postQuery({ topic: this.prefix + "/+", depth: 1 });
      return expect(query).to.eventually.deep.equal([
        {
          topic: this.prefix + "/topic1",
          payload: "foo",
          children: [
            {
              topic: this.prefix + "/topic1/child",
              payload: "one" }
          ]
        },
        {
          topic: this.prefix + "/topic2",
          payload: "bar",
          children: [
            {
              topic: this.prefix + "/topic2/child",
              payload: "two"
            }
          ]
        }
      ]);
    });

    it("should support leading wildcard", function() {
      const query = postQuery({ topic: "+/" + this.topic });
      return expect(query).to.eventually.deep.equal([
        { topic: this.prefix }
      ]);
    });
  });
});
