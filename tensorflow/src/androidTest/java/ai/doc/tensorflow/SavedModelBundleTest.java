package ai.doc.tensorflow;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileSystemException;

import static org.junit.Assert.*;

public class SavedModelBundleTest {
    private Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private Context testContext = InstrumentationRegistry.getInstrumentation().getContext();

    private float epsilon = 0.01f;

    /** Set up a models directory to copy assets to for testing */

    @Before
    public void setUp() throws Exception {
        File f = new File(testContext.getFilesDir(), "models");
        if (!f.mkdirs()) {
            throw new FileSystemException("on create: " + f.getPath());
        }
    }

    /** Tear down the models directory */

    @After
    public void tearDown() throws Exception {
        File f = new File(testContext.getFilesDir(), "models");
        deleteRecursive(f);
    }

    /** Create a model bundle from a file, copying the asset to models */

    private File bundleForFile(String filename) throws IOException {
        File dir = new File(testContext.getFilesDir(), "models");
        File file = new File(dir, filename);

        AndroidAssets.copyAsset(testContext, filename, file);
        return file;
    }

    /** Delete a directory and all its contents */

    private void deleteRecursive(File f) throws FileSystemException {
        if (f.isDirectory())
            for (File child : f.listFiles())
                deleteRecursive(child);

        if (!f.delete()) {
            throw new FileSystemException("on delete: " + f.getPath());
        }
    }

    /** Create a direct native order byte buffer with floats **/

    private ByteBuffer byteBufferWithFloats(float[] floats) {
        int size = floats.length * 4; // dims x bytes for dtype

        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.order(ByteOrder.nativeOrder());

        for (float f : floats) {
            buffer.putFloat(f);
        }

        return buffer;
    }

    /** Compares the contents of a float byte buffer to floats */

    private void assertByteBufferEqualToFloats(ByteBuffer buffer, float epsilon, float[] floats) {
        for (float f : floats) {
            assertEquals(buffer.getFloat(), f, epsilon);
        }
    }

    @Test
    public void test1x1NumberModel() {
        try {

            // Prepare Model

            File tioBundle = bundleForFile("1_in_1_out_number_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input = new Tensor(DataType.FLOAT32, new int[]{1}, "input");
            ByteBuffer buffer = byteBufferWithFloats(new float[]{2});
            input.setBytes(buffer);

            // Prepare Outputs

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1}, "output");

            // Run Model

            Tensor[] inputs = {input};
            Tensor[] outputs = {output};

            model.run(inputs, outputs);

            // Read Output

            ByteBuffer out = output.getBytes();
            assertEquals(out.getFloat(), 25, epsilon);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void test1x1VectorsModel() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("1_in_1_out_vectors_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input = new Tensor(DataType.FLOAT32, new int[]{1,4}, "input");
            input.setBytes(byteBufferWithFloats(new float[] {
                    1, 2, 3, 4
            }));

            // Prepare Outputs

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1,4}, "output");

            // Run Model

            Tensor[] inputs = {input};
            Tensor[] outputs = {output};

            model.run(inputs, outputs);

            // Read Output

            assertByteBufferEqualToFloats(output.getBytes(), epsilon, new float[] {
                    2, 2, 4, 4
            });

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void test2x2VectorsModel() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("2_in_2_out_vectors_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input1 = new Tensor(DataType.FLOAT32, new int[]{1,4}, "input1");
            input1.setBytes(byteBufferWithFloats(new float[] {
                    1, 2, 3, 4
            }));

            Tensor input2 = new Tensor(DataType.FLOAT32, new int[]{1,4}, "input2");
            input2.setBytes(byteBufferWithFloats(new float[] {
                    10, 20, 30, 40
            }));

            // Prepare Outputs

            Tensor output1 = new Tensor(DataType.FLOAT32, new int[]{1,1}, "output1");
            Tensor output2 = new Tensor(DataType.FLOAT32, new int[]{1,1}, "output2");

            // Run Model

            Tensor[] inputs = {input1, input2};
            Tensor[] outputs = {output1, output2};

            model.run(inputs, outputs);

            // Read Output

            ByteBuffer out1 = output1.getBytes();
            assertEquals(out1.getFloat(), 240, epsilon);

            ByteBuffer out2 = output2.getBytes();
            assertEquals(out2.getFloat(), 64, epsilon);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void test2x2MatricesModel() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("2_in_2_out_matrices_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input1 = new Tensor(DataType.FLOAT32, new int[]{4,4}, "input1");
            input1.setBytes(byteBufferWithFloats(new float[]{
                    1,    2,    3,    4,
                    10,   20,   30,   40,
                    100,  200,  300,  400,
                    1000, 2000, 3000, 4000
            }));

            Tensor input2 = new Tensor(DataType.FLOAT32, new int[]{4,4}, "input2");
            input2.setBytes(byteBufferWithFloats(new float[] {
                    5,    6,    7,    8,
                    50,   60,   70,   80,
                    500,  600,  700,  800,
                    5000, 6000, 7000, 8000
            }));

            // Prepare Outputs

            Tensor output1 = new Tensor(DataType.FLOAT32, new int[]{4,4}, "output1");
            Tensor output2 = new Tensor(DataType.FLOAT32, new int[]{4,4}, "output2");

            // Run Model

            Tensor[] inputs = {input1, input2};
            Tensor[] outputs = {output1, output2};

            model.run(inputs, outputs);

            // Read Output

            assertByteBufferEqualToFloats(output1.getBytes(), epsilon, new float[] {
                    56,       72,       56,       72,
                    5600,     7200,     5600,     7200,
                    560000,   720000,   560000,   720000,
                    56000000, 72000000, 56000000, 72000000,
            });

            assertByteBufferEqualToFloats(output2.getBytes(), epsilon, new float[] {
                    18,    18,    18,    18,
                    180,   180,   180,   180,
                    1800,  1800,  1800,  1800,
                    18000, 18000, 18000, 18000
            });

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}