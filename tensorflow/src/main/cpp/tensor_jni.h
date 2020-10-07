//
// Created by Philip Dow on 10/6/20.
//

#ifndef TENSORIO_TENSORFLOW_TENSOR_JNI_H
#define TENSORIO_TENSORFLOW_TENSOR_JNI_H

#include <jni.h>

enum TensorDataType {
    kDTypeFloat32   = 1,
    kDTypeInt32     = 2,
    kDTypeInt8      = 3,
    kDTypeInt64     = 4,
    kDTypeString    = 5
};

jstring GetTensorName(JNIEnv *env, jobject obj);

#endif //TENSORIO_TENSORFLOW_TENSOR_JNI_H
