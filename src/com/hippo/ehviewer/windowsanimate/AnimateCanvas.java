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

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.AbsoluteLayout;

@SuppressWarnings("deprecation")
public class AnimateCanvas extends AbsoluteLayout {

    private final List<Ripple> mRippleList = new LinkedList<Ripple>();
    private final List<WindowsAnimate.AnimateBitmap> mAnimateBitmapList = new LinkedList<WindowsAnimate.AnimateBitmap>();

    public AnimateCanvas(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    void addRenderingRipple(Ripple ripple) {
        if (!mRippleList.contains(ripple))
            mRippleList.add(ripple);
    }

    void removeRenderingRipple(Ripple ripple) {
        mRippleList.remove(ripple);
    }

    void addAnimateBitmap(WindowsAnimate.AnimateBitmap ab) {
        mAnimateBitmapList.add(ab);
    }

    void removeAnimateBitmap(WindowsAnimate.AnimateBitmap ab) {
        mAnimateBitmapList.remove(ab);
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (Ripple ripple : mRippleList) {
            ripple.draw(canvas);
        }
        for (WindowsAnimate.AnimateBitmap ab : mAnimateBitmapList) {
            ab.draw(canvas);
        }
    }
}
