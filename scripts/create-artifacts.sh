#!/usr/bin/env sh
set -eo pipefail

docker run --rm -v $PWD:/usr/src/hivemq-retained-message-query-plugin -w /usr/src/hivemq-retained-message-query-plugin maven:3.9.5-eclipse-temurin-11-alpine mvn package -P TravisCI
docker run -v $PWD:/app --workdir /app -e COMMIT_TAG -e COMMIT_HASH -e CI_JOB_ID debian:bookworm-slim scripts/package.sh
