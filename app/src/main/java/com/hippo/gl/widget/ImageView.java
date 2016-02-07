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

package com.hippo.gl.widget;

import android.graphics.Color;

import com.hippo.gl.glrenderer.GLCanvas;
import com.hippo.gl.glrenderer.ImageTexture;
import com.hippo.gl.image.Image;
import com.hippo.gl.view.GLView;

public class ImageView extends GLView implements ImageTexture.Callback {

    private ImageTexture mImageTexture;

    public ImageView(ImageTexture imageTexture) {
        mImageTexture = imageTexture;
        imageTexture.setCallback(this);
        setBackgroundColor(Color.WHITE);
    }

    @Override
    public void onRender(GLCanvas canvas) {
        mImageTexture.draw(canvas, 0, 0);
    }

    @Override
    public void invalidateImage(Image image) {
        invalidate();
    }
}
