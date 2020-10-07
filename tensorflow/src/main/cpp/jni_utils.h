//
// Created by Philip Dow on 10/6/20.
//

#ifndef TENSORIO_TENSORFLOW_JNI_UTILS_H
#define TENSORIO_TENSORFLOW_JNI_UTILS_H

#include <jni.h>
#include <string>

// Native Handles
// See http://www.studiofuga.com/2017/03/10/a-c-smart-pointer-wrapper-for-use-with-jni/

jfieldID inline getHandleField(JNIEnv *env, jobject obj)
{
    jclass c = env->GetObjectClass(obj);
    // J is the type signature for long:
    return env->GetFieldID(c, "handle", "J");
}

template <typename T>
T *getHandle(JNIEnv *env, jobject obj)
{
    jlong handle = env->GetLongField(obj, getHandleField(env, obj));
    return reinterpret_cast<T *>(handle);
}

template <typename T>
void setHandle(JNIEnv *env, jobject obj, T *t)
{
    jlong handle = reinterpret_cast<jlong>(t);
    env->SetLongField(obj, getHandleField(env, obj), handle);
}

// Conversion Utilities

std::string jstring2string(JNIEnv *env, jstring javaString);

// Exceptions
// See tensorflow/tflite/java

extern const char kIllegalArgumentException[];

void ThrowException(JNIEnv* env, const char* clazz, const char* fmt, ...);

#endif //TENSORIO_TENSORFLOW_JNI_UTILS_H
