# HiveMQ Retained Message Query Extension

A HiveMQ extension that allows to query retained messages via HTTP instead of using MQTT subscriptions.

## Development Setup

HiveMQ is needed to run and test the extension. An evaluation version is available [here](http://www.hivemq.com/downloads/).

The extension was developed using IntelliJ IDEA. The repo contains the project file: `hivemq-retained-message-query-extension.iml`.

It can also be built on the command line using Maven:

```bash
$ mvn package
```

Maven is configured to use the [HiveMQ Maven Plugin](http://www.hivemq.com/docs/plugins/latest/#maven-plugin-chapter) that will automatically start the plugin when using the `RunWithHiveMQ` profile:

```bash
$ mvn package -PRunWithHiveMQ
```

## Tests

There is a test suite in `test/test.js`. Tests are written in JavaScript and run using [mocha](http://mochajs.org/) on Node.js:

```bash
$ npm install # install test dependencies
$ npm test # run tests against localhost
```

To run the tests against a different HiveMQ instance, you can set the `BROKER` environment variable:

```bash
BROKER=broker.example.com npm test
```

## HTTP API

The extension provides an HTTP API to query retained messages without using the MQTT protocol. The API uses JSON to define the query and represent the results.

## Query

### Endpoint

Retained message payloads can be queried by sending an HTTP POST request to the `/query` endpoint. Using jQuery, you can send a query like this:

```javascript
$.post(httpBrokerUri + "/query", JSON.stringify(query));
```

### Query Objects

In the request body we pass a JSON-encoded *query object* containing the details of our query:

```json
{
  "topic": "path/of/the/topic",
  "depth": 0,
  "flatten": false
}
```

* `topic` (String) is mandatory. It specifies the path of the topic we want to query.
* `depth` (Number) is optional, defaults to `0`. It specifies how many levels of child topics should be included in the response.
* `flatten` (Boolean) is optional, defaults to `false`. When set to `true`, the query will return an array of result objects instead of a single result objects. The array contains the result object for the queried topic as well as the result objects for all of its children, as far as they are included by the `depth` parameter.

### Result Objects

The *result object* that we get back from the broker is also a JSON object:

```json
{
  "topic": "path/of/the/topic",
  "payload": "23",
  "children": [
    {
      "topic": "path/of/child/topic",
      "payload": "\"foo\""
    }
  ]
}
```

* `topic` (String) is always present. This is the path of the topic we queried.
* `payload` (String) is optional. It will be included if there is a retained payload for the given topic. The payload is always a string. For topics following our convention to use JSON in payloads, that means we have to `JSON.parse()` the payload on the client side.
* `children` (Array) is optional. It will be included when there are child topics and the `depth` parameter in the query is set to include them, unless the `flatten` parameter is set. The items of the array are *result objects* themselves, including `topic`, an optional `payload` and optional `children`.

### Wildcard Queries

The query API has limited support for wildcard queries: The topic in the *query object* may contain one or more `+` wildcards. Instead of a single *result object*, the broker will return an array of *result objects*.

While the `#` wildcard is not supported, a similar result can be achieved using the `depth` parameter.

### Batch Queries

It is also possible to query multiple topics at once. When the request body contains an array of *query objects* instead of a single *query object*, the broker will return an array with a *result object* for each query.

### Examples

Topic           | Payload
--------------- | -------
`foo/bar1`      | `"hello"`
`foo/bar1/baz1` | `["night", "day"]`
`foo/bar2`      | `13`
`foo/bar2/baz1` | `true`
`foo/bar2/baz2` | `false`

Let's assume we have the above retained messages published to the broker. We can now issue some queries:

##### Single Query (Depth 0)

```json
// Query
{ "topic": "foo/bar1" }

// Result
{ "topic": "foo/bar1", "payload": "\"hello\"" }
```

##### Single Query (Depth 1)

```json
// Query
{ "topic": "foo", "depth": 1 }

// Result
{
  "topic": "foo",
  "children": [
    { "topic": "foo/bar1", "payload": "\"hello\"" },
    { "topic": "foo/bar2", "payload": "13" }
  ]
}
```

##### Single Query (Depth 2)

```json
// Query
{ "topic": "foo", "depth": 2 }

// Result
{
  "topic": "foo",
  "children": [
    {
      "topic": "foo/bar1",
      "payload": "\"hello\"",
      "children": [
        { "topic": "foo/bar1/baz1", "payload": "[\"night\", \"day\"]" }
      ]
    },
    {
      "topic": "foo/bar2",
      "payload": "13",
      "children": [
        { "topic": "foo/bar2/baz1", "payload": "true" },
        { "topic": "foo/bar2/baz2", "payload": "false" }
      ]
    }
  ]
}
```

##### Flattened Single Query (Depth 2)

```json
// Query
{ "topic": "foo", "depth": 2, "flatten": true }

// Result
[
  { "topic": "foo" },
  { "topic": "foo/bar1", "payload": "\"hello\"" },
  { "topic": "foo/bar1/baz1", "payload": "[\"night\", \"day\"]" }
  { "topic": "foo/bar2", "payload": "13" },
  { "topic": "foo/bar2/baz1", "payload": "true" },
  { "topic": "foo/bar2/baz2", "payload": "false" }
]
```

##### Batch Query

```json
// Query
[{ "topic": "foo/bar1" }, { "topic": "foo/bar2", "depth": 1 }]

// Result
[
  {
    "topic": "foo/bar1",
    "payload": "\"hello\""
  },
  {
    "topic": "foo/bar2",
    "payload": "13",
    "children": [
      { "topic": "foo/bar2/baz1", "payload": "true" },
      { "topic": "foo/bar2/baz2", "payload": "false" }
    ]
  }
]
```

##### Wildcard Query

```json
// Query
{ "topic": "foo/+/baz1" }

// Result
[
  { "topic": "foo/bar1/baz1", "payload": "[\"night\", \"day\"]" },
  { "topic": "foo/bar2/baz1", "payload": "true" }
]
```

## CORS

It can be necessary to send CORS headers along with the response e.g. if there is no upstream server which handles it. The internal HTTP Server can be configured to provide these CORS headers by adding the following section to `conf/config.xml`:

```
<retained-message-query-extension>
    <cors-header>true</cors-header>
</retained-message-query-extension>
```

Or by setting the environment variable `QUERY_PLUGIN_CORS` to `true`.

The default is `false` to avoid [duplicate CORS header errors](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS/Errors/CORSMultipleAllowOriginNotAllowed).

## HTTP API PORT

The default port for the HTTP API is `8080`. It can be changed by setting the environment variable `QUERY_PLUGIN_PORT` to the desired port.
