package ai.doc.tensorflow;

public class NativeLibrary {
    public static void init() {
        System.loadLibrary("tensorio-tensorflow");
    }
}
