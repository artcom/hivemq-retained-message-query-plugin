const { connectAsync, HttpClient, unpublishRecursively } = require("@artcom/mqtt-topping")

const tcpBrokerUri = process.env.TCP_BROKER_URI || "tcp://localhost"
const httpBrokerUri = process.env.HTTP_BROKER_URI || "http://localhost:8080/query"

describe("Query API", () => {
  let mqttClient
  let httpClient
  let testTopic

  beforeEach(async () => {
    mqttClient = await connectAsync(tcpBrokerUri, { appId: "retainedTopicQueryExtensionTest" })
    httpClient = new HttpClient(httpBrokerUri)

    testTopic = `test/topping-${Math.random()}`

    await mqttClient.publish(`${testTopic}/topic1`, "foo")
    await mqttClient.publish(`${testTopic}/topic2`, "bar")
  })

  afterEach(async () => {
    await unpublishRecursively(mqttClient, httpClient, testTopic)
    mqttClient.disconnect()
  })

  describe("Single Queries", () => {
    it("should return the payload of a topic", async () => {
      const response = await httpClient.query({ topic: `${testTopic}/topic1` })

      expect(response).toEqual({
        topic: `${testTopic}/topic1`,
        payload: "foo"
      })
    })

    it("should return the payload of a topic with trailing slash", async () => {
      await mqttClient.publish(`${testTopic}/topic3`, "baz")
      const response = await httpClient.query({ topic: `${testTopic}/topic3` })
      
      expect(response).toEqual({
        topic: `${testTopic}/topic3`,
        payload: "baz"
      })
    })

    it("should return error for inexistent topic", () => {
      expect.assertions(1)

      return httpClient.query({ topic: `${testTopic}/does-not-exist` })
        .catch(error => {
          expect(JSON.parse(error.message)).toEqual({
            topic: `${testTopic}/does-not-exist`,
            error: 404
          })
        })
    })

    it("should return error for unpublished topic", async () => {
      expect.assertions(1)

      await mqttClient.unpublish(`${testTopic}/topic1`)

      return httpClient.query({ topic: `${testTopic}/topic1` })
        .catch(error => {
          expect(JSON.parse(error.message)).toEqual({
            topic: `${testTopic}/topic1`,
            error: 404
          })
        })
    })

    it("should return no payload for unpublished topic with children", async () => {
      await mqttClient.publish(`${testTopic}/topic1/foo`, "bar")
      await mqttClient.unpublish(`${testTopic}/topic1`)
      
      const response = await httpClient.query({ topic: `${testTopic}/topic1`, depth: 1  })
      
      expect(response).toEqual({
        topic: `${testTopic}/topic1`,
        children: [
          { topic: `${testTopic}/topic1/foo`, payload: "bar" }
        ]
      })
    })

    it("should return error for unpublished nested topic", async () => {
      expect.assertions(1)

      await mqttClient.publish(`${testTopic}/foo/bar`, "baz")
      await mqttClient.unpublish(`${testTopic}/foo/bar`)

      return httpClient.query({ topic: `${testTopic}/foo/bar` })
        .catch(error => {
          expect(JSON.parse(error.message)).toEqual({
            topic: `${testTopic}/foo/bar`,
            error: 404
          })
        })
    })

    describe("with Depth Parameter", () => {
      beforeEach(async () => {
        await mqttClient.publish(`${testTopic}/topic2/deepTopic`, "baz")
      })

      it("should return empty result for intermediary topic", async () => {
        const response = await httpClient.query({ topic: testTopic, depth: 0 })

        expect(response).toEqual({
          topic: testTopic
        })
      })

      it("should return singleton array for flattened intermediary topic", async () => {
        const response = await httpClient.query({ topic: testTopic, depth: 0, flatten: true  })
        
        expect(response).toEqual([
          { topic: testTopic }
        ])
      })

      it("should return array with error object for flattened query of non-existent topic",
        async () => {
        const response = await httpClient.query({
          topic: "does-not-exist",
          depth: 0,
          flatten: true
        })
          
        expect(response).toEqual([
          {
            error: 404,
            topic: "does-not-exist"
          }
        ])
      })

      it("should return the payload of immediate children", async () => {
        const response = await httpClient.query({ topic: testTopic, depth: 1 })
        
        return expect(response).toEqual({
          topic: testTopic,
          children: [
            { topic: `${testTopic}/topic1`, payload: "foo" },
            { topic: `${testTopic}/topic2`, payload: "bar" }
          ]
        })
      })

      it("should return the payload of deeper children", async () => {
        const response = await httpClient.query({ topic: testTopic, depth: 2 })
        
        expect(response).toEqual({
          topic: testTopic,
          children: [
            {
              topic: `${testTopic}/topic1`,
              payload: "foo"
            },
            {
              topic: `${testTopic}/topic2`,
              payload: "bar",
              children: [
                {
                  topic: `${testTopic}/topic2/deepTopic`,
                  payload: "baz"
                }
              ]
            }
          ]
        })
      })

      it("should support empty string subtopics", async () => {
        await mqttClient.publish(`${testTopic}//foo1`, "bar1")
        await mqttClient.publish(`${testTopic}/foo2/`, "bar2")

        const response = await httpClient.query({ topic: testTopic, depth: -1 })

        expect(response).toEqual({
          topic: testTopic,
          children: [
            {
              topic: `${testTopic}/`,
              children: [
                {
                  topic: `${testTopic}//foo1`,
                  payload: "bar1"
                }
              ]
            },
            {
              topic: `${testTopic}/foo2`,
              children: [
                {
                  topic: `${testTopic}/foo2/`,
                  payload: "bar2"
                }
              ]
            },
            {
              topic: `${testTopic}/topic1`,
              payload: "foo"
            },
            {
              topic: `${testTopic}/topic2`,
              payload: "bar",
              children: [
                {
                  topic: `${testTopic}/topic2/deepTopic`,
                  payload: "baz"
                }
              ]
            }
          ]
        })
      })

      it("should return flattened list of topics", async () => {
        const response = await httpClient.query({ topic: testTopic, depth: 2, flatten: true })

        expect(response).toEqual([
          { topic: testTopic },
          { topic: `${testTopic}/topic1`, payload: "foo" },
          { topic: `${testTopic}/topic2`, payload: "bar" },
          { topic: `${testTopic}/topic2/deepTopic`, payload: "baz" }
        ])
      })

      it("should return children of the root node", async () => {
        const response = await httpClient.query({ topic: null, depth: 1 })
        expect(response.children).toEqual([{ topic: "test" }])
      })

      it("should return all children", async () => {
        const response = await httpClient.query({ topic: testTopic, depth: -1})

        expect(response).toEqual({
          topic: testTopic,
          children: [
            {
              topic: `${testTopic}/topic1`,
              payload: "foo"
            },
            {
              topic: `${testTopic}/topic2`,
              payload: "bar",
              children: [
                {
                  topic: `${testTopic}/topic2/deepTopic`,
                  payload: "baz"
                }
              ]
            }
          ]
        })
      })
    })

    describe("Batch Queries", () => {
      it("should return the values of multiple topics", async () => {
        const response = await httpClient.queryBatch([
            { topic: `${testTopic}/topic1` },
            { topic: `${testTopic}/topic2` }
          ])
        
        expect(response).toEqual([
          { topic: `${testTopic}/topic1`, payload: "foo" },
          { topic: `${testTopic}/topic2`, payload: "bar" }
        ])
      })

      it("should return values and errors for multiple topics", async () => {
        const response = await httpClient.queryBatch([
            { topic: `${testTopic}/topic1` },
            { topic: `${testTopic}/does-not-exist` }
          ])

        expect(response).toEqual([
          {
            topic: `${testTopic}/topic1`,
            payload: "foo"
          },
          new Error(JSON.stringify({
            error: 404,
            topic: `${testTopic}/does-not-exist`
          }))
        ])
      })

      it("should support different depth parameters", async () => {
        await mqttClient.publish(`${testTopic}/topic1/child`, "one")
        await mqttClient.publish(`${testTopic}/topic2/child`, "two")

        const response = await httpClient.queryBatch([
          { topic: `${testTopic}/topic1` },
          { topic: `${testTopic}/topic2`, depth: 1 }
        ])

        expect(response).toEqual([
          {
            topic: `${testTopic}/topic1`,
            payload: "foo"
          },
          {
            topic: `${testTopic}/topic2`,
            payload: "bar",
            children: [
              {
                topic: `${testTopic}/topic2/child`,
                payload: "two"
              }
            ]
          }
        ])
      })

      it("should support different flatten parameters", async () => {
        await mqttClient.publish(`${testTopic}/topic1/child`, "one")
        await mqttClient.publish(`${testTopic}/topic2/child`, "two")

        const response = await httpClient.queryBatch([
          { topic: `${testTopic}/topic1` },
          { topic: `${testTopic}/topic2`, depth: 1, flatten: true }
        ])

        expect(response).toEqual([
          {
            topic: `${testTopic}/topic1`,
            payload: "foo"
          },
          [
            { topic: `${testTopic}/topic2`, payload: "bar" },
            { topic: `${testTopic}/topic2/child`, payload: "two" }
          ]
        ])
      })

      it("should support wildcard queries", async () => {
        const response = await httpClient.queryBatch([
          { topic: `${testTopic}/+` },
          { topic: `${testTopic}/topic2` }
        ])

        expect(response).toEqual([
          [
            { topic: `${testTopic}/topic1`, payload: "foo" },
            { topic: `${testTopic}/topic2`, payload: "bar" }
          ],
          { topic: `${testTopic}/topic2`, payload: "bar" }
        ])
      })

      it("should support wildcard queries without results", async () => {
        const response = await httpClient.queryBatch([
          { topic: `${testTopic}/+/does-not-exist` },
          { topic: `${testTopic}/topic2` }
        ])

        expect(response).toEqual([
          [],
          { topic: `${testTopic}/topic2`, payload: "bar" }
        ])
      })
    })

    describe("Invalid Queries", () => {
      it("should return an error when topic is missing", () => {
        expect.assertions(1)

        return httpClient.query({ invalid: "query" })
          .catch(error => {
            expect(JSON.parse(error.message)).toEqual({
              error: 400,
              message: "The request body must be a JSON object with a 'topic' and optional 'depth'" +
                " property, or a JSON array of such objects."
          })
        })
      })
    })

    describe("Wildcard Queries", () => {
      beforeEach(async () => {
        await mqttClient.publish(`${testTopic}/topic1/child`, "one")
        await mqttClient.publish(`${testTopic}/topic2/child`, "two")
      })

      it("should return all children", async () => {
        const response = await httpClient.query({ topic: `${testTopic}/+` })
        
        expect(response).toEqual([
          { topic: `${testTopic}/topic1`, payload: "foo" },
          { topic: `${testTopic}/topic2`, payload: "bar" }
        ])
      })

      it("should return empty array when no topics match", async () => {
        const response = await httpClient.query({ topic: `${testTopic}/+/does-not-exist` })
        expect(response).toEqual([])
      })

      it("should return all matching children", async () => {
        const response = await httpClient.query({ topic: `${testTopic}/+/child` })
        expect(response).toEqual([
          { topic: `${testTopic}/topic1/child`, payload: "one" },
          { topic: `${testTopic}/topic2/child`, payload: "two" }
        ])
      })

      it("should return all matching deep children", async () => {
        await mqttClient.publish(`${testTopic}/topic2/deep/child`, "deep")
        const response = await httpClient.query({ topic: `${testTopic}/+/deep/child` })

        expect(response).toEqual([
          { topic: `${testTopic}/topic2/deep/child`, payload: "deep" }
        ])
      })

      it("should support the depth parameter", async () => {
        const response = await httpClient.query({ topic: `${testTopic}/+`, depth: 1 })
        
        expect(response).toEqual([
          {
            topic: `${testTopic}/topic1`,
            payload: "foo",
            children: [
              {
                topic: `${testTopic}/topic1/child`,
                payload: "one" }
            ]
          },
          {
            topic: `${testTopic}/topic2`,
            payload: "bar",
            children: [
              {
                topic: `${testTopic}/topic2/child`,
                payload: "two"
              }
            ]
          }
        ])
      })

      it("should support the depth and flatten parameter", async () => {
        const response = await httpClient.query({
          topic: `${testTopic}/+`,
          depth: 1,
          flatten: true 
        })

        expect(response).toEqual([
          { topic: `${testTopic}/topic1`, payload: "foo" },
          { topic: `${testTopic}/topic1/child`, payload: "one" },
          { topic: `${testTopic}/topic2`, payload: "bar" },
          { topic: `${testTopic}/topic2/child`, payload: "two" }
        ])
      })

      it("should support leading wildcard", async () => {
        const response = await httpClient.query({ topic: testTopic.replace("test", "+") })

        expect(response).toEqual([
          { topic: testTopic }
        ])
      })

      it("should support multiple wildcards", async () => {
        await mqttClient.publish(`${testTopic}/topic2/otherChild`, "three")
        const response = await httpClient.query({ topic: `${testTopic}/+/+` })

        expect(response).toEqual([
          { topic: `${testTopic}/topic1/child`, payload: "one" },
          { topic: `${testTopic}/topic2/child`, payload: "two" },
          { topic: `${testTopic}/topic2/otherChild`, payload: "three" }
        ])
      })

      it("should support empty string subtopics", async () => {
        await mqttClient.publish(`${testTopic}/topic3//foo`, true)
        const response = await httpClient.query({ topic: `${testTopic}/+//foo`, depth: 1 })

        expect(response).toEqual([
          { topic: `${testTopic}/topic3//foo`, payload: true }
        ])
      })
    })
  })
})
