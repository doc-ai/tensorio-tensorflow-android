package ai.doc.tensorflow;

public class Test {

    static {
        System.loadLibrary("native-lib");
    }

    public native String stringFromJNI();
}
