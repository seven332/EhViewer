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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.app;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.util.AppHandler;
import com.hippo.util.Messenger;

import java.util.Locale;

public abstract class StatsActivity extends AppCompatActivity {

    private static final int DEFAULT_MESSAGE_SIZE = 128;
    private static final long STATS_CLOCK_INTERVAL_MS = 1000;
    private static final int BYTES_IN_MEGABYTE = 1024 * 1024;

    private TextView mStatsTextView;
    private boolean mShowStats;
    private Runnable mStatsClockTickRunnable;
    private Handler mHandler;

    private Messenger.Receiver mReceiver = new Messenger.Receiver() {
        @Override
        public void onReceive(int id, Object obj) {
            if (obj instanceof Boolean) {
                boolean showStats = (Boolean) obj;
                if (mShowStats != showStats) {
                    mShowStats = showStats;
                    ViewGroup vg = (ViewGroup) getWindow().getDecorView();
                    if (showStats) {
                        vg.addView(mStatsTextView, generateLayoutParams());
                        updateStats();
                        scheduleNextStatsClockTick();
                    } else {
                        vg.removeView(mStatsTextView);
                        cancelNextStatsClockTick();
                    }
                }
            }
        }
    };

    @SuppressLint("RtlHardcoded")
    private ViewGroup.LayoutParams generateLayoutParams() {
        Resources resources = getResources();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.RIGHT;
        lp.topMargin = resources.getDimensionPixelOffset(R.dimen.stats_text_view_padding_top);
        lp.rightMargin = resources.getDimensionPixelOffset(R.dimen.stats_text_view_padding_right);
        return lp;
    }

    protected abstract boolean isShowStats();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStatsTextView = new TextView(this);
        mHandler = AppHandler.getInstance();
        mStatsClockTickRunnable = new Runnable() {
            @Override
            public void run() {
                updateStats();
                scheduleNextStatsClockTick();
            }
        };
        Messenger.getInstance().register(
                Constants.MESSENGER_ID_SHOW_APP_STATUS, mReceiver);

        if (isShowStats()) {
            mShowStats = true;
            ((ViewGroup) getWindow().getDecorView()).addView(mStatsTextView,
                    generateLayoutParams());
        } else {
            mShowStats = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Messenger.getInstance().unregister(Constants.MESSENGER_ID_SHOW_APP_STATUS, mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mShowStats) {
            updateStats();
            scheduleNextStatsClockTick();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mShowStats) {
            cancelNextStatsClockTick();
        }
    }

    private void scheduleNextStatsClockTick() {
        mHandler.postDelayed(mStatsClockTickRunnable, STATS_CLOCK_INTERVAL_MS);
    }

    private void cancelNextStatsClockTick() {
        mHandler.removeCallbacks(mStatsClockTickRunnable);
    }

    private void updateStats() {
        final Runtime runtime = Runtime.getRuntime();
        final long heapMemory = runtime.totalMemory() - runtime.freeMemory();
        final StringBuilder sb = new StringBuilder(DEFAULT_MESSAGE_SIZE);
        appendSize(sb, "Java allocated:          ", heapMemory, "\n");
        appendSize(sb, "Native allocated:        ", Debug.getNativeHeapAllocatedSize(), "\n");
        mStatsTextView.setText(sb.toString());
    }

    private static void appendSize(StringBuilder sb, String prefix, long bytes, String suffix) {
        String value = String.format(Locale.getDefault(), "%.2f", (float) bytes / BYTES_IN_MEGABYTE);
        appendValue(sb, prefix, value + " MB", suffix);
    }

    private static void appendValue(StringBuilder sb, String prefix, String value, String suffix) {
        sb.append(prefix).append(value).append(suffix);
    }
}
