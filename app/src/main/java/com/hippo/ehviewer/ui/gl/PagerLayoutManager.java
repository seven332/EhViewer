/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.gl;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

import com.hippo.gl.anim.Animation;
import com.hippo.gl.view.GLView;
import com.hippo.gl.widget.GLEdgeView;
import com.hippo.gl.widget.GLProgressView;
import com.hippo.yorozuya.AssertUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PagerLayoutManager extends GalleryView.LayoutManager {

    private static final String TAG = PagerLayoutManager.class.getSimpleName();

    @IntDef({MODE_LEFT_TO_RIGHT, MODE_RIGHT_TO_LEFT})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Mode {}

    private static final int MODE_LEFT_TO_RIGHT = 0;
    private static final int MODE_RIGHT_TO_LEFT = 1;

    private static final Interpolator SMOOTH_SCROLLER_INTERPOLATOR = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    private GalleryView.PageIterator mIterator;

    private GLProgressView mProgress;
    private GalleryPageView mPrevious;
    private GalleryPageView mCurrent;
    private GalleryPageView mNext;

    @Mode
    private int mMode = MODE_RIGHT_TO_LEFT;
    private int mOffset;
    private int mInterval;
    private int mProgressSpec;

    private int[] mScrollRemain = new int[2];

    private boolean mCanScrollBetweenPages = false;

    private int mDeltaX;
    private int mDeltaY;

    public PagerLayoutManager(@NonNull GalleryView galleryView, int interval, int progressSize) {
        super(galleryView);
        mInterval = interval;
        mProgressSpec = GLView.MeasureSpec.makeMeasureSpec(progressSize, GLView.MeasureSpec.EXACTLY);
    }

    public void setMode(@Mode int mode) {
        // TODO
        if (mIterator != null) {

        } else {

        }
    }

    @Override
    public void onAttach(GalleryView.PageIterator iterator) {
        AssertUtils.assertEquals("The PagerLayoutManager is attached", mIterator, null);
        mIterator = iterator;
    }

    private void removeProgress() {
        if (mProgress != null) {
            mGalleryView.removeComponent(mProgress);
            mProgress = null;
        }
    }

    private void removePage(@NonNull GalleryPageView page) {
        mGalleryView.removeComponent(page);
        mIterator.unbind(page);
        mGalleryView.releasePage(page);
    }

    private void removeAllPages() {
        // Remove gallery view
        if (mPrevious != null) {
            removePage(mPrevious);
            mPrevious = null;
        }
        if (mCurrent != null) {
            removePage(mCurrent);
            mCurrent = null;
        }
        if (mNext != null) {
            removePage(mNext);
            mNext = null;
        }
    }

    @Override
    public GalleryView.PageIterator onDetach() {
        AssertUtils.assertNotEquals("The PagerLayoutManager is not attached", mIterator, null);

        removeAllPages();

        // Clear iterator
        GalleryView.PageIterator iterator = mIterator;
        mIterator = null;

        return iterator;
    }

    private GalleryPageView getLeftPage() {
        switch (mMode) {
            case MODE_LEFT_TO_RIGHT:
                return mPrevious;
            default:
            case MODE_RIGHT_TO_LEFT:
                return mNext;
        }
    }

    private void setLeftPage(GalleryPageView page) {
        switch (mMode) {
            case MODE_LEFT_TO_RIGHT:
                mPrevious = page;
                break;
            default:
            case MODE_RIGHT_TO_LEFT:
                mNext = page;
                break;
        }
    }

    private GalleryPageView getRightPage() {
        switch (mMode) {
            case MODE_LEFT_TO_RIGHT:
                return mNext;
            default:
            case MODE_RIGHT_TO_LEFT:
                return mPrevious;
        }
    }

    private void setRightPage(GalleryPageView page) {
        switch (mMode) {
            case MODE_LEFT_TO_RIGHT:
                mNext = page;
                break;
            default:
            case MODE_RIGHT_TO_LEFT:
                mPrevious = page;
                break;
        }
    }

    @Override
    public void onFill() {
        GalleryView.PageIterator iterator = mIterator;
        GalleryView galleryView = mGalleryView;
        AssertUtils.assertNotEquals("The PagerLayoutManager is not attached", iterator, null);

        int width = galleryView.getWidth();
        int height = galleryView.getHeight();

        if (!iterator.isValid()) {
            // Remove all pages
            removeAllPages();

            // Ensure progress
            if (mProgress == null) {
                mProgress = galleryView.obtainProgress();
                galleryView.addComponent(mProgress);
            }

            // Measure and layout progress
            mProgress.measure(mProgressSpec, mProgressSpec);
            int progressWidth = mProgress.getMeasuredWidth();
            int progressHeight = mProgress.getMeasuredHeight();
            int progressLeft = width / 2 - progressWidth / 2;
            int progressTop = height / 2 - progressHeight / 2;
            mProgress.layout(progressLeft, progressTop, progressLeft + progressWidth, progressTop + progressHeight);
        } else {
            // Remove progress
            removeProgress();

            // Ensure pages
            if (mCurrent == null) {
                mCurrent = galleryView.obtainPage();
                iterator.bind(mCurrent);
                galleryView.addComponent(mCurrent);
            }
            if (mPrevious == null && iterator.hasPrevious()) {
                iterator.mark();
                iterator.previous();
                mPrevious = galleryView.obtainPage();
                iterator.bind(mPrevious);
                galleryView.addComponent(mPrevious);
                iterator.reset();
            }
            if (mNext == null && iterator.hasNext()) {
                iterator.mark();
                iterator.next();
                mNext = galleryView.obtainPage();
                iterator.bind(mNext);
                galleryView.addComponent(mNext);
                iterator.reset();
            }

            // Measure and layout pages
            int offset = mOffset;
            int widthSpec = GLView.MeasureSpec.makeMeasureSpec(width, GLView.MeasureSpec.EXACTLY);
            int heightSpec = GLView.MeasureSpec.makeMeasureSpec(height, GLView.MeasureSpec.EXACTLY);
            if (mCurrent != null) {
                mCurrent.measure(widthSpec, heightSpec);
                mCurrent.layout(offset, 0, width + offset, height);
            }
            GalleryPageView leftPage = getLeftPage();
            if (leftPage != null) {
                leftPage.measure(widthSpec, heightSpec);
                leftPage.layout(-mInterval - width + offset, 0, -mInterval + offset, height);
            }
            GalleryPageView rightPage = getRightPage();
            if (rightPage != null) {
                rightPage.measure(widthSpec, heightSpec);
                rightPage.layout(width + mInterval + offset, 0, width + mInterval + width + offset, height);
            }
        }
    }

    @Override
    public void onDown() {
        mDeltaX = 0;
        mDeltaY = 0;
    }

    @Override
    public void onUp() {
        mGalleryView.getEdgeView().onRelease();
    }

    private void pagePrevious() {
        GalleryView.PageIterator iterator = mIterator;
        if (!iterator.hasPrevious()) {
            return;
        }
        iterator.previous();

        if (mNext != null) {
            removePage(mNext);
        }
        mNext = mCurrent;
        mCurrent = mPrevious;
        mPrevious = null;

        if (iterator.hasPrevious()) {
            iterator.mark();
            iterator.previous();
            mPrevious = mGalleryView.obtainPage();
            iterator.bind(mPrevious);
            mGalleryView.addComponent(mPrevious);
            iterator.reset();
        }
    }

    private void pageNext() {
        GalleryView.PageIterator iterator = mIterator;
        if (!iterator.hasNext()) {
            return;
        }
        iterator.next();

        if (mPrevious != null) {
            removePage(mPrevious);
        }
        mPrevious = mCurrent;
        mCurrent = mNext;
        mNext = null;

        if (iterator.hasNext()) {
            iterator.mark();
            iterator.next();
            mPrevious = mGalleryView.obtainPage();
            iterator.bind(mPrevious);
            mGalleryView.addComponent(mPrevious);
            iterator.reset();
        }
    }

    private void pageLeft() {
        switch (mMode) {
            case MODE_LEFT_TO_RIGHT:
                pagePrevious();
                break;
            default:
            case MODE_RIGHT_TO_LEFT:
                pageNext();
                break;
        }
    }

    private void pageRight() {
        switch (mMode) {
            case MODE_LEFT_TO_RIGHT:
                pageNext();
                break;
            default:
            case MODE_RIGHT_TO_LEFT:
                pagePrevious();
                break;
        }
    }

    private int scrollBetweenPages(int dx) {
        GalleryPageView leftPage = getLeftPage();
        GalleryPageView rightPage = getRightPage();
        int width = mGalleryView.getWidth();

        int remain;
        if (dx < 0) { // Try to show left
            if (leftPage == null) {
                return dx;
            }

            if (dx > mOffset - width -  mInterval) {
                remain = 0;
                mOffset -= dx;
            } else {
                // Go to left page
                pageLeft();
                remain = dx + width + mInterval - mOffset;
                mOffset = 0;
            }
        } else { // Try to show right
            if (rightPage == null) {
                return dx;
            }

            if (dx < width + mInterval + mOffset) {
                remain = 0;
                mOffset -= dx;
            } else {
                // Go to right page
                pageRight();
                remain = dx - width - mInterval - mOffset;
                mOffset = 0;
            }
        }

        return remain;
    }

    public void overScrollEdge(float x, float y, int dx, int dy) {
        GLEdgeView edgeView = mGalleryView.getEdgeView();

        mDeltaX += dx;
        mDeltaY += dy;

        if (mDeltaX < 0) {
            edgeView.onPull(-mDeltaX, y, GLEdgeView.LEFT);
            if (!edgeView.isFinished(GLEdgeView.RIGHT)) {
                edgeView.onRelease(GLEdgeView.RIGHT);
            }
        } else if (mDeltaX > 0) {
            edgeView.onPull(mDeltaX, y, GLEdgeView.RIGHT);
            if (!edgeView.isFinished(GLEdgeView.LEFT)) {
                edgeView.onRelease(GLEdgeView.LEFT);
            }
        }

        // TODO What about top and bottom
    }

    @Override
    public void onScroll(float dx, float dy, float totalX, float totalY, float x, float y) {
        if (mCurrent == null) {
            return;
        }

        boolean needFill = false;
        boolean canImageScroll = true;
        int remainX = (int) dx;
        int remainY = (int) dy;

        if (mGalleryView.isFirstScroll()) {
            mCanScrollBetweenPages = Math.abs(dx) > Math.abs(dy) * 1.5;
        }

        while (remainX != 0 || remainY != 0) {
            if (mOffset == 0 && canImageScroll) {
                ImageView image = mCurrent.getImageView();
                image.scroll(remainX, remainY, mScrollRemain);
                remainX = mScrollRemain[0];
                remainY = mScrollRemain[1];
                canImageScroll = false;
                mDeltaX = 0;
                mDeltaY = 0;
            } else if (remainX == 0 ||
                    (getLeftPage() == null && mOffset == 0 && remainX < 0) ||
                    (getRightPage() == null && mOffset == 0 && remainX > 0)) {
                // On edge
                overScrollEdge(x, y, remainX, remainY);
                remainX = 0;
                remainY = 0;
            } else if (mCanScrollBetweenPages) {
                remainX = scrollBetweenPages(remainX);
                canImageScroll = true;
                needFill = true;
                mDeltaX = 0;
                mDeltaY = 0;
            } else {
                Log.w(TAG, "Something wrong?");
                remainX = 0;
                remainY = 0;
                mDeltaX = 0;
                mDeltaY = 0;
            }
        }

        if (needFill) {
            mGalleryView.requestFill();
        }
    }

    @Override
    public void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mCurrent == null) {
            return;
        }



    }

    @Override
    public void onScale(float focusX, float focusY, float scale) {
        if (mCurrent == null) {
            return;
        }



    }

    @Override
    public boolean onUpdateAnimation(long time) {
        return false;
    }








    class SmoothScroller extends Animation {

        private int mDx;
        private int mDy;
        private int mLastX;
        private int mLastY;

        public void startSmoothScroll(int dx, int dy, int duration) {
            mDx = dx;
            mDy = dy;
            mLastX = 0;
            mLastY = 0;
            setDuration(duration);
            start();
            // TODO invalidate();
        }

        @Override
        protected void onCalculate(float progress) {
            int x = (int) (mDx * progress);
            int y = (int) (mDy * progress);
            int offsetX = x - mLastX;
            while (offsetX != 0) {
                int oldOffsetX = offsetX;
                // TODO offsetX = scrollBetweenPagesHMode(offsetX);
                // Avoid loop infinitely
                if (offsetX == oldOffsetX) {
                    break;
                } else {
                    // TODO mRequestFill = true;
                }
            }
            mLastX = x;
            mLastY = y;
        }
    }
}
