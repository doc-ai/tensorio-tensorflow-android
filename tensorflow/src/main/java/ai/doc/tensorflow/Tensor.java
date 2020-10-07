
// Reference:
// See org.tensorflow.lite.Tensor

// TODO: I think it's much more likely we store a byte array and then build the tensor in cpp as needed

package ai.doc.tensorflow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    /** Returns the total number of elements in the tensor according to dims */

    public long count() {
        int count = 1;
        for(int v : getShape()) {
            count *= v;
        }
        return count;
    }

    /** Returns the byte size of the tensor, which is the number of elements x sizeof(dtype) */

    public long size() {
        return count() * dtype.byteSize();
    }

    /** Returns the name of the Tensor */

    public String getName() {
        return name;
    }

    /**
     * Set this tensor's bytes. You must use a directly allocated ByteBuffer, created with
     * `ByteBuffer.allocateDirect()` and use native ordering for the buffer, set with
     * `buffer.order(ByteOrder.nativeOrder())`. The buffer should be filled with values of the type
     * you specified when you created this tensor.
     */

    public void setBytes(ByteBuffer buffer) {
        assert (buffer.isDirect() && buffer.order() == ByteOrder.nativeOrder());
        createBackingTensor();
        writeBytes(buffer, size());
    }

    /**
     * Get this tensor's bytes. If you set the tensor's bytes yourself it will return those values,
     * otherwise it returns the bytes read from a model's outputs in the tensor with the name you
     * have specified.
     */

    public ByteBuffer getBytes() {
        return readBytes(size()).order(ByteOrder.nativeOrder());
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

    /** Writes bytes to the backing tensor */

    private native void writeBytes(ByteBuffer buffer, long size);

    /** Reads bytes from the backing tensor */

    private native ByteBuffer readBytes(long size);
}
