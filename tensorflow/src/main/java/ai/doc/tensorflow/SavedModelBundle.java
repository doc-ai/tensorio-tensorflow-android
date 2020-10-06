package ai.doc.tensorflow;

import java.io.File;

public class SavedModelBundle implements AutoCloseable {

    static {
        NativeLibrary.load();
    }

    @Override
    public void close() throws Exception {
        unload();
    }

    // Public Constructor

    public SavedModelBundle(File file) {
        load(file.getPath() + "/predict");
    }

    // Java Native Interface

    /** The pointer to the underlying SavedModelBundle **/

    private long handle;

    private native void load(String dir);

    private native void unload();

    public native String run();

}
