/*
 * SavedModelBundleTest.java
 * TensorIO TensorFlow
 *
 * Created by Philip Dow
 * Copyright (c) 2020 - Present doc.ai (http://doc.ai)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.doc.tensorflow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileSystemException;

import static ai.doc.tensorflow.SavedModelBundle.Mode;

import static org.junit.Assert.*;

public class SavedModelBundleTest {
    private Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private Context testContext = InstrumentationRegistry.getInstrumentation().getContext();

    private float epsilon = 0.01f;

    /** Borrowed from Tensor/IO to normalize pixels from 4 byte int values to 4 byte float values */

    public abstract static class PixelNormalizer {

        public abstract float normalize(int value, int channel);

        public static PixelNormalizer PixelNormalizerSingleBias(final float scale, final float bias) {
            return new PixelNormalizer() {
                @Override
                public float normalize(int value, int channel) {
                    return (value * scale) + bias;
                }
            };
        }

        /**
         * Normalizes pixel values from a range of `[0,255]` to `[0,1]`.
         * This is equivalent to applying a scaling factor of `1.0/255.0` and no channel bias.
         */

        public static PixelNormalizer PixelNormalizerZeroToOne(){
            float scale = 1.0f/255.0f;
            return PixelNormalizerSingleBias(scale, 0.0f);
        }

        /**
         * Normalizes pixel values from a range of `[0,255]` to `[-1,1]`.
         * This is equivalent to applying a scaling factor of `2.0/255.0` and a bias of `-1` to each channel.
         */

        public static PixelNormalizer PixelNormalizerNegativeOneToOne(){
            float scale = 2.0f/255.0f;
            float bias = -1f;
            return PixelNormalizerSingleBias(scale, bias);
        }
    }


    /** Set up a models directory to copy assets to for testing */

    @Before
    public void setUp() throws Exception {
        File f = new File(testContext.getFilesDir(), "models");
        if (!f.mkdirs()) {
            throw new FileSystemException("on create: " + f.getPath());
        }

        File e = new File(testContext.getFilesDir(), "exports");
        if (!e.mkdirs()) {
            throw new FileSystemException("on create: " + f.getPath());
        }
    }

    /** Tear down the models directory */

    @After
    public void tearDown() throws Exception {
        File f = new File(testContext.getFilesDir(), "models");
        deleteRecursive(f);

        File e = new File(testContext.getFilesDir(), "exports");
        deleteRecursive(e);
    }

    /** Create a model bundle from a file, copying the asset to models */

    private File bundleForFile(String filename) throws IOException {
        File dir = new File(testContext.getFilesDir(), "models");
        File file = new File(dir, filename);

        AndroidAssets.copyAsset(testContext, filename, file);
        return file;
    }

    /** Creates an export directory with name */

    private File exportForFile(String filename) throws IOException {
        File dir = new File(testContext.getFilesDir(), "exports");
        File file = new File(dir, filename);

        if (!file.mkdirs()) {
            throw new FileSystemException("on create: " + file.getPath());
        }

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

    /** Create a direct native order byte buffer with floats */

    private ByteBuffer byteBufferWithFloats(float[] floats) {
        int size = floats.length * 4; // dims * bytes_per_float

        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.order(ByteOrder.nativeOrder());

        for (float f : floats) {
            buffer.putFloat(f);
        }

        return buffer;
    }

    /** Create a direct native order byte buffer with int32s */

    private ByteBuffer byteBufferWithInts(int[] ints) {
        int size = ints.length * 4; // dims * bytes_per_int32

        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.order(ByteOrder.nativeOrder());

        for (int i : ints) {
            buffer.putInt(i);
        }

        return buffer;
    }

    /** Create a direct native order byte buffer with int64s */

    private ByteBuffer byteBufferWithLongs(long[] longs) {
        int size = longs.length * 8; // dims * bytes_per_int64

        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.order(ByteOrder.nativeOrder());

        for (long l : longs) {
            buffer.putLong(l);
        }

        return buffer;
    }

    /** Creates a direct native order byte buffer with a bitmap and normalizes to [0,1] */

    private ByteBuffer byteBufferWithBitmap(Bitmap bitmap) {
        // width * height * channels * bytes_per_float
        int size = bitmap.getWidth() * bitmap.getHeight() * 3 * 4;

        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.order(ByteOrder.nativeOrder());

        // Read Bitmap into int array

        int[] intValues = new int[bitmap.getWidth() * bitmap.getHeight()]; // 4 bytes per int
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight()); // Returns ARGB pixels

        // Write Individual Pixels to Buffer

        int pixel = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                final int val = intValues[pixel++];
                writePixelToBuffer(val, buffer, false, PixelNormalizer.PixelNormalizerZeroToOne());
            }
        }

        return buffer;
    }

    /**
     * Writes a pixel to a buffer, normalizing and converting it as needed.
     *
     * Before calling this method the first time in a loop, rewind the buffer. The buffer then
     * increments its index with every call to put.
     *
     * @param pixelValue 4 byte pixel value to write with ARGB or BGRA format wit
     * @param buffer The buffer to write to
     * @param quantized true if the buffer expects quantized (byte) data, false otherwise (float)
     * @param normalizer The normalizer than converts a single byte pixel-channel value to a
     *                   floating point value
     */

    private void writePixelToBuffer(int pixelValue, @NonNull ByteBuffer buffer, boolean quantized, @NonNull PixelNormalizer normalizer) {
        if (quantized) {
            buffer.put((byte) ((pixelValue >> 16) & 0xFF));
            buffer.put((byte) ((pixelValue >> 8) & 0xFF));
            buffer.put((byte) (pixelValue & 0xFF));
        } else {
            buffer.putFloat(normalizer.normalize((pixelValue >> 16) & 0xFF, 0));
            buffer.putFloat(normalizer.normalize((pixelValue >> 8) & 0xFF, 1));
            buffer.putFloat(normalizer.normalize(pixelValue & 0xFF, 2));
        }
    }

    /** Compares the contents of a float byte buffer to floats */

    private void assertByteBufferEqualToFloats(ByteBuffer buffer, float epsilon, float[] floats) {
        for (float f : floats) {
            assertEquals(buffer.getFloat(), f, epsilon);
        }
    }

    // Single Valued Tests

    @Test
    public void testScalarModel() {
        try {

            // Prepare Model

            File tioBundle = bundleForFile("scalar_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Serve);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input = new Tensor(DataType.FLOAT32, new int[]{1}, true, "input", byteBufferWithFloats(new float[]{2}));

            // Prepare Outputs

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1}, true,"output");

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
    public void test1x1NumberModel() {
        try {

            // Prepare Model

            File tioBundle = bundleForFile("1_in_1_out_number_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Serve);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input = new Tensor(DataType.FLOAT32, new int[]{1}, false, "input", byteBufferWithFloats(new float[]{2}));

            // Prepare Outputs

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1}, false,"output");

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

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Serve);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input = new Tensor(DataType.FLOAT32, new int[]{1,4}, false,"input", byteBufferWithFloats(new float[] {
                    1, 2, 3, 4
            }));

            // Prepare Outputs

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1,4}, false, "output");

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

    // Multiple Input/Output Tests

    @Test
    public void test2x2VectorsModel() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("2_in_2_out_vectors_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Serve);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input1 = new Tensor(DataType.FLOAT32, new int[]{1,4}, false, "input1", byteBufferWithFloats(new float[] {
                    1, 2, 3, 4
            }));

            Tensor input2 = new Tensor(DataType.FLOAT32, new int[]{1,4}, false, "input2", byteBufferWithFloats(new float[] {
                    10, 20, 30, 40
            }));

            // Prepare Outputs

            Tensor output1 = new Tensor(DataType.FLOAT32, new int[]{1,1}, false, "output1");
            Tensor output2 = new Tensor(DataType.FLOAT32, new int[]{1,1}, false, "output2");

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

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Serve);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input1 = new Tensor(DataType.FLOAT32, new int[]{4,4}, false, "input1", byteBufferWithFloats(new float[]{
                    1,    2,    3,    4,
                    10,   20,   30,   40,
                    100,  200,  300,  400,
                    1000, 2000, 3000, 4000
            }));

            Tensor input2 = new Tensor(DataType.FLOAT32, new int[]{4,4}, false, "input2", byteBufferWithFloats(new float[] {
                    5,    6,    7,    8,
                    50,   60,   70,   80,
                    500,  600,  700,  800,
                    5000, 6000, 7000, 8000
            }));

            // Prepare Outputs

            Tensor output1 = new Tensor(DataType.FLOAT32, new int[]{4,4}, false, "output1");
            Tensor output2 = new Tensor(DataType.FLOAT32, new int[]{4,4}, false, "output2");

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

    // Int32 and Int64 Tests
    // Uses the same graph as the 1_in_1_out_number_test but with int32 data types

    // This graph takes and produces int32s but they are cast to float32s for the internal ops
    // The current tensorflow build doesn't fully support int32 data types for all ops

    @Test
    public void testInt32Model() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("int32io_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Serve);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input = new Tensor(DataType.INT32, new int[]{1}, false, "input", byteBufferWithInts(new int[]{2}));

            // Prepare Outputs

            Tensor output = new Tensor(DataType.INT32, new int[]{1}, false, "output");

            // Run Model

            Tensor[] inputs = {input};
            Tensor[] outputs = {output};

            model.run(inputs, outputs);

            // Read Output

            ByteBuffer out = output.getBytes();
            assertEquals(out.getInt(), 25);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    // This graph takes and produces int64s but they are cast to float32s for the internal ops
    // The current tensorflow build doesn't fully support int64s data types for all ops

    @Test
    public void testInt64Model() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("int64io_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Serve);
            assertNotNull(model);

            // Prepare Inputs

            Tensor input = new Tensor(DataType.INT64, new int[]{1}, false, "input", byteBufferWithLongs(new long[]{2}));

            // Prepare Outputs

            Tensor output = new Tensor(DataType.INT64, new int[]{1}, false, "output");

            // Run Model

            Tensor[] inputs = {input};
            Tensor[] outputs = {output};

            model.run(inputs, outputs);

            // Read Output

            ByteBuffer out = output.getBytes();
            assertEquals(out.getLong(), 25);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    // Real World Tests

    @Test
    public void testCatsVsDogsPredict() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("cats-vs-dogs-predict.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Serve);
            assertNotNull(model);

            // Prepare Input

            InputStream stream = testContext.getAssets().open("cat.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(stream);

            Tensor input = new Tensor(DataType.FLOAT32, new int[]{1,128,128,3}, false, "image", byteBufferWithBitmap(bitmap));

            // Prepare output

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1,1}, false, "sigmoid");

            // Run Model

            Tensor[] inputs = {input};
            Tensor[] outputs = {output};

            model.run(inputs, outputs);

            // Read Output

            float sigmoid = output.getBytes().getFloat();
            assertTrue(sigmoid < 0.5);

        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testCatsVsDogsTrain() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("cats-vs-dogs-train.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "train");

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Train);
            assertNotNull(model);

            // Prepare Input

            InputStream stream = testContext.getAssets().open("cat.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(stream);

            Tensor input = new Tensor(DataType.FLOAT32, new int[]{1,128,128,3}, false, "image", byteBufferWithBitmap(bitmap));

            // Prepare Label

            Tensor labels = new Tensor(DataType.INT32, new int[]{1,1}, false, "labels", byteBufferWithInts(new int[]{
                    0
            }));

            // Prepare Output

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1}, false, "sigmoid_cross_entropy_loss/value");

            // Run Training

            String[] trainingOps = {"train"};
            Tensor[] inputs = {input, labels};
            Tensor[] outputs = {output};

            float[] losses = new float[4];
            int epochs = 4;

            for (int epoch = 0; epoch < epochs; epoch++) {
                model.train(inputs, outputs, trainingOps);

                // Read Output

                float loss = output.getBytes().getFloat();
                losses[epoch] = loss;;
            }

            assertNotEquals(losses[0], losses[1]);
            assertNotEquals(losses[1], losses[2]);
            assertNotEquals(losses[2], losses[3]);

        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void textExportModel() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("cats-vs-dogs-train.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "train");

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Train);
            assertNotNull(model);

            // Prepare Input

            InputStream stream = testContext.getAssets().open("cat.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(stream);

            Tensor input = new Tensor(DataType.FLOAT32, new int[]{1, 128, 128, 3}, false, "image", byteBufferWithBitmap(bitmap));

            // Prepare Label

            Tensor labels = new Tensor(DataType.INT32, new int[]{1, 1}, false, "labels", byteBufferWithInts(new int[]{
                    0
            }));

            // Prepare Output

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1}, false, "sigmoid_cross_entropy_loss/value");

            // Run Training

            String[] trainingOps = {"train"};
            Tensor[] inputs = {input, labels};
            Tensor[] outputs = {output};

            int epochs = 4;
            for (int epoch = 0; epoch < epochs; epoch++) {
                model.train(inputs, outputs, trainingOps);

            }

            File exportDir = exportForFile("cats-vs-dogs");
            model.export(exportDir);

            File checkpointsIndex = new File(exportDir, SavedModelBundle.CheckpointsIndex);
            File checkpointsData = new File(exportDir, SavedModelBundle.CheckpointsData);

            assertTrue(checkpointsIndex.exists());
            assertTrue(checkpointsData.exists());

        } catch (IOException e) {
            fail();
        }
    }

    // Tree Test

    @Test
    public void testTreeModel() {
        try {
            // Prepare Model

            File tioBundle = bundleForFile("tree_test.tiobundle");
            assertNotNull(tioBundle);

            File modelDir = new File(tioBundle, "predict");

            SavedModelBundle model = new SavedModelBundle(modelDir, Mode.Serve);
            assertNotNull(model);

            // Prepare Inputs

            Tensor survived = new Tensor(DataType.INT64, new int[]{1}, false, "survived", byteBufferWithLongs(new long[]{2}));
            Tensor sex = new Tensor(DataType.INT64, new int[]{1}, false, "sex", byteBufferWithLongs(new long[]{2}));
            Tensor age = new Tensor(DataType.FLOAT32, new int[]{1}, false, "age", byteBufferWithFloats(new float[]{2}));
            Tensor n_siblings_spouses = new Tensor(DataType.INT64, new int[]{1}, false, "n_siblings_spouses", byteBufferWithLongs(new long[]{2}));
            Tensor parch = new Tensor(DataType.INT64, new int[]{1}, false, "parch", byteBufferWithLongs(new long[]{2}));
            Tensor embark_town = new Tensor(DataType.INT64, new int[]{1}, false, "embark_town", byteBufferWithLongs(new long[]{2}));
            Tensor klass = new Tensor(DataType.INT64, new int[]{1}, false, "class", byteBufferWithLongs(new long[]{2}));
            Tensor deck = new Tensor(DataType.INT64, new int[]{1}, false, "deck", byteBufferWithLongs(new long[]{2}));
            Tensor alone = new Tensor(DataType.INT64, new int[]{1}, false, "alone", byteBufferWithLongs(new long[]{2}));

            // Prepare Outputs

            Tensor output = new Tensor(DataType.FLOAT32, new int[]{1}, false, "boosted_trees/BoostedTreesPredict");

            // Run Model

            Tensor[] inputs = {survived, sex, age, n_siblings_spouses, parch, embark_town, klass, deck, alone};
            Tensor[] outputs = {output};

            model.run(inputs, outputs);

            // Read Output

            ByteBuffer out = output.getBytes();
            assertEquals(out.getFloat(), 56.928, epsilon);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

}