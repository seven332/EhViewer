/*
 * Copyright (C) 2014-2015 Hippo Seven
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class CheckTextView extends TextView implements OnClickListener{

    private static int MASK = 0x61000000;

    private Rect mRect = new Rect();

    private boolean mChecked = false;

    public CheckTextView(Context context) {
        super(context);
        init();
    }

    public CheckTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        mChecked = !mChecked;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mRect.set(0, 0, right - left, bottom - top);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mChecked) {
            canvas.drawColor(MASK);
        }
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        invalidate();
    }

    public boolean isChecked() {
        return mChecked;
    }
}
