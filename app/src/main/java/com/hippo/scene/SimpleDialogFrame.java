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

package com.hippo.scene;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class SimpleDialogFrame extends FrameLayout {

    private int mOriginLeft;
    private int mOriginTop;
    private int mDrawLeft;
    private int mDrawTop;

    public SimpleDialogFrame(Context context) {
        super(context);
    }

    public SimpleDialogFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SimpleDialogFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mDrawLeft = mOriginLeft = left;
        mDrawTop = mOriginTop = top;
    }

    /**
     * I want to set left, but draw start from origin left.
     */
    @SuppressWarnings("unused")
    public void setDrawLeft(int drawLeft) {
        mDrawLeft = drawLeft;
        setLeft(drawLeft);
        setScrollX(drawLeft - mOriginLeft);
    }

    @SuppressWarnings("unused")
    public int getDrawLeft() {
        return mDrawLeft;
    }

    /**
     * I want to set top, but draw start from origin top.
     */
    @SuppressWarnings("unused")
    public void setDrawTop(int drawTop) {
        mDrawTop = drawTop;
        setTop(drawTop);
        setScrollY((drawTop - mOriginTop));
    }

    @SuppressWarnings("unused")
    public int getDrawTop() {
        return mDrawTop;
    }

    @SuppressWarnings("unused")
    public void setDrawRight(int drawRight) {
        setRight(drawRight);
    }

    @SuppressWarnings("unused")
    public int getDrawRight() {
        return getRight();
    }

    @SuppressWarnings("unused")
    public void setDrawBottom(int drawBottom) {
        setBottom(drawBottom);
    }

    @SuppressWarnings("unused")
    public int getDrawBottom() {
        return getBottom();
    }
}
