
// Reference:
// See org.tensorflow.lite.Tensor

// TODO: I think it's much more likely we store a byte array and then build the tensor in cpp as needed

package ai.doc.tensorflow;

/**
 * A Tensor is backed by an underlying TensorFlow Tensor that is either prepared in advance of
 * sending it into a model or read as an output from the model. The Java object encapsulates
 * information about the tensor such as its data type, shape, and name that is needed to construct
 * the backing object or properly read it from the model.
 */

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
        createBackingTensor();
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

    // Private Methods

    private boolean isBacked = false;

    private void createBackingTensor() {
        if (!isBacked) {
            create();
        }
    }

    // Java Native Interface

    /**
     * The pointer to the underlying Tensor. The underlying tensor will be created via java methods
     * if the user writes data to the tensor before sending it into a model. Otherwise the underlying
     * tensor will be created from c++ when reading data out of a model.
     * */

    private long handle = 0;

    /** Allocates a tensorflow Tensor and backs this instance with it */

    private native void create();

    /** Frees the memory associated with the underlying tensorflow Tensor */

    private native void delete();

    /** TESTING */

    private native void writeFloat(float value);
    private native float readFloat();

}
