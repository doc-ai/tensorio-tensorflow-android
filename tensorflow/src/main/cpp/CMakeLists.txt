# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Set distribution directory, contains static libraries and header files

set(distribution_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../distribution)

## BUILD FLAGS ##

# See tensorflow/examples/android/jni and tensorflow/contrib/android/cmake
# For more information on why these flags are required:
# https://github.com/tensorflow/tensorflow/issues/3308

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} \
                              -Wl,--allow-multiple-definition \
                              -Wl,--whole-archive -fPIE -v")

## STATIC LIBRARIES ##

add_library(libnsync STATIC IMPORTED)
set_target_properties(libnsync PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/tensorflow/lib/${ANDROID_ABI}/libnsync.a)

add_library(libprotobuf SHARED IMPORTED)
set_target_properties(libprotobuf PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/tensorflow/lib/${ANDROID_ABI}/libprotobuf.so)

add_library(libtensorflow-core STATIC IMPORTED)
set_target_properties(libtensorflow-core PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/tensorflow/lib/${ANDROID_ABI}/libtensorflow-core.a)

## LOCAL LIBRARY ##

add_library(tensorio-tensorflow SHARED
        jni_utils.cpp
        tensor_jni.cpp
        saved_model_bundle_jni.cpp)

## HEADER SEARCH PATHS ##

target_include_directories(tensorio-tensorflow PRIVATE ${distribution_DIR}/tensorflow/include)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(
        tensorio-tensorflow
        android
        libtensorflow-core
        libprotobuf
        libnsync
        z
        log )
