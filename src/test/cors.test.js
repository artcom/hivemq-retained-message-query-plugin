const axios = require("axios")
const { connectAsync, HttpClient, unpublishRecursively } = require("@artcom/mqtt-topping")

const tcpBrokerUri = process.env.TCP_BROKER_URI || "tcp://localhost"
const httpBrokerUri = process.env.HTTP_BROKER_URI || "http://localhost:8080/query"

describe("CORS Support", () => {
  let mqttClient
  let httpClient
  let testTopic

  beforeEach(async () => {
    mqttClient = await connectAsync(tcpBrokerUri, { appId: "retainedTopicQueryExtensionTest" })
    httpClient = new HttpClient(httpBrokerUri)

    testTopic = `test/topping-${Math.random()}`

    await mqttClient.publish(`${testTopic}/foo`, "bar")
  })

  afterEach(async () => {
    await unpublishRecursively(mqttClient, httpClient, testTopic)
    mqttClient.disconnect()
  })

  it("should handle preflight requests", async () => {
    const response = await axios(httpBrokerUri, {
      method: "OPTIONS",
      headers: {
        origin: "localhost",
        "access-control-request-methods": "POST",
        "access-control-request-headers": "origin, content-type, accept, authorization",
      },
    })

    expect(response.headers).toMatchObject({
      "access-control-allow-headers":
        "Access-Control-Request-Methods, Access-Control-Request-Headers, Content-Type",
      "access-control-allow-methods": "OPTIONS, POST",
      "access-control-allow-origin": "*",
    })
  })

  it("should set Access-Control-Allow-Origin", async () => {
    const response = await axios(httpBrokerUri, {
      method: "POST",
      data: { topic: `${testTopic}/foo` },
      headers: { Origin: "localhost" },
    })

    expect(response.headers).toMatchObject({
      "access-control-allow-origin": "*",
    })
  })
})
