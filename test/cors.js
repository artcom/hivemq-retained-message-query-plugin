const chai = require("chai")
const request = require("request-promise")

chai.use(require("chai-as-promised"))
const expect = chai.expect

const config = require("./config")
const hooks = require("./hooks")

describe("CORS Support", function() {
  before(hooks.connectMqttClient)
  beforeEach(hooks.publishTestData({ topic1: "foo" }))
  afterEach(hooks.unpublishTestData)
  after(hooks.disconnectMqttClient)

  const urls = [config.QUERY_URL, config.JSON_URL]

  urls.forEach((url) => {
    it(`should handle preflight requests for ${url}`, function() {
      const options = request(url, {
        method: "OPTIONS",
        json: { topic: `${this.prefix}/topic1` },
        headers: {
          "origin": "localhost",
          "access-control-request-method": "POST",
          "access-control-request-headers": "origin, content-type, accept, authorization"
        },
        resolveWithFullResponse: true
      })

      return expect(options).to.eventually.have.property("headers").that.includes({
        "access-control-allow-origin": "*",
        "access-control-allow-methods": "POST",
        "access-control-allow-headers": "origin, content-type, accept, authorization"
      })
    })

    it(`should set Access-Control-Allow-Origin for ${url}`, function() {
      const post = request.post(url, {
        json: { topic: `${this.prefix}/topic1` },
        headers: { Origin: "localhost" },
        resolveWithFullResponse: true
      })

      return expect(post).to.eventually.have.property("headers").that.includes({
        "access-control-allow-origin": "*"
      })
    })
  })
})
