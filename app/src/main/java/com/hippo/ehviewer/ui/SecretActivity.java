/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer.ui;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.effect.DrawableTransition;
import com.hippo.ehviewer.util.Secret;
import com.hippo.ehviewer.widget.MaterialToast;

import java.util.Arrays;

public class SecretActivity extends AbsActivity implements View.OnClickListener {

    private final long[] mHits = new long[3];
    private Bitmap mBmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBmp = Secret.getSecretImage(this);
        if (mBmp == null) {
            finish();
            return;
        }

        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setImageBitmap(mBmp);
        iv.setOnClickListener(this);
        iv.setSoundEffectsEnabled(false);
        setContentView(iv);
        DrawableTransition.transit(iv.getDrawable(), false, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBmp != null) {
            mBmp.recycle();
            mBmp = null;
        }
    }

    @Override
    public void onClick(View v) {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
        mHits[mHits.length-1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
            Arrays.fill(mHits, 0);
            MaterialToast.showToast(R.string.photo_footnotes);
        }
    }
}
