/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.gl;

import android.content.Context;

import com.hippo.gl.glrenderer.TextTexture;
import com.hippo.gl.view.Gravity;
import com.hippo.gl.widget.GLFrameLayout;
import com.hippo.gl.widget.GLLinearLayout;
import com.hippo.gl.widget.GLProgressView;
import com.hippo.gl.widget.GLTextView;
import com.hippo.gl.widget.GLTextureView;
import com.hippo.yorozuya.LayoutUtils;

public class GalleryPageView extends GLFrameLayout {

    private ImageView mImage;
    private GLLinearLayout mInfo;
    private GLTextView mPage;
    private GLTextureView mError;
    private GLProgressView mProgress;

    // TODO
    private static TextTexture sPageTextTexture;

    public GalleryPageView(Context context) {
        // Add image
        mImage = new ImageView();
        GravityLayoutParams glp = new GravityLayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        addComponent(mImage, glp);

        // Add other panel
        mInfo = new GLLinearLayout();
        mInfo.setOrientation(GLLinearLayout.VERTICAL);
        glp = new GravityLayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        glp.gravity = Gravity.CENTER;
        addComponent(mInfo, glp);

        // Add page
        mPage = new GLTextView();
        mPage.setTextTexture(sPageTextTexture);
        GLLinearLayout.LayoutParams lp = new GLLinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        mInfo.addComponent(mPage, lp);

        // Add error
        mError = new GLTextureView();
        lp = new GLLinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        mInfo.addComponent(mError, lp);

        // Add progress
        mProgress = new GLProgressView();
        mProgress.setMinimumWidth(LayoutUtils.dp2pix(context, 56)); // 56dp
        mProgress.setMinimumHeight(LayoutUtils.dp2pix(context, 56)); // 56dp
        lp = new GLLinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        mInfo.addComponent(mProgress, lp);
    }

    public void showImage() {
        //mImage.setVisibility(VISIBLE);
        //mInfo.setVisibility(INVISIBLE);
    }

    public ImageView getImageView() {
        return mImage;
    }
}
