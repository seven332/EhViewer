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

package com.hippo.widget;

import android.view.ViewTreeObserver;

import com.hippo.util.ViewUtils;

public class GlobalLayoutSet {

    private OnAllLayoutListener mOnAllLayoutListener;

    private int mSize;

    public void setOnAllLayoutListener(OnAllLayoutListener listener) {
        mOnAllLayoutListener = listener;
    }

    public void addListenerToObserver(ViewTreeObserver observer) {
        LayoutListener ll = new LayoutListener(observer);
        observer.addOnGlobalLayoutListener(ll);
        mSize++;
    }

    private class LayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private ViewTreeObserver mObserver;

        private LayoutListener(ViewTreeObserver observer) {
            mObserver = observer;
        }

        @Override
        public void onGlobalLayout() {
            ViewUtils.removeOnGlobalLayoutListener(mObserver, this);

            if (--mSize == 0) {
                mOnAllLayoutListener.onAllLayout();
            }
        }
    }

    public interface OnAllLayoutListener {
        void onAllLayout();
    }
}
