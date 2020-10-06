package ai.doc.tensorflow;

import java.io.File;

public class SavedModelBundle implements AutoCloseable {

    static {
        NativeLibrary.load();
    }

    // Public Constructor

    // TODO: Implement normal constructor and then access private handle directly from JNI

    public SavedModelBundle(File file) {

    }

    public static SavedModelBundle load(File file) {
        return load(file.getPath() + "/predict");
    }

    // Public Methods

    public String run() {
        return run(handle);
    }

    public void unload() {
        unload(handle);
    }

    // JNI Memory Management

    /** The pointer to the underlying SavedModelBundle **/

    private long handle;

    private SavedModelBundle(long handle) {
        this.handle = handle;
    }

    private static SavedModelBundle fromHandle(long handle) {
        return new SavedModelBundle(handle);
    }

    @Override
    public void close() throws Exception {
        unload();
    }

    // Native Methods

    private static native SavedModelBundle load(String dir);

    private native void unload(long handle);

    private native String run(long handle);

}
