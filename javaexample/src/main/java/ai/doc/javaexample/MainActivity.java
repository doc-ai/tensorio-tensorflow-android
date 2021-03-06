/*
 * MainActivity.java
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

package ai.doc.javaexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ai.doc.tensorflow.AndroidAssets;
import ai.doc.tensorflow.DataType;
import ai.doc.tensorflow.SavedModelBundle;
import ai.doc.tensorflow.Tensor;

import static ai.doc.tensorflow.SavedModelBundle.Mode;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SavedModelBundle savedModelBundle;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            setUp();
        } catch (Exception e) {
            Log.v(TAG, "Exception: setUp");
        }

        this.tv = findViewById(R.id.sample_text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            tearDown();
        } catch (Exception e) {
            Log.v(TAG, "Exception: tearDown");
        }
    }

    // User Actions

    public void onLoadModel(View view) {
        try {
            File tioBundle = bundleForFile("1_in_1_out_number_test.tiobundle");
            File modelDir = new File(tioBundle, "predict");

            this.savedModelBundle = new SavedModelBundle(modelDir, Mode.Serve);
            tv.setText("Model is loaded");
        } catch (IOException e) {
            Log.v(TAG, "Exception: bundleForFile");
        }
    }

    public void onRunModel(View view) {
        Tensor input = new Tensor(DataType.FLOAT32, new int[]{1}, "input");
        ByteBuffer buffer = ByteBuffer.allocateDirect(1 * 4); // dims x bytes for dtype

        buffer.order(ByteOrder.nativeOrder());
        buffer.putFloat(2);

        input.setBytes(buffer);

        Tensor output = new Tensor(DataType.FLOAT32, new int[]{1}, "output");

        Tensor[] inputs = {input};
        Tensor[] outputs = {output};

        this.savedModelBundle.run(inputs, outputs);

        ByteBuffer out = output.getBytes();
        float value = out.getFloat();

        tv.setText(String.valueOf(value));
    }

    public void onUnloadModel(View view) {
        this.savedModelBundle = null;
        tv.setText("Model is not loaded");
    }

    // Asset Management

    /** Set up a models directory to copy assets to for testing */

    public void setUp() throws Exception {
        File f = new File(getApplicationContext().getFilesDir(), "models");

        if (f.exists()) {
            deleteRecursive(f);
        }
        if (!f.mkdirs()) {
            throw new Exception("on create: " + f.getPath());
        }
    }

    /** Tear down the models directory */

    public void tearDown() throws Exception {
        File f = new File(getApplicationContext().getFilesDir(), "models");
        deleteRecursive(f);
    }

    /** Create a model bundle from a file, copying the asset to models */

    private File bundleForFile(String filename) throws IOException {
        File dir = new File(getApplicationContext().getFilesDir(), "models");
        File file = new File(dir, filename);

        AndroidAssets.copyAsset(getApplicationContext(), filename, file);
        return file;
    }

    /** Delete a directory and all its contents */

    private void deleteRecursive(File f) throws Exception {
        if (f.isDirectory())
            for (File child : f.listFiles())
                deleteRecursive(child);

        if (!f.delete()) {
            throw new Exception("on delete: " + f.getPath());
        }
    }
}
