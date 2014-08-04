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

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class FswView extends View {
    private List<OnFitSystemWindowsListener> mListeners;

    public FswView(Context context) {
        super(context);
        init();
    }
    public FswView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public FswView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init() {
        mListeners = new LinkedList<OnFitSystemWindowsListener>();
        setFitsSystemWindows(true);
    }

    public void addOnFitSystemWindowsListener(OnFitSystemWindowsListener l) {
        mListeners.add(l);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        boolean re = super.fitSystemWindows(insets);
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        for (OnFitSystemWindowsListener l : mListeners)
            l.onfitSystemWindows(paddingLeft, paddingTop,
                    paddingRight, paddingBottom);
        return re;
    }

    public interface OnFitSystemWindowsListener {
        void onfitSystemWindows(int paddingLeft,
                int paddingTop, int paddingRight, int paddingBottom);
    }
}
