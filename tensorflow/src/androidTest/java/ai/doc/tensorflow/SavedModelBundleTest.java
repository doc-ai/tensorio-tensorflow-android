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

    @Test
    public void test1In1OutNumberModel() {
        try {

            // Prepare Model

            File tioBundle = bundleForFile("1_in_1_out_number_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input = new Tensor(DataType.FLOAT32, new int[]{1}, "input");

            ByteBuffer buffer = ByteBuffer.allocateDirect(1 * 4); // dims x bytes for dtype

            buffer.order(ByteOrder.nativeOrder());
            buffer.putFloat(2);

            input.setBytes(buffer);

            // Prepare Outputs

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1}, "output");

            // Run Model

            Tensor[] inputs = {input};
            Tensor[] outputs = {output};

            model.run(inputs, outputs);

            // Read Output

            ByteBuffer out = output.getBytes();
            float value = out.getFloat();

            assertEquals(value, 25.0, 0.01);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}