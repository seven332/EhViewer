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

import android.annotation.TargetApi;
import android.os.Build;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;

public class HotspotTouchHelper implements View.OnTouchListener {

    private final Hotspotable mOwner;

    private boolean mPrePressed = false;
    private boolean mHasPerformedLongPress = false;

    private CheckForLongPress mPendingCheckForLongPress = null;
    private CheckForTap mPendingCheckForTap = null;
    private PerformClick mPerformClick = null;
    private UnsetPressedState mUnsetPressedState = null;

    /**
     * Cache the touch slop from the context that created the view.
     */
    private int mTouchSlop = -1;

    public HotspotTouchHelper(Hotspotable owner){
        mOwner = owner;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean onTouch(View v, MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE)
            mOwner.setHotspot(x, y);

        if (!v.isEnabled()) {
            if (event.getAction() == MotionEvent.ACTION_UP && v.isPressed()) {
                v.setPressed(false);
            }
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return (v.isClickable() || v.isLongClickable());
        }

        TouchDelegate touchDelegate = v.getTouchDelegate();
        if (touchDelegate != null) {
            if (touchDelegate.onTouchEvent(event)) {
                return true;
            }
        }

        if (v.isClickable() || v.isLongClickable()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (v.isPressed() || mPrePressed) {
                        // take focus if we don't have it already and we should in
                        // touch mode.
                        boolean focusTaken = false;
                        if (v.isFocusable() && v.isFocusableInTouchMode() && !v.isFocused()) {
                            focusTaken = v.requestFocus();
                        }

                        if (mPrePressed) {
                            // The button is being released before we actually
                            // showed it as pressed.  Make it show the pressed
                            // state now (before scheduling the click) to ensure
                            // the user sees it.
                            setPressed(v, true, x, y);
                        }

                        if (!mHasPerformedLongPress) {
                            // This is a tap, so remove the longpress check
                            removeLongPressCallback(v);

                            // Only perform take click actions if we were in the pressed state
                            if (!focusTaken) {
                                // Use a Runnable and post this rather than calling
                                // performClick directly. This lets other visual state
                                // of the view update before click actions start.
                                if (mPerformClick == null) {
                                    mPerformClick = new PerformClick(v);
                                }
                                if (!v.post(mPerformClick)) {
                                    v.performClick();
                                }
                            }
                        }

                        if (mUnsetPressedState == null) {
                            mUnsetPressedState = new UnsetPressedState(v);
                        }

                        if (mPrePressed) {
                            v.postDelayed(mUnsetPressedState,
                                    ViewConfiguration.getPressedStateDuration());
                        } else if (!v.post(mUnsetPressedState)) {
                            // If the post failed, unpress right now
                            mUnsetPressedState.run();
                        }

                        removeTapCallback(v);
                    }
                    break;

                case MotionEvent.ACTION_DOWN:
                    mHasPerformedLongPress = false;

                    if (performButtonActionOnTouchDown(v, event)) {
                        break;
                    }

                    mPrePressed = true;
                    if (mPendingCheckForTap == null) {
                        mPendingCheckForTap = new CheckForTap(v);
                    }
                    mPendingCheckForTap.x = event.getX();
                    mPendingCheckForTap.y = event.getY();
                    v.postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                    break;

                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    removeTapCallback(v);
                    removeLongPressCallback(v);
                    break;

                case MotionEvent.ACTION_MOVE:
                    mOwner.setHotspot(x, y);

                    if (mTouchSlop == -1)
                        mTouchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();

                    // Be lenient about moving outside of buttons
                    if (!pointInView(v, x, y, mTouchSlop)) {
                        // Outside button
                        removeTapCallback(v);
                        if (v.isPressed()) {
                            // Remove any future long press/tap checks
                            removeLongPressCallback(v);

                            v.setPressed(false);
                        }
                    }
                    break;
            }

            return true;
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setPressed(View v, boolean pressed, float x, float y){
        v.setPressed(pressed);
        mOwner.setHotspot(x, y);
    }

    /**
     * Utility method to determine whether the given point, in local coordinates,
     * is inside the view, where the area of the view is expanded by the slop factor.
     * This method is called while processing touch-move events to determine if the event
     * is still within the view.
     */
    public boolean pointInView(View v, float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < ((v.getRight() - v.getLeft()) + slop) &&
                localY < ((v.getBottom() - v.getTop()) + slop);
    }

    /**
     * Performs button-related actions during a touch down event.
     *
     * @param event The event.
     * @return True if the down was consumed.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected boolean performButtonActionOnTouchDown(View v, MotionEvent event) {
        if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if ((event.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0) {
                if (v.showContextMenu()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove the longpress detection timer.
     */
    private void removeLongPressCallback(View v) {
        if (mPendingCheckForLongPress != null) {
            v.removeCallbacks(mPendingCheckForLongPress);
        }
    }

    /**
     * Remove the tap detection timer.
     */
    private void removeTapCallback(View v) {
        if (mPendingCheckForTap != null) {
            mPrePressed = false;
            v.removeCallbacks(mPendingCheckForTap);
        }
    }

    private void checkForLongClick(View v, int delayOffset) {
        if (v.isLongClickable()) {
            mHasPerformedLongPress = false;

            if (mPendingCheckForLongPress == null) {
                mPendingCheckForLongPress = new CheckForLongPress(v);
            }
            v.postDelayed(mPendingCheckForLongPress,
                    ViewConfiguration.getLongPressTimeout() - delayOffset);
        }
    }

    private final class CheckForLongPress implements Runnable {

        private final View mView;

        public CheckForLongPress(View v) {
            mView = v;
        }

        @Override
        public void run() {
            if (mView.isPressed() && (mView.getParent() != null)) {
                if (mView.performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }
    }

    private final class CheckForTap implements Runnable {
        public float x;
        public float y;
        private final View mView;

        public CheckForTap(View v) {
            mView = v;
        }

        @Override
        public void run() {
            mPrePressed = false;
            setPressed(mView, true, x, y);
            checkForLongClick(mView, ViewConfiguration.getTapTimeout());
        }
    }

    private final class PerformClick implements Runnable {
        private final View mView;

        public PerformClick(View v) {
            mView = v;
        }

        @Override
        public void run() {
            mView.performClick();
        }
    }

    private final class UnsetPressedState implements Runnable {
        private final View mView;

        public UnsetPressedState(View v) {
            mView = v;
        }

        @Override
        public void run() {
            mView.setPressed(false);
        }
    }
}
