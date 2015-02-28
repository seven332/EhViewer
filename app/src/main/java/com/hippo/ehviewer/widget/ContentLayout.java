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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import com.hippo.scene.StageLayout;

import org.jetbrains.annotations.NotNull;

public class ContentLayout extends StageLayout {

    private OnGetPaddingListener mOnGetPaddingListener;

    private int mFitPaddingLeft = -1;
    private int mFitPaddingTop = -1;
    private int mFitPaddingRight = -1;
    private int mFitPaddingBottom = -1;

    public ContentLayout(Context context) {
        super(context);
    }

    public ContentLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContentLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ContentLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(@NotNull Rect insets) {
        mFitPaddingLeft = 0;
        mFitPaddingTop = 0;
        mFitPaddingRight = 0;
        mFitPaddingBottom = insets.bottom;
        if (mOnGetPaddingListener != null) {
            mOnGetPaddingListener.onGetPadding(mFitPaddingLeft, mFitPaddingTop,
                    mFitPaddingRight, mFitPaddingBottom);
        }

        insets.set(insets.left, insets.top, insets.right, 0);

        return super.fitSystemWindows(insets);
    }

    public void setOnGetPaddingListener(OnGetPaddingListener listener) {
        mOnGetPaddingListener = listener;
    }

    public int getFitPaddingLeft() {
        return mFitPaddingLeft;
    }

    public int getFitPaddingTop() {
        return mFitPaddingTop;
    }

    public int getFitPaddingRight() {
        return mFitPaddingRight;
    }

    public int getFitPaddingBottom() {
        return mFitPaddingBottom;
    }

    public interface OnGetPaddingListener {
        public void onGetPadding(int l, int t, int r, int b);
    }
}
