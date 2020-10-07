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

    /** TESTING: Runs the model with a single input and output tensor **/

    public native void run(Tensor input, Tensor output);

}
