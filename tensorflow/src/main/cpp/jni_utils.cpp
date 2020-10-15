/*
 * jni_utils.cpp
 * TensorIO TensorFlow
 *
 * Created by Philip Dow on 10/6/20
 * Copyright (c) 2020 - Present doc.ai (http://doc.ai)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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