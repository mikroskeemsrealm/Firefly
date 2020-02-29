#!/usr/bin/env bash

pushd Firefly-Proxy
git rebase --interactive upstream/upstream
popd
