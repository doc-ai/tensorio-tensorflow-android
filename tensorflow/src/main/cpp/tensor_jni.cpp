//
// Created by Philip Dow on 10/6/20.
//

#include "tensor_jni.h"

#include <vector>

#include "tensorflow/core/public/session.h"

#include "jni_utils.h"

jstring GetTensorName(JNIEnv *env, jobject obj) {
    jclass c = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(c, "name", "Ljava/lang/String;");
    return (jstring) env->GetObjectField(obj, fieldId);
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_Tensor_create(JNIEnv *env, jobject thiz) {

    // TODO: Get dimensions
    // TODO: Get type

    std::vector<tensorflow::int64> dims;
    dims.push_back(1);

    tensorflow::gtl::ArraySlice<tensorflow::int64> dim_sizes(dims);
    auto shape = tensorflow::TensorShape(dim_sizes);

    auto tensor = new tensorflow::Tensor(tensorflow::DT_FLOAT, shape);

    // JNI Memory Management

    setHandle<tensorflow::Tensor>(env, thiz, tensor);
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_Tensor_delete(JNIEnv *env, jobject thiz) {
    auto tensor = getHandle<tensorflow::Tensor>(env, thiz);
    delete tensor;
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_Tensor_writeFloat(JNIEnv *env, jobject thiz, jfloat value) {
    auto tensor = getHandle<tensorflow::Tensor>(env, thiz);
    auto flat_tensor = tensor->flat<float_t>();
    auto buffer = flat_tensor.data();
    buffer[0] = value;
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_ai_doc_tensorflow_Tensor_readFloat(JNIEnv *env, jobject thiz) {
    auto tensor = getHandle<tensorflow::Tensor>(env, thiz);
    auto flat_tensor = tensor->flat<float_t>();
    auto buffer = flat_tensor.data();
    return buffer[0];
}