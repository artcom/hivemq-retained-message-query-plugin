const chai = require("chai")
const request = require("request-promise")

chai.use(require("chai-as-promised"))
const expect = chai.expect

const config = require("./config")
const hooks = require("./hooks")

function postQuery(json, additionalOptions = {}) {
  const options = Object.assign({ json }, additionalOptions)
  return request.post(config.JSON_URL, options)
}

describe("Json API", function() {
  before(hooks.connectMqttClient)

  beforeEach(hooks.publishTestData({
    "topic1": "\"payload1\"",
    "topic1/topic1": "\"payload11\"",
    "topic1/topic2": "\"payload12\"",
    "topic1/topic3/topic1": "\"payload131\"",
    "topic2": "\"payload2\"",
    "topic2/topic1": "\"payload21\"",
    "topic2/topic2": "invalid-json",
    "topic2/topic3": "invalid-json",
    "topic2/topic3/topic1": "\"payload231\"",
    "topic3": "{\"key1\":\"value1\", \"key2\":[1, 2, 3]}"
  }))

  afterEach(hooks.unpublishTestData)
  after(hooks.disconnectMqttClient)

  describe("Single Queries", function() {
    it("should return multi-level hierarchy of a topic", function() {
      const query = postQuery({ topic: this.prefix })
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
          topic3: {
            topic1: "payload231"
          }
        },
        topic3: {
          key1: "value1",
          key2: [1, 2, 3]
        }
      })
    })

    it("should return empty object for leaf topic", function() {
      const query = postQuery({ topic: `${this.prefix}/topic1/topic1` })

      return expect(query).to.eventually.deep.equal({})
    })

    it("should return empty object for non-existing topic", function() {
      const query = postQuery({ topic: `${this.prefix}/does-not-exist` })

      return expect(query).to.eventually.deep.equal({})
    })
  })

  describe("Batch Queries", function() {
    it("should return the values of multiple topics", function() {
      const query = postQuery([
        { topic: `${this.prefix}/topic1` },
        { topic: `${this.prefix}/topic2` }
      ])

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
          topic3: {
            topic1: "payload231"
          }
        }
      ])
    })

    it("should return values and empty objects for multiple topics", function() {
      const query = postQuery([
        { topic: `${this.prefix}/topic1` },
        { topic: `${this.prefix}/does-not-exist` }
      ])

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
      ])
    })

    it("should support wildcard queries", function() {
      const query = postQuery([
        { topic: `${this.prefix}/+` },
        { topic: `${this.prefix}/topic2` }
      ])

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
            topic3: {
              topic1: "payload231"
            }
          },
          {
          }
        ],
        {
          topic1: "payload21",
          topic3: {
            topic1: "payload231"
          }
        }
      ])
    })

    it("should support wildcard queries without results", function() {
      const query = postQuery([
        { topic: `${this.prefix}/+/does-not-exist` },
        { topic: `${this.prefix}/topic2` }
      ])

      return expect(query).to.eventually.deep.equal([
        [],
        {
          topic1: "payload21",
          topic3: {
            topic1: "payload231"
          }
        }
      ])
    })
  })

  describe("Wildcard Queries", function() {
    it("should return all children", function() {
      const query = postQuery({ topic: `${this.prefix}/+` })
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
          topic3: {
            topic1: "payload231"
          }
        },
        {
        }
      ])
    })

    it("should return empty array when no topics match", function() {
      const query = postQuery({ topic: `${this.prefix}/+/does-not-exist` })
      return expect(query).to.eventually.deep.equal([])
    })

    it("should return all matching children", function() {
      const query = postQuery({ topic: `${this.prefix}/+/topic3` })
      return expect(query).to.eventually.deep.equal([
        { topic1: "payload131" },
        { topic1: "payload231" }
      ])
    })

    it("should support leading wildcard", function() {
      const query = postQuery({ topic: `+/${this.topic}` })
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
            topic3: {
              topic1: "payload231"
            }
          },
          topic3: {
            key1: "value1",
            key2: [1, 2, 3]
          }
        }
      ])
    })
  })
})
