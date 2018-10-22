/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.ocrreader;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.samples.vision.ocrreader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class OcrGraphic extends GraphicOverlay.Graphic {
    Canvas canvas;
    private int id;

    private static final int TEXT_COLOR = Color.WHITE;

    private static Paint rectPaint;
    private static Paint textPaint;
    private final Text textBlock;

    OcrGraphic(GraphicOverlay overlay, Text text) {
        super(overlay);

        textBlock = text;

        if (rectPaint == null) {
            rectPaint = new Paint();
            rectPaint.setColor(Color.RED);
            rectPaint.setStyle(Paint.Style.STROKE);
            rectPaint.setStrokeWidth(4.0f);
        }

        if (textPaint == null) {
            textPaint = new Paint();
            textPaint.setColor(TEXT_COLOR);
            textPaint.setTextSize(54.0f);
        }
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Text getTextBlock() {
        return textBlock;
    }

    /**
     * Checks whether a point is within the bounding box of this graphic.
     * The provided point should be relative to this graphic's containing overlay.
     *
     * @param x An x parameter in the relative context of the canvas.
     * @param y A y parameter in the relative context of the canvas.
     * @return True if the provided point is contained within this graphic's bounding box.
     */
    public boolean contains(float x, float y) {
        if (textBlock == null) {
            return false;
        }
        RectF rect = new RectF(textBlock.getBoundingBox());
        rect = translateRect(rect);
        return rect.contains(x, y);
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        this.canvas = canvas;
        if (textBlock == null) {
            return;
        }

        // Draws the bounding box around the TextBlock.
        RectF rect = new RectF(textBlock.getBoundingBox());
        rect = translateRect(rect);
        //canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, rectPaint);
        canvas.drawRect(rect, rectPaint);

//        // Break the text into multiple lines and draw each one according to its own bounding box.
//        List<? extends Text> textComponents = textBlock.getComponents();
//        for (Text currentText : textComponents) {
//            float left = translateX(currentText.getBoundingBox().left);
//            float bottom = translateY(currentText.getBoundingBox().bottom);
//            final Text data = currentText;
//            final List<? extends Text> components = currentText.getComponents();
//            String[] dl = data.getValue().split(" ");
//            int len[] = new int[dl.length];
//            len[0] = 0;
//            for (int i = 1; i < dl.length; i++) {
//                len[i] = len[i - 1] + dl[i - 1].length() + 1;
//            }
//            final int arr[] = len;

//            List<correctData> mistake = spellCheck(data);
//            int i = 0;
//            for (correctData c : mistake) {
//                Log.d("correctedwords", c.data + " " + c.offset);
//                while (i < arr.length && arr[i] != c.offset) {
//                    i++;
//                }
//                rect = translateRect(new RectF(components.get(i).getBoundingBox()));


//            }

        //canvas.drawText(currentText.getValue(), left, bottom, textPaint);
//        }
    }

    public ArrayList<correctData> spellCheck(Text word) throws IOException {
        String host = "https://api.cognitive.microsoft.com";
        String path = "/bing/v7.0/spellcheck";

        // NOTE: Replace this example key with a valid subscription key.
        String key = "945fd5e0999e46b482e686a6c5f42916";

        String mkt = "en-US";
        String mode = "proof";
        String text = word.getValue().toString();

        String params = "?mkt=" + mkt + "&mode=" + mode;
        URL url = new URL(host + path + params);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", "" + (text.length() + 5));
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", key);
        connection.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes("text=" + text);
        wr.flush();
        wr.close();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String line;
        ArrayList<correctData> data = new ArrayList<>();
        while ((line = in.readLine()) != null) {
            JSONObject jsonObj = null;
            try {
                jsonObj = new JSONObject(line);
                if (jsonObj.getString("_type").equals("SpellCheck")) {
                    // Getting JSON Array node
                    JSONArray ft = jsonObj.getJSONArray("flaggedTokens");
                    // looping through All Contacts
                    for (int i = 0; i < ft.length(); i++) {
                        JSONObject c = ft.getJSONObject(i);
                        int offset = c.getInt("offset");
                        JSONArray sugg = c.getJSONArray("suggestions");
                        String correct = sugg.getJSONObject(i).getString("suggestion");
                        data.add(new correctData(offset, correct));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        in.close();

        return data;
    }

    void redraw(float a, float b, float c, float d) {
        Log.d("camehere", "check123");
        //canvas.drawLine(a,b,c,d,rectPaint);
        Log.d("camehere", "check123");
    }
}

class correctData {
    int offset;
    String data;

    public correctData(int offset, String data) {
        this.offset = offset;
        this.data = data;
    }
}

