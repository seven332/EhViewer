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

package com.hippo.ehviewer.windowsanimate;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * @author Hippo
 *
 * Get lots of code from android.graphics.drawable.RippleDrawable
 */
public class RippleHelpDrawable extends Drawable {

    private final Ripple mRipple;

    private boolean mActive = false;

    @SuppressWarnings("deprecation")
    public RippleHelpDrawable(WindowsAnimate windowsAnimate, View view, boolean keepBound) {
        mRipple = new Ripple(windowsAnimate, view, getBounds(), keepBound);
        view.setBackgroundDrawable(this);
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        boolean active = false;
        boolean enabled = false;
        int N = stateSet.length;
        for (int i = 0; i < N; i++) {
            if (stateSet[i] == android.R.attr.state_enabled) {
                enabled = true;
            }
            if ((stateSet[i] == android.R.attr.state_focused) ||
                    (stateSet[i] == android.R.attr.state_pressed)) {
                active = true;
            }
        }

        setActive((active) && (enabled));

        return false;
    }

    private void setActive(boolean active) {
        if (mActive != active) {
            mActive = active;
            if (active) {
                mRipple.enter();
            } else {
                mRipple.exit();
            }
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mRipple.onBoundsChanged(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
        // None
    }

    @Override
    public void setAlpha(int alpha) {
        // None
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // None
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}
