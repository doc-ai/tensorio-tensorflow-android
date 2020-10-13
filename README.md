# Tensor/IO TensorFlow Android

## Requirements:

Ensure the following have been installed in Android Studio:

- Android NDK 21.1.6352462
- Android Cmake 3.10.2

You can do this in Preferences: Appearance and Behavoir > System Settings > Android SDK > SDK Tools. Click on Show Package Contents at the bottom right and expand the NDK and CMake options to select the appropriate packages.

Additionally the library has a minSdk requirement of API 22.

## Installation

If you are downloading this repository over git, a number of large binary files are included with it. You must install [Git Large File Storage](https://git-lfs.github.com) first and configure it.

## Running the Example Application

After you have done this, simply open the project folder in Android Studio, let gradle and all that jazz do its job, select the app configuration, which is probably selected by default, select an x86 or x86_64 emulator or an Android device, and press play.

## Using the Library

Until we have jitpack support you must build the Android library on your machine to produce an AAR file, then from the project that will use this library import that file. We are following these instructions:

https://developer.android.com/studio/projects/android-library

First, open up the main project and build and run the app. This will prouduce the following two files:

```
tensorflow/build/outputs/aar/tensorflow-release.aar
tensorflow/build/outputs/aar/tensorflow-debug.aar
```

Next, from the project you want to use this library in, add the AAR. Choose **File** > **New** > **New Module** and select **Import .JAR/.AAR Package**. Select the *tensorflow-release.aar* build output from the previous step.

Make sure the library is listed in your *settings.gradle* file. Android Studio probably takes care of this automatically:

```groovy
include ':tensorflow-release'
include ':app'
```

Finally add the library as a dependency in your app's *build.gradle* file:

```groovy
dependencies {
	implementation project(":tensorflow-release")
}
```

You may also need to make the following changes to your app's *build.gradle*. Ensure the minSdk version is at least API 22:

```groovy
defaultConfig {
	minSdkVersion 22
}
```

If you see an error about "More than one file found with OS independent path" add the following to the android section of the gradle file:

```groovy
packagingOptions {
    pickFirst 'lib/armeabi-v7a/libc++_shared.so'
    pickFirst 'lib/arm64-v8a/libc++_shared.so'
    pickFirst 'lib/x86/libc++_shared.so'
    pickFirst 'lib/x86_64/libc++_shared.so'
}
```
