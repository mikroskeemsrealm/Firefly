#!/usr/bin/env bash

set -e
git submodule update --recursive --init && ./scripts/applyPatches.sh
if [ "$1" == "--jar" ]; then
    ./gradlew clean build
fi
