//
// Created by Philip Dow on 10/5/20.
//

#include <jni.h>
#include <android/log.h>

#include <string>
#include <unordered_set>
#include <vector>

#include "tensorflow/cc/saved_model/loader.h"
#include "tensorflow/cc/saved_model/tag_constants.h"
#include "tensorflow/core/public/session.h"

#include "jni_utils.h"
#include "tensor_jni.h"

jobject DataType(JNIEnv *env, const char* name) {
    jclass klass = env->FindClass("ai/doc/tensorflow/DataType");
    jfieldID fieldId = env->GetStaticFieldID(klass, name,"Lai/doc/tensorflow/DataType;");
    return env->GetStaticObjectField(klass, fieldId);
}

tensorflow::Tensor CreateTensor(float value) {
    std::vector<tensorflow::int64> dims;
    dims.push_back(1);

    tensorflow::gtl::ArraySlice<tensorflow::int64> dim_sizes(dims);
    tensorflow::TensorShape shape = tensorflow::TensorShape(dim_sizes);
    tensorflow::Tensor tensor(tensorflow::DT_FLOAT, shape);

    auto flat_tensor = tensor.flat<float_t>();
    auto buffer = flat_tensor.data();

    buffer[0] = value;

    return tensor;
}

float ReadTensor(tensorflow::Tensor tensor) {
    auto flat_tensor = tensor.flat<float_t>();
    auto buffer = flat_tensor.data();
    float val = buffer[0];

    return val;
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_create(JNIEnv *env, jobject thiz, jstring dir) {
    auto saved_model_bundle = new tensorflow::SavedModelBundle();

    const char *c_dir = env->GetStringUTFChars(dir, nullptr);
    std::string s_dir = c_dir;

    // TensorFlow: Load Model

    std::unordered_set<std::string> tags = {tensorflow::kSavedModelTagServe};

    tensorflow::SessionOptions session_opts;
    tensorflow::RunOptions run_opts;
    tensorflow::Status status;

    status = LoadSavedModel(session_opts, run_opts, s_dir, tags, saved_model_bundle);

    if ( status != tensorflow::Status::OK() ) {
        __android_log_print(ANDROID_LOG_VERBOSE, "Tensor/IO TensorFlow", "LoadSavedModel Status not OK");
        ThrowException(env, kIllegalArgumentException,"Internal error: Unable to load model.");
        return;
    }

    // JNI Memory Management

    setHandle<tensorflow::SavedModelBundle>(env, thiz, saved_model_bundle);

    // Cleanup

    env->ReleaseStringUTFChars(dir, c_dir);
    saved_model_bundle = nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_delete(JNIEnv *env, jobject thiz /*, jlong handle*/) {
    auto saved_model_bundle = getHandle<tensorflow::SavedModelBundle>(env, thiz);
    tensorflow::Status status;

    // TensorFlow: Unload Model

    status = saved_model_bundle->session->Close();

    // Cleanup

    delete saved_model_bundle;
}


extern "C"
JNIEXPORT jstring JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_run(JNIEnv *env, jobject thiz /*, jlong handle*/) {
    auto saved_model_bundle = getHandle<tensorflow::SavedModelBundle>(env, thiz);
    tensorflow::Status status;

    // TensorFlow: Run Model

    tensorflow::Tensor input_tensor = CreateTensor(2);
    std::vector<tensorflow::Tensor> outputs;

    std::string input_name = "input";
    std::string output_name = "output";

    std::pair<std::string, tensorflow::Tensor> input = std::pair<std::string, tensorflow::Tensor>(input_name, input_tensor);
    std::vector<std::pair<std::string, tensorflow::Tensor>> inputs;
    inputs.push_back(input);

    std::vector<std::string> output_names;
    output_names.push_back(output_name);

    tensorflow::Session *session = saved_model_bundle->session.get();
    status = session->Run(inputs, output_names, {}, &outputs);

    if ( status != tensorflow::Status::OK() ) {
        __android_log_print(ANDROID_LOG_VERBOSE, "Tensor/IO TensorFlow", "%s", status.error_message().c_str());
        ThrowException(env, kIllegalArgumentException,"Internal error: Unable to run model.");
        return nullptr;
    }

    tensorflow::Tensor output = outputs[0];
    float value = ReadTensor(output);

    std::string hello = "The value is: " + std::to_string(value);
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jobject JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_runTensor(JNIEnv *env, jobject thiz, jobject tensor) {
    auto saved_model_bundle = getHandle<tensorflow::SavedModelBundle>(env, thiz);
    auto input_tensor = *getHandle<tensorflow::Tensor>(env, tensor);
    tensorflow::Status status;

    // Get name of input tensor

    jstring input_name_jstring = GetTensorName(env, tensor);
    const char *input_name_chars = env->GetStringUTFChars(input_name_jstring, nullptr);
    std::string input_name = input_name_chars;

    env->ReleaseStringUTFChars(input_name_jstring, input_name_chars);

    // Prepare inputs

    std::pair<std::string, tensorflow::Tensor> input = std::pair<std::string, tensorflow::Tensor>(
            input_name, input_tensor);
    std::vector<std::pair<std::string, tensorflow::Tensor>> inputs;
    inputs.push_back(input);

    // Get name of output tensor

    std::string output_name = "output";

    // Prepare output names

    std::vector<std::string> output_names;
    output_names.push_back(output_name);

    std::vector<tensorflow::Tensor> outputs;

    tensorflow::Session *session = saved_model_bundle->session.get();
    status = session->Run(inputs, output_names, {}, &outputs);

    if (status != tensorflow::Status::OK()) {
        __android_log_print(ANDROID_LOG_VERBOSE, "Tensor/IO TensorFlow", "%s",
                            status.error_message().c_str());
        ThrowException(env, kIllegalArgumentException, "Internal error: Unable to run model.");
        return nullptr;
    }

    // Get Output

    tensorflow::Tensor output = outputs[0];
    tensorflow::Tensor *outputPtr = new tensorflow::Tensor(output);

    // Return Java Tensor

    jobject dtype = DataType(env, "FLOAT32");
    jstring name = env->NewStringUTF("output");

    jintArray shape = env->NewIntArray(1);
    jint anint[1];
    anint[0] = 1;
    env->SetIntArrayRegion(shape, 0, 1, anint);

    jclass tensorClass = env->FindClass("ai/doc/tensorflow/Tensor");
    jmethodID constructorId = env->GetMethodID(tensorClass, "<init>",
                                               "(Lai/doc/tensorflow/DataType;[ILjava/lang/String;J)V");
    jobject tensorObject = env->NewObject(tensorClass, constructorId, dtype, shape, name,
                                          reinterpret_cast<jlong>(outputPtr));

    return tensorObject;
}
