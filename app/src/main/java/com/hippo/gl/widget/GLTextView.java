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
import android.text.TextUtils;

import com.hippo.gl.glrenderer.GLCanvas;
import com.hippo.gl.glrenderer.TextTexture;
import com.hippo.gl.view.GLView;
import com.hippo.gl.view.Gravity;
import com.hippo.yorozuya.ArrayUtils;

public class GLTextView extends GLView {

    TextTexture mTextTexture;

    private String mText = "";
    private int[] mIndexes = ArrayUtils.EMPTY_INT_ARRAY;

    private int mGravity = Gravity.NO_GRAVITY;

    private void generateIndexes() {
        if (mTextTexture == null || TextUtils.isEmpty(mText)) {
            mIndexes = ArrayUtils.EMPTY_INT_ARRAY;
        } else {
            mIndexes = mTextTexture.getTextIndexes(mText);
        }
    }

    public void setTextTexture(TextTexture textTexture) {
        if (mTextTexture == textTexture) {
            return;
        }
        mTextTexture = textTexture;

        generateIndexes();
        requestLayout();
    }

    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        if (text.equals(mText)) {
            return;
        }
        mText = text;

        generateIndexes();
        requestLayout();
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            invalidate();
        }
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        if (mTextTexture == null) {
            return super.getSuggestedMinimumWidth();
        } else {
            return Math.max((int) mTextTexture.getTextWidth(mIndexes) + mPaddings.left + mPaddings.right,
                    super.getSuggestedMinimumWidth());
        }
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        if (mTextTexture == null) {
            return super.getSuggestedMinimumHeight();
        } else {
            return Math.max((int) mTextTexture.getTextHeight() + mPaddings.top + mPaddings.bottom,
                    super.getSuggestedMinimumHeight());
        }
    }

    @Override
    public void onRender(GLCanvas canvas) {
        Rect paddings = getPaddings();
        int x = getDefaultBegin(getWidth(), (int) mTextTexture.getTextWidth(mIndexes),
                paddings.left, paddings.right, Gravity.getPosition(mGravity, Gravity.HORIZONTAL));
        int y = getDefaultBegin(getHeight(), (int) mTextTexture.getTextHeight(),
                paddings.top, paddings.bottom, Gravity.getPosition(mGravity, Gravity.VERTICAL));
        mTextTexture.drawText(canvas, mIndexes, x, y);
    }
}
