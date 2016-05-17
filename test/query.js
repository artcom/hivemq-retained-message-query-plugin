const chai = require("chai")
const request = require("request-promise")
const _ = require("lodash")

chai.use(require("chai-as-promised"))
const expect = chai.expect

const config = require("./config")
const hooks = require("./hooks")

function postQuery(json, additionalOptions = {}) {
  const options = _.assign({ json }, additionalOptions)
  return request.post(config.QUERY_URL, options)
}

function postErrorQuery(json) {
  return postQuery(json, { simple: false, resolveWithFullResponse: true })
}

describe("Query API", function() {
  before(hooks.connectMqttClient)

  beforeEach(hooks.publishTestData({
    topic1: "foo",
    topic2: "bar"
  }))

  afterEach(hooks.unpublishTestData)
  after(hooks.disconnectMqttClient)

  describe("Single Queries", function() {
    it("should return the payload of a topic", function() {
      const query = postQuery({ topic: `${this.prefix}/topic1` })
      return expect(query).to.eventually.deep.equal({
        topic: `${this.prefix}/topic1`,
        payload: "foo"
      })
    })

    it("should return error for inexistent topic", function() {
      const query = postErrorQuery({ topic: `${this.prefix}/does-not-exist` })

      return expect(query).to.eventually.include({
        statusCode: 404
      }).and.have.property("body").that.deep.equals({
        topic: `${this.prefix}/does-not-exist`,
        error: 404
      })
    })

    it("should return error for unpublished topic", function() {
      const query = this.publish({
        topic1: null
      }).then(() =>
        postErrorQuery({ topic: `${this.prefix}/topic1` })
      )

      return expect(query).to.eventually.include({
        statusCode: 404
      }).and.have.property("body").that.deep.equals({
        topic: `${this.prefix}/topic1`,
        error: 404
      })
    })

    it("should return no payload for unpublished topic with children", function() {
      const query = this.publish({
        "topic1/foo": "bar"
      }).then(() => this.publish({
        topic1: null
      })).then(() =>
        postQuery({ topic: `${this.prefix}/topic1`, depth: 1 })
      )

      return expect(query).to.eventually.deep.equal({
        topic: `${this.prefix}/topic1`,
        children: [
          { topic: `${this.prefix}/topic1/foo`, payload: "bar" }
        ]
      })
    })

    it("should return error for unpublished nested topic", function() {
      const query = this.publish({
        "foo/bar": "baz"
      }).then(() => this.publish({
        "foo/bar": null
      })).then(() =>
        postErrorQuery({ topic: `${this.prefix}/foo/bar` })
      )

      return expect(query).to.eventually.include({
        statusCode: 404
      }).and.have.property("body").that.deep.equals({
        topic: `${this.prefix}/foo/bar`,
        error: 404
      })
    })

    describe("with Depth Parameter", function() {
      beforeEach(function() {
        return this.publish({
          "topic2/deepTopic": "baz"
        })
      })

      it("should return empty result for intermediary topic", function() {
        const query = postQuery({ topic: this.prefix, depth: 0 })
        return expect(query).to.eventually.deep.equal({
          topic: this.prefix
        })
      })

      it("should return singleton array for flattened intermediary topic", function() {
        const query = postQuery({ topic: this.prefix, depth: 0, flatten: true })
        return expect(query).to.eventually.deep.equal([
          { topic: this.prefix }
        ])
      })

      it("should return the payload of immediate children", function() {
        const query = postQuery({ topic: this.prefix, depth: 1 })
        return expect(query).to.eventually.deep.equal({
          topic: this.prefix,
          children: [
            { topic: `${this.prefix}/topic1`, payload: "foo" },
            { topic: `${this.prefix}/topic2`, payload: "bar" }
          ]
        })
      })

      it("should return the payload of deeper children", function() {
        const query = postQuery({ topic: this.prefix, depth: 2 })
        return expect(query).to.eventually.deep.equal({
          topic: this.prefix,
          children: [
            {
              topic: `${this.prefix}/topic1`,
              payload: "foo"
            },
            {
              topic: `${this.prefix}/topic2`,
              payload: "bar",
              children: [
                {
                  topic: `${this.prefix}/topic2/deepTopic`,
                  payload: "baz"
                }
              ]
            }
          ]
        })
      })

      it("should return flattened list of topics", function() {
        const query = postQuery({ topic: this.prefix, depth: 2, flatten: true })
        return expect(query).to.eventually.deep.equal([
          { topic: this.prefix },
          { topic: `${this.prefix}/topic1`, payload: "foo" },
          { topic: `${this.prefix}/topic2`, payload: "bar" },
          { topic: `${this.prefix}/topic2/deepTopic`, payload: "baz" }
        ])
      })

      it("should return children of the root node", function() {
        const query = postQuery({ topic: "", depth: 1 })
        return expect(query).to.eventually.have.property("children").that.includes({
          topic: "test"
        })
      })

      it("should return all children", function() {
        const query = postQuery({ topic: this.prefix, depth: -1 })
        return expect(query).to.eventually.deep.equal({
          topic: this.prefix,
          children: [
            {
              topic: `${this.prefix}/topic1`,
              payload: "foo"
            },
            {
              topic: `${this.prefix}/topic2`,
              payload: "bar",
              children: [
                {
                  topic: `${this.prefix}/topic2/deepTopic`,
                  payload: "baz"
                }
              ]
            }
          ]
        })
      })
    })
  })

  describe("Batch Queries", function() {
    it("should return the values of multiple topics", function() {
      const query = postQuery([
        { topic: `${this.prefix}/topic1` },
        { topic: `${this.prefix}/topic2` }
      ])

      return expect(query).to.eventually.deep.equal([
        { topic: `${this.prefix}/topic1`, payload: "foo" },
        { topic: `${this.prefix}/topic2`, payload: "bar" }
      ])
    })

    it("should return values and errors for multiple topics", function() {
      const query = postQuery([
        { topic: `${this.prefix}/topic1` },
        { topic: `${this.prefix}/does-not-exist` }
      ])

      return expect(query).to.eventually.deep.equal([
        { topic: `${this.prefix}/topic1`, payload: "foo" },
        { topic: `${this.prefix}/does-not-exist`, error: 404 }
      ])
    })

    it("should support different depth parameters", function() {
      const query = this.publish({
        "topic1/child": "one",
        "topic2/child": "two"
      }).then(() =>
        postQuery([
          { topic: `${this.prefix}/topic1` },
          { topic: `${this.prefix}/topic2`, depth: 1 }
        ])
      )

      return expect(query).to.eventually.deep.equal([
        {
          topic: `${this.prefix}/topic1`,
          payload: "foo"
        },
        {
          topic: `${this.prefix}/topic2`,
          payload: "bar",
          children: [
            {
              topic: `${this.prefix}/topic2/child`,
              payload: "two"
            }
          ]
        }
      ])
    })

    it("should support different flatten parameters", function() {
      const query = this.publish({
        "topic1/child": "one",
        "topic2/child": "two"
      }).then(() =>
        postQuery([
          { topic: `${this.prefix}/topic1` },
          { topic: `${this.prefix}/topic2`, depth: 1, flatten: true }
        ])
      )

      return expect(query).to.eventually.deep.equal([
        {
          topic: `${this.prefix}/topic1`,
          payload: "foo"
        },
        [
          { topic: `${this.prefix}/topic2`, payload: "bar" },
          { topic: `${this.prefix}/topic2/child`, payload: "two" }
        ]
      ])
    })

    it("should support wildcard queries", function() {
      const query = postQuery([
        { topic: `${this.prefix}/+` },
        { topic: `${this.prefix}/topic2` }
      ])

      return expect(query).to.eventually.deep.equal([
        [
          { topic: `${this.prefix}/topic1`, payload: "foo" },
          { topic: `${this.prefix}/topic2`, payload: "bar" }
        ],
        { topic: `${this.prefix}/topic2`, payload: "bar" }
      ])
    })

    it("should support wildcard queries without results", function() {
      const query = postQuery([
        { topic: `${this.prefix}/+/does-not-exist` },
        { topic: `${this.prefix}/topic2` }
      ])

      return expect(query).to.eventually.deep.equal([
        [],
        { topic: `${this.prefix}/topic2`, payload: "bar" }
      ])
    })
  })

  describe("Invalid Queries", function() {
    it("should return an error when topic is missing", function() {
      const query = postErrorQuery({ invalid: "query" })

      return expect(query).to.eventually.include({
        statusCode: 400
      }).and.to.have.property("body").that.deep.equals({
        error: 400,
        message: "The request body must be a JSON object with a 'topic' and optional 'depth'" +
          " property, or a JSON array of such objects."
      })
    })

    it("should return an error when topic has leading slash", function() {
      const query = postErrorQuery({ topic: "/leading/slash" })

      return expect(query).to.eventually.include({
        statusCode: 400
      }).and.to.have.property("body").that.deep.equals({
        topic: "/leading/slash",
        error: 400,
        message: "The topic cannot start with a slash."
      })
    })

    it("should return an error when topic has trailing slash", function() {
      const query = postErrorQuery({ topic: "trailing/slash/" })

      return expect(query).to.eventually.include({
        statusCode: 400
      }).and.to.have.property("body").that.deep.equals({
        topic: "trailing/slash/",
        error: 400,
        message: "The topic cannot end with a slash."
      })
    })

    it("should return an error when using multiple wildcards", function() {
      const query = postErrorQuery({ topic: "using/+/multiple/+/wildcards" })

      return expect(query).to.eventually.include({
        statusCode: 400
      }).and.to.have.property("body").that.deep.equals({
        topic: "using/+/multiple/+/wildcards",
        error: 400,
        message: "The topic cannot contain more than one wildcard."
      })
    })
  })

  describe("Wildcard Queries", function() {
    beforeEach(function() {
      return this.publish({
        "topic1/child": "one",
        "topic2/child": "two"
      })
    })

    it("should return all children", function() {
      const query = postQuery({ topic: `${this.prefix}/+` })
      return expect(query).to.eventually.deep.equal([
        { topic: `${this.prefix}/topic1`, payload: "foo" },
        { topic: `${this.prefix}/topic2`, payload: "bar" }
      ])
    })

    it("should return empty array when no topics match", function() {
      const query = postQuery({ topic: `${this.prefix}/+/does-not-exist` })
      return expect(query).to.eventually.deep.equal([])
    })

    it("should return all matching children", function() {
      const query = postQuery({ topic: `${this.prefix}/+/child` })
      return expect(query).to.eventually.deep.equal([
        { topic: `${this.prefix}/topic1/child`, payload: "one" },
        { topic: `${this.prefix}/topic2/child`, payload: "two" }
      ])
    })

    it("should return all matching deep children", function() {
      const query = this.publish({
        "topic2/deep/child": "deep"
      }).then(() =>
        postQuery({ topic: `${this.prefix}/+/deep/child` })
      )

      return expect(query).to.eventually.deep.equal([
        { topic: `${this.prefix}/topic2/deep/child`, payload: "deep" }
      ])
    })

    it("should support the depth parameter", function() {
      const query = postQuery({ topic: `${this.prefix}/+`, depth: 1 })
      return expect(query).to.eventually.deep.equal([
        {
          topic: `${this.prefix}/topic1`,
          payload: "foo",
          children: [
            {
              topic: `${this.prefix}/topic1/child`,
              payload: "one" }
          ]
        },
        {
          topic: `${this.prefix}/topic2`,
          payload: "bar",
          children: [
            {
              topic: `${this.prefix}/topic2/child`,
              payload: "two"
            }
          ]
        }
      ])
    })

    it("should support the depth and flatten parameter", function() {
      const query = postQuery({ topic: `${this.prefix}/+`, depth: 1, flatten: true })
      return expect(query).to.eventually.deep.equal([
        { topic: `${this.prefix}/topic1`, payload: "foo" },
        { topic: `${this.prefix}/topic1/child`, payload: "one" },
        { topic: `${this.prefix}/topic2`, payload: "bar" },
        { topic: `${this.prefix}/topic2/child`, payload: "two" }
      ])
    })

    it("should support leading wildcard", function() {
      const query = postQuery({ topic: `+/${this.topic}` })
      return expect(query).to.eventually.deep.equal([
        { topic: this.prefix }
      ])
    })
  })
})
