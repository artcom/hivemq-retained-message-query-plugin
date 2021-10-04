const { connectAsync, HttpClient, unpublishRecursively } = require("@artcom/mqtt-topping")

const tcpBrokerUri = process.env.TCP_BROKER_URI || "tcp://localhost"
const httpBrokerUri = process.env.HTTP_BROKER_URI || "http://localhost:8080/query"

describe("Will", () => {
  let mqttClient
  let httpClient
  let testTopic

  beforeEach(async () => {
    httpClient = new HttpClient(httpBrokerUri)
    testTopic = `test/topping-${Math.random()}`
  })

  afterEach(async () => {
    const client = await connectAsync(tcpBrokerUri, { appId: "Test", deviceId: "DeviceId" })
    await unpublishRecursively(client, httpClient, testTopic)
    client.disconnect()
  })

  test("should publish last will on timeout", async () => {
    const willTopic = `${testTopic}/lastWill`
    const willPayload = { foo: "bar" }

    mqttClient = await connectAsync(
      tcpBrokerUri,
      {
        appId: "Test",
        deviceId: "DeviceId",
        connectTimeout: 500,
        will: {
          topic: willTopic,
          payload: willPayload,
          retain: true
        }
      }
    )

    // eslint-disable-next-line no-underscore-dangle
    mqttClient.client._client.stream.destroy()
    mqttClient.disconnect()

    // ensure that the timeout applies on the server
    await new Promise(resolve => setTimeout(resolve, 600))

    const response = await httpClient.query({ topic: willTopic })
    expect(response).toEqual({ topic: willTopic, payload: willPayload })
  })
})
