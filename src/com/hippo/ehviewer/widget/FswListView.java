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

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ListView;

public class FswListView extends ListView {
    
    private OnFitSystemWindowsListener mListener;
    
    public FswListView(Context context) {
        super(context);
    }
    public FswListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public FswListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setOnFitSystemWindowsListener(OnFitSystemWindowsListener l) {
        mListener = l;
    }
    
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        boolean re = super.fitSystemWindows(insets);
        if (mListener != null)
            mListener.onfitSystemWindows(getPaddingLeft(), getPaddingTop(),
                    getPaddingRight(), getPaddingBottom());
        return re;
    }
}
