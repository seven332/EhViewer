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

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import com.hippo.ehviewer.R;
import com.hippo.gl.annotation.RenderThread;
import com.hippo.gl.glrenderer.GLCanvas;
import com.hippo.gl.util.GalleryUtils;
import com.hippo.gl.view.AnimationTime;
import com.hippo.gl.view.GLRoot;
import com.hippo.gl.view.GLView;
import com.hippo.gl.widget.GLEdgeView;
import com.hippo.gl.widget.GLProgressView;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ResourcesUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Stack;

public class GalleryView extends GLView implements GestureRecognizer.Listener {

    @IntDef({PAGE_STATE_UNKNOWN_SEEN, PAGE_STATE_SEEN, PAGE_STATE_UNSEEN})
    @Retention(RetentionPolicy.SOURCE)
    private @interface PageState {}

    @IntDef({SCALE_ORIGIN, SCALE_FIT_WIDTH, SCALE_FIT_HEIGHT, SCALE_FIT, SCALE_FIXED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Scale {}

    @IntDef({START_POSITION_TOP_LEFT, START_POSITION_TOP_RIGHT, START_POSITION_BOTTOM_LEFT,
            START_POSITION_BOTTOM_RIGHT, START_POSITION_CENTER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StartPosition {}

    @IntDef({LAYOUT_MODE_LEFT_TO_RIGHT, LAYOUT_MODE_RIGHT_TO_LEFT, LAYOUT_MODE_TOP_TO_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LayoutMode {}

    private static final int NO_POSITION = -1;
    private static final int MAX_SETTLE_DURATION = 600; // ms
    private static final int BACKGROUND_COLOR = 0xff212121;
    private static final int INTERVAL_IN_DP = 24;
    private static final int PROGRESS_SIZE_IN_DP = 48;

    private static final int PAGE_STATE_UNKNOWN_SEEN = 0;
    private static final int PAGE_STATE_SEEN = 1;
    private static final int PAGE_STATE_UNSEEN = 2;

    public static final int SCALE_ORIGIN = 0;
    public static final int SCALE_FIT_WIDTH = 1;
    public static final int SCALE_FIT_HEIGHT = 2;
    public static final int SCALE_FIT = 3;
    public static final int SCALE_FIXED = 4;

    public static final int START_POSITION_TOP_LEFT = 0;
    public static final int START_POSITION_TOP_RIGHT = 1;
    public static final int START_POSITION_BOTTOM_LEFT = 2;
    public static final int START_POSITION_BOTTOM_RIGHT = 3;
    public static final int START_POSITION_CENTER = 4;

    public static final int LAYOUT_MODE_LEFT_TO_RIGHT = 0;
    public static final int LAYOUT_MODE_RIGHT_TO_LEFT = 1;
    public static final int LAYOUT_MODE_TOP_TO_BOTTOM = 2;

    private static final Interpolator SMOOTH_SCALER_INTERPOLATOR = new OvershootInterpolator(0.7f);

    private static final float[] LEFT_AREA = {0.0f, 0.0f, 1.0f / 3.0f, 1f};
    private static final float[] RIGHT_AREA = {2.0f / 3.0f, 0.0f, 1.0f, 1f};
    private static final float[] CENTER_AREA = {1.0f / 3.0f, 2.0f / 5.0f, 2.0f / 3.0f, 3.0f / 5.0f};

    private Context mContext;
    private GestureRecognizer mGestureRecognizer;

    private LayoutManager mPagerLayoutManager;
    private LayoutManager mLayoutManager;

    private Recycler mRecycler;
    private GLEdgeView mEdgeView;
    private GLProgressView mProgressCache;

    private int mProgressColor;

    private boolean mEnableRequestLayout = true;

    private boolean mScale = false;
    private boolean mScroll = false;
    private boolean mFirstScroll = false;
    private boolean mTouched = false;

    private Rect mLeftArea = new Rect();
    private Rect mRightArea = new Rect();
    private Rect mCenterArea = new Rect();

    @LayoutMode
    private int mLayoutMode = LAYOUT_MODE_RIGHT_TO_LEFT;
    @LayoutMode
    private int mLastLayoutMode = mLayoutMode;

    @Scale
    private int mScaleMode = SCALE_FIT;
    @StartPosition
    private int mStartPosition = START_POSITION_TOP_LEFT;

    private boolean mRequestFill = false;

    private ActionListener mListener;

    public GalleryView(Context context, PageIterator iterator, ActionListener listener) {
        mContext = context;
        mListener = listener;
        mEdgeView = new GLEdgeView(context);
        mGestureRecognizer = new GestureRecognizer(context, this);
        mRecycler = new Recycler();

        mProgressColor = ResourcesUtils.getAttrColor(context, R.attr.colorPrimary);

        int interval = LayoutUtils.dp2pix(context, INTERVAL_IN_DP);
        int progressSize = LayoutUtils.dp2pix(context, PROGRESS_SIZE_IN_DP);
        mPagerLayoutManager = new PagerLayoutManager(context, this, interval, progressSize);
        mPagerLayoutManager.onAttach(iterator);
        mLayoutManager = mPagerLayoutManager;

        setBackgroundColor(BACKGROUND_COLOR);
    }

    @Override
    public void onAttachToRoot(GLRoot root) {
        super.onAttachToRoot(root);

        mEdgeView.onAttachToRoot(root);
    }

    @Override
    public void onDetachFromRoot() {
        super.onDetachFromRoot();

        mEdgeView.onDetachFromRoot();
    }

    /*
    public void setMode(Mode mode) {
        if (mMode != mode) {
            Mode oldMode = mMode;
            mMode = mode;
            resetPageMoving();
            superRequestLayout();

            mListener.onSetMode(mode);
            if (oldMode == Mode.NONE) {
                mFirstShownIndex = MathUtils.clamp(mFirstShownIndex, 0, mAdapter.getPages() - 1);
                mListener.onScrollToPage(mFirstShownIndex, true);
            }
        }
    }
    */

    public void superRequestLayout() {
        super.requestLayout();
    }

    public void requestFill() {
        mRequestFill = true;
        invalidate();
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
        return true;
    }


    GLEdgeView getEdgeView() {
        return mEdgeView;
    }

    public boolean isFirstScroll() {
        boolean firstScroll = mFirstScroll;
        mFirstScroll = false;
        return firstScroll;
    }

    @Override
    public boolean onScroll(float dx, float dy, float totalX, float totalY, float x, float y) {
        if (mScale) {
            return false;
        }
        mScroll = true;

        mLayoutManager.onScroll(dx, dy, totalX, totalY, x, y);

        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        mLayoutManager.onFling(e1, e2, velocityX, velocityY);
        return true;
    }

    @Override
    public boolean onScaleBegin(float focusX, float focusY) {
        return onScale(focusX, focusY, 1.0f);
    }

    @Override
    public boolean onScale(float focusX, float focusY, float scale) {
        if (mScroll || !mLayoutManager.canScale()) {
            return false;
        }
        mScale = true;

        mLayoutManager.onScale(focusX, focusY, scale);

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
        mFirstScroll = true;
        mLayoutManager.onDown();
    }

    @Override
    public void onUp() {
        mTouched = false;
        mLayoutManager.onUp();
    }

    @Override
    public void onPointerDown(float x, float y) {
        if (!mScroll && mLayoutManager.canScale()) {
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

        mEdgeView.layout(left, top, right, bottom);

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

    @RenderThread
    private void fill() {
        // Must be in render thread
        GalleryUtils.assertInRenderThread();

        // Disable request layout
        mEnableRequestLayout = false;
        mLayoutManager.onFill();
        mEnableRequestLayout = true;
    }

    @Override
    public void render(GLCanvas canvas) {
        long time = AnimationTime.get();
        boolean invalidate = mLayoutManager.onUpdateAnimation(time);
        if (invalidate) {
            invalidate();
        }

        // mSmoothScroller may do fill too, so check fill request after scroll animator
        if (mRequestFill) {
            mRequestFill = false;
            fill();
        }

        super.render(canvas);
        mEdgeView.render(canvas);
    }

    public GalleryPageView obtainPage() {
        GalleryPageView page = mRecycler.obtain();
        if (page == null) {
            page = new GalleryPageView(mContext);
        }
        return page;
    }

    public void releasePage(GalleryPageView page) {
        mRecycler.release(page);
    }

    /**
     * Indeterminate GLProgressView
     */
    public GLProgressView obtainProgress() {
        GLProgressView progress;
        if (mProgressCache != null) {
            progress = mProgressCache;
            mProgressCache = null;
        } else {
            progress = new GLProgressView();
            progress.setColor(mProgressColor);
            progress.setBgColor(BACKGROUND_COLOR);
            progress.setIndeterminate(true);
        }
        return progress;
    }

    /**
     * @param progress Indeterminate GLProgressView
     */
    public void releaseProgress(GLProgressView progress) {
        mProgressCache = progress;
    }

    public interface PageIterator {

        void mark();

        void reset();

        boolean hasNext();

        boolean hasPrevious();

        boolean isValid();

        void next();

        void previous();

        void bind(GalleryPageView view);

        void unbind(GalleryPageView view);
    }

    public static abstract class LayoutManager {

        protected GalleryView mGalleryView;

        public LayoutManager(@NonNull GalleryView galleryView) {
            mGalleryView = galleryView;
        }

        public abstract void onAttach(PageIterator iterator);

        public abstract PageIterator onDetach();

        public abstract void onFill();

        public abstract void onDown();

        public abstract void onUp();

        public abstract void onScroll(float dx, float dy, float totalX, float totalY, float x, float y);

        public abstract void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);

        public abstract boolean canScale();

        public abstract void onScale(float focusX, float focusY, float scale);

        public abstract boolean onUpdateAnimation(long time);
    }

    /*
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
    */

    private static class Recycler {

        private static final int MAX_SIZE = 3;

        private int mSize = 0;

        private Stack<GalleryPageView> mStack = new Stack<>();

        @Nullable
        private GalleryPageView obtain() {
            if (mSize != 0) {
                mSize--;
                return mStack.pop();
            } else {
                return null;
            }
        }

        public void release(@Nullable GalleryPageView page) {
            if (page == null) {
                return;
            }

            if (mSize < MAX_SIZE) {
                mSize++;
                mStack.push(page);
            }
        }
    }

    public interface ActionListener {

        void onTapCenter();

        void onScrollToPage(int page, boolean internal);
    }
}
