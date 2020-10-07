
// Reference:
// See org.tensorflow.lite.DataType

/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package ai.doc.tensorflow;

/** Represents the type of elements in a TensorFlow Lite {@link Tensor} as an enum. */

// See types.pb.h for tensorflow::DataType values

public enum DataType {
    /** 32-bit single precision floating point. DT_FLOAT = 1 */
    FLOAT32(1),

    /** 32-bit signed integer. DT_INT32 = 3 */
    INT32(3),

    /** 8-bit unsigned integer. DT_UINT8 = 4 */
    UINT8(4),

    /** 64-bit signed integer. DT_INT64 = 9 */
    INT64(9),

    /** Strings. DT_STRING = 7 */
    STRING(7);

    private final int value;

    DataType(int value) {
        this.value = value;
    }

    /** Returns the size of an element of this type, in bytes, or -1 if element size is variable. */

    public int byteSize() {
        switch (this) {
            case FLOAT32:
                return 4;
            case INT32:
                return 4;
            case UINT8:
                return 1;
            case INT64:
                return 8;
            case STRING:
                return -1;
        }
        throw new IllegalArgumentException(
                "DataType error: DataType " + this + " is not supported yet");
    }

    /** Corresponding value of the TfLiteType enum in the TensorFlow Lite C API. */

    int c() {
        return value;
    }

    /** Converts a C TfLiteType enum value to the corresponding type. */

    static DataType fromC(int c) {
        for (DataType t : values) {
            if (t.value == c) {
                return t;
            }
        }
        throw new IllegalArgumentException(
                "DataType error: DataType "
                        + c
                        + " is not recognized in Java");
    }

    /** Gets string names of the data type. */

    String toStringName() {
        switch (this) {
            case FLOAT32:
                return "float";
            case INT32:
                return "int";
            case UINT8:
                return "byte";
            case INT64:
                return "long";
            case STRING:
                return "string";
        }
        throw new IllegalArgumentException(
                "DataType error: DataType " + this + " is not supported yet");
    }

    // Cached to avoid copying it
    private static final DataType[] values = values();
}
