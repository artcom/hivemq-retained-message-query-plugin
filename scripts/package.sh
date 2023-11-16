#!/usr/bin/env bash
set -eo pipefail

apt update && apt install -y zip

DIST_DIR="target/hivemq-retained-message-query-extension"
ARTIFACTS_DIR="artifacts"

echo "Packaging"
mkdir "${ARTIFACTS_DIR}"
cd "${DIST_DIR}"
echo {\"version\": \"$COMMIT_TAG\", \"commit\": \"$COMMIT_HASH\", \"buildJob\": $CI_JOB_ID} > build.json
zip -r -9 "../../${ARTIFACTS_DIR}/hivemq-retained-message-query-extension-${COMMIT_TAG}.zip" *
tar czvf "../../${ARTIFACTS_DIR}/hivemq-retained-message-query-extension-${COMMIT_TAG}.tar.gz" *
