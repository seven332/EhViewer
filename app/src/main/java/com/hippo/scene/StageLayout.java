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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.FrameLayout;

public class StageLayout extends FrameLayout {

    private OnGetFitPaddingBottomListener mOnGetFitPaddingBottomListener;

    private int mFitPaddingBottom = -1;

    public StageLayout(Context context) {
        super(context);
    }

    public StageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StageLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void dispatchSaveInstanceState(@NonNull SparseArray<Parcelable> container) {
        // Do nothing
    }

    @Override
    protected void dispatchRestoreInstanceState(@NonNull SparseArray<Parcelable> container) {
        // Do nothing
    }

    public void setOnGetFitPaddingListener(OnGetFitPaddingBottomListener listener) {
        mOnGetFitPaddingBottomListener = listener;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(@NonNull Rect insets) {
        mFitPaddingBottom = insets.bottom;
        if (mOnGetFitPaddingBottomListener != null) {
            mOnGetFitPaddingBottomListener.onGetFitPaddingBottom(mFitPaddingBottom);
        }

        insets.set(insets.left, insets.top, insets.right, 0);

        return super.fitSystemWindows(insets);
    }

    public int getFitPaddingBottom() {
        return mFitPaddingBottom;
    }

    public interface OnGetFitPaddingBottomListener {
        void onGetFitPaddingBottom(int b);
    }
}
