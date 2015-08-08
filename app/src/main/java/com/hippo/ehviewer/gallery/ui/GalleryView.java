package com.hippo.ehviewer.gallery.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import com.hippo.ehviewer.gallery.anim.Animation;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.util.GalleryUtils;
import com.hippo.yorozuya.IntArray;
import com.hippo.yorozuya.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class GalleryView extends GLView implements GestureRecognizer.Listener {

    private static final String TAG = GalleryView.class.getSimpleName();

    private static final int NO_POSITION = -1;

    private static final int MAX_SETTLE_DURATION = 600; // ms

    public static final int BACKGROUND_COLOR = 0xff212121;

    private static final int UNKNOWN_SEEN = 0;
    private static final int SEEN = 1;
    private static final int UNSEEN = 2;

    private static final Interpolator SMOOTH_SCROLLER_INTERPOLATOR = new Interpolator() {

        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    private static final float[] LEFT_AREA = {0.0f, 0.0f, 1.0f / 3.0f, 1f};
    private static final float[] RIGHT_AREA = {2.0f / 3.0f, 0.0f, 1.0f, 1f};
    private static final float[] CENTER_AREA = {1.0f / 3.0f, 2.0f / 5.0f, 2.0f / 3.0f, 3.0f / 5.0f};

    private static final Interpolator SMOOTH_SCALER_INTERPOLATOR = new OvershootInterpolator();

    private boolean mEnableRequestLayout = true;

    private GestureRecognizer mGestureRecognizer;

    private Adapter mAdapter;

    private int mFirstShownIndex = 0;
    private int mLayoutOffset = 0;

    private SparseArray<GalleryPageView> mAttachedPage = new SparseArray<>();
    private List<GLView> mInvalidAttachedComponent = new ArrayList<>();
    private IntArray mMissedPageInfo = new IntArray(5 * 3);
    private List<GalleryPageView> mUsedPage = new ArrayList<>();
    private SparseArray<GalleryPageView> mPageMap = new SparseArray<>();

    private EdgeView mEdgeView;

    private boolean mScale = false;
    private boolean mScroll = false;
    private boolean mTouched = false;

    private Rect mLeftArea = new Rect();
    private Rect mRightArea = new Rect();
    private Rect mCenterArea = new Rect();

    private int mDeltaX;
    private int[] mScrollRemain = new int[2];

    private boolean mHaveChangePage = false;

    private Mode mMode = Mode.NONE;
    private Mode mLastLayoutMode = Mode.NONE;

    private Scale mScaleMode = Scale.FIT; // TODO
    private StartPosition mStartPosition = StartPosition.TOP_RIGHT; // TODO
    private float mLastScale = 1f;

    private int mProgressSpec = MeasureSpec.makeMeasureSpec(48, MeasureSpec.EXACTLY);
    public int mInterval = 24;

    private SmoothScroller mSmoothScroller;
    private SmoothScaler mSmoothScaler;
    private Recycler mRecycler;

    private boolean mRequestFill = false;

    private ActionListener mListener;

    public enum Mode {
        NONE, // Just a progress view
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        TOP_TO_BOTTOM
    }

    public enum Scale {
        ORIGIN,
        FIT_WIDTH,
        FIT_HEIGHT,
        FIT,
        FIXED
    }

    public enum StartPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    public GalleryView(Context context, int firstShownIndex, ActionListener listener) {
        mFirstShownIndex = firstShownIndex;
        mListener = listener;
        mEdgeView = new EdgeView(context);
        mGestureRecognizer = new GestureRecognizer(context, this);
        mSmoothScroller = new SmoothScroller();
        mSmoothScroller.setInterpolator(SMOOTH_SCROLLER_INTERPOLATOR);
        mSmoothScaler = new SmoothScaler();
        mSmoothScaler.setInterpolator(SMOOTH_SCALER_INTERPOLATOR);
        mRecycler = new Recycler();
    }

    @Override
    protected void onAttachToRoot(GLRoot root) {
        super.onAttachToRoot(root);

        mEdgeView.onAttachToRoot(root);
    }

    @Override
    protected void onDetachFromRoot() {
        super.onDetachFromRoot();

        mEdgeView.onDetachFromRoot();
    }

    public void setMode(Mode mode) {
        if (mMode != mode) {
            Mode oldMode = mMode;
            mMode = mode;
            superRequestLayout();

            mListener.onSetMode(mode);
            if (oldMode == Mode.NONE) {
                mFirstShownIndex = MathUtils.clamp(mFirstShownIndex, 0, mAdapter.getPages() - 1);
                mListener.onScrollToPage(mFirstShownIndex, true);
            }
        }
    }

    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;
    }

    public void setProgressSize(int size) {
        mProgressSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        superRequestLayout();
    }

    public void setInterval(int interval) {
        mInterval = interval;
        superRequestLayout();
    }

    public int getPages() {
        return mAdapter.getPages();
    }

    public void superRequestLayout() {
        super.requestLayout();
    }

    private void requestFill() {
        if (GalleryUtils.isRenderThread()) {
            fill();
        } else {
            mRequestFill = true;
            invalidate();
        }
    }

    public void scrollToPage(int page) {
        scrollToPageInternal(page, false);
    }

    private void scrollToPageInternal(int page, boolean internal) {
        mSmoothScroller.forceStop();
        mSmoothScaler.forceStop();

        if (page < 0 || page >= mAdapter.getPages()) {
            return;
        }

        mFirstShownIndex = page;
        mLayoutOffset = 0;
        requestFill();

        mListener.onScrollToPage(page, internal);
    }

    public void smoothScrollToPage(int page) {
        mSmoothScroller.forceStop();
        mSmoothScaler.forceStop();

        // TODO
    }

    private void scrollX(int offset) {
        if (offset == 0 || (mMode != Mode.LEFT_TO_RIGHT && mMode != Mode.RIGHT_TO_LEFT)) {
            return;
        }

        mLayoutOffset += offset;
        boolean reverse = mMode == Mode.RIGHT_TO_LEFT;

        // Fix TODO What if offset is very large
        int oldFirstShownIndex = mFirstShownIndex;
        int width = getWidth();
        if (!mHaveChangePage) {
            if (mLayoutOffset > mInterval) {
                mHaveChangePage = true;
                mFirstShownIndex += reverse ? 1 : -1;
                mLayoutOffset = -width + mLayoutOffset - mInterval;
            } else if (mLayoutOffset < -mInterval) {
                mHaveChangePage = true;
                mFirstShownIndex += reverse ? -1 : 1;
                mLayoutOffset = width + mLayoutOffset + mInterval;
            }
        } else {
            if (mLayoutOffset > width) {
                mHaveChangePage = false;
                mFirstShownIndex += reverse ? 1 : -1;
                mLayoutOffset = mLayoutOffset - width - mInterval;
            } else if (mLayoutOffset < -width) {
                mHaveChangePage = false;
                mFirstShownIndex += reverse ? -1 : 1;
                mLayoutOffset = mLayoutOffset + width + mInterval;
            } else if (mLayoutOffset < mInterval && mLayoutOffset > -mInterval) {
                mHaveChangePage = false;
            }
        }

        if (mFirstShownIndex != oldFirstShownIndex) {
            mListener.onScrollToPage(mFirstShownIndex, true);
        }

        requestFill();
    }

    @Override
    protected boolean dispatchTouchEvent(MotionEvent event) {
        // Do not pass event to component, so handle event here
        mGestureRecognizer.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onSingleTapUp(float x, float y) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(float x, float y) {
        if ((mMode == Mode.LEFT_TO_RIGHT || mMode == Mode.RIGHT_TO_LEFT) && mLayoutOffset == 0) {
            if (mLeftArea.contains((int) x, (int) y)) {
                if (mMode == Mode.LEFT_TO_RIGHT) {
                    if (mFirstShownIndex > 0) {
                        scrollToPageInternal(mFirstShownIndex - 1, true);
                    } else {
                        mEdgeView.onPull(getWidth() * 2, (int) y, EdgeView.LEFT);
                    }
                } else {
                    if (mFirstShownIndex < mAdapter.getPages() - 1) {
                        scrollToPageInternal(mFirstShownIndex + 1, true);
                    } else {
                        mEdgeView.onPull(getWidth() * 2, (int) y, EdgeView.LEFT);
                    }
                }
            } else if (mRightArea.contains((int) x, (int) y)) {
                if (mMode == Mode.LEFT_TO_RIGHT) {
                    if (mFirstShownIndex < mAdapter.getPages() - 1) {
                        scrollToPageInternal(mFirstShownIndex + 1, true);
                    } else {
                        mEdgeView.onPull(getWidth() * 2, (int) y, EdgeView.RIGHT);
                    }
                } else {
                    if (mFirstShownIndex > 0) {
                        scrollToPageInternal(mFirstShownIndex - 1, true);
                    } else {
                        mEdgeView.onPull(getWidth() * 2, (int) y, EdgeView.RIGHT);
                    }
                }
            } else if (mCenterArea.contains((int) x, (int) y)) {
                mListener.onTapCenter();
            }
        }

        return true;
    }

    @Override
    public boolean onDoubleTap(float x, float y) {
        return true;
    }

    @Override
    public boolean onDoubleTapConfirmed(float x, float y) {
        if (mScale) {
            return false;
        }

        if ((mMode == Mode.LEFT_TO_RIGHT || mMode == Mode.RIGHT_TO_LEFT) &&
                mLayoutOffset == 0) {
            GalleryPageView page = mPageMap.get(mFirstShownIndex);
            if (page != null && page.isLoaded()) {
                float scale = page.getScale();
                float fitScale = page.getFitScale();
                float centerScale = (1f + fitScale) / 2;
                float targetScale;
                if ((fitScale > 1f && scale > centerScale) ||
                        (fitScale < 1f && scale < centerScale)) {
                    targetScale = 1f;
                } else {
                    targetScale = fitScale;
                }
                mSmoothScaler.startSmoothScaler(page, x, y, scale, targetScale, 500); // TODO
            }
        }

        return true;
    }

    // Try to scroll to make mLayoutOffset == 0
    private int onScrollX1(int dx) {
        int remain;
        int actualDx;
        if (mLayoutOffset > 0 && dx > 0) {
            remain = dx - mLayoutOffset;
            if (remain > 0) {
                // Get remain
                actualDx = dx - remain;
            } else {
                // No remain
                remain = 0;
                actualDx = dx;
            }
        } else if (mLayoutOffset < 0 && dx < 0) {
            remain = dx - mLayoutOffset;
            if (remain < 0) {
                // Get remain
                actualDx = dx - remain;
            } else {
                // No remain
                remain = 0;
                actualDx = dx;
            }
        } else {
            remain = 0;
            actualDx = dx;
        }

        onScrollX2(actualDx);

        return remain;
    }

    private int onScrollX2(int dx) {
        int remain = 0;
        if (mMode == Mode.LEFT_TO_RIGHT) {
            if (mFirstShownIndex == 0 && mLayoutOffset - dx > 0) { // Start
                scrollX(-mLayoutOffset);
                remain = dx - mLayoutOffset;
            } else if (mFirstShownIndex == mAdapter.getPages() - 1 && mLayoutOffset - dx < 0) { // end
                scrollX(-mLayoutOffset);
                remain = dx - mLayoutOffset;
            } else {
                scrollX(-dx);
            }
        } else if (mMode == Mode.RIGHT_TO_LEFT) {
            if (mFirstShownIndex == 0 && mLayoutOffset - dx < 0) { // Start
                scrollX(-mLayoutOffset);
                remain = dx - mLayoutOffset;
            } else if (mFirstShownIndex == mAdapter.getPages() - 1 && mLayoutOffset - dx > 0) { // end
                scrollX(-mLayoutOffset);
                remain = dx - mLayoutOffset;
            } else {
                scrollX(-dx);
            }
        }
        return remain;
    }

    @Override
    public boolean onScroll(float dx, float dy, float totalX, float totalY, float x, float y) {
        if (mScale) {
            return false;
        }
        mScroll = true;

        int dxI = (int) dx;
        int dyI = (int) dy;
        if (mMode == Mode.LEFT_TO_RIGHT || mMode == Mode.RIGHT_TO_LEFT) {
            int remain = dxI;
            if (remain != 0 && mLayoutOffset != 0) {
                remain = onScrollX1(dxI);
            }
            if (remain != 0 && mLayoutOffset == 0) {
                GalleryPageView page = mPageMap.get(mFirstShownIndex);
                if (page != null) {
                    page.scroll(remain, dyI, mScrollRemain);
                    remain = mScrollRemain[0];
                }
            }
            if (remain != 0) {
                remain = onScrollX2(dxI);
            }

            if (remain == 0) {
                if (mDeltaX != 0) {
                    mDeltaX = 0;
                    mEdgeView.onRelease();
                }
            } else if (remain < 0) {
                if (mDeltaX > 0) {
                    mDeltaX = 0;
                    mEdgeView.onRelease();
                }
                mDeltaX += remain;
                mEdgeView.onPull(-mDeltaX, y, EdgeView.LEFT);
            } else {
                if (mDeltaX < 0) {
                    mDeltaX = 0;
                    mEdgeView.onRelease();
                }
                mDeltaX += remain;
                mEdgeView.onPull(mDeltaX, y, EdgeView.RIGHT);
            }

        } else if (mMode == Mode.TOP_TO_BOTTOM) {
            // TODO
        }

        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // TODO



        return false;
    }

    @Override
    public boolean onScaleBegin(float focusX, float focusY) {
        return onScale(focusX, focusY, 1.0f);
    }

    private boolean canScale() {
        if (mMode != Mode.LEFT_TO_RIGHT && mMode != Mode.RIGHT_TO_LEFT) {
            return false;
        }
        if (mLayoutOffset != 0) {
            return false;
        }
        if (mScroll) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onScale(float focusX, float focusY, float scale) {
        if (!canScale()) {
            return false;
        }
        mScale = true;

        GalleryPageView page = mPageMap.get(mFirstShownIndex);
        if (page != null && page.isLoaded()) {
            page.scale(focusX, focusY, scale);
            mLastScale = page.getScale();
        }

        return true;
    }

    @Override
    public void onScaleEnd() {

    }

    @Override
    public void onDown(float x, float y) {
        mTouched = true;
        mScale = false;
        mScroll = false;
        mSmoothScroller.forceStop();
        mSmoothScaler.forceStop();
    }

    @Override
    public void onUp() {
        mTouched = false;
        mDeltaX = 0;
        mEdgeView.onRelease();

        if ((mMode == Mode.LEFT_TO_RIGHT || mMode == Mode.RIGHT_TO_LEFT)) {
            if (mLayoutOffset != 0) {
                // Scroll
                final float pageDelta = 7 * (float) Math.abs(mLayoutOffset) / (getWidth() + mInterval);
                int duration = (int) ((pageDelta + 1) * 100);
                duration = Math.min(duration, MAX_SETTLE_DURATION);
                mSmoothScroller.startSmoothScroll(-mLayoutOffset, 0, duration);
            } else {
                // Update AnimateState
                GalleryPageView page = mPageMap.get(mFirstShownIndex);
                if (page != null) {
                    page.setAnimateState(ScaledImageView.ANIMATE_STATE_RUN);
                }
            }





        } else if (mMode == Mode.TOP_TO_BOTTOM) {
            // TODO
        }




    }

    @Override
    public void onPointerDown(float x, float y) {
        if (canScale()) {
            mScale = true;
        }
    }

    @Override
    public void onPointerUp() {

    }

    @Override
    public void requestLayout() {
        if (mEnableRequestLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
        mSmoothScroller.forceStop();
        mSmoothScaler.forceStop();
        mEdgeView.layout(left, top, right, bottom);

        /* TODO set all seen UNKNOWN_SEEN in order to setScaleAndOffset
        if (mLastLayoutMode != mMode) {
            mLastLayoutMode = mMode;
            for () {

            }
        }
        */


        mLayoutOffset = 0;
        mHaveChangePage = false;
        fill();

        if (changeSize) {
            int width = right - left;
            int height = bottom - top;
            mLeftArea.set((int) (LEFT_AREA[0] * width), (int) (LEFT_AREA[1] * height),
                    (int) (LEFT_AREA[2] * width), (int) (LEFT_AREA[3] * height));
            mRightArea.set((int) (RIGHT_AREA[0] * width), (int) (RIGHT_AREA[1] * height),
                    (int) (RIGHT_AREA[2] * width), (int) (RIGHT_AREA[3] * height));
            mCenterArea.set((int) (CENTER_AREA[0] * width), (int) (CENTER_AREA[1] * height),
                    (int) (CENTER_AREA[2] * width), (int) (CENTER_AREA[3] * height));
        }
    }

    public GalleryPageView getPage(int index) {
        return mPageMap.get(index);
    }

    private void unbindGalleryPage(GalleryPageView view) {
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        if (lp.index != NO_POSITION) {
            mAdapter.unbindPage(view, lp.index);
        }
    }

    private void attachePage(GalleryPageView page, int index) {
        mPageMap.put(index, page);
    }

    // Set lp.index NO_POSITION
    private void detachPage(GalleryPageView page) {
        LayoutParams lp = (LayoutParams) page.getLayoutParams();
        int index = lp.index;
        lp.index = NO_POSITION;
        if (index != NO_POSITION) {
            mPageMap.remove(index);
        }
    }


    private void fillX(int start, int end, int startIndex, boolean reverse) {
        for (int i = 0, n = getComponentCount(); i < n; i++) {
            GLView component = getComponent(i);
            if (component instanceof GalleryPageView) {
                mAttachedPage.put(((LayoutParams) component.getLayoutParams()).index,
                        (GalleryPageView) component);
            } else {
                mInvalidAttachedComponent.add(component);
            }
        }

        // Remove invalid view
        int n = mInvalidAttachedComponent.size();
        while (n-- > 0) {
            removeComponentAt(n);
        }
        mInvalidAttachedComponent.clear();

        // Fill
        int width = getWidth();
        int height = getHeight();
        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        int offset = width + mInterval;
        if (reverse) {
            offset = -offset;
        }
        int pages = mAdapter.getPages();
        for (int spec = start, i = startIndex; reverse ? spec > end : spec < end && i < pages; i++) {
            GalleryPageView page = mAttachedPage.get(i);
            if (page != null) {
                mAttachedPage.remove(i);
                page.measure(widthSpec, heightSpec);
                if (reverse) {
                    page.layout(spec - width, 0, spec, height);
                } else {
                    page.layout(spec, 0, spec + width, height);
                }
            } else {
                // Add index, left, top, right, bottom
                mMissedPageInfo.add(i);
                if (reverse) {
                    mMissedPageInfo.add(spec - width);
                    mMissedPageInfo.add(0);
                    mMissedPageInfo.add(spec);
                    mMissedPageInfo.add(height);
                } else {
                    mMissedPageInfo.add(spec);
                    mMissedPageInfo.add(0);
                    mMissedPageInfo.add(spec + width);
                    mMissedPageInfo.add(height);
                }
            }
            spec += offset;
        }

        // Fill missed page
        int count = mAttachedPage.size();
        n = mMissedPageInfo.size();
        for (int i = 0; i < n; i += 5) {
            int index = mMissedPageInfo.get(i);
            GalleryPageView page;
            if (i < count) {
                page = mAttachedPage.valueAt(i);
                // Unbind
                unbindGalleryPage(page);
                detachPage(page);
                mUsedPage.add(page);
            } else {
                page = mRecycler.obtain();
                addComponent(page);
            }
            // Bind
            ((LayoutParams) page.getLayoutParams()).index = index;
            ((LayoutParams) page.getLayoutParams()).seen = UNKNOWN_SEEN;
            attachePage(page, index);
            // Update index in LayoutParams
            mAdapter.bindPage(page, index);
            page.measure(widthSpec, heightSpec);
            page.layout(mMissedPageInfo.get(i + 1), mMissedPageInfo.get(i + 2),
                    mMissedPageInfo.get(i + 3), mMissedPageInfo.get(i + 4));

        }
        mMissedPageInfo.clear();

        // Clear unuse page
        for (int i = 0; i < count; i++) {
            GalleryPageView page = mAttachedPage.valueAt(i);
            if (mUsedPage.contains(page)) {
                continue;
            }
            unbindGalleryPage(page);
            detachPage(page);
            removeComponent(page);
            mRecycler.release(page);
        }
        mAttachedPage.clear();
        mUsedPage.clear();

        for (int i = 0; i < getComponentCount(); i++) {
            // Update seen
            GalleryPageView page = (GalleryPageView) getComponent(i);
            LayoutParams lp = ((LayoutParams) page.getLayoutParams());
            int oldSeen = lp.seen;
            int seen = page.bounds().intersects(0, 0, width, height) ? SEEN : UNSEEN;
            lp.seen = seen;
            if (oldSeen == UNKNOWN_SEEN || (oldSeen == SEEN && seen == UNSEEN)) {
                page.setScaleOffset(mScaleMode, mStartPosition, mLastScale);
            }

            // Update AnimateState
            if (seen == UNSEEN) {
                page.setAnimateState(ScaledImageView.ANIMATE_STATE_STOP);
            } else if (!mTouched && mLayoutOffset == 0 && mFirstShownIndex == lp.index) {
                page.setAnimateState(ScaledImageView.ANIMATE_STATE_RUN);
            } else {
                page.setAnimateState(ScaledImageView.ANIMATE_STATE_DEFALUE);
            }
        }
    }

    private void fillY(int start, int end, int startIndex, boolean reverse) {

    }

    private void fill() {
        mRequestFill = false;
        // Must be in render thread
        GalleryUtils.assertInRenderThread();

        // Disable request layout
        mEnableRequestLayout = false;

        if (mMode == Mode.NONE) {
            // Try to get old ProgressView
            ProgressView progressView = null;
            if (getComponentCount() == 1) {
                GLView component = getComponent(0);
                if (component instanceof ProgressView) {
                    progressView = (ProgressView) component;
                }
            }
            if (progressView == null) {
                progressView = new ProgressView();
            }

            // Remove all component and unbind
            int i = getComponentCount();
            while (i-- > 0) {
                GLView component = getComponent(i);
                removeComponentAt(i);

                // Remove from map, unbind gallery page, add to recycler
                if (component instanceof GalleryPageView) {
                    GalleryPageView page = (GalleryPageView) component;
                    unbindGalleryPage(page);
                    detachPage(page);
                    mRecycler.release(page);
                }
            }

            addComponent(progressView);

            // Measure and layout progress
            progressView.measure(mProgressSpec, mProgressSpec);
            int progressWidth = progressView.getMeasuredWidth();
            int progressHeight = progressView.getMeasuredHeight();
            int progressLeft = getWidth() / 2 - progressWidth / 2;
            int progressTop = getHeight() / 2 - progressHeight / 2;
            progressView.layout(progressLeft, progressTop, progressLeft + progressWidth, progressTop + progressHeight);

            // Bind progress view, just setIndeterminate(true);
            progressView.setIndeterminate(true);
        } else if (mMode == Mode.LEFT_TO_RIGHT || mMode == Mode.RIGHT_TO_LEFT) {
            // TODO better to avoid make load image when stop
            int width = getWidth();
            int start;
            int end;
            int startIndex;
            boolean reverse = mMode == Mode.RIGHT_TO_LEFT;

            if (reverse) {
                if (mFirstShownIndex == 0) {
                    start = width;
                    startIndex = 0;
                } else {
                    startIndex = mFirstShownIndex - 1;
                    start = width * 2 + mInterval;
                }
                if (mFirstShownIndex == mAdapter.getPages() - 1) {
                    end = 0;
                } else {
                    end = -width - mInterval * 2 - 1;
                }
            } else {
                if (mFirstShownIndex == 0) {
                    start = 0;
                    startIndex = 0;
                } else {
                    startIndex = mFirstShownIndex - 1;
                    start = -width - mInterval;
                }
                if (mFirstShownIndex == mAdapter.getPages() - 1) {
                    end = width;
                } else {
                    end = width * 2 + mInterval * 2 + 1;
                }
            }

            // Set offset
            start += mLayoutOffset;

            fillX(start, end, startIndex, reverse);
        }

        mEnableRequestLayout = true;
    }

    @Override
    protected void render(GLCanvas canvas) {
        long time = AnimationTime.get();
        boolean invalidate = mSmoothScroller.calculate(time);
        invalidate |= mSmoothScaler.calculate(time);
        if (invalidate) {
            invalidate();
        }

        // mSmoothScroller may do fill too, so check fill request after scroll animator
        if (mRequestFill) {
            fill();
        }

        super.render(canvas);
        mEdgeView.render(canvas);
    }

    @Override
    protected boolean checkLayoutParams(GLView.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected GLView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected GLView.LayoutParams generateLayoutParams(GLView.LayoutParams p) {
        return p == null ? generateDefaultLayoutParams() : new LayoutParams(p);
    }

    public static class LayoutParams extends GLView.LayoutParams {

        private int index = NO_POSITION;
        private int seen = UNKNOWN_SEEN;

        public LayoutParams(GLView.LayoutParams source) {
            super(source);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }

    public static abstract class Adapter {

        public abstract int getPages();

        public abstract GalleryPageView createPage();

        public abstract void bindPage(GalleryPageView view, int index);

        public abstract void unbindPage(GalleryPageView view, int index);
    }

    class SmoothScaler extends Animation {

        private GalleryPageView mPage;
        private float mFocusX;
        private float mFocusY;
        private float mStartScale;
        private float mEndScale;
        private float mLastScale;

        public void startSmoothScaler(GalleryPageView page, float focusX, float focusY,
                float startScale, float endScale, int duration) {
            mPage = page;
            mFocusX = focusX;
            mFocusY = focusY;
            mStartScale = startScale;
            mEndScale = endScale;
            mLastScale = startScale;
            setDuration(duration);
            start();
            invalidate();
        }

        @Override
        protected void onCalculate(float progress) {
            float scale = MathUtils.lerp(mStartScale, mEndScale, progress);
            mPage.scale(mFocusX, mFocusY, scale / mLastScale);
            mLastScale = scale;
            if (progress == 1.0f) {
                mPage = null;
            }
        }
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
            invalidate();
        }

        @Override
        protected void onCalculate(float progress) {
            int x = (int) (mDx * progress);
            int y = (int) (mDy * progress);
            scrollX(x - mLastX);
            mLastX = x;
        }
    }

    class Recycler {

        private int mSize = 0;

        private Stack<GalleryPageView> mStack = new Stack<>();

        private GalleryPageView obtain() {
            if (mSize != 0) {
                mSize--;
                return mStack.pop();
            } else {
                return mAdapter.createPage();
            }
        }

        public void release(GalleryPageView page) {
            if (page == null) {
                return;
            }

            if (mSize < 3) { // 3 is max size
                mSize++;
                mStack.push(page);
            }
        }
    }

    public interface ActionListener {

        void onTapCenter();

        void onSetMode(Mode mode);

        void onScrollToPage(int page, boolean internal);
    }
}
