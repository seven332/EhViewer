/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class OlScrollView extends ScrollView {
    private OnLayoutListener mListener;
    
    public OlScrollView(Context context) {
        super(context);
    }
    public OlScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public OlScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @SuppressLint("WrongCall")
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mListener != null)
            mListener.onLayout();
    }
    
    public void setOnLayoutListener(OnLayoutListener l) {
        mListener = l;
    }
}
