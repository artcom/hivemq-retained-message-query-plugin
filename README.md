# HiveMQ HTTP API Plugin

The HTTP API Plugins allows to query retained messages via HTTP instead of using MQTT subscriptions.

## Development Setup

HiveMQ is needed to run and test the plugin. On Mac OS X, it can be installed via Homebrew:

```bash
$ brew tap hivemq/hivemq
$ brew install hivemq
```

The plugin was developed using IntelliJ IDEA. The repo contains the project file: `hivemq-http-api-plugin.iml`.

It can also be built on the command line using Maven:

```bash
$ mvn package
```

Maven is configured to use the [HiveMQ Maven Plugin](http://www.hivemq.com/docs/plugins/2.3.1/#maven-plugin-chapter) that will automatically start the plugin when using the `RunWithHiveMQ` profile:

```bash
$ mvn package -PRunWithHiveMQ
```

## Tests

There is a test suite in `test/test.js`. Tests are written in JavaScript and run using [mocha](http://mochajs.org/) on Node.js:

```bash
npm install # install test dependencies
npm test # run tests against localhost
```

To run the tests against a different HiveMQ instance, you can set the `BROKER` environment variable:

```bash
BROKER=broker.example.com npm test
```
