# Buendia client app

Follow these instructions to get your system set up to do Buendia client development.
See the [Buendia wiki](https://github.com/projectbuendia/buendia/wiki) for more details about the app.

## Prerequisites

##### JDK 7
  * If `java -version` does not report a version >= 1.7, install JDK 7:
      * Linux: `sudo apt-get install openjdk-7-jdk`
      * Mac OS: Download from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)

##### Android Studio 1.2
  * Visit https://developer.android.com/sdk/installing/studio.html and follow the steps to install Android Studio.

## Android Studio project setup

1.  Get the Buendia client source code:

        git clone --recursive https://github.com/projectbuendia/client`

2.  Launch Android Studio and click "Open an existing Android Studio project".

3.  Navigate to the root directory of your `client` repo and click "Choose".

4.  Wait a few minutes while Android Studio builds the project for the first time.

    * During the first build, it's normal to get errors about missing parts of the SDK (e.g. "Error: failed to find target android-21", "Error: failed to find Build Tools revision 19.1.0").  In each case, just click the suggested resolution to install the necessary SDK component (e.g. "Install missing platform(s) and sync project", "Install Build Tools 19.1.0 and sync project") until the build finishes without errors.

You are now ready to develop the Buendia client in Android Studio.

## Building and running the client

On your tablet, enable Developer options by opening the Settings app, selecting **About tablet**, and tapping the Build number 7 times.  Then, in the Settings app under **Developer options**, turn on **USB debugging**.

Connect a USB cable from your computer to your tablet.  Click OK when asked if you want to allow USB debugging.

Click the Run button (green triangle in the toolbar at the top).  For **Module** select **app** and click OK.

Wait a few minutes for the app to build (you can see progress in the status bar at the bottom).  When it's done, Android Studio will automatically install it on the tablet and start it.

The default setting for the server URL is configured in app/build.gradle.

## Client tests

The client-side tests include both unit tests and functional tests, all located under the project's `androidTest` folder.  These tests run either on a tablet attached to your computer running Android Studio, or in an Android emulator (AVD).

You can run just the tests in a single file, or run all the tests under a given folder (such as the `androidTest/java` folder for all the tests in the entire project).  In the Project pane, right-click a file or folder, choose **Create Run Configuration**, and then choose **Android Tests** (the one with the icon containing the little green Android robot).  For the **Specific instrumentation runner**, select `GoogleInstrumentationTestRunner`.  Then you can run or debug this configuration to run the tests.  In the run configuration, you can choose the Target Device (**USB device** to use an attached tablet, or **Emulator** to use an emulator).

## Hardware

In the field, we use [Sony Xperia Z2](http://www.sonymobile.com/gb/products/tablets/xperia-z2-tablet/) tablets. These are a good choice because they are waterproof, have a great screen, and have a reasonably-new version of Android.

For development we recommend you at least use a 10" tablet. The Sony tablet mentioned above would be ideal, but you could try another device or emulator (e.g. a Nexus 9 or Nexus 10) with a sufficient resolution (the app is designed for 1920x1200).
