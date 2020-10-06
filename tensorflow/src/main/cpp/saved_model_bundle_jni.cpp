//
// Created by Philip Dow on 10/5/20.
//

#include <jni.h>
#include <string>
#include <unordered_set>
#include <vector>

#include "tensorflow/cc/saved_model/loader.h"
#include "tensorflow/cc/saved_model/tag_constants.h"
#include "tensorflow/core/public/session.h"

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

extern "C" JNIEXPORT jobject JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_load(JNIEnv* env, jclass clazz, jstring dir) {
    tensorflow::SavedModelBundle *saved_model_bundle = new tensorflow::SavedModelBundle();
    jobject bundle = nullptr;

    const char *c_dir = env->GetStringUTFChars(dir, nullptr);
    std::string s_dir = c_dir;

    // TensorFlow: Load Model

    std::unordered_set<std::string> tags = {tensorflow::kSavedModelTagServe};

    tensorflow::SessionOptions session_opts;
    tensorflow::RunOptions run_opts;
    tensorflow::Status status;

    status = LoadSavedModel(session_opts, run_opts, s_dir, tags, saved_model_bundle);

    if ( status != tensorflow::Status::OK() ) {
        bundle = nullptr;
    } else {
        bundle = nullptr;
    }

    // JNI Memory Management

    jmethodID method = env->GetStaticMethodID(clazz, "fromHandle","(J)Lai/doc/tensorflow/SavedModelBundle;");
    bundle = env->CallStaticObjectMethod(clazz, method, reinterpret_cast<jlong>(saved_model_bundle));

    // To go the other direction:
    // reinterpret_cast<tensorflow::SavedModelBundle*>(handle)

    // Cleanup

    env->ReleaseStringUTFChars(dir, c_dir);
    saved_model_bundle = nullptr;

    // Exit

    return bundle;
}

extern "C" JNIEXPORT jstring JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_run(JNIEnv *env, jobject thiz, jlong handle) {
    tensorflow::SavedModelBundle *saved_model_bundle = reinterpret_cast<tensorflow::SavedModelBundle*>(handle);
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
        std::string error_string = "Error running model: " + status.error_message();
        return env->NewStringUTF(error_string.c_str());
    }

    tensorflow::Tensor output = outputs[0];
    float value = ReadTensor(output);

    std::string hello = "The value is: " + std::to_string(value);
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_unload(JNIEnv *env, jobject thiz, jlong handle) {
    tensorflow::SavedModelBundle *saved_model_bundle = reinterpret_cast<tensorflow::SavedModelBundle*>(handle);

    // TensorFlow: Unload Model

    saved_model_bundle->session.get()->Close();

    // Cleanup

    delete saved_model_bundle;
}