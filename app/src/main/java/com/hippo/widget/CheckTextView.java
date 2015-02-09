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
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.hippo.util.MathUtils;
import com.nineoldandroids.animation.ObjectAnimator;

public class CheckTextView extends TextView implements OnClickListener, Hotspotable {

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_CHECKED = "checked";

    private static final int MASK = 0x8a000000;
    private static final long ANIMATION_DURATION = 150;

    private boolean mChecked = false;

    private float mX;
    private float mY;
    private float mRadius = 0f;
    private float mMaxRadius;

    private Paint mPaint;

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
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(MASK);

        setOnTouchListener(new HotspotTouchHelper(this));
        setOnClickListener(this);
    }

    @Override
    public void setHotspot(float x, float y) {
        mX = x;
        mY = y;
        mMaxRadius = MathUtils.coverageRadius(getWidth(), getHeight(), x, y);
    }

    public void setRadius(float radius) {
        float bigger = Math.max(mRadius, radius);
        mRadius = radius;
        invalidate((int) (mX - bigger), (int) (mY - bigger), (int) (mX + bigger), (int) (mY + bigger));
    }

    public float getRadius() {
        return mRadius;
    }

    @Override
    public void onClick(View v) {
        mChecked = !mChecked;

        ObjectAnimator radiusAnim;
        if (mChecked) {
            radiusAnim = ObjectAnimator.ofFloat(this, "radius",
                    0f, mMaxRadius);
        } else {
            radiusAnim = ObjectAnimator.ofFloat(this, "radius",
                    mMaxRadius, 0f);
        }
        radiusAnim.setAutoCancel(true);
        radiusAnim.setDuration(ANIMATION_DURATION); // TODO decide duration according to mMaxRadius
        radiusAnim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radius = mRadius;
        if (radius > 0f) {
            canvas.drawCircle(mX, mY, radius, mPaint);
        }
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        invalidate();
    }

    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        state.putBoolean(STATE_KEY_CHECKED, mChecked);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_SUPER));
            mChecked = savedState.getBoolean(STATE_KEY_CHECKED);
        }
    }

}
