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

package com.hippo.ehviewer.util;

import android.animation.TimeInterpolator;

import com.hippo.ehviewer.AppHandler;

public abstract class TimeRunner {
    private static final int INTERVAL = 5;

    private static final int RUNNING = 0x0;
    private static final int START = 0x1;
    private static final int END = 0x2;
    private static final int CANCEL = 0x3;

    private boolean mDoInThread = false;
    private int mDuration = 0;
    private int mDelay = 0;
    private TimeInterpolator mInterpolator;
    private volatile boolean mCancel = false;

    private class Respond implements Runnable {
        private final int state;
        private float interpolatedTime;
        private int runningTime;

        public Respond(int state) {
            this.state = state;
        }

        public Respond(float interpolatedTime, int runningTime) {
            this.state = RUNNING;
            this.interpolatedTime = interpolatedTime;
            this.runningTime = runningTime;
        }

        @Override
        public void run() {
            switch (state) {
            case RUNNING:
                TimeRunner.this.onRun(interpolatedTime, runningTime);
                break;
            case START:
                TimeRunner.this.onStart();
                break;
            case END:
                TimeRunner.this.onEnd();
                break;
            case CANCEL:
                TimeRunner.this.onCancel();
            }
        }
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public void setDoInThread(boolean doInThread) {
        mDoInThread = doInThread;
    }

    public void setInterpolator(TimeInterpolator interpolator) {
        mInterpolator = interpolator;
    }

    public void cancel() {
        mCancel = true;
    }

    public void start() {
        start(0);
    }

    public void start(int delay) {
        mCancel = false;
        mDelay = delay;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(mDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mDoInThread)
                    onStart();
                else
                    AppHandler.getInstance().post(new Respond(START));

                // Check cancel
                if (mCancel) {
                    if (mDoInThread)
                        onStart();
                    else
                        AppHandler.getInstance().post(new Respond(CANCEL));
                    return;
                }

                long startTime = System.currentTimeMillis();
                for (int runningTime = 0; runningTime < mDuration; runningTime = (int)(System.currentTimeMillis() - startTime)) {
                    // Check cancel
                    if (mCancel) {
                        if (mDoInThread)
                            onStart();
                        else
                            AppHandler.getInstance().post(new Respond(CANCEL));
                        return;
                    }

                    float percent = runningTime / (float)mDuration;
                    if (mInterpolator != null)
                        percent = mInterpolator.getInterpolation(percent);

                    if (mDoInThread)
                        TimeRunner.this.onRun(percent, runningTime);
                    else
                        AppHandler.getInstance().post(
                                new Respond(percent, runningTime));
                    try {
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (mDoInThread)
                    onEnd();
                else
                    AppHandler.getInstance().post(new Respond(END));
            }
        }).start();
    }

    protected abstract void onStart();
    protected abstract void onRun(float percent, int runningTime);
    protected abstract void onEnd();
    protected abstract void onCancel();
}
