/*
 * saved_model_bundle_jni.cpp
 * TensorIO TensorFlow
 *
 * Created by Philip Dow on 10/5/20
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

// TODO: They aren't all illegal argument exceptions

jobject DataType(JNIEnv *env, const char* name) {
    jclass klass = env->FindClass("ai/doc/tensorflow/DataType");
    jfieldID fieldId = env->GetStaticFieldID(klass, name,"Lai/doc/tensorflow/DataType;");
    return env->GetStaticObjectField(klass, fieldId);
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_create(JNIEnv *env, jobject thiz, jstring dir, jstring mode) {
    auto saved_model_bundle = new tensorflow::SavedModelBundle();
    auto sDir = jstring2string(env, dir);

    // TensorFlow: Load Model

    auto modeString = jstring2string(env, mode);
    std::unordered_set<std::string> tags;

    if (modeString == "serve") {
        tags = {tensorflow::kSavedModelTagServe};
    }  else if (modeString == "train") {
        tags = {tensorflow::kSavedModelTagTrain};
    } else {
        __android_log_print(ANDROID_LOG_VERBOSE, "Tensor/IO TensorFlow", "Mode must be one of 'serve' or 'train'");
        ThrowException(env, kIllegalArgumentException,"Internal error: Bad mode provided.");
        return;
    }

    tensorflow::SessionOptions session_opts;
    tensorflow::RunOptions run_opts;
    tensorflow::Status status;

    status = LoadSavedModel(session_opts, run_opts, sDir, tags, saved_model_bundle);

    if ( status != tensorflow::Status::OK() ) {
        __android_log_print(ANDROID_LOG_VERBOSE, "Tensor/IO TensorFlow", "LoadSavedModel Status not OK");
        ThrowException(env, kIllegalArgumentException,"Internal error: Unable to load model.");
        return;
    }

    // JNI Memory Management

    setHandle<tensorflow::SavedModelBundle>(env, thiz, saved_model_bundle);
    saved_model_bundle = nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_delete(JNIEnv *env, jobject thiz /*, jlong handle*/) {
    auto saved_model_bundle = getHandle<tensorflow::SavedModelBundle>(env, thiz);
    tensorflow::Status status = saved_model_bundle->session->Close();
    delete saved_model_bundle;
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_run(JNIEnv *env, jobject thiz, jobjectArray inputTensors, jobjectArray outputTensors) {
    auto saved_model_bundle = getHandle<tensorflow::SavedModelBundle>(env, thiz);
    tensorflow::Status status;

    // Prepare Inputs
    // Inputs are pairs of names and tensors

    std::vector<std::pair<std::string, tensorflow::Tensor>> inputs;
    jsize inputCount = env->GetArrayLength(inputTensors);

    for (jsize i = 0; i < inputCount; i++) {
        jobject inputTensor = env->GetObjectArrayElement(inputTensors, i);
        auto input_tensor = *getHandle<tensorflow::Tensor>(env, inputTensor);
        auto input_name = jstring2string(env, GetTensorName(env, inputTensor));
        auto input = std::pair<std::string, tensorflow::Tensor>(input_name, input_tensor);
        inputs.push_back(input);
    }

    // Prepare Outputs
    // We request outputs by name only but also provide a vector to capture them

    std::vector<std::string> output_names;
    jsize outputCount = env->GetArrayLength(outputTensors);

    for (jsize i = 0; i < outputCount; i++) {
        jobject outputTensor = env->GetObjectArrayElement(outputTensors, i);
        auto output_name = jstring2string(env, GetTensorName(env, outputTensor));
        output_names.push_back(output_name);
    }

    std::vector<tensorflow::Tensor> outputs;

    // Run Model

    auto session = saved_model_bundle->session.get();
    status = session->Run(inputs, output_names, {}, &outputs);

    if (status != tensorflow::Status::OK()) {
        __android_log_print(ANDROID_LOG_VERBOSE, "Tensor/IO TensorFlow", "%s", status.error_message().c_str());
        ThrowException(env, kIllegalArgumentException, "Internal error: Unable to run model.");
        return;
    }

    // Get Outputs

    assert(outputCount == outputs.size());

    for (jsize i = 0; i < outputs.size(); i++) {
        jobject outputTensor = env->GetObjectArrayElement(outputTensors, i);
        auto output = outputs[i]; // TODO: Avoid copying?
        auto outputPtr = new tensorflow::Tensor(output);
        setHandle<tensorflow::Tensor>(env, outputTensor, outputPtr);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_train(JNIEnv *env, jobject thiz, jobjectArray inputTensors,
                                              jobjectArray outputTensors, jobjectArray trainingOpNames) {
    auto saved_model_bundle = getHandle<tensorflow::SavedModelBundle>(env, thiz);
    tensorflow::Status status;

    // Prepare Inputs
    // Inputs are pairs of names and tensors

    std::vector<std::pair<std::string, tensorflow::Tensor>> inputs;
    jsize inputCount = env->GetArrayLength(inputTensors);

    for (jsize i = 0; i < inputCount; i++) {
        jobject inputTensor = env->GetObjectArrayElement(inputTensors, i);
        auto input_tensor = *getHandle<tensorflow::Tensor>(env, inputTensor);
        auto input_name = jstring2string(env, GetTensorName(env, inputTensor));
        auto input = std::pair<std::string, tensorflow::Tensor>(input_name, input_tensor);
        inputs.push_back(input);
    }

    // Prepare Outputs
    // We request outputs by name only but also provide a vector to capture them

    std::vector<std::string> output_names;
    jsize outputCount = env->GetArrayLength(outputTensors);

    for (jsize i = 0; i < outputCount; i++) {
        jobject outputTensor = env->GetObjectArrayElement(outputTensors, i);
        auto output_name = jstring2string(env, GetTensorName(env, outputTensor));
        output_names.push_back(output_name);
    }

    std::vector<tensorflow::Tensor> outputs;

    // Prepare Training Ops
    // Training ops are just the graph ops by name to run for the training step

    std::vector<std::string> training_names;
    jsize trainingCount = env->GetArrayLength(trainingOpNames);

    for (jsize i = 0; i < trainingCount; i++) {
        auto opName = (jstring) env->GetObjectArrayElement(trainingOpNames, i);
        auto training_name = jstring2string(env, opName);
        training_names.push_back(training_name);
    }

    // Run Training Step

    auto session = saved_model_bundle->session.get();

    status = session->Run(inputs, {}, training_names, nullptr);

    if ( status != tensorflow::Status::OK() ) {
        __android_log_print(ANDROID_LOG_VERBOSE, "Tensor/IO TensorFlow", "%s", status.error_message().c_str());
        ThrowException(env, kIllegalArgumentException, "Internal error: Train: Unable to run training step.");
    }

    // Run Output Step (usually loss)

    status = session->Run(inputs, output_names, {}, &outputs);

    if ( status != tensorflow::Status::OK() ) {
        __android_log_print(ANDROID_LOG_VERBOSE, "Tensor/IO TensorFlow", "%s", status.error_message().c_str());
        ThrowException(env, kIllegalArgumentException, "Internal error: Train: Unable to run output step.");
    }

    // Get Outputs

    assert(outputCount == outputs.size());

    for (jsize i = 0; i < outputs.size(); i++) {
        jobject outputTensor = env->GetObjectArrayElement(outputTensors, i);
        auto output = outputs[i]; // TODO: Avoid copying?
        auto outputPtr = new tensorflow::Tensor(output);
        setHandle<tensorflow::Tensor>(env, outputTensor, outputPtr);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_ai_doc_tensorflow_SavedModelBundle_export(JNIEnv *env, jobject thiz, jstring path) {
    auto saved_model_bundle = getHandle<tensorflow::SavedModelBundle>(env, thiz);
    auto meta_graph_def = saved_model_bundle->meta_graph_def;
    auto session = saved_model_bundle->session.get();
    tensorflow::Status status;

    auto sPath = jstring2string(env, path);
    sPath += "/checkpoint";

    tensorflow::Tensor checkpoint_tensor(tensorflow::DT_STRING, tensorflow::TensorShape());
    checkpoint_tensor.scalar<std::string>()() = sPath;

    std::vector<std::pair<std::string, tensorflow::Tensor>> checkpoint_feed_dict = {{meta_graph_def.saver_def().filename_tensor_name(), checkpoint_tensor}};
    status = session->Run(checkpoint_feed_dict, {}, {meta_graph_def.saver_def().save_tensor_name()}, nullptr);

    if ( status != tensorflow::Status::OK() ) {
        __android_log_print(ANDROID_LOG_VERBOSE, "Tensor/IO TensorFlow", "%s", status.error_message().c_str());
        ThrowException(env, kIllegalArgumentException, "Internal error: Export: Unable to export model checkpoint.");
    }
}

// Reference Code

// Creating, Writing to, and Reading From a TensorFlow Tensor

//tensorflow::Tensor CreateTensor(float value) {
//    std::vector<tensorflow::int64> dims;
//    dims.push_back(1);
//
//    tensorflow::gtl::ArraySlice<tensorflow::int64> dim_sizes(dims);
//    tensorflow::TensorShape shape = tensorflow::TensorShape(dim_sizes);
//    tensorflow::Tensor tensor(tensorflow::DT_FLOAT, shape);
//
//    auto flat_tensor = tensor.flat<float_t>();
//    auto buffer = flat_tensor.data();
//
//    buffer[0] = value;
//
//    return tensor;
//}
//
//float ReadTensor(tensorflow::Tensor tensor) {
//    auto flat_tensor = tensor.flat<float_t>();
//    auto buffer = flat_tensor.data();
//    float val = buffer[0];
//
//    return val;
//}

// Create a Java Tensor

//    jobject dtype = DataType(env, "FLOAT32");
//    jstring name = env->NewStringUTF("output");
//
//    jintArray shape = env->NewIntArray(1);
//    jint anint[1];
//    anint[0] = 1;
//    env->SetIntArrayRegion(shape, 0, 1, anint);
//
//    jclass tensorClass = env->FindClass("ai/doc/tensorflow/Tensor");
//    jmethodID constructorId = env->GetMethodID(tensorClass, "<init>","(Lai/doc/tensorflow/DataType;[ILjava/lang/String;)V");
//    jobject tensorObject = env->NewObject(tensorClass, constructorId, dtype, shape, name);
