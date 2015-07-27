# Buendia client app

Follow these instructions to get your system set up to do Buendia client development.
See the [Buendia wiki](https://github.com/projectbuendia/buendia/wiki) for more details about the app.

## Prerequisites

##### JDK 7
  * If `java -version` does not report a version >= 1.7, install JDK 7 (available from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)).

##### Android Studio 1.2
  * Visit https://developer.android.com/sdk/installing/studio.html and follow the steps to install Android Studio.

## Android Studio project setup

1.  Get the Buendia client source code:

        git clone --recursive https://github.com/projectbuendia/client`

1.  Launch Android Studio and click "Open an existing Android Studio project".

1.  Navigate to the root directory of your `client` repo and click "Choose".

1.  Wait a few minutes while Android Studio builds the project for the first time.

    * It's normal to get errors about missing parts of the SDK (e.g. "Error: failed to find target android-21", "Error: failed to find Build Tools revision 19.1.0").  In each case, just click the suggested resolution to install the necessary SDK component (e.g. "Install missing platform(s) and sync project", "Install Build Tools 19.1.0 and sync project") until the build finishes without errors.

You are now ready to develop the Buendia client in Android Studio.

## Hardware

In the field, we use [Sony Xperia Z2](http://www.sonymobile.com/gb/products/tablets/xperia-z2-tablet/) tablets. These are a good choice because they are waterproof, have a great screen, and have a reasonably-new version of Android.

For development we recommend you at least use a 10" tablet. The Sony tablet mentioned above would be ideal, but you could try another device or emulator (e.g. a Nexus 9 or Nexus 10) with a sufficient resolution (the app is designed for 1920x1200).
