/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.hippo.ehviewer.gallery.ui;

import android.content.Context;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

// This class aggregates three gesture detectors: GestureDetector,
// ScaleGestureDetector, and DownUpDetector.
public class GestureRecognizer {
    @SuppressWarnings("unused")
    private static final String TAG = "GestureRecognizer";

    public interface Listener {
        boolean onSingleTapConfirmed(float x, float y);
        boolean onDoubleTap(float x, float y);
        boolean onDoubleTapConfirmed(float x, float y);
        void onLongPress(MotionEvent e);
        boolean onScrollBegin(float dx, float dy, float totalX, float totalY);
        boolean onScroll(float dx, float dy, float totalX, float totalY);
        boolean onScrollEnd();
        boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
        boolean onScaleBegin(float focusX, float focusY);
        boolean onScale(float focusX, float focusY, float scale);
        void onScaleEnd();
        void onDown(float x, float y);
        void onUp();
    }

    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;
    private final DownUpDetector mDownUpDetector;
    private final Listener mListener;

    private boolean mStillScroll = false;

    public GestureRecognizer(Context context, Listener listener) {
        mListener = listener;
        mScaleDetector = new ScaleGestureDetector(
                context, new MyScaleListener());
        mGestureDetector = new GestureDetector(context, new MyGestureListener(),
                null, true /* ignoreMultitouch */);
        mDownUpDetector = new DownUpDetector(new MyDownUpListener());
    }

    public void onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        mDownUpDetector.onTouchEvent(event);
    }

    public boolean isDown() {
        return mDownUpDetector.isDown();
    }

    public void cancelScale() {
        long now = SystemClock.uptimeMillis();
        MotionEvent cancelEvent = MotionEvent.obtain(
                now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        mScaleDetector.onTouchEvent(cancelEvent);
        cancelEvent.recycle();
    }

    private class MyGestureListener
                extends GestureDetector.SimpleOnGestureListener {
        float firstX = 0;
        float firstY = 0;
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return mListener.onSingleTapConfirmed(e.getX(), e.getY());
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return mListener.onDoubleTap(e.getX(), e.getY());
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP ||
                    e.getAction() == MotionEvent.ACTION_CANCEL)
                return mListener.onDoubleTapConfirmed(e.getX(), e.getY());
            else
                return super.onDoubleTapEvent(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            mListener.onLongPress(e);
        }

        @Override
        public boolean onScroll(
                MotionEvent e1, MotionEvent e2, float dx, float dy) {
            if (e1.getX() != firstX || e1.getY() != firstY) {
                mStillScroll = true;
                firstX = e1.getX();
                firstY = e1.getY();
                return mListener.onScrollBegin(
                        dx, dy, e2.getX() - e1.getX(), e2.getY() - e1.getY());
            }
            else
                return mListener.onScroll(
                        dx, dy, e2.getX() - e1.getX(), e2.getY() - e1.getY());
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            return mListener.onFling(e1, e2, velocityX, velocityY);
        }
    }

    private class MyScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return mListener.onScaleBegin(
                    detector.getFocusX(), detector.getFocusY());
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return mListener.onScale(detector.getFocusX(),
                    detector.getFocusY(), detector.getScaleFactor());
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mListener.onScaleEnd();
        }
    }

    private class MyDownUpListener implements DownUpDetector.DownUpListener {
        @Override
        public void onDown(MotionEvent e) {
            mListener.onDown(e.getX(), e.getY());
        }

        @Override
        public void onUp(MotionEvent e) {
            mListener.onUp();
            if (mStillScroll) {
                mStillScroll = false;
                mListener.onScrollEnd();
            }
        }
    }
}
