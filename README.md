# Tensor/IO TensorFlow Android

## Requirements:

Ensure the following have been installed in Android Studio:

- Android NDK 19.2.5345600
- Android Cmake 3.10.2

You can do this in Preferences: Appearance and Behavoir > System Settings > Android SDK > SDK Tools. Click on Show Package Contents at the bottom right and expand the NDK and CMake options to select the appropriate packages.

## Installation

If you are downloading this repository over git, a number of large binary files are included with it. You must install [Git Large File Storage](https://git-lfs.github.com) first and configure it.

## Running the App

After you have done this, simply open the project folder in Android Studio, let gradle and all that jazz do its job, select the app configuration, which is probably selected by default, select an x86 or x86_64 emulator or an Android device, and press play.