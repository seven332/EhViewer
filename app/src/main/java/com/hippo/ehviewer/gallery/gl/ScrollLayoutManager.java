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

package com.hippo.ehviewer.gallery.gl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import com.hippo.gl.view.GLView;
import com.hippo.gl.widget.GLEdgeView;
import com.hippo.gl.widget.GLProgressView;
import com.hippo.gl.widget.GLTextureView;
import com.hippo.yorozuya.AssertUtils;

import java.util.Iterator;
import java.util.LinkedList;

// TODO Not safe ! Ensure change component in render thread
public class ScrollLayoutManager extends GalleryView.LayoutManager {

    private static final String TAG = ScrollLayoutManager.class.getSimpleName();

    private static final float RESERVATIONS = 0.5f;

    private GalleryView.PageIterator mIterator;

    private GLProgressView mProgress;
    private String mErrorStr;
    private GLTextureView mErrorView;
    private LinkedList<GalleryPageView> mPages = new LinkedList<>();

    private int mOffset;
    private int mDeltaX;
    private int mDeltaY;

    private int mInterval;
    private int mProgressSpec;

    private PageFling mPageFling;

    public ScrollLayoutManager(Context context, @NonNull GalleryView galleryView,
            int interval) {
        super(galleryView);

        mInterval = interval;
        mProgressSpec = GLView.MeasureSpec.makeMeasureSpec(GLView.LayoutParams.WRAP_CONTENT,
                GLView.LayoutParams.WRAP_CONTENT);
        mPageFling = new PageFling(context);
    }

    private void resetParameters() {
        mOffset = 0;
        mDeltaX = 0;
        mDeltaY = 0;
    }

    private void cancelAllAnimations() {
        mPageFling.cancel();
    }

    @Override
    public void onAttach(GalleryView.PageIterator iterator) {
        AssertUtils.assertEquals("The ScrollLayoutManager is attached", mIterator, null);
        AssertUtils.assertNotEquals("The iterator is null", iterator, null);
        mIterator = iterator;
        // Reset parameters
        resetParameters();
    }

    private void removeProgress() {
        if (mProgress != null) {
            mGalleryView.removeComponent(mProgress);
            mGalleryView.releaseProgress(mProgress);
            mProgress = null;
        }
    }

    private void removeErrorView() {
        if (mErrorView != null) {
            mGalleryView.removeComponent(mErrorView);
            mGalleryView.releaseErrorView(mErrorView);
            mErrorView = null;
            mErrorStr = null;
        }
    }

    private void removePage(@NonNull GalleryPageView page) {
        mGalleryView.removeComponent(page);
        mIterator.unbind(page);
        mGalleryView.releasePage(page);
    }

    private void removeAllPages() {
        for (GalleryPageView page : mPages) {
            removePage(page);
        }
        mPages.clear();
    }

    @Override
    public GalleryView.PageIterator onDetach() {
        AssertUtils.assertNotEquals("The PagerLayoutManager is not attached", mIterator, null);

        // Cancel all animations
        cancelAllAnimations();

        // Remove all view
        removeProgress();
        removeErrorView();
        removeAllPages();

        // Clear iterator
        GalleryView.PageIterator iterator = mIterator;
        mIterator = null;

        return iterator;
    }

    @Override
    public synchronized void onFill() {
        GalleryView.PageIterator iterator = mIterator;
        GalleryView galleryView = mGalleryView;
        AssertUtils.assertNotEquals("The PagerLayoutManager is not attached", iterator, null);

        int width = galleryView.getWidth();
        int height = galleryView.getHeight();
        String errorStr = iterator.getError();

        if (errorStr != null) {
            // Remove progress and all pages
            removeProgress();
            removeAllPages();

            // Ensure error view
            if (mErrorView == null) {
                mErrorView = galleryView.obtainErrorView();
                galleryView.addComponent(mErrorView);
            }

            // Update error string
            if (!errorStr.equals(mErrorStr)) {
                mErrorStr = errorStr;
                galleryView.bindErrorView(mErrorView, errorStr);
            }

            // Place error view center
            placeCenter(mErrorView);
        } else if (iterator.isWaiting()) {
            // Remove error view and all pages
            removeErrorView();
            removeAllPages();

            // Ensure progress
            if (mProgress == null) {
                mProgress = galleryView.obtainProgress();
                galleryView.addComponent(mProgress);
            }

            // Place progress center
            placeCenter(mProgress);
        } else {
            // Remove progress and error view
            removeProgress();
            removeErrorView();


            // TODO ensure first shown




            int minY = (int) (-height * RESERVATIONS);

            // Remove useless top view
            Iterator<GalleryPageView> pages = mPages.iterator();
            int nextTop = mOffset;
            while (pages.hasNext()) {
                GalleryPageView page = pages.next();
                nextTop += page.getHeight() + mInterval;
                if (nextTop < minY) {
                    removePage(page);
                    pages.remove();
                    // Update offset
                    mOffset = nextTop;
                    if (iterator.hasNext()) {
                        iterator.next();
                    } else {
                        Log.e(TAG, "iterator should has next");
                    }
                } else {
                    break;
                }
            }

            // Fill missing top
            int oldOffset = mOffset;
            int y = oldOffset;
            int widthSpec = GLView.MeasureSpec.makeMeasureSpec(width, GLView.MeasureSpec.EXACTLY);
            int heightSpec = GLView.MeasureSpec.makeMeasureSpec(height, GLView.MeasureSpec.UNSPECIFIED);
            int pageSize = 0;
            while (y >= minY && iterator.hasPrevious()) {
                iterator.previous();

                GalleryPageView page = galleryView.obtainPage();
                iterator.bind(page);
                galleryView.addComponent(page);
                mPages.add(0, page);

                // Add interval
                y -= mInterval;

                // Measure and layout
                page.measure(widthSpec, heightSpec);
                page.layout(0, y - page.getMeasuredHeight(), width, y);

                // Update y
                y -= page.getMeasuredHeight();

                // size increase
                pageSize++;
            }
            mOffset = y;

            // Fill from oldOffset
            iterator.mark();
            // Go to oldOffset
            int maxY = (int) (height * (1 + RESERVATIONS));
            y = oldOffset;
            pages = mPages.iterator();
            for (int i = 0; i < pageSize; i++) {
                if (iterator.hasNext()) {
                    iterator.next();
                } else {
                    Log.e(TAG, "iterator should has next");
                }
                pages.next();
            }
            // Do fill
            while (true) {
                GalleryPageView page;
                if (pages == null || !pages.hasNext()) {
                    pages = null;
                    // New page
                    page = galleryView.obtainPage();
                    iterator.bind(page);
                    galleryView.addComponent(page);
                    mPages.add(page);
                } else {
                    page = pages.next();
                }

                // Measure and layout
                page.measure(widthSpec, heightSpec);
                page.layout(0, y, width, y + page.getMeasuredHeight());

                // size increase
                pageSize++;

                // Update y and check out of range
                y += page.getMeasuredHeight();
                if (y > maxY) {
                    break;
                }

                y += mInterval;

                // Check has next
                if (!iterator.hasNext()) {
                    break;
                }

                iterator.next();
            }
            iterator.reset();


            // TODO Avoid page not fill bottom, but has previous


            // Remove useless page
            while (mPages.size() > pageSize) {
                GalleryPageView page = mPages.removeLast();
                removePage(page);
            }
        }
    }

    @Override
    public void onDown() {
        mDeltaX = 0;
        mDeltaY = 0;

        cancelAllAnimations();
    }

    @Override
    public void onUp() {
        mGalleryView.getEdgeView().onRelease();
    }

    @Override
    public void onDoubleTapConfirmed(float x, float y) {

    }

    public void overScrollEdge(int dx, int dy, float x, float y) {
        GLEdgeView edgeView = mGalleryView.getEdgeView();

        mDeltaX += dx;
        mDeltaY += dy;

        /*
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
        */

        if (mDeltaY < 0) {
            edgeView.onPull(-mDeltaY, x, GLEdgeView.TOP);
            if (!edgeView.isFinished(GLEdgeView.BOTTOM)) {
                edgeView.onRelease(GLEdgeView.BOTTOM);
            }
        } else if (mDeltaY > 0) {
            edgeView.onPull(mDeltaY, x, GLEdgeView.BOTTOM);
            if (!edgeView.isFinished(GLEdgeView.TOP)) {
                edgeView.onRelease(GLEdgeView.TOP);
            }
        }
    }

    // True for get top or bottom
    private boolean scrollInternal(float dx, float dy, boolean fling, float x, float y) {
        if (mPages.size() == 0) {
            return false;
        }

        int height = mGalleryView.getHeight();
        boolean requestFill = false;
        int remainY = (int) dy;
        boolean result = false;

        while (remainY != 0) {
            if (remainY < 0) { // Try to show top
                int limit;
                if (mIterator.hasPrevious()) {
                    limit = (int) (-height * RESERVATIONS);
                } else {
                    limit = 0;
                }

                if (mOffset - remainY < limit) {
                    mOffset -= remainY;
                    remainY = 0;
                    requestFill = true;
                    mDeltaX = 0;
                    mDeltaY = 0;
                } else {
                    if (mIterator.hasPrevious()) {
                        mOffset = limit;
                        remainY = remainY + limit - mOffset;
                        onFill();
                        requestFill = false;
                        mDeltaX = 0;
                        mDeltaY = 0;
                    } else {
                        if (mOffset != limit) {
                            mOffset = limit;
                            requestFill = true;
                        }
                        if (!fling) {
                            overScrollEdge(0, remainY + limit - mOffset, x, y);
                        }
                        remainY = 0;
                        result = true;
                    }
                }
            } else { // Try to show bottom
                int bottom = mOffset;
                boolean hasNext = true;
                GalleryView.PageIterator iterator = mIterator;
                iterator.mark();
                for (int i = 0, size = mPages.size(); i < size; i++) {
                    if (i != 0) {
                        bottom += mInterval;
                    }
                    bottom += mPages.get(i).getHeight();
                    if (iterator.hasNext()) {
                        iterator.next();
                    } else {
                        hasNext = false;
                        break;
                    }
                }
                iterator.reset();

                int limit;
                if (hasNext) {
                    limit = (int) (height * (1 + RESERVATIONS));
                } else {
                    limit = height;
                }
                // Fix limit for page not fill screen
                limit = Math.min(bottom, limit);

                if (bottom - remainY > limit) {
                    mOffset -= remainY;
                    remainY = 0;
                    requestFill = true;
                    mDeltaX = 0;
                    mDeltaY = 0;
                } else {
                    if (hasNext) {
                        mOffset -= bottom - limit;
                        remainY = remainY + limit - bottom;
                        onFill();
                        requestFill = false;
                        mDeltaX = 0;
                        mDeltaY = 0;
                    } else {
                        if (mOffset != limit) {
                            mOffset -= bottom - limit;
                            requestFill = true;
                        }
                        if (!fling) {
                            overScrollEdge(0, remainY + limit - bottom, x, y);
                        }
                        remainY = 0;
                        result = true;
                    }
                }
            }
        }

        if (requestFill) {
            mGalleryView.requestFill();
        }

        return result;
    }

    @Override
    public void onScroll(float dx, float dy, float totalX, float totalY, float x, float y) {
        scrollInternal(dx, dy, false, x, y);
    }

    private boolean hasNextPage() {
        boolean hasNext = true;
        GalleryView.PageIterator iterator = mIterator;
        iterator.mark();

        for (int i = 0, size = mPages.size(); i < size; i++) {
            if (iterator.hasNext()) {
                iterator.next();
            } else {
                hasNext = false;
                break;
            }
        }

        iterator.reset();
        return hasNext;
    }

    @Override
    public void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mPages.size() <= 0) {
            return;
        }

        int maxY;
        if (mIterator.hasPrevious()) {
            maxY = Integer.MAX_VALUE;
        } else {
            maxY = -mOffset;
        }

        int bottom = mOffset;
        boolean hasNext = true;
        GalleryView.PageIterator iterator = mIterator;
        iterator.mark();
        for (int i = 0, size = mPages.size(); i < size; i++) {
            if (i != 0) {
                bottom += mInterval;
            }
            bottom += mPages.get(i).getHeight();
            if (iterator.hasNext()) {
                iterator.next();
            } else {
                hasNext = false;
                break;
            }
        }
        iterator.reset();

        int minY;
        if (hasNext) {
            minY = Integer.MIN_VALUE;
        } else {
            minY = mGalleryView.getHeight() - bottom;
        }

        mPageFling.startFling((int) velocityX, 0, 0,
                (int) velocityY, minY, maxY);
    }

    @Override
    public boolean canScale() {
        return false;
    }

    @Override
    public void onScale(float focusX, float focusY, float scale) {

    }

    @Override
    public boolean onUpdateAnimation(long time) {
        boolean invalidate = mPageFling.calculate(time);
        return invalidate;
    }

    @Override
    public void onDataChanged() {
        AssertUtils.assertNotEquals("The PagerLayoutManager is not attached", mIterator, null);

        // Cancel all animations
        cancelAllAnimations();
        // Remove all views
        removeProgress();
        removeErrorView();
        removeAllPages();
        // Reset parameters
        resetParameters();
        mGalleryView.requestFill();
    }

    @Override
    public GalleryPageView findPageById(int id) {
        for (int i = 0, n = mPages.size(); i < n; i++) {
            GalleryPageView page = mPages.get(i);
            if (page.getId() == id) {
                return page;
            }
        }
        return null;
    }

    class PageFling extends Fling {

        private int mVelocityX;
        private int mVelocityY;
        private int mDx;
        private int mDy;
        private int mLastX;
        private int mLastY;

        public PageFling(Context context) {
            super(context);
        }

        public void startFling(int velocityX, int minX, int maxX,
                int velocityY, int minY, int maxY) {
            mVelocityX = velocityX;
            mVelocityY = velocityY;
            mDx = (int) (getSplineFlingDistance(velocityX) * Math.signum(velocityX));
            mDy = (int) (getSplineFlingDistance(velocityY) * Math.signum(velocityY));
            mLastX = 0;
            mLastY = 0;
            int durationX = getSplineFlingDuration(velocityX);
            int durationY = getSplineFlingDuration(velocityY);

            if (mDx < minX) {
                durationX = adjustDuration(0, mDx, minX, durationX);
                mDx = minX;
            }
            if (mDx > maxX) {
                durationX = adjustDuration(0, mDx, maxX, durationX);
                mDx = maxX;
            }
            if (mDy < minY) {
                durationY = adjustDuration(0, mDy, minY, durationY);
                mDy = minY;
            }
            if (mDy > maxY) {
                durationY = adjustDuration(0, mDy, maxY, durationY);
                mDy = maxY;
            }

            if (mDx == 0 && mDy == 0) {
                return;
            }

            setDuration(Math.max(durationX, durationY));
            start();
            mGalleryView.invalidate();
        }

        @Override
        protected void onCalculate(float progress) {
            int x = (int) (mDx * progress);
            int y = (int) (mDy * progress);
            int offsetX = x - mLastX;
            int offsetY = y - mLastY;
            if (scrollInternal(-offsetX, -offsetY, true, 0, 0)) {
                cancel();
                onFinish();
            }
            mLastX = x;
            mLastY = y;
        }

        @Override
        protected void onFinish() {
            boolean topEdge = !mIterator.hasPrevious() && mOffset >= 0;

            int bottom = mOffset;
            boolean hasNext = true;
            GalleryView.PageIterator iterator = mIterator;
            iterator.mark();
            for (int i = 0, size = mPages.size(); i < size; i++) {
                if (i != 0) {
                    bottom += mInterval;
                }
                bottom += mPages.get(i).getHeight();
                if (iterator.hasNext()) {
                    iterator.next();
                } else {
                    hasNext = false;
                    break;
                }
            }
            iterator.reset();
            boolean bottomEdge = !hasNext && bottom <= mGalleryView.getHeight();

            if (topEdge && bottomEdge) {
                return;
            }

            GLEdgeView edgeView = mGalleryView.getEdgeView();
            if (topEdge && edgeView.isFinished(GLEdgeView.TOP)) {
                edgeView.onAbsorb(mVelocityY, GLEdgeView.TOP);
            } else if (bottomEdge && edgeView.isFinished(GLEdgeView.BOTTOM)) {
                edgeView.onAbsorb(-mVelocityY, GLEdgeView.BOTTOM);
            }
        }
    }
}
