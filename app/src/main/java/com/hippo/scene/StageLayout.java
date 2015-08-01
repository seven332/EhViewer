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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class StageLayout extends FrameLayout {

    private List<OnLayoutListener> mOnLayoutListeners = new ArrayList<>(3);
    private List<OnLayoutListener> mOnLayoutListenersCopy = new ArrayList<>(3);

    public StageLayout(Context context) {
        super(context);
    }

    public StageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void dispatchSaveInstanceState(@NonNull SparseArray<Parcelable> container) {
        // Do nothing
    }

    @Override
    protected void dispatchRestoreInstanceState(@NonNull SparseArray<Parcelable> container) {
        // Do nothing
    }

    public void addOnLayoutListener(OnLayoutListener layoutListener) {
        mOnLayoutListeners.add(layoutListener);
    }

    public void removeOnLayoutListener(OnLayoutListener layoutListener) {
        mOnLayoutListeners.remove(layoutListener);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t,
                            int r, int b) {
        super.onLayout(changed, l, t, r, b);

        mOnLayoutListenersCopy.clear();
        for (int i = 0, n = mOnLayoutListeners.size(); i < n; i++) {
            mOnLayoutListenersCopy.add(mOnLayoutListeners.get(i));
        }
        for (OnLayoutListener listener : mOnLayoutListenersCopy) {
            listener.onLayout(this);
        }
    }


    public interface OnLayoutListener {
        void onLayout(View view);
    }
}
