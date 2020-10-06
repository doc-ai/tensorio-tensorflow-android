#include <jni.h>
#include <string>
#include <vector>

#include "tensorflow/core/framework/tensor.h"
#include "tensorflow/cc/saved_model/loader.h"
#include "tensorflow/cc/saved_model/tag_constants.h"
#include "tensorflow/core/public/session.h"

tensorflow::Tensor CreateTensor() {
    std::vector<tensorflow::int64> dims;
    dims.push_back(1);
    dims.push_back(1);

    tensorflow::gtl::ArraySlice<tensorflow::int64> dim_sizes(dims);
    tensorflow::TensorShape shape = tensorflow::TensorShape(dim_sizes);
    tensorflow::Tensor tensor(tensorflow::DT_FLOAT, shape);

    auto flat_tensor = tensor.flat<float_t>();
    auto buffer = flat_tensor.data();

    buffer[0] = (float)64.0;

    return tensor;
}

float ReadTensor(tensorflow::Tensor tensor) {
    auto flat_tensor = tensor.flat<float_t>();
    auto buffer = flat_tensor.data();
    float val = buffer[0];

    return val;
}

extern "C" JNIEXPORT jstring JNICALL
Java_ai_doc_tensorflow_Test_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    // Write and Read a Tensor

    tensorflow::Tensor tensor = CreateTensor();
    float value = ReadTensor(tensor);

    std::string hello = "Hello from C++, the tensor value is: " + std::to_string(value);
    return env->NewStringUTF(hello.c_str());
}
