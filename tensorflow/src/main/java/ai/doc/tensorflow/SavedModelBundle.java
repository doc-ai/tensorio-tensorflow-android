/*
 * SavedModelBundle.java
 * TensorIO TensorFlow
 *
 * Created by Philip Dow
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

package ai.doc.tensorflow;

import java.io.File;

/** Manages access to an underlying saved model bundle */

public class SavedModelBundle implements AutoCloseable {

    static {
        NativeLibrary.init();
    }

    @Override
    public void close() throws Exception {
        delete();
    }

    // Public Constructor

    /** Instantiates a model in the Saved Model format with a directory at the file path */

    public SavedModelBundle(File file) {
        create(file.getPath());
    }

    // Java Native Interface

    /** The pointer to the underlying SavedModelBundle */

    private long handle;

    /** Loads a model at a file path and backs this instance with a SavedModelBundle */

    private native void create(String dir);

    /** Unloads the model and frees the memory associated with it */

    private native void delete();

    /** Runs the model with input and output tensors **/

    public native void run(Tensor[] inputs, Tensor[] outputs);

}
