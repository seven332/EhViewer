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

import android.graphics.RectF;

import com.hippo.gl.glrenderer.GLCanvas;
import com.hippo.gl.glrenderer.Texture;
import com.hippo.gl.view.GLView;

public class ImageView extends GLView {

    private Texture mTexture;

    private RectF mDst = new RectF();

    private RectF mSrcActual = new RectF();
    private RectF mDstActual = new RectF();

    @Override
    public void onRender(GLCanvas canvas) {
        if (mTexture != null) {





            mTexture.draw(canvas, mSrcActual, mDstActual);
        }
    }
}
