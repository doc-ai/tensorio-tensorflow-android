//
// Created by Philip Dow on 10/6/20.
//

#include "tensor_jni.h"

#include <vector>

#include "tensorflow/core/public/session.h"

#include "jni_utils.h"

// tensor->flat<float_t>()
// tensor->flat<uint8_t>()
// tensor->flat<int32_t>()
// tensor->flat<int64_t>()

jstring GetTensorName(JNIEnv *env, jobject obj) {
    jclass c = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(c, "name", "Ljava/lang/String;");
    return (jstring) env->GetObjectField(obj, fieldId);
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_Tensor_create(JNIEnv *env, jobject thiz, jint dtype, jintArray shape) {

    // Prepare Shape

    std::vector<tensorflow::int64> dims;
    jint *jShape = env->GetIntArrayElements(shape, nullptr);
    jsize shapeCount = env->GetArrayLength(shape);

    for (jsize i = 0; i < shapeCount; i++) {
        dims.push_back(jShape[i]);
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
Java_ai_doc_tensorflow_Tensor_writeBytes(JNIEnv *env, jobject thiz, jobject src, jlong size) {
    // TODO: flat depends on dtype

    auto tensor = getHandle<tensorflow::Tensor>(env, thiz);
    auto flat_tensor = tensor->flat<float_t>();
    auto buffer = flat_tensor.data();

    char* src_data_raw = static_cast<char*>(env->GetDirectBufferAddress(src));

    if (!src_data_raw) {
        ThrowException(env, kIllegalArgumentException,"Input ByteBuffer is not a direct buffer");
        return;
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
Java_ai_doc_tensorflow_Tensor_readBytes(JNIEnv *env, jobject thiz, jlong size) {
    // TODO: flat depends on dtype

    auto tensor = getHandle<tensorflow::Tensor>(env, thiz);
    auto flat_tensor = tensor->flat<float_t>();
    auto buffer = flat_tensor.data();

    // tensor->flat<float_t>()
    // tensor->flat<uint8_t>()
    // tensor->flat<int32_t>()
    // tensor->flat<int64_t>()

    return env->NewDirectByteBuffer(buffer, size);
}



// Reference Code

// Read and Write Single Floats

//extern "C"
//JNIEXPORT void JNICALL
//Java_ai_doc_tensorflow_Tensor_writeFloat(JNIEnv *env, jobject thiz, jfloat value) {
//    auto tensor = getHandle<tensorflow::Tensor>(env, thiz);
//    auto flat_tensor = tensor->flat<float_t>();
//    auto buffer = flat_tensor.data();
//
//    float *ptr = (float*) malloc(sizeof(float)*1);
//    ptr[0] = value;
//
//    // doesn't work
//    // buffer = ptr;
//
//    // works
//    // buffer[0] = ptr[0];
//
//    memcpy(buffer, ptr, sizeof(float)*1);
//
//    // works
//    // buffer[0] = value;
//
//    free(ptr);
//}
//
//extern "C"
//JNIEXPORT jfloat JNICALL
//Java_ai_doc_tensorflow_Tensor_readFloat(JNIEnv *env, jobject thiz) {
//    auto tensor = getHandle<tensorflow::Tensor>(env, thiz);
//    auto flat_tensor = tensor->flat<float_t>();
//    auto buffer = flat_tensor.data();
//    return buffer[0];
//}