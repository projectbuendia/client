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

##### JDK 7 or higher
  * If `java -version` reports a version lower than 1.7, install JDK 7:
      * Linux: `sudo apt-get install openjdk-7-jdk`
      * Mac OS: Download from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
  * *Note*: JDK 8 works fine for client development, but to run the [OpenMRS server](//github.com/projectbuendia/buendia) you have to use JDK 7, not 8.

##### Android Studio
  * Visit https://developer.android.com/sdk/installing/studio.html and follow the steps to install Android Studio.

##### A tablet running Android 4.4.2 or higher (optional)
  * We have tested the application with [Sony Xperia Z2](http://www.sonymobile.com/gb/products/tablets/xperia-z2-tablet/) tablets in the field: they are waterproof, have a great screen, and come with KitKat.  However, in 2019 these are no longer in production.
  * Any Android tablet should do fine. We recommend a tablet with at least an 8-inch display; the app is designed for 1200 x 1920 resolution.
  * The app will also run on an Android phone. The user interface can be scaled down to be usable on a phone (though a bit unwieldy).
  * You can also use an emulator instead of a real device (see below).

##### Set up your computer to communicate with your tablet or phone
  * You can skip this step if you're using an emulator (see below).
  * On the device, enable Developer options by opening the Settings app, selecting **Software options**, and tapping the Build number 7 times.
  * In the Settings app under **Developer options**, turn on **USB debugging**.
  * Connect a USB cable from your computer to your device.  Click OK when asked if you want to allow USB debugging.
  * Now you need to set up your computer:
      * If you're developing on Mac OS X, it just works. You're done.
      * If you're developing on Windows, you need to install a USB driver for adb. For an installation guide and links to OEM drivers, see this OEM USB Drivers [document](http://developer.android.com/tools/extras/oem-usb.html).
      * If you're developing on Ubuntu Linux, you need to add a `udev` rules file that contains a USB configuration for each type of device you want to use for development. In the rules file, each device manufacturer is identified by a unique vendor ID, as specified by the `ATTR{idVendor}` property. For a list of vendor IDs, see [USB Vendor IDs](http://developer.android.com/tools/device.html#VendorIds). To set up device detection on Ubuntu Linux.
          * Log in as root and create this file: /etc/udev/rules.d/51-android.rules.
          * Use this format to add each vendor to the file: `SUBSYSTEM=="usb", ATTR{idVendor}=="054c", MODE="0666", GROUP="plugdev"`. In this example, the vendor ID **054c** is for **Sony**. The MODE assignment specifies read/write permissions, and GROUP defines which Unix group owns the device node.
          * Now execute: `chmod a+r /etc/udev/rules.d/51-android.rules`.


### Android Studio project setup

1.  Get the Buendia client source code:

        git clone --recursive git@github.com:projectbuendia/client.git

        git submodule update --init

2.  Launch Android Studio and click **Open an existing Android Studio project**.

3.  Navigate to the root directory of your `client` repo, select the `build.gradle` file there and click **Open**.

4.  Wait a few minutes while Android Studio builds the project for the first time.

    * During the first build, it's normal to get errors about missing parts of the SDK (e.g. "Error: failed to find target android-21", "Error: failed to find Build Tools revision 19.1.0").  In each case, just click the suggested resolution to install the necessary SDK component (e.g. "Install missing platform(s) and sync project", "Install Build Tools 19.1.0 and sync project") until the build finishes without errors.

You are now ready to develop the Buendia client in Android Studio.


### Building and running the client on a tablet

Connect a USB cable from your computer to your device.

On Android Studio, click the Run button (green triangle in the toolbar at the top).  For **Module** select **app** and click **OK**.

You should see your device appear in the "Select Deployment Target" window.  Select it and click **OK**.

Wait a few minutes for the app to build (you can see progress in the status bar at the bottom).  When it's done, Android Studio will automatically install it on the tablet and start it.

By default, the app is configured to use a server address pointing at a server on the Internet that contains dummy data.  Feel free to add and edit users and patients.


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

### Faking Tablet Resolution on Non Tablet Devices

#### Resolution of field devices

In the field, the Buendia project uses Sony Xperia Z2 Tablets. The user interface, has thus been written assuming a tablet interface, and in particular, assuming the specific resolution and DPI of the Sony Xperia Z2.

You can determine the specific resolution / DPI of the Sony Xperia Z2 Tablet by plugging it into a computer and running the following commands (make sure you have [`adb` set up and USB debugging turned on](https://github.com/projectbuendia/buendia/wiki/Debug-and-Run-Buendia-in-Android-Studio-using-a-Xperia-z2-Tablet)):

    $ adb shell wm size
    Physical size: 1920x1200
    $ adb shell wm density
    Physical density: 240

#### Faking the resolution on an Android phone

If you've got an Android _phone_, but not an Android _tablet_, it's still possible to develop the Buendia app, but it will be much easier if you fake the screen resolution of the tablet on your phone. You can do this by plugging your Android device into a computer, and running:

    $ adb shell wm size 1920x1200
    $ adb shell wm density 240
    $ adb reboot

**Note:**

- You may need to use a size of `1200x1920` instead of `1920x1200`; it depends on the default orientation of your phone screen.
- It's important to reboot because most apps assume that the density (in particular) doesn't change over the life of the application.
- These settings persist through reboots.

#### Resetting your Android phone to normal
Once you're finished developing, you probably want to be able to use your phone as normal again :smile:

I suggest the following commands:

    $ adb shell wm size reset
    $ adb shell wm density reset
    $ adb reboot

#### Testing Notes

- Works on a release-build Nexus 5 running Lollipop; your mileage may vary.

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
