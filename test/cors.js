const axios = require("axios")
const chai = require("chai")

chai.use(require("chai-as-promised"))
const expect = chai.expect

const config = require("./config")
const hooks = require("./hooks")

describe("CORS Support", function() {
  before(hooks.connectMqttClient)
  beforeEach(hooks.publishTestData("test", { topic1: "foo" }))
  afterEach(hooks.unpublishTestData)
  after(hooks.disconnectMqttClient)

  it("should handle preflight requests", function() {
    const response = axios(config.QUERY_URL, {
      method: "OPTIONS",
      data: { topic: `${this.testTopic}/topic1` },
      headers: {
        "origin": "localhost",
        "access-control-request-method": "POST",
        "access-control-request-headers": "origin, content-type, accept, authorization"
      }
    })

    return expect(response).to.eventually.have.property("headers").that.includes({
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "POST",
      "access-control-allow-headers": "origin, content-type, accept, authorization"
    })
  })

  it("should set Access-Control-Allow-Origin", function() {
    const response = axios(config.QUERY_URL, {
      method: "POST",
      data: { topic: `${this.testTopic}/topic1` },
      headers: { Origin: "localhost" }
    })

    return expect(response).to.eventually.have.property("headers").that.includes({
      "access-control-allow-origin": "*"
    })
  })
})
