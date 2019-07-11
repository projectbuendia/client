#!/bin/bash
#
# This script executes Android instrumentation tests in Firebase Test Lab, as
# described in:
#
# https://medium.com/ninety-nine-news/testing-android-apps-with-firebase-test-lab-and-circleci-957c690b5dc5
#
# Current device targets:
#
# * flo = Asus Nexus 7 (2013)
#
# TODO: add more

PROJECT_ID=${1:-buendia-client}

gcloud config set project $PROJECT_ID
gcloud firebase test android run \
    --type instrumentation \
    --app ./build/outputs/apk/debug/app-debug.apk \
    --test ./build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
    --timeout 3m \
    --device model=Nexus7,version=21 \
    --use-orchestrator
