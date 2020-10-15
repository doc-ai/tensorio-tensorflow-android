/*
 * tensor_jni.h
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
