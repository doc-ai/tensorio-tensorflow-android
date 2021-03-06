/*
 * tensor_jni.cpp
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

#include "tensor_jni.h"

#include <vector>
#include <android/log.h>

#include "tensorflow/core/public/session.h"

#include "jni_utils.h"

jstring GetTensorName(JNIEnv *env, jobject obj) {
    jclass c = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(c, "name", "Ljava/lang/String;");
    return (jstring) env->GetObjectField(obj, fieldId);
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_Tensor_create(JNIEnv *env, jobject thiz, jint dtype, jboolean scalar, jintArray shape) {

    // Prepare Shape

    std::vector<tensorflow::int64> dims;
    jint *jShape = env->GetIntArrayElements(shape, nullptr);
    jsize shapeCount = env->GetArrayLength(shape);

    // If scalar first dimension may be -1 or fixed batch size, but all other dimensions will be 1 and can be ignored

    if (scalar) {
        dims.push_back(jShape[0]);
    } else {
        for (jsize i = 0; i < shapeCount; i++) {
            dims.push_back(jShape[i]);
        }
    }

    tensorflow::gtl::ArraySlice<tensorflow::int64> dim_sizes(dims);
    auto tensorShape = tensorflow::TensorShape(dim_sizes);

    // Prepare Data Type

    auto tensorType = static_cast<tensorflow::DataType>(dtype);

    // Create Tensor

    auto tensor = new tensorflow::Tensor(tensorType, tensorShape);

    // JNI Memory Management

    setHandle<tensorflow::Tensor>(env, thiz, tensor);
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_Tensor_delete(JNIEnv *env, jobject thiz) {
    delete getHandle<tensorflow::Tensor>(env, thiz);
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_Tensor_writeBytes(JNIEnv *env, jobject thiz, jobject src, jlong size, jint dtype) {
    auto tensor = getHandle<tensorflow::Tensor>(env, thiz);
    auto tensorType = static_cast<tensorflow::DataType>(dtype);
    void *buffer = nullptr;

    char* src_data_raw = static_cast<char*>(env->GetDirectBufferAddress(src));

    if (!src_data_raw) {
        ThrowException(env, kIllegalArgumentException,"Input ByteBuffer is not a direct buffer");
        return;
    }

    switch (tensorType) {
        case tensorflow::DT_FLOAT: {
            auto flat = tensor->flat<float_t>();
            buffer = flat.data();
            break;
        }
        case tensorflow::DT_UINT8: {
            auto flat = tensor->flat<uint8_t>();
            buffer = flat.data();
            break;
        }
        case tensorflow::DT_INT32: {
            auto flat = tensor->flat<int32_t>();
            buffer = flat.data();
            break;
        }
        case tensorflow::DT_INT64: {
            // int64_t ends up typed to long?
            // which throws a DataTypeToEnum "Specified Data Type not supported error in tensorflow/core/framework/types.h
            auto flat = tensor->flat<long long>();
            buffer = flat.data();
            break;
        }
        default:
            __android_log_print(ANDROID_LOG_ERROR, "Tensor/IO TensorFlow", "Illegal tensor type requested, must be one of float, uint8, int32, or int64");
            ThrowException(env, kIllegalArgumentException, "Internal Error: Tensor:.writeBytes: Illegal tensor type requested");
            break;
    }

    // works
    memcpy(buffer, src_data_raw, size);

    // doesn't work
    // buffer = (float*)src_data_raw;

    // works
    // buffer[0] = ((float*)src_data_raw)[0];
}

extern "C"
JNIEXPORT jobject JNICALL
Java_ai_doc_tensorflow_Tensor_readBytes(JNIEnv *env, jobject thiz, jlong size, jint dtype) {
    auto tensor = getHandle<tensorflow::Tensor>(env, thiz);
    auto tensorType = static_cast<tensorflow::DataType>(dtype);
    void *buffer = nullptr;

    switch (tensorType) {
        case tensorflow::DT_FLOAT: {
            auto flat = tensor->flat<float_t>();
            buffer = flat.data();
            break;
        }
        case tensorflow::DT_UINT8: {
            auto flat = tensor->flat<uint8_t>();
            buffer = flat.data();
            break;
        }
        case tensorflow::DT_INT32: {
            auto flat = tensor->flat<int32_t>();
            buffer = flat.data();
            break;
        }
        case tensorflow::DT_INT64: {
            // int64_t ends up typed to long?
            // which throws a DataTypeToEnum "Specified Data Type not supported error in tensorflow/core/framework/types.h
            auto flat = tensor->flat<long long>();
            buffer = flat.data();
            break;
        }
        default:
            __android_log_print(ANDROID_LOG_ERROR, "Tensor/IO TensorFlow", "Illegal tensor type requested, must be one of float, uint8, int32, or int64");
            ThrowException(env, kIllegalArgumentException, "Internal Error: Tensor.readBytes: Illegal tensor type requested");
            break;
    }

    return env->NewDirectByteBuffer(buffer, size);
}
