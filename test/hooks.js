const _ = require("lodash")
const mqtt = require("mqtt")
const Promise = require("bluebird")

const config = require("./config")

function connectMqttClient(done) {
  this.client = Promise.promisifyAll(mqtt.connect(config.TCP_BROKER_URI))
  this.client.on("connect", () => done())
}

function publishTestData(testData) {
  return function() {
    this.topic = `hivemq-api-${Date.now()}`
    this.prefix = `test/${this.topic}`
    this.publishedTopics = new Set()

    this.publish = (data) => {
      Object.keys(data).forEach((topic) => this.publishedTopics.add(`${this.prefix}/${topic}`))

      return Promise.all(_.map(data, (payload, topic) =>
        this.client.publishAsync(`${this.prefix}/${topic}`, payload, { retain: true, qos: 2 })
      ))
    }

    return this.publish(testData)
  }
}

function unpublishTestData() {
  return Promise.all(Array.from(this.publishedTopics).map((topic) =>
    this.client.publishAsync(topic, null, { retain: true, qos: 2 })
  ))
}

function disconnectMqttClient() {
  this.client.end()
}

module.exports = {
  connectMqttClient,
  publishTestData,
  unpublishTestData,
  disconnectMqttClient
}
