#!/usr/bin/env bash

set -e
./scripts/build.sh
pushd MikroCord-Proxy

# Note: don't care about javadocs and they are pretty brittle because of Lombok
mvn source:jar deploy -Dbuild.number=1337 -Dmaven.deploy.skip=false -P deployment

popd
