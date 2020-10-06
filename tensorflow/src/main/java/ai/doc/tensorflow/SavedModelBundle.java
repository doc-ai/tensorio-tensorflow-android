package ai.doc.tensorflow;

import java.io.File;

public class SavedModelBundle {

    static {
        NativeLibrary.load();
    }

    public SavedModelBundle(File file) {
        load(file.getPath() + "/predict");
    }

    private SavedModelBundle(long handle) {
        this.handle = handle;
    }

    private static SavedModelBundle fromHandle(long handle) {
        return new SavedModelBundle(handle);
    }

    private static native SavedModelBundle load(String dir);

    private long handle;
}
