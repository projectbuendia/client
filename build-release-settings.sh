#!/bin/bash

# TODO: add check that file exists.

OPENMRS_PASSWORD=`cat .openmrs_password`

VER=""

if [[ $# == 1 ]]; then
  echo "Building version number $1"
  VER="-PversionNumber=$1"
fi

./gradlew app:assembleDebug -Pserver="server" -PopenmrsUser="buendia" -PopenmrsPassword="$OPENMRS_PASSWORD" -PrequireWifi="true" $VER

# if file exists ./app/build/outputs/apk/app-debug.apk
APK_PATH=`find . -name app-debug.apk`
echo "APK available at" `find . -name app-debug.apk`
echo "Install it with \`adb install -r $APK_PATH\`"
