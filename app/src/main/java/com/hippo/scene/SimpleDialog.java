/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.scene;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.hippo.ehviewer.R;

public class SimpleDialog extends SceneDialog {

    private static final int BACKGROUND_COLOR = 0x61000000;

    private Builder mBuilder;

    private StageActivity mActivity;

    private SimpleDialogFrame mFrame;
    private TextView mTitle;

    private SimpleDialog(Builder builder) {
        mBuilder = builder;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO
        setBackgroundColor(BACKGROUND_COLOR);

        mActivity = getStageActivity();

        mFrame = new SimpleDialogFrame(mActivity);
        setContentView(mFrame);

        mActivity.getLayoutInflater().inflate(R.layout.simple_dialog, mFrame);

        mTitle = (TextView) findViewById(R.id.title);

        bindDialog();
    }

    private void bindDialog() {
        mTitle.setText(mBuilder.mTitle);
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mFrame.setFitPaddingBottom(b);
    }

    public static class Builder {

        private String mTitle;

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public @NonNull SceneDialog build() {
            return new SimpleDialog(this);
        }

        public void show() {
            new SimpleDialog(this).show();
        }
    }
}
