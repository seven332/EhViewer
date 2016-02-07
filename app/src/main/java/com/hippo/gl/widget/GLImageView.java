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

import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.IntDef;

import com.hippo.gl.glrenderer.GLCanvas;
import com.hippo.gl.glrenderer.Texture;
import com.hippo.gl.view.GLView;
import com.hippo.yorozuya.MathUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class GLImageView extends GLView {

    @IntDef({SCALE_ORIGIN, SCALE_FIT_WIDTH, SCALE_FIT_HEIGHT, SCALE_FIT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Scale {}

    @IntDef({START_POSITION_TOP_LEFT, START_POSITION_TOP_RIGHT, START_POSITION_BOTTOM_LEFT,
            START_POSITION_BOTTOM_RIGHT, START_POSITION_CENTER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StartPosition {}

    public static final int SCALE_ORIGIN = 0;
    public static final int SCALE_FIT_WIDTH = 1;
    public static final int SCALE_FIT_HEIGHT = 2;
    public static final int SCALE_FIT = 3;

    public static final int START_POSITION_TOP_LEFT = 0;
    public static final int START_POSITION_TOP_RIGHT = 1;
    public static final int START_POSITION_BOTTOM_LEFT = 2;
    public static final int START_POSITION_BOTTOM_RIGHT = 3;
    public static final int START_POSITION_CENTER = 4;

    private Texture mTexture;

    private RectF mDst = new RectF();
    private RectF mSrcActual = new RectF();
    private RectF mDstActual = new RectF();
    private Rect mValidRect = new Rect();

    private boolean mPositionInRootDirty = true;

    private void applyPositionInRoot() {
        int width = mTexture.getWidth();
        int height = mTexture.getHeight();
        RectF dst = mDst;
        RectF dstActual = mDstActual;
        RectF srcActual = mSrcActual;

        dstActual.set(dst);
        getValidRect(mValidRect);
        if (dstActual.intersect(mValidRect.left, mValidRect.top, mValidRect.right, mValidRect.bottom)) {
            srcActual.left = MathUtils.lerp(0, width,
                    MathUtils.delerp(dst.left, dst.right, dstActual.left));
            srcActual.right = MathUtils.lerp(0, width,
                    MathUtils.delerp(dst.left, dst.right, dstActual.right));
            srcActual.top = MathUtils.lerp(0, height,
                    MathUtils.delerp(dst.top, dst.bottom, dstActual.top));
            srcActual.bottom = MathUtils.lerp(0, height,
                    MathUtils.delerp(dst.top, dst.bottom, dstActual.bottom));
        } else {
            // Can't be seen, set src and dst empty
            srcActual.setEmpty();
            dstActual.setEmpty();
        }
    }

    @Override
    protected void onPositionInRootChanged(int x, int y, int oldX, int oldY) {
        mPositionInRootDirty = true;
    }

    @Override
    public void onRender(GLCanvas canvas) {
        Texture texture = mTexture;
        if (texture == null) {
            return;
        }






        if (mPositionInRootDirty) {
            mPositionInRootDirty = false;
            applyPositionInRoot();
        } else {
            mSrcActual.set(0, 0, texture.getWidth(), texture.getHeight());
            mDstActual.set(mDst);
        }

        texture.draw(canvas, mSrcActual, mDstActual);
    }
}
