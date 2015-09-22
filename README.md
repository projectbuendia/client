# client

This repository contains the Buendia app,
which runs on tablets with Android 4.4.2 (KitKat) or higher.
See the [Buendia wiki](https://github.com/projectbuendia/buendia/wiki) for more details about the project and about the OpenMRS module that this app communicates with.

#### Copyright notice

    Copyright 2015 The Project Buendia Authors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


## Developer setup

Follow the instructions below to get your system set up to do Buendia client development.


### Prerequisites

##### JDK 7
  * If `java -version` does not report a version >= 1.7, install JDK 7:
      * Linux: `sudo apt-get install openjdk-7-jdk`
      * Mac OS: Download from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)

##### Android Studio 1.2
  * Visit https://developer.android.com/sdk/installing/studio.html and follow the steps to install Android Studio.

##### (Optional) Tablet with Android 4.4.2 (KitKat) or higher
  * We use [Sony Xperia Z2](http://www.sonymobile.com/gb/products/tablets/xperia-z2-tablet/) tablets in the field: they are waterproof, have a great screen, and come with KitKat.  Otherwise, we recommend at least a 10" tablet; the app is designed for 1200 x 1920 resolution.  Without a tablet, you can use an emulator (see below).


### Android Studio project setup

1.  Get the Buendia client source code:

        git clone --recursive https://github.com/projectbuendia/client

2.  Launch Android Studio and click **Open an existing Android Studio project**.

3.  Navigate to the root directory of your `client` repo and click **Choose**.

4.  Wait a few minutes while Android Studio builds the project for the first time.

    * During the first build, it's normal to get errors about missing parts of the SDK (e.g. "Error: failed to find target android-21", "Error: failed to find Build Tools revision 19.1.0").  In each case, just click the suggested resolution to install the necessary SDK component (e.g. "Install missing platform(s) and sync project", "Install Build Tools 19.1.0 and sync project") until the build finishes without errors.

You are now ready to develop the Buendia client in Android Studio.

### Building and running the client on a tablet

On your tablet, enable Developer options by opening the Settings app, selecting **About tablet**, and tapping the Build number 7 times.  Then, in the Settings app under **Developer options**, turn on **USB debugging**.

Connect a USB cable from your computer to your tablet.  Click OK when asked if you want to allow USB debugging.

Click the Run button (green triangle in the toolbar at the top).  For **Module** select **app** and click **OK**.

Wait a few minutes for the app to build (you can see progress in the status bar at the bottom).  When it's done, Android Studio will automatically install it on the tablet and start it.

### Client tests

The client-side tests include both unit tests and functional tests, all located under the project's `androidTest` folder.  These tests run best on a real tablet attached to your computer.  (When run on an Android emulator, some tests work and some don't.)

You can run just the tests in a single file, or run all the tests under a given folder (such as the `androidTest/java` folder for all the tests in the entire project).  In the Project pane, right-click a file or folder, choose **Create Run Configuration**, and then choose **Android Tests** (the one with the icon containing the little green Android robot).  It's necessary to set the **Specific instrumentation runner** to `AndroidJUnitRunner`.  Then you can **Run** or **Debug** this run configuration to run the tests.

### Changing the default OpenMRS server settings

You can always manually change the OpenMRS server, username, and password on the Settings page in the app.  It's tedious to keep doing this every time the app is uninstalled and reinstalled, though.  To change the default settings, which are built into the app, edit these lines in `app/build.gradle`:

    def serverDefault = serverDev
    def openmrsUserDefault = 'buendia'
    def openmrsPasswordDefault = 'buendia'

By default, the client is built with its **Server** set to `dev.projectbuendia.org`, which is an instance of the OpenMRS server with dummy data that we use for development.  For release builds, we set this to simply `server`, which is the hostname of the Edison server in real deployments.  (When you set the **Server** to, say, `blarg`, the **OpenMRS base URL** is automatically set to `http://blarg:9000/openmrs`.  If you need to use a different port or path, you can edit the URL directly in the Advanced page in Settings.)

If you want to run the client with an OpenMRS server that you have built locally, you have three options:

  1. Deploy OpenMRS with your locally built server module on an Internet-accessible website; run the client on a real tablet with Internet access and set its **OpenMRS base URL** to point at that website
  2. Run OpenMRS on your own computer; assign your computer an IP address that's reachable from the tablet; run the client on a real tablet and set its **OpenMRS base URL** to point at your computer
  3. Run OpenMRS on your own computer; run the client in an Android emulator on your computer

An Android emulator runs significantly slower than a real tablet, but in terms of setup it's usually the quickest way to get a locally built client running with a locally built OpenMRS server.  Using a local client and local server also enables you to do development while completely offline.

### Using an emulator and a locally built OpenMRS server

To set up an emulator with some settings that are known to work:

  * In Android Studio, open **Tools** > **Android** > **AVD Manager**
  * Click **Create Virtual Device**
  * Click **New Hardware Profile** and select:
      * Device Type: Phone/Tablet
      * Screensize: 10.1 inches
      * Resolution: 1200 x 1920 px
      * Memory: RAM: 2 GB
      * Input: no hardware buttons, no keyboard
      * Navigation Style: None
      * Supported device states: Portrait only
      * Cameras: both front and back
      * Sensors: all sensors (Accelerometer, Gyroscope, GPS, Proximity)
      * Default Skin: No Skin
  * Click **OK** and with your new hardware profile selected, click **Next**
  * For **System Image**, choose the KitKat image with API level 19 and Target Android 4.4.2 and click **OK**
  * Click **Show Advanced Settings** and select:
      * Startup size and orientation:
          * Scale: Auto
          * Orientation: Portrait
      * Camera:
          * Front: None
          * Back: None
      * Network:
          * Speed: Full
          * Latency: None
      * Emulated Performance:
          * Host GPU: on
          * Store a snapshot for faster startup: off
      * Memory and Storage:
          * RAM: 2 GB
          * VM heap: 256 MB
          * Internal Storage: 1 GB
          * SD card: Studio-managed, 1 GB
      * Custom skin definition: No Skin
      * Keyboard:
          * Enable keyboard input: turn this off for a realistic simulation (on-screen soft keyboard); turn this on for the convenience of typing with your real keyboard instead of clicking the tablet keyboard
  * Click **Finish**

The emulated tablet will not have access to the Internet, but it will see your computer at IP address 10.0.2.2, so you'll need to run an OpenMRS server on your computer and then set the client's **OpenMRS base URL** to `http://10.0.2.2:9000/openmrs`.  If you edit `app/build.gradle` and change

    def openmrsRootUrlDefault = openmrsRootUrlDev;

to

    def openmrsRootUrlDefault = openmrsRootUrlLocalhost;

the client will have its server URL set to `http://10.0.2.2:9000/openmrs` by default.

### Android SDK packages

If you're using Android Studio, you don't need to worry about installing SDK packages; Android Studio will take care of it for you (see **Android Studio project setup** above).  You only need to install the packages yourself if you want to build the client from the command line.

The set of Android SDK packages needed to build the client is:

  * Android SDK Platform 5.0.1 (API level 21)
  * Android SDK Build-tools, revision 19.1
  * Android Support Library, revision 23
  * Android Support Repository, revision 17

The graphical Android SDK Manager at `$ANDROID_HOME/tools/android` will let you select and install these packages interactively; or you can install them all with the command:

    $ANDROID_HOME/tools/android update sdk --no-ui --all --filter android-21,build-tools-19.1.0,extra-android-support,extras-android-m2repository,platform-tools

`ANDROID_HOME` is usually `/opt/android-sdk-linux` on a Linux machine and `~/Library/Android/sdk` on a Mac.

To build the client from the command line, go to the root of your `client` repo and run `./gradlew clean assembleDebug`.  The resulting apk will be at `app/build/outputs/apk/app-debug.apk`.
