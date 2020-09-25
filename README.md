# Tensor/IO TensorFlow Android

## Requirements:

Ensure the following have been installed in Android Studio's Preferences: Appearance and Behavoir > System Settings > Android SDK > SDK Tools. Click on Show Package Contents at the bottom right and expand the NDK and CMake options to select the appropriate packages.

- Android NDK 19.2.5345600
- Android Cmake 3.4.1+

## Installation

The binary libs used by this project are not included in the GitHub repo. Libraries for three architectures are available. Download all of them from:

```
gs://tensorio-build/android/release/1.15/ndk/19.2.5345600/api/22/arch/
  arm64-v8a/
    libnsync.a
    libprotobuf.so
    libtensorflow-core.a
  x86/
    libnsync.a
    libprotobuf.so
    libtensorflow-core.a
  x86_64/
    libnsync.a
    libprotobuf.so
    libtensorflow-core.a
```

And place them in the *distribution/tensorflow/lib/* directory. The directory should look like this when you are finished:

```
distribution/tensorflow/lib/
├── arm64-v8a
│   ├── libnsync.a
│   ├── libprotobuf.so
│   └── libtensorflow-core.a
├── x86
│   ├── libnsync.a
│   ├── libprotobuf.so
│   └── libtensorflow-core.a
└── x86_64
    ├── libnsync.a
    ├── libprotobuf.so
    └── libtensorflow-core.a
```

## Running the App

After you have done this, simply open the project folder in Android Studio let gradle and all that jazz do its job, select the app configuration, which is probably selected by default, and select an x86 or x86_64 emulator or an Android device, then press play.