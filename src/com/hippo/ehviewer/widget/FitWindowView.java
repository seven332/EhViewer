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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class FitWindowView extends View {

    private List<OnFitSystemWindowsListener> mListeners;

    public FitWindowView(Context context) {
        super(context);
        init();
    }

    public FitWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FitWindowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FitWindowView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mListeners = new LinkedList<OnFitSystemWindowsListener>();
        setFitsSystemWindows(true);
    }

    public void addOnFitSystemWindowsListener(OnFitSystemWindowsListener l) {
        mListeners.add(l);
    }

    public void removeOnFitSystemWindowsListener(OnFitSystemWindowsListener l) {
        mListeners.remove(l);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected boolean fitSystemWindows(Rect insets) {
        int l = insets.left;
        int t = insets.top;
        int r = insets.right;
        int b = insets.bottom;
        for (OnFitSystemWindowsListener listener : mListeners)
            listener.onFitSystemWindows(l, t, r, b);
        return super.fitSystemWindows(insets);
    }

    public static interface OnFitSystemWindowsListener {
        public void onFitSystemWindows(int l, int t, int r, int b);
    }
}
