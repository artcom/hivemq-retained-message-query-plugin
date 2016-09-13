const BROKER = process.env.BROKER || "localhost"
const TCP_BROKER_URI = process.env.TCP_BROKER_URI || `tcp://${BROKER}`
const HTTP_BROKER_URI = process.env.HTTP_BROKER_URI || `http://${BROKER}:8080`

const QUERY_URL = `${HTTP_BROKER_URI}/query`

module.exports = {
  TCP_BROKER_URI,
  QUERY_URL
}
