/*
 * Copyright (C) 2014-2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.util.ViewUtils;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGBuilder;
import com.larvalabs.svgandroid.SVGParseException;

public class StartActivity extends AppCompatActivity {

    private static final String TAG = StartActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        TextView text = (TextView) findViewById(R.id.text_welcome);

        try {
            SVG svg = new SVGBuilder().readFromResource(getResources(), R.raw.sad_pandroid).build();
            ImageView image = (ImageView) findViewById(R.id.image_mascot);
            ViewUtils.removeHardwareAccelerationSupport(image);
            image.setImageDrawable(svg.getDrawable());
        } catch (SVGParseException e) {
            // Empty, I think this exception will never be caught.
        }

        finish();
        Intent intent = new Intent(StartActivity.this, ContentActivity.class);
        startActivity(intent);
    }
}
