#!/usr/bin/env bash

pushd MikroCord-Proxy
git rebase --interactive upstream/upstream
popd
