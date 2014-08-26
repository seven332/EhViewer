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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.BatteryManager;
import android.util.AttributeSet;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.drawable.BatteryDrawable;
import com.hippo.ehviewer.util.Ui;

public class BatteryView extends TextView {
    @SuppressWarnings("unused")
    private static final String TAG = BatteryView.class.getSimpleName();

    private int mLevel = 0;
    private boolean mCharging = false;

    private BatteryDrawable mDrawable;

    private boolean mAttached = false;
    private boolean mIsChargerWorking = false;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
            boolean charging = (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    == BatteryManager.BATTERY_STATUS_CHARGING);

            if (mLevel != level || mCharging != charging) {
                mLevel = level;
                mCharging = charging;
                if (mCharging && mLevel != 100) {
                    startCharger();
                } else {
                    stopCharger();
                    mDrawable.setElect(mLevel);
                }
                setText(String.valueOf(mLevel) + "%");
            }
        }
    };

    private final Runnable mCharger = new Runnable() {

        private int level = 0;

        @Override
        public void run() {
            level += 5;
            if (level > mLevel)
                level = 0;
            mDrawable.setElect(level, mLevel <= BatteryDrawable.WARN_LIMIT);
            getHandler().postDelayed(mCharger, 500);
        }
    };

    public BatteryView(Context context) {
        super(context);
        init();
    }

    public BatteryView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();

        TypedArray typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.BatteryView, defStyleAttr, 0);
        int color = typedArray.getColor(R.styleable.BatteryView_bvColor, Color.WHITE);
        typedArray.recycle();

        mDrawable.setColor(color);
    }

    private void init() {
        mDrawable = new BatteryDrawable();
        // TODO how to get size ?
        int height = Ui.dp2pix(12);
        mDrawable.setBounds(0, 0, (int) (height / 0.618f), height);
        setCompoundDrawables(mDrawable, null, null, null);
        setCompoundDrawablePadding(Ui.dp2pix(5));
    }

    private void startCharger() {
        if (!mIsChargerWorking) {
            getHandler().post(mCharger);
            mIsChargerWorking = true;
        }
    }

    private void stopCharger() {
        if (mIsChargerWorking) {
            getHandler().removeCallbacks(mCharger);
            mIsChargerWorking = false;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;

            registerReceiver();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAttached) {
            unregisterReceiver();
            stopCharger();
            mAttached = false;
        }
    }

    private void registerReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
    }
}
