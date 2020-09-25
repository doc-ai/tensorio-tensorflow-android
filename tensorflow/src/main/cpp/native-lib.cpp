#include <jni.h>
#include <string>
#include <vector>
#include <cstdarg>

#include "tensorflow/core/framework/graph.pb.h"
#include "tensorflow/core/framework/tensor.h"
#include "tensorflow/core/graph/default_device.h"
#include "tensorflow/core/graph/graph_def_builder.h"
#include "tensorflow/core/lib/core/threadpool.h"
#include "tensorflow/core/lib/strings/str_util.h"
#include "tensorflow/core/lib/strings/stringprintf.h"
#include "tensorflow/core/lib/core/errors.h"
#include "tensorflow/core/platform/init_main.h"
#include "tensorflow/core/platform/logging.h"
#include "tensorflow/core/platform/types.h"
#include "tensorflow/core/public/session.h"
#include "tensorflow/core/public/session_options.h"

char **new_argv(int count, ...)
{
    va_list args;
    int i;
    char **argv = (char **) malloc((count+1) * sizeof(char*));
    char *temp;
    va_start(args, count);
    for (i = 0; i < count; i++) {
        temp = va_arg(args, char*);
        argv[i] = (char *) malloc(sizeof(temp));
        argv[i] = temp;
    }
    argv[i] = NULL;
    va_end(args);
    return argv;
}

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

    // Tensorflow Initialization

    int argc = 1;
    char **argv = new_argv(1, "native-lib");

    tensorflow::port::InitMain(argv[0], &argc, &argv);

    tensorflow::SessionOptions options;
    std::unique_ptr<tensorflow::Session> session(NewSession(options));

    // Write and Read a Tensor

    tensorflow::Tensor tensor = CreateTensor();
    float value = ReadTensor(tensor);

    std::string hello = "Hello from C++, the tensor value is: " + std::to_string(value);
    return env->NewStringUTF(hello.c_str());
}
