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
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.Log;

import com.hippo.ehviewer.gallery.GalleryProvider;
import com.hippo.gl.view.GLView;
import com.hippo.gl.widget.GLEdgeView;
import com.hippo.gl.widget.GLProgressView;
import com.hippo.gl.widget.GLTextureView;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.LayoutUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ScrollLayoutManager extends GalleryView.LayoutManager {

    private static final String TAG = ScrollLayoutManager.class.getSimpleName();

    private static final int INTERVAL = 24;

    private static final float RESERVATIONS = 0.5f;

    private GalleryView.Adapter mAdapter;

    private GLProgressView mProgress;
    private String mErrorStr;
    private GLTextureView mErrorView;
    private final LinkedList<GalleryPageView> mPages = new LinkedList<>();

    private int mOffset;
    private int mDeltaX;
    private int mDeltaY;
    private int mFirstShownLoadedPageIndex = GalleryPageView.INVALID_INDEX;
    private boolean mScrollUp;
    private boolean mStopAnimationFinger;

    private final int mInterval;

    private final PageFling mPageFling;

    // Current index
    private int mIndex;

    private int mBottomStateBottom;
    private boolean mBottomStateHasNext;

    public ScrollLayoutManager(Context context, @NonNull GalleryView galleryView) {
        super(galleryView);

        mInterval = LayoutUtils.dp2pix(context, INTERVAL);
        mPageFling = new PageFling(context);
    }

    private void resetParameters() {
        mOffset = 0;
        mDeltaX = 0;
        mDeltaY = 0;
        mFirstShownLoadedPageIndex = GalleryPageView.INVALID_INDEX;
        mScrollUp = false;
        mStopAnimationFinger = false;
    }

    // Return true for animations are running
    private boolean cancelAllAnimations() {
        boolean running = mPageFling.isRunning();
        mPageFling.cancel();
        return running;
    }

    @Override
    public void onAttach(GalleryView.Adapter adapter) {
        AssertUtils.assertEquals("The ScrollLayoutManager is attached", mAdapter, null);
        AssertUtils.assertNotEquals("The iterator is null", adapter, null);
        mAdapter = adapter;
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
        mAdapter.unbind(page);
        mGalleryView.releasePage(page);
    }

    private void removeAllPages() {
        for (GalleryPageView page : mPages) {
            removePage(page);
        }
        mPages.clear();
    }

    @Override
    public GalleryView.Adapter onDetach() {
        AssertUtils.assertNotEquals("The PagerLayoutManager is not attached", mAdapter, null);

        // Cancel all animations
        cancelAllAnimations();

        // Remove all view
        removeProgress();
        removeErrorView();
        removeAllPages();

        // Clear iterator
        GalleryView.Adapter iterator = mAdapter;
        mAdapter = null;

        return iterator;
    }

    private GalleryPageView obtainPage() {
        GalleryPageView page = mGalleryView.obtainPage();
        page.getImageView().setScaleOffset(ImageView.SCALE_FIT, ImageView.START_POSITION_TOP_RIGHT, 1.0f);
        return page;
    }

    private void fillPages() {
        GalleryView.Adapter adapter = mAdapter;
        GalleryView galleryView = mGalleryView;
        int width = galleryView.getWidth();
        int height = galleryView.getHeight();
        int size = adapter.size();

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
                if (mIndex < size - 1) {
                    mIndex++;
                } else {
                    Log.e(TAG, "The page is out of show range, but it has no next page");
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
        while (y >= minY && mIndex > 0) {
            mIndex--;

            GalleryPageView page = obtainPage();
            galleryView.addComponent(page);
            mAdapter.bind(page, mIndex);
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

        // Fix offset, ensure it is not positive
        if (mOffset > 0) {
            int offset = mOffset;
            // Update offset
            mOffset = 0;
            oldOffset -= offset;
            // Translate pages
            pages = mPages.iterator();
            for (int i = 0; i < pageSize; i++) {
                pages.next().offsetTopAndBottom(-offset);
            }
        }

        // Fill from oldOffset
        int maxY = (int) (height * (1 + RESERVATIONS));
        y = oldOffset;
        pages = mPages.iterator();
        for (int i = 0; i < pageSize; i++) {
            pages.next();
        }
        int index = mIndex + pageSize;
        // Do fill
        while (true) {
            GalleryPageView page;
            if (pages == null || !pages.hasNext()) {
                pages = null;
                // New page
                page = obtainPage();
                galleryView.addComponent(page);
                mAdapter.bind(page, index);
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
            if (index >= size - 1) {
                break;
            }

            index++;
        }

        // Remove useless page
        while (mPages.size() > pageSize) {
            GalleryPageView page = mPages.removeLast();
            removePage(page);
        }
    }

    @Override
    public void onFill() {
        GalleryView.Adapter adapter = mAdapter;
        GalleryView galleryView = mGalleryView;
        AssertUtils.assertNotEquals("The PagerLayoutManager is not attached", adapter, null);

        int height = galleryView.getHeight();
        int size = adapter.size();
        String errorStr = adapter.getError();

        if (size == GalleryProvider.STATE_WAIT) { // Wait here, show progress bar
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
        } else if (size <= GalleryProvider.STATE_ERROR || size == 0) { // Get error or empty, show error text
            // Ensure error is not null
            if (0 == size) {
                errorStr = galleryView.getEmptyStr();
            } else if (null == errorStr) {
                errorStr = galleryView.getDefaultErrorStr();
            }

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
        } else {
            LinkedList<GalleryPageView> pages = mPages;

            // Remove progress and error view
            removeProgress();
            removeErrorView();

            // Ensure index in range
            int index = mIndex;
            if (index < 0) {
                Log.e(TAG, "index < 0, index = " + index);
                index = 0;
                mIndex = index;
                removeAllPages();
            } else if (index >= size) {
                Log.e(TAG, "index >= size, index = " + index + ", size = " + size);
                index = size - 1;
                mIndex = index;
                removeAllPages();
            }

            // Find first shown loaded page top
            GalleryPageView firstShownLoadedPage = null;
            int firstShownLoadedPageTop = mOffset;
            if (mFirstShownLoadedPageIndex != GalleryPageView.INVALID_INDEX) {
                for (GalleryPageView page : pages) {
                    // Check first shown loaded page
                    if (mFirstShownLoadedPageIndex == page.getIndex()) {
                        firstShownLoadedPage = page;
                        break;
                    }
                    firstShownLoadedPageTop += page.getHeight() + mInterval;
                }
            }

            fillPages();

            // Ensure first shown loaded page top is the same
            if (firstShownLoadedPage != null && mFirstShownLoadedPageIndex == firstShownLoadedPage.getIndex()) {
                int newTop = firstShownLoadedPage.bounds().top;
                if (firstShownLoadedPageTop != newTop) {
                    mOffset += firstShownLoadedPageTop - newTop;
                    fillPages();
                }
            }

            // Avoid page not fill bottom, but has previous
            while (true) {
                int bottomDist = height - pages.getLast().bounds().bottom;
                int topDist = 0 - pages.getFirst().bounds().top;
                if (bottomDist > 0 && topDist > 0) {
                    int translate = Math.min(bottomDist, topDist);
                    // Update offset
                    mOffset += translate;
                    // Translate pages
                    for (GalleryPageView page : pages) {
                        page.offsetTopAndBottom(-translate);
                    }
                    // Break if no previous
                    if (mIndex <= 0) {
                        break;
                    }
                    // Refill pages
                    fillPages();
                    // Break if last page bottom fit gallery view bottom
                    if (bottomDist <= topDist) {
                        break;
                    }
                } else {
                    break;
                }
            }

            // Get first shown loaded image
            mFirstShownLoadedPageIndex = GalleryPageView.INVALID_INDEX;
            for (GalleryPageView page : mPages) {
                // Check first shown loaded page
                if (mScrollUp && !page.isLoaded()) {
                    continue;
                }

                Rect bound = page.bounds();
                int pageTop = bound.top;
                int pageBottom = bound.bottom;
                if ((pageTop > 0 && pageTop < height) || (pageBottom > 0 && pageBottom < height)) {
                    mFirstShownLoadedPageIndex = page.getIndex();
                    break;
                }
            }
        }
    }

    @Override
    public void onDown() {
        mDeltaX = 0;
        mDeltaY = 0;
        mScrollUp = false;
        mStopAnimationFinger = cancelAllAnimations();
    }

    @Override
    public void onUp() {
        mGalleryView.getEdgeView().onRelease();
    }

    @Override
    public void onDoubleTapConfirmed(float x, float y) {

    }

    @Override
    public void onLongPress(float x, float y) {

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

    private void getBottomState() {
        List<GalleryPageView> pages = mPages;
        int bottom = mOffset;
        int i = 0;
        for (GalleryPageView page : pages) {
            if (i != 0) {
                bottom += mInterval;
            }
            bottom += page.getHeight();
            i++;
        }
        boolean hasNext = mIndex + pages.size() < mAdapter.size();

        mBottomStateBottom = bottom;
        mBottomStateHasNext = hasNext;
    }

    // True for get top or bottom
    private boolean scrollInternal(float dx, float dy, boolean fling, float x, float y) {
        if (mPages.size() == 0) {
            return false;
        }

        GalleryView galleryView = mGalleryView;
        int height = galleryView.getHeight();
        boolean requestFill = false;
        int remainY = (int) dy;
        boolean result = false;

        while (remainY != 0) {
            if (remainY < 0) { // Try to show top
                int limit;
                if (mIndex > 0) {
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
                    if (mIndex > 0) {
                        mOffset = limit;
                        remainY = remainY + limit - mOffset;
                        galleryView.forceFill();
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
                getBottomState();
                int bottom = mBottomStateBottom;
                boolean hasNext = mBottomStateHasNext;

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
                        galleryView.forceFill();
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
        mScrollUp = dy < 0;
        scrollInternal(dx, dy, false, x, y);
    }

    @Override
    public void onFling(float velocityX, float velocityY) {
        if (mPages.size() <= 0) {
            return;
        }

        mScrollUp = velocityY > 0;

        int maxY;
        if (mIndex > 0) {
            maxY = Integer.MAX_VALUE;
        } else {
            maxY = -mOffset;
        }

        getBottomState();
        int bottom = mBottomStateBottom;
        boolean hasNext = mBottomStateHasNext;
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
        AssertUtils.assertNotEquals("The PagerLayoutManager is not attached", mAdapter, null);

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
    public void bindUnloadedPage() {
        if (null == mAdapter || 0 <= mPages.size()) {
            return;
        }

        int index = mIndex;
        for (GalleryPageView page: mPages) {
            if (!page.getImageView().isLoaded()) {
                mAdapter.bind(page, index);
            }
            index++;
        }
    }

    @Override
    public void onPageLeft() {
        int size = mAdapter.size();
        if (size <= 0 || mPages.isEmpty()) {
            return;
        }

        GalleryView galleryView = mGalleryView;
        if (mIndex == 0 && mOffset >= 0) {
            GLEdgeView edgeView = galleryView.getEdgeView();
            edgeView.onPull(galleryView.getHeight(),
                    galleryView.getWidth() / 2, GLEdgeView.TOP);
            edgeView.onRelease(GLEdgeView.TOP);
        } else {
            // Cancel all animations
            cancelAllAnimations();
            mOffset += galleryView.getHeight() / 2;
            // Backup offset
            int offset = mOffset;
            // Reset parameters
            resetParameters();
            // Restore offset
            mOffset = offset;
            // Request fill
            mGalleryView.requestFill();
        }
    }

    @Override
    public void onPageRight() {
        int size = mAdapter.size();
        if (size <= 0 || mPages.isEmpty()) {
            return;
        }

        GalleryView galleryView = mGalleryView;
        getBottomState();
        int bottom = mBottomStateBottom;
        boolean hasNext = mBottomStateHasNext;
        if (!hasNext && bottom <= galleryView.getHeight()) {
            GLEdgeView edgeView = galleryView.getEdgeView();
            edgeView.onPull(galleryView.getHeight(),
                    galleryView.getWidth() / 2, GLEdgeView.BOTTOM);
            edgeView.onRelease(GLEdgeView.BOTTOM);
        } else {
            // Cancel all animations
            cancelAllAnimations();
            mOffset -= galleryView.getHeight() / 2;
            // Backup offset
            int offset = mOffset;
            // Reset parameters
            resetParameters();
            // Restore offset
            mOffset = offset;
            // Request fill
            mGalleryView.requestFill();
        }
    }

    @Override
    public boolean isTapOrPressEnable() {
        return !mStopAnimationFinger;
    }

    @Override
    public GalleryPageView findPageByIndex(int index) {
        for (GalleryPageView page : mPages) {
            if (page.getIndex() == index) {
                return page;
            }
        }
        return null;
    }

    @Override
    public int getCurrentIndex() {
        int height = mGalleryView.getHeight();
        for (GalleryPageView page : mPages) {
            Rect bound = page.bounds();
            int pageTop = bound.top;
            int pageBottom = bound.bottom;
            if ((pageTop > 0 && pageTop < height) || (pageBottom > 0 && pageBottom < height)) {
                return page.getIndex();
            }
        }
        return GalleryPageView.INVALID_INDEX;
    }

    @Override
    public void setCurrentIndex(int index) {
        int size = mAdapter.size();
        if (size <= 0) {
            // Can't get size now, assume size is MAX
            size = Integer.MAX_VALUE;
        }
        if (index == mIndex || index < 0 || index >= size) {
            return;
        }
        if (mPages.isEmpty()) {
            mIndex = index;
        } else {
            // Fix the index page
            GalleryPageView targetPage = null;
            for (GalleryPageView page : mPages) {
                if (page.getIndex() == index) {
                    targetPage =page;
                    break;
                }
            }

            if (targetPage != null) {
                // Cancel all animations
                cancelAllAnimations();
                mOffset -= targetPage.bounds().top;
                // Backup offset
                int offset = mOffset;
                // Reset parameters
                resetParameters();
                // Restore offset
                mOffset = offset;
                // Request fill
                mGalleryView.requestFill();
            } else {
                mIndex = index;
                // Cancel all animations
                cancelAllAnimations();
                // Remove all view
                removeProgress();
                removeErrorView();
                removeAllPages();
                // Reset parameters
                resetParameters();
                // Request fill
                mGalleryView.requestFill();
            }
        }
    }

    @Override
    public int getIndexUnder(float x, float y) {
        if (mPages.isEmpty()) {
            return GalleryPageView.INVALID_INDEX;
        } else {
            int intX = (int) x;
            int intY = (int) y;
            for (GalleryPageView page : mPages) {
                if (page.bounds().contains(intX, intY)) {
                    return page.getIndex();
                }
            }
            return GalleryPageView.INVALID_INDEX;
        }
    }

    @Override
    int getInternalCurrentIndex() {
        int currentIndex = getCurrentIndex();
        if (currentIndex == GalleryPageView.INVALID_INDEX) {
            currentIndex = mIndex;
        }
        return currentIndex;
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
            int index = mIndex;

            boolean topEdge = index <= 0 && mOffset >= 0;

            getBottomState();
            int bottom = mBottomStateBottom;
            boolean hasNext = mBottomStateHasNext;
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
