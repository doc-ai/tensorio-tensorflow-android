//
// Created by Philip Dow on 10/6/20.
//

#include "jni_utils.h"

#include <cstdarg>
#include <cstdlib>

// Conversion Utilities

std::string jstring2string(JNIEnv *env, jstring javaString) {
    const char *chars = env->GetStringUTFChars(javaString, nullptr);
    std::string cppString = chars;

    env->ReleaseStringUTFChars(javaString, chars);

    return cppString;
}

// Exceptions

const char kIllegalArgumentException[] = "java/lang/IllegalArgumentException";

void ThrowException(JNIEnv* env, const char* clazz, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    const size_t max_msg_len = 512;
    auto* message = static_cast<char*>(malloc(max_msg_len));
    if (message && (vsnprintf(message, max_msg_len, fmt, args) >= 0)) {
        env->ThrowNew(env->FindClass(clazz), message);
    } else {
        env->ThrowNew(env->FindClass(clazz), "");
    }
    if (message) {
        free(message);
    }
    va_end(args);
}