#!/usr/bin/env bash

if [ "$TEST_SUITE" == "integration" ] || [ "$TEST_SUITE" == "integration_large" ]
then
    if [ "$TRAVIS_BRANCH" == "development" ] || [ "$TRAVIS_PULL_REQUEST" == "true" ]
    then
    "$EXECUTE_INTEGRATION"="true"
    fi
fi