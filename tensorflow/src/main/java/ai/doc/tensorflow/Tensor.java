
// Reference:
// See org.tensorflow.lite.Tensor

// TODO: I think it's much more likely we store a byte array and then build the tensor in cpp as needed

package ai.doc.tensorflow;

public class Tensor implements AutoCloseable {

    static {
        NativeLibrary.init();
    }

    @Override
    public void close() throws Exception {
        delete();
    }

    // Public Constructor

    public Tensor(DataType dtype, int[] shape, String name) {
        this.dtype = dtype;
        this.shape = shape;
        this.name = name;
        create();
    }

    // Private Constructor (remove)

    private Tensor(DataType dtype, int[] shape, String name, long handle) {
        this.dtype = dtype;
        this.shape = shape;
        this.name = name;
        this.handle = handle;
    }

    /** Returns the {@link DataType} of elements stored in the Tensor */

    public DataType getDataType() {
        return dtype;
    }

    /** Returns the shape of the Tensor */

    public int[] getShape() {
        return shape;
    }

    /** Returns the name of the Tensor */

    public String getName() {
        return name;
    }

    /** TESTING */

    public void setFloatValue(float value) {
        writeFloat(value);
    }

    public float getFloatValue() {
        return readFloat();
    }

    // Private Variables

    /** The type of the underlying tensor */

    private final DataType dtype;

    /** The shape of the underlying tensor */

    private final int[] shape;

    /** The name of the underlying tensor */

    private final String name;

    // Java Native Interface

    /** The pointer to the underlying Tensor */

    private long handle;

    /** Allocates a tensorflow Tensor and backs this instance with it */

    private native void create();

    /** Frees the memory associated with the underlying tensorflow Tensor */

    private native void delete();

    /** TESTING */

    private native void writeFloat(float value);
    private native float readFloat();

}
