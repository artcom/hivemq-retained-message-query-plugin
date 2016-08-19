const axios = require("axios")
const chai = require("chai")

chai.use(require("chai-as-promised"))
const expect = chai.expect

const config = require("./config")
const hooks = require("./hooks")

function postQuery(json, expectedStatus = 200) {
  return axios.post(config.QUERY_URL, json, {
    validateStatus: (status) => status === expectedStatus
  }).then((response) => response.data)
}

describe("Query API", function() {
  before(hooks.connectMqttClient)
  after(hooks.disconnectMqttClient)

  const prefixes = [
    { prefix: "test", description: "without leading slash" },
    { prefix: "/test", description: "with leading slash" }
  ]

  prefixes.forEach(function({ prefix, description }) {
    context(description, function() {
      beforeEach(hooks.publishTestData(prefix, {
        topic1: "foo",
        topic2: "bar"
      }))

      afterEach(hooks.unpublishTestData)

      describe("Single Queries", function() {
        it("should return the payload of a topic", function() {
          const query = postQuery({ topic: `${this.testTopic}/topic1` })
          return expect(query).to.eventually.deep.equal({
            topic: `${this.testTopic}/topic1`,
            payload: "foo"
          })
        })

        it("should return the payload of a topic with trailing slash", function() {
          const query = this.publish({
            "topic3/": "baz"
          }).then(() =>
            postQuery({ topic: `${this.testTopic}/topic3/` })
          )

          return expect(query).to.eventually.deep.equal({
            topic: `${this.testTopic}/topic3/`,
            payload: "baz"
          })
        })

        it("should return error for inexistent topic", function() {
          const expectedStatus = 404
          const query = postQuery({ topic: `${this.testTopic}/does-not-exist` }, expectedStatus)

          return expect(query).to.eventually.deep.equal({
            topic: `${this.testTopic}/does-not-exist`,
            error: expectedStatus
          })
        })

        it("should return error for unpublished topic", function() {
          const expectedStatus = 404
          const query = this.publish({
            topic1: null
          }).then(() =>
            postQuery({ topic: `${this.testTopic}/topic1` }, expectedStatus)
          )

          return expect(query).to.eventually.deep.equal({
            topic: `${this.testTopic}/topic1`,
            error: expectedStatus
          })
        })

        it("should return no payload for unpublished topic with children", function() {
          const query = this.publish({
            "topic1/foo": "bar"
          }).then(() => this.publish({
            topic1: null
          })).then(() =>
            postQuery({ topic: `${this.testTopic}/topic1`, depth: 1 })
          )

          return expect(query).to.eventually.deep.equal({
            topic: `${this.testTopic}/topic1`,
            children: [
              { topic: `${this.testTopic}/topic1/foo`, payload: "bar" }
            ]
          })
        })

        it("should return error for unpublished nested topic", function() {
          const expectedStatus = 404
          const query = this.publish({
            "foo/bar": "baz"
          }).then(() => this.publish({
            "foo/bar": null
          })).then(() =>
            postQuery({ topic: `${this.testTopic}/foo/bar` }, expectedStatus)
          )

          return expect(query).to.eventually.deep.equal({
            topic: `${this.testTopic}/foo/bar`,
            error: expectedStatus
          })
        })

        describe("with Depth Parameter", function() {
          beforeEach(function() {
            return this.publish({
              "topic2/deepTopic": "baz"
            })
          })

          it("should return empty result for intermediary topic", function() {
            const query = postQuery({ topic: this.testTopic, depth: 0 })
            return expect(query).to.eventually.deep.equal({
              topic: this.testTopic
            })
          })

          it("should return singleton array for flattened intermediary topic", function() {
            const query = postQuery({ topic: this.testTopic, depth: 0, flatten: true })
            return expect(query).to.eventually.deep.equal([
              { topic: this.testTopic }
            ])
          })

          it("should return array with error object for flattened query of non-existent topic",
            function() {
              const query = postQuery({ topic: "does-not-exist", depth: 0, flatten: true })
              return expect(query).to.eventually.deep.equal([
                {
                  error: 404,
                  topic: "does-not-exist"
                }
              ])
            })

          it("should return the payload of immediate children", function() {
            const query = postQuery({ topic: this.testTopic, depth: 1 })
            return expect(query).to.eventually.deep.equal({
              topic: this.testTopic,
              children: [
                { topic: `${this.testTopic}/topic1`, payload: "foo" },
                { topic: `${this.testTopic}/topic2`, payload: "bar" }
              ]
            })
          })

          it("should return the payload of deeper children", function() {
            const query = postQuery({ topic: this.testTopic, depth: 2 })
            return expect(query).to.eventually.deep.equal({
              topic: this.testTopic,
              children: [
                {
                  topic: `${this.testTopic}/topic1`,
                  payload: "foo"
                },
                {
                  topic: `${this.testTopic}/topic2`,
                  payload: "bar",
                  children: [
                    {
                      topic: `${this.testTopic}/topic2/deepTopic`,
                      payload: "baz"
                    }
                  ]
                }
              ]
            })
          })

          it("should support empty string subtopics", function() {
            const query = this.publish({
              "/foo1": "bar1",
              "foo2/": "bar2"
            }).then(() =>
              postQuery({ topic: this.testTopic, depth: -1 })
            )

            return expect(query).to.eventually.deep.equal({
              topic: this.testTopic,
              children: [
                {
                  topic: `${this.testTopic}/`,
                  children: [
                    {
                      topic: `${this.testTopic}//foo1`,
                      payload: "bar1"
                    }
                  ]
                },
                {
                  topic: `${this.testTopic}/foo2`,
                  children: [
                    {
                      topic: `${this.testTopic}/foo2/`,
                      payload: "bar2"
                    }
                  ]
                },
                {
                  topic: `${this.testTopic}/topic1`,
                  payload: "foo"
                },
                {
                  topic: `${this.testTopic}/topic2`,
                  payload: "bar",
                  children: [
                    {
                      topic: `${this.testTopic}/topic2/deepTopic`,
                      payload: "baz"
                    }
                  ]
                }
              ]
            })
          })

          it("should return flattened list of topics", function() {
            const query = postQuery({ topic: this.testTopic, depth: 2, flatten: true })
            return expect(query).to.eventually.deep.equal([
              { topic: this.testTopic },
              { topic: `${this.testTopic}/topic1`, payload: "foo" },
              { topic: `${this.testTopic}/topic2`, payload: "bar" },
              { topic: `${this.testTopic}/topic2/deepTopic`, payload: "baz" }
            ])
          })

          it("should return children of the root node", function() {
            const depth = prefix.startsWith("/") ? 2 : 1
            return postQuery({ topic: null, depth }).then((result) => {
              expect(result).to.have.property("topic", null)

              if (prefix.startsWith("/")) {
                const slashRoot = result.children.find((child) => child.topic === "")
                expect(slashRoot).to.have.property("topic", "")
                expect(slashRoot.children).to.include({ topic: prefix })
              } else {
                expect(result.children).to.include({ topic: prefix })
              }
            })
          })

          it("should return all children", function() {
            const query = postQuery({ topic: this.testTopic, depth: -1 })
            return expect(query).to.eventually.deep.equal({
              topic: this.testTopic,
              children: [
                {
                  topic: `${this.testTopic}/topic1`,
                  payload: "foo"
                },
                {
                  topic: `${this.testTopic}/topic2`,
                  payload: "bar",
                  children: [
                    {
                      topic: `${this.testTopic}/topic2/deepTopic`,
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
            { topic: `${this.testTopic}/topic1` },
            { topic: `${this.testTopic}/topic2` }
          ])

          return expect(query).to.eventually.deep.equal([
            { topic: `${this.testTopic}/topic1`, payload: "foo" },
            { topic: `${this.testTopic}/topic2`, payload: "bar" }
          ])
        })

        it("should return values and errors for multiple topics", function() {
          const query = postQuery([
            { topic: `${this.testTopic}/topic1` },
            { topic: `${this.testTopic}/does-not-exist` }
          ])

          return expect(query).to.eventually.deep.equal([
            {
              topic: `${this.testTopic}/topic1`,
              payload: "foo"
            },
            {
              topic: `${this.testTopic}/does-not-exist`,
              error: 404
            }
          ])
        })

        it("should support different depth parameters", function() {
          const query = this.publish({
            "topic1/child": "one",
            "topic2/child": "two"
          }).then(() =>
            postQuery([
              { topic: `${this.testTopic}/topic1` },
              { topic: `${this.testTopic}/topic2`, depth: 1 }
            ])
          )

          return expect(query).to.eventually.deep.equal([
            {
              topic: `${this.testTopic}/topic1`,
              payload: "foo"
            },
            {
              topic: `${this.testTopic}/topic2`,
              payload: "bar",
              children: [
                {
                  topic: `${this.testTopic}/topic2/child`,
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
              { topic: `${this.testTopic}/topic1` },
              { topic: `${this.testTopic}/topic2`, depth: 1, flatten: true }
            ])
          )

          return expect(query).to.eventually.deep.equal([
            {
              topic: `${this.testTopic}/topic1`,
              payload: "foo"
            },
            [
              { topic: `${this.testTopic}/topic2`, payload: "bar" },
              { topic: `${this.testTopic}/topic2/child`, payload: "two" }
            ]
          ])
        })

        it("should support wildcard queries", function() {
          const query = postQuery([
            { topic: `${this.testTopic}/+` },
            { topic: `${this.testTopic}/topic2` }
          ])

          return expect(query).to.eventually.deep.equal([
            [
              { topic: `${this.testTopic}/topic1`, payload: "foo" },
              { topic: `${this.testTopic}/topic2`, payload: "bar" }
            ],
            { topic: `${this.testTopic}/topic2`, payload: "bar" }
          ])
        })

        it("should support wildcard queries without results", function() {
          const query = postQuery([
            { topic: `${this.testTopic}/+/does-not-exist` },
            { topic: `${this.testTopic}/topic2` }
          ])

          return expect(query).to.eventually.deep.equal([
            [],
            { topic: `${this.testTopic}/topic2`, payload: "bar" }
          ])
        })
      })

      describe("Invalid Queries", function() {
        it("should return an error when topic is missing", function() {
          const expectedStatus = 400
          const query = postQuery({ invalid: "query" }, expectedStatus)

          return expect(query).to.eventually.deep.equal({
            error: expectedStatus,
            message: "The request body must be a JSON object with a 'topic' and optional 'depth'" +
              " property, or a JSON array of such objects."
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
          const query = postQuery({ topic: `${this.testTopic}/+` })
          return expect(query).to.eventually.deep.equal([
            { topic: `${this.testTopic}/topic1`, payload: "foo" },
            { topic: `${this.testTopic}/topic2`, payload: "bar" }
          ])
        })

        it("should return empty array when no topics match", function() {
          const query = postQuery({ topic: `${this.testTopic}/+/does-not-exist` })
          return expect(query).to.eventually.deep.equal([])
        })

        it("should return all matching children", function() {
          const query = postQuery({ topic: `${this.testTopic}/+/child` })
          return expect(query).to.eventually.deep.equal([
            { topic: `${this.testTopic}/topic1/child`, payload: "one" },
            { topic: `${this.testTopic}/topic2/child`, payload: "two" }
          ])
        })

        it("should return all matching deep children", function() {
          const query = this.publish({
            "topic2/deep/child": "deep"
          }).then(() =>
            postQuery({ topic: `${this.testTopic}/+/deep/child` })
          )

          return expect(query).to.eventually.deep.equal([
            { topic: `${this.testTopic}/topic2/deep/child`, payload: "deep" }
          ])
        })

        it("should support the depth parameter", function() {
          const query = postQuery({ topic: `${this.testTopic}/+`, depth: 1 })
          return expect(query).to.eventually.deep.equal([
            {
              topic: `${this.testTopic}/topic1`,
              payload: "foo",
              children: [
                {
                  topic: `${this.testTopic}/topic1/child`,
                  payload: "one" }
              ]
            },
            {
              topic: `${this.testTopic}/topic2`,
              payload: "bar",
              children: [
                {
                  topic: `${this.testTopic}/topic2/child`,
                  payload: "two"
                }
              ]
            }
          ])
        })

        it("should support the depth and flatten parameter", function() {
          const query = postQuery({ topic: `${this.testTopic}/+`, depth: 1, flatten: true })
          return expect(query).to.eventually.deep.equal([
            { topic: `${this.testTopic}/topic1`, payload: "foo" },
            { topic: `${this.testTopic}/topic1/child`, payload: "one" },
            { topic: `${this.testTopic}/topic2`, payload: "bar" },
            { topic: `${this.testTopic}/topic2/child`, payload: "two" }
          ])
        })

        it("should support leading wildcard", function() {
          const query = postQuery({ topic: this.testTopic.replace("test", "+") })
          return expect(query).to.eventually.deep.equal([
            { topic: this.testTopic }
          ])
        })

        it("should support multiple wildcards", function() {
          const query = this.publish({
            "topic2/otherChild": "three"
          }).then(() =>
            postQuery({ topic: `${this.testTopic}/+/+` })
          )

          return expect(query).to.eventually.deep.equal([
            { topic: `${this.testTopic}/topic1/child`, payload: "one" },
            { topic: `${this.testTopic}/topic2/child`, payload: "two" },
            { topic: `${this.testTopic}/topic2/otherChild`, payload: "three" }
          ])
        })

        it("should support empty string subtopics", function() {
          const query = this.publish({ "topic3//foo": "true" })
            .then(() => postQuery({ topic: `${this.testTopic}/+//foo`, depth: 1 }))

          return expect(query).to.eventually.deep.equal([
            { topic: `${this.testTopic}/topic3//foo`, payload: "true" }
          ])
        })
      })
    })
  })
})
