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
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ResponedScrollView extends ScrollView {

    public static final int SCROLL_START = 0x0;
    public static final int SCROLL_END = 0x1;

    public static final long CHECK_DELAY = 100L;

    private boolean isPosted = false;
    private int mLastScrollY;
    private OnScrollStateChangedListener mListener;

    public ResponedScrollView(Context context) {
        super(context);
    }

    public ResponedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResponedScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnScrollStateChangedListener(OnScrollStateChangedListener l) {
        mListener = l;
    }

    private void wakeScrollObserver() {
        mLastScrollY = getScrollY();
        postDelayed(mScrollObserver, CHECK_DELAY);
        isPosted = true;
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP && !isPosted) {
            wakeScrollObserver();
            if (mListener != null)
                mListener.onScrollStateChanged(this, SCROLL_START);
        }
        return super.onTouchEvent(ev);
    }

    private final Runnable mScrollObserver = new Runnable() {
        @Override
        public void run() {
            int newScrollY = getScrollY();
            if(newScrollY == mLastScrollY) {
                isPosted = false;
                if (mListener != null)
                    mListener.onScrollStateChanged(ResponedScrollView.this, SCROLL_END);
            } else {
                wakeScrollObserver();
            }
        }
    };

    public interface OnScrollStateChangedListener {
        public void onScrollStateChanged(ResponedScrollView view, int state);
    }
}
