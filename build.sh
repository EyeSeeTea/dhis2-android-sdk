#!/bin/bash
set -xe

# You can run it from any directory.
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR=$DIR/

android-wait-for-emulator
sleep 180
adb devices
adb shell input keyevent 82 &
if [ "$TEST_SUITE" == "integration" ]
then
"$PROJECT_DIR"/gradlew clean
"$PROJECT_DIR"/gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.size=small
fi
if [ "$TEST_SUITE" == "integration_large" ]
then
"$PROJECT_DIR"/gradlew clean
ulimit -s 1082768  # Require a bit of more memory.
"$PROJECT_DIR"/gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.size=medium
fi
fi

