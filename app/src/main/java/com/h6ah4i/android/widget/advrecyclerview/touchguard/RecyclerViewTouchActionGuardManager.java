/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.h6ah4i.android.widget.advrecyclerview.touchguard;

import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Hooks touch events to avoid unexpected scrolling.
 */
public class RecyclerViewTouchActionGuardManager {
    private static final String TAG = "ARVTouchActionGuardMgr";

    private static final boolean LOCAL_LOGV = false;
    private static final boolean LOCAL_LOGD = false;

    private RecyclerView.OnItemTouchListener mInternalUseOnItemTouchListener;
    private RecyclerView mRecyclerView;
    private boolean mGuarding;
    private int mInitialTouchY;
    private int mLastTouchY;
    private int mTouchSlop;
    private boolean mEnabled;
    private boolean mInterceptScrollingWhileAnimationRunning;

    /**
     * Constructor.
     */
    public RecyclerViewTouchActionGuardManager() {
        mInternalUseOnItemTouchListener = new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return RecyclerViewTouchActionGuardManager.this.onInterceptTouchEvent(rv, e);
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                RecyclerViewTouchActionGuardManager.this.onTouchEvent(rv, e);
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        };
    }

    /**
     * Indicates this manager instance has released or not.
     *
     * @return True if this manager instance has released
     */
    public boolean isReleased() {
        return (mInternalUseOnItemTouchListener == null);
    }

    /**
     * Attaches {@link android.support.v7.widget.RecyclerView} instance.
     *
     * @param rv The {@link android.support.v7.widget.RecyclerView} instance
     */
    public void attachRecyclerView(RecyclerView rv) {
        if (rv == null) {
            throw new IllegalArgumentException("RecyclerView cannot be null");
        }

        if (isReleased()) {
            throw new IllegalStateException("Accessing released object");
        }

        if (mRecyclerView != null) {
            throw new IllegalStateException("RecyclerView instance has already been set");
        }

        mRecyclerView = rv;
        mRecyclerView.addOnItemTouchListener(mInternalUseOnItemTouchListener);

        mTouchSlop = ViewConfiguration.get(rv.getContext()).getScaledTouchSlop();
    }

    /**
     * Detach the {@link android.support.v7.widget.RecyclerView} instance and release internal field references.
     *
     * This method should be called in order to avoid memory leaks.
     */
    public void release() {
        if (mRecyclerView != null && mInternalUseOnItemTouchListener != null) {
            mRecyclerView.removeOnItemTouchListener(mInternalUseOnItemTouchListener);
        }
        mInternalUseOnItemTouchListener = null;
        mRecyclerView = null;
    }

    /*package*/ boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (!mEnabled) {
            return false;
        }

        final int action = MotionEventCompat.getActionMasked(e);

        if (LOCAL_LOGV) {
            Log.v(TAG, "onInterceptTouchEvent() action = " + action);
        }

        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleActionUpOrCancel();
                break;

            case MotionEvent.ACTION_DOWN:
                handleActionDown(e);
                break;

            case MotionEvent.ACTION_MOVE:
                if (handleActionMove(rv, e)) {
                    return true;
                }
                break;
        }

        return false;
    }

    /*package*/ void onTouchEvent(RecyclerView rv, MotionEvent e) {
        if (!mEnabled) {
            return;
        }

        final int action = MotionEventCompat.getActionMasked(e);

        if (LOCAL_LOGV) {
            Log.v(TAG, "onTouchEvent() action = " + action);
        }

        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleActionUpOrCancel();
                break;
        }
    }

    private boolean handleActionMove(RecyclerView rv, MotionEvent e) {
        if (!mGuarding) {
            mLastTouchY = (int) (e.getY() + 0.5f);

            final int distance = mLastTouchY - mInitialTouchY;

            if (mInterceptScrollingWhileAnimationRunning && (Math.abs(distance) > mTouchSlop) && isAnimationRunning(rv)) {
                // intercept vertical move touch events while animation is running
                mGuarding = true;
            }
        }

        return mGuarding;
    }

    private static boolean isAnimationRunning(RecyclerView rv) {
        final RecyclerView.ItemAnimator itemAnimator = rv.getItemAnimator();
        return (itemAnimator != null) && (itemAnimator.isRunning());
    }

    private void handleActionUpOrCancel() {
        mGuarding = false;
        mInitialTouchY = 0;
        mLastTouchY = 0;
    }

    private void handleActionDown(MotionEvent e) {
        mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);
        mGuarding = false;
    }

    /**
     * Sets whether to use touch guard feature. If set false, all touch event interceptions will be disabled.
     *
     * @param enabled enabled / disabled
     */
    public void setEnabled(boolean enabled) {
        if (mEnabled == enabled) {
            return;
        }
        mEnabled = enabled;

        if (!mEnabled) {
            handleActionUpOrCancel();
        }
    }

    /**
     * Checks whether the touch guard feature is enabled.
     *
     * @return True for currently touch guard feature is enabled, otherwise false
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Sets whether to use interception of "vertical scroll while animation running".
     *
     * @param enabled enabled / disabled
     */
    public void setInterceptVerticalScrollingWhileAnimationRunning(boolean enabled) {
        mInterceptScrollingWhileAnimationRunning = enabled;
    }

    /**
     * Checks whether the interception of "vertical scroll while animation running" is enabled.
     *
     * @return enabled / disabled
     */
    public boolean isInterceptScrollingWhileAnimationRunning() {
        return mInterceptScrollingWhileAnimationRunning;
    }
}
