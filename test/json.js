"use strict";

const chai = require("chai");
const mqtt = require("mqtt");
const Promise = require("bluebird");
const request = require("request-promise");
const _ = require("lodash");

chai.use(require("chai-as-promised"));
const expect = chai.expect;

const QUERY_URL = (process.env.HTTP_BROKER_URI || "http://localhost:8080") + "/json";
const TCP_BROKER_URI = process.env.TCP_BROKER_URI || "tcp://localhost";

function postQuery(json, additionalOptions={}) {
  const options = _.assign({ json }, additionalOptions);
  return request.post(QUERY_URL, options);
}

function postErrorQuery(json) {
  return postQuery(json, { simple: false, resolveWithFullResponse: true });
}

describe("Json API", function() {
  let client;

  before(function(done) {
    client = Promise.promisifyAll(mqtt.connect(TCP_BROKER_URI));
    client.on("connect", done);
  });

  beforeEach(function() {
    this.unpublishAll = {};

    this.publish = function(data) {
      _.assign(this.unpublishAll, _.mapValues(data, _.constant(null)));
      return Promise.all(_.map(data, (payload, topic) =>
        client.publishAsync(topic, payload, { retain: true, qos: 2 })
      ));
    };

    this.topic = "hivemq-api-" + Date.now();
    this.prefix = "test/" + this.topic;

    return this.publish({
      [this.prefix + "/topic1"]: "\"payload1\"",
      [this.prefix + "/topic1/topic1"]: "\"payload11\"",
      [this.prefix + "/topic1/topic2"]: "\"payload12\"",
      [this.prefix + "/topic1/topic3"]: "\"payload13\"",
      [this.prefix + "/topic1/topic3/topic1"]: "\"payload131\"",
      [this.prefix + "/topic2"]: "\"payload2\"",
      [this.prefix + "/topic2/topic1"]: "\"payload21\"",
      [this.prefix + "/topic2/topic2"]: "\"payload22\"",
      [this.prefix + "/topic2/topic3"]: "invalid-json",
      [this.prefix + "/topic2/topic3/topic1"]: "\"payload231\"",
      [this.prefix + "/topic3"]: "{\"key1\":\"value1\", \"key2\":[1, 2, 3]}",
    });
  });

  afterEach(function() {
    this.publish(this.unpublishAll);
  });

  after(function() {
    client.end();
  });

  describe("Single Queries", function() {
    it("should return multi-level hierarchy of a topic", function() {
      const query = postQuery({ topic: this.prefix });
      return expect(query).to.eventually.deep.equal({
        topic1: {
          topic1: "payload11",
          topic2: "payload12",
          topic3: {
            topic1: "payload131"
          }
        },
        topic2: {
          topic1: "payload21",
          topic2: "payload22",
          topic3: {
            topic1: "payload231"
          }
        },
        topic3: {
          key1: "value1",
          key2: [1, 2, 3]
        }
      });
    });

    it("should return empty object for leaf topic", function() {
      const query = postQuery({ topic: this.prefix + "/topic1/topic1" });

      return expect(query).to.eventually.deep.equal({});
    });

    it("should return empty object for non-existing topic", function() {
      const query = postQuery({ topic: this.prefix + "/does-not-exist" });

      return expect(query).to.eventually.deep.equal({});
    });
  });

  describe("Batch Queries", function() {
    it("should return the values of multiple topics", function() {
      const query = postQuery([
        { topic: this.prefix + "/topic1" },
        { topic: this.prefix + "/topic2" }
      ]);

      return expect(query).to.eventually.deep.equal([
        {
          topic1: "payload11",
          topic2: "payload12",
          topic3: {
            topic1: "payload131"
          }
        },
        {
          topic1: "payload21",
          topic2: "payload22",
          topic3: {
            topic1: "payload231"
          }
        }
      ]);
    });

    it("should return values and empty objects for multiple topics", function() {
      const query = postQuery([
        { topic: this.prefix + "/topic1" },
        { topic: this.prefix + "/does-not-exist" }
      ]);

      return expect(query).to.eventually.deep.equal([
        {
          topic1: "payload11",
          topic2: "payload12",
          topic3: {
            topic1: "payload131"
          }
        },
        {
        }
      ]);
    });

    it("should support wildcard queries", function() {
      const query = postQuery([
        { topic: this.prefix + "/+" },
        { topic: this.prefix + "/topic2" }
      ]);

      return expect(query).to.eventually.deep.equal([
        [
          {
            topic1: "payload11",
            topic2: "payload12",
            topic3: {
              topic1: "payload131"
            }
          },
          {
            topic1: "payload21",
            topic2: "payload22",
            topic3: {
              topic1: "payload231"
            }
          },
            {
              "key1": "value1",
              "key2": [1, 2, 3]
            }
        ],
        {
          topic1: "payload21",
          topic2: "payload22",
          topic3: {
            topic1: "payload231"
          }
        }
      ]);
    });

    it("should support wildcard queries without results", function() {
      const query = postQuery([
        { topic: this.prefix + "/+/does-not-exist" },
        { topic: this.prefix + "/topic2" }
      ]);

      return expect(query).to.eventually.deep.equal([
        [],
        {
          topic1: "payload21",
          topic2: "payload22",
          topic3: {
            topic1: "payload231"
          }
        }
      ]);
    });
  });

  describe("Wildcard Queries", function() {
    it("should return all children", function() {
      const query = postQuery({ topic: this.prefix + "/+" });
      return expect(query).to.eventually.deep.equal([
        {
          topic1: "payload11",
          topic2: "payload12",
          topic3: {
            topic1: "payload131"
          }
        },
        {
          topic1: "payload21",
          topic2: "payload22",
          topic3: {
            topic1: "payload231"
          }
        },
        {
          key1: "value1",
          key2: [1, 2, 3]
        }
      ]);
    });

    it("should return empty array when no topics match", function() {
      const query = postQuery({ topic: this.prefix + "/+/does-not-exist" });
      return expect(query).to.eventually.deep.equal([]);
    });

    it("should return all matching children", function() {
      const query = postQuery({ topic: this.prefix + "/+/topic3" });
      return expect(query).to.eventually.deep.equal([
        { topic1: "payload131" },
        { topic1: "payload231" }
      ]);
    });

    it("should support leading wildcard", function() {
      const query = postQuery({ topic: "+/" + this.topic });
      return expect(query).to.eventually.deep.equal([
        {
          topic1: {
            topic1: "payload11",
            topic2: "payload12",
            topic3: {
              topic1: "payload131"
            }
          },
          topic2: {
            topic1: "payload21",
            topic2: "payload22",
            topic3: {
              topic1: "payload231"
            }
          },
          topic3: {
            key1: "value1",
            key2: [1, 2, 3]
          }
        }
      ]);
    });
  });

  describe("CORS Support", function() {
    it("should handle preflight requests", function() {
      const options = request(QUERY_URL, {
        method: "OPTIONS",
        json: { topic: this.prefix + "/topic1" },
        headers: {
          "origin": "localhost",
          "access-control-request-method": "POST",
          "access-control-request-headers": "origin, content-type, accept, authorization"
        },
        resolveWithFullResponse: true
      });

      return expect(options).to.eventually.have.property("headers").that.includes({
        "access-control-allow-origin": "*",
        "access-control-allow-methods": "POST",
        "access-control-allow-headers": "origin, content-type, accept, authorization"
      });
    });

    it("should set Access-Control-Allow-Origin", function() {
      const post = request.post(QUERY_URL, {
        json: { topic: this.prefix + "/topic1" },
        headers: { "Origin": "localhost" },
        resolveWithFullResponse: true
      });

      return expect(post).to.eventually.have.property("headers").that.includes({
        "access-control-allow-origin": "*"
      });
    });
  });
});
