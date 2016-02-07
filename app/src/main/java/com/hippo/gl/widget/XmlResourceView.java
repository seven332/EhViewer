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

import com.hippo.gl.glrenderer.GLCanvas;
import com.hippo.gl.glrenderer.XmlResourceTexture;
import com.hippo.gl.view.GLView;

public class XmlResourceView extends GLView {

    private XmlResourceTexture mTexture;

    public void setTexture(XmlResourceTexture texture) {
        mTexture = texture;

        if (texture != null) {
            int width = getWidth();
            int height = getHeight();
            if (width > 0 && height > 0) {
                texture.setBound(width, height);
                invalidate();
            }
        }
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
        super.onLayout(changeSize, left, top, right, bottom);

        if (changeSize && mTexture != null) {
            mTexture.setBound(right - left, bottom - top);
        }
    }

    @Override
    public void onRender(GLCanvas canvas) {
        super.onRender(canvas);

        if (mTexture != null) {
            canvas.drawTexture(mTexture, 0, 0, getWidth(), getHeight());
        }
    }
}
