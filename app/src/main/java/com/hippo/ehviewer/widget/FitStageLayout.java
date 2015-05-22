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
import android.util.AttributeSet;

import com.hippo.scene.StageLayout;
import com.hippo.widget.FitPaddingImpl;

public class FitStageLayout extends StageLayout implements FitPaddingImpl {

    private OnFitPaddingBottomListener mOnFitPaddingBottomListener;

    public FitStageLayout(Context context) {
        super(context);
    }

    public FitStageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FitStageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnFitPaddingBottomListener(OnFitPaddingBottomListener listener) {
        mOnFitPaddingBottomListener = listener;
    }

    @Override
    public void onFitPadding(int left, int top, int right, int bottom) {
        setPadding(0, top, 0, 0);

        if (mOnFitPaddingBottomListener != null) {
            mOnFitPaddingBottomListener.onFitPaddingBottom(bottom);
        }
    }

    public interface OnFitPaddingBottomListener {
        void onFitPaddingBottom(int bottom);
    }
}
