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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import com.hippo.ehviewer.R;
import com.hippo.gl.annotation.RenderThread;
import com.hippo.gl.glrenderer.BasicTexture;
import com.hippo.gl.glrenderer.GLCanvas;
import com.hippo.gl.glrenderer.MovableTextTexture;
import com.hippo.gl.glrenderer.StringTexture;
import com.hippo.gl.glrenderer.Texture;
import com.hippo.gl.util.GalleryUtils;
import com.hippo.gl.view.AnimationTime;
import com.hippo.gl.view.GLRoot;
import com.hippo.gl.view.GLView;
import com.hippo.gl.widget.GLEdgeView;
import com.hippo.gl.widget.GLProgressView;
import com.hippo.gl.widget.GLTextureView;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ResourcesUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Stack;

public class GalleryView extends GLView implements GestureRecognizer.Listener {

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

    public static final int BACKGROUND_COLOR = 0xff212121;
    private static final int INTERVAL_IN_DP = 24;
    private static final int PROGRESS_SIZE_IN_DP = 48;
    private static final int PAGE_MIN_HEIGHT_IN_DP = 256;

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

    private static final float[] LEFT_AREA = {0.0f, 0.0f, 1.0f / 3.0f, 1f};
    private static final float[] RIGHT_AREA = {2.0f / 3.0f, 0.0f, 1.0f, 1f};
    private static final float[] CENTER_AREA = {1.0f / 3.0f, 2.0f / 5.0f, 2.0f / 3.0f, 3.0f / 5.0f};

    private Context mContext;
    private MovableTextTexture mPageTextTexture;
    private GestureRecognizer mGestureRecognizer;
    private PageIterator mIterator;

    private PagerLayoutManager mPagerLayoutManager;
    private ScrollLayoutManager mScrollLayoutManager;
    private LayoutManager mLayoutManager;

    private Recycler mRecycler;
    private GLEdgeView mEdgeView;
    private GLProgressView mProgressCache;
    private GLTextureView mErrorViewCache;

    private int mProgressColor;
    private int mProgressSize;
    private float mErrorSize;
    private int mErrorColor;
    private int mPageMinHeight;
    private int mInterval;

    private boolean mEnableRequestFill = true;

    private boolean mScale = false;
    private boolean mScroll = false;
    private boolean mFirstScroll = false;
    private boolean mTouched = false;

    private Rect mLeftArea = new Rect();
    private Rect mRightArea = new Rect();
    private Rect mCenterArea = new Rect();

    @LayoutMode
    private int mLayoutMode = LAYOUT_MODE_RIGHT_TO_LEFT;

    @Scale
    private int mScaleMode = SCALE_FIT;
    @StartPosition
    private int mStartPosition = START_POSITION_TOP_LEFT;

    private boolean mRequestFill = false;

    private ActionListener mListener;

    public GalleryView(Context context, PageIterator iterator, MovableTextTexture pageTextTexture,
            ActionListener listener, @LayoutMode int layoutMode) {
        mContext = context;
        mIterator = iterator;
        mPageTextTexture = pageTextTexture;
        mListener = listener;
        mEdgeView = new GLEdgeView(context);
        mGestureRecognizer = new GestureRecognizer(context, this);
        mRecycler = new Recycler();

        iterator.setGalleryView(this);

        mLayoutMode = layoutMode;
        mProgressColor = ResourcesUtils.getAttrColor(context, R.attr.colorPrimary);
        mProgressSize = LayoutUtils.dp2pix(context, PROGRESS_SIZE_IN_DP);
        mErrorSize = context.getResources().getDimension(R.dimen.text_large);
        mErrorColor = context.getResources().getColor(R.color.red_500);
        mPageMinHeight = LayoutUtils.dp2pix(context, PAGE_MIN_HEIGHT_IN_DP);
        mInterval = LayoutUtils.dp2pix(context, INTERVAL_IN_DP);

        setBackgroundColor(BACKGROUND_COLOR);
    }

    private void ensurePagerLayoutManager() {
        if (mPagerLayoutManager == null) {
            mPagerLayoutManager = new PagerLayoutManager(mContext, this);
        }
    }

    private void ensureScrollLayoutManager() {
        if (mScrollLayoutManager == null) {
            mScrollLayoutManager = new ScrollLayoutManager(mContext, this, mInterval);
        }
    }

    @Override
    public void onAttachToRoot(GLRoot root) {
        super.onAttachToRoot(root);
        mEdgeView.onAttachToRoot(root);

        switch (mLayoutMode) {
            case LAYOUT_MODE_LEFT_TO_RIGHT:
                ensurePagerLayoutManager();
                mPagerLayoutManager.setMode(PagerLayoutManager.MODE_LEFT_TO_RIGHT);
                mPagerLayoutManager.onAttach(mIterator);
                mIterator = null;
                mLayoutManager = mPagerLayoutManager;
                break;
            case LAYOUT_MODE_RIGHT_TO_LEFT:
                ensurePagerLayoutManager();
                mPagerLayoutManager.setMode(PagerLayoutManager.MODE_RIGHT_TO_LEFT);
                mPagerLayoutManager.onAttach(mIterator);
                mIterator = null;
                mLayoutManager = mPagerLayoutManager;
                break;
            case LAYOUT_MODE_TOP_TO_BOTTOM:
                ensureScrollLayoutManager();
                mScrollLayoutManager.onAttach(mIterator);
                mIterator = null;
                mLayoutManager = mScrollLayoutManager;
                break;
        }
    }

    @Override
    public void onDetachFromRoot() {
        super.onDetachFromRoot();
        mEdgeView.onDetachFromRoot();

        mIterator = mLayoutManager.onDetach();
        mLayoutManager = null;
    }

    public void setLayoutMode(@LayoutMode int layoutMode) {
        if (mLayoutMode == layoutMode) {
            return;
        }
        mLayoutMode = layoutMode;

        if (mLayoutManager == null) {
            return;
        }

        switch (mLayoutMode) {
            case LAYOUT_MODE_LEFT_TO_RIGHT:
                if (mLayoutManager == mPagerLayoutManager) {
                    // mPagerLayoutManager already attached, just change mode
                    mPagerLayoutManager.setMode(PagerLayoutManager.MODE_LEFT_TO_RIGHT);
                } else {
                    ensurePagerLayoutManager();
                    mPagerLayoutManager.setMode(PagerLayoutManager.MODE_LEFT_TO_RIGHT);
                    mPagerLayoutManager.onAttach(mLayoutManager.onDetach());
                    mLayoutManager = mPagerLayoutManager;
                }
                break;
            case LAYOUT_MODE_RIGHT_TO_LEFT:
                if (mLayoutManager == mPagerLayoutManager) {
                    // mPagerLayoutManager already attached, just change mode
                    mPagerLayoutManager.setMode(PagerLayoutManager.MODE_RIGHT_TO_LEFT);
                } else {
                    ensurePagerLayoutManager();
                    mPagerLayoutManager.setMode(PagerLayoutManager.MODE_RIGHT_TO_LEFT);
                    mPagerLayoutManager.onAttach(mLayoutManager.onDetach());
                    mLayoutManager = mPagerLayoutManager;
                }
                break;
            case LAYOUT_MODE_TOP_TO_BOTTOM:
                ensureScrollLayoutManager();
                mScrollLayoutManager.onAttach(mLayoutManager.onDetach());
                mLayoutManager = mScrollLayoutManager;
                break;
        }
    }

    @Override
    public void requestLayout() {
        // Do not need requestLayout, because the size will not change
        requestFill();
    }

    public void requestFill() {
        if (mEnableRequestFill) {
            mRequestFill = true;
            invalidate();
        }
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

        if (mLayoutManager != null) {
            mLayoutManager.onDoubleTapConfirmed(x, y);
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

        if (mLayoutManager != null) {
            mLayoutManager.onScroll(dx, dy, totalX, totalY, x, y);
        }

        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mLayoutManager != null) {
            mLayoutManager.onFling(e1, e2, velocityX, velocityY);
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(float focusX, float focusY) {
        return onScale(focusX, focusY, 1.0f);
    }

    @Override
    public boolean onScale(float focusX, float focusY, float scale) {
        if (mScroll || (mLayoutManager != null && !mLayoutManager.canScale())) {
            return false;
        }
        mScale = true;

        if (mLayoutManager != null) {
            mLayoutManager.onScale(focusX, focusY, scale);
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
        mFirstScroll = true;
        if (mLayoutManager != null) {
            mLayoutManager.onDown();
        }
    }

    @Override
    public void onUp() {
        mTouched = false;
        if (mLayoutManager != null) {
            mLayoutManager.onUp();
        }
    }

    @Override
    public void onPointerDown(float x, float y) {
        if (!mScroll && (mLayoutManager != null && mLayoutManager.canScale())) {
            mScale = true;
        }
    }

    @Override
    public void onPointerUp() {

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
        mEnableRequestFill = false;
        if (mLayoutManager != null) {
            mLayoutManager.onFill();
        }
        mEnableRequestFill = true;
    }

    @Override
    public void render(GLCanvas canvas) {
        long time = AnimationTime.get();
        if (mLayoutManager != null && mLayoutManager.onUpdateAnimation(time)) {
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

    public GalleryPageView findPageById(int id) {
        if (mLayoutManager != null) {
            return mLayoutManager.findPageById(id);
        } else {
            return null;
        }
    }

    public GalleryPageView obtainPage() {
        GalleryPageView page = mRecycler.obtain();
        if (page == null) {
            page = new GalleryPageView(mContext, mPageTextTexture, mProgressColor, mProgressSize);
            page.setMinimumHeight(mPageMinHeight);
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
            progress.setMinimumWidth(mProgressSize);
            progress.setMinimumHeight(mProgressSize);
        }
        return progress;
    }

    /**
     * @param progress Indeterminate GLProgressView
     */
    public void releaseProgress(GLProgressView progress) {
        mProgressCache = progress;
    }

    public GLTextureView obtainErrorView() {
        GLTextureView errorView;
        if (mErrorViewCache != null) {
            errorView = mErrorViewCache;
            mErrorViewCache = null;
        } else {
            errorView = new GLTextureView();
        }
        return errorView;
    }

    public void unbindErrorView(GLTextureView errorView) {
        Texture texture = errorView.getTexture();
        if (texture != null) {
            errorView.setTexture(null);
            if (texture instanceof BasicTexture) {
                ((BasicTexture) texture).recycle();
            }
        }
    }

    public void bindErrorView(GLTextureView errorView, String error) {
        unbindErrorView(errorView);

        Texture texture = StringTexture.newInstance(error, mErrorSize, mErrorColor);
        errorView.setTexture(texture);
    }

    public void releaseErrorView(GLTextureView errorView) {
        unbindErrorView(errorView);
        mErrorViewCache = errorView;
    }

    public void onDataChanged() {
        if (mLayoutManager != null){
            mLayoutManager.onDataChanged();
        }
    }

    public static abstract class PageIterator {

        protected GalleryView mGalleryView;

        private void setGalleryView(@NonNull GalleryView galleryView) {
            mGalleryView = galleryView;
        }

        public abstract void mark();

        public abstract void reset();

        /**
         * @return True for waiting now, layout manager
         * should show a progress bar or something like it
         */
        public abstract boolean isWaiting();

        /**
         * @return Null for no error
         */
        public abstract String getError();

        public abstract boolean hasNext();

        public abstract boolean hasPrevious();

        public abstract void next();

        public abstract void previous();

        public void bind(GalleryPageView view) {
            view.setId(onBind(view));
        }

        public void unbind(GalleryPageView view) {
            onUnbind(view);
            view.setId(NO_ID);
        }

        public abstract int onBind(GalleryPageView view);

        public abstract void onUnbind(GalleryPageView view);

        public void notifyDataChanged() {
            mGalleryView.onDataChanged();
        }
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

        public abstract void onDoubleTapConfirmed(float x, float y);

        public abstract void onScroll(float dx, float dy, float totalX, float totalY, float x, float y);

        public abstract void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);

        public abstract boolean canScale();

        public abstract void onScale(float focusX, float focusY, float scale);

        public abstract boolean onUpdateAnimation(long time);

        public abstract void onDataChanged();

        public abstract GalleryPageView findPageById(int id);

        protected void placeCenter(GLView view) {
            int spec = GLView.MeasureSpec.makeMeasureSpec(GLView.LayoutParams.WRAP_CONTENT,
                    GLView.LayoutParams.WRAP_CONTENT);
            view.measure(spec, spec);
            int viewWidth = view.getMeasuredWidth();
            int viewHeight = view.getMeasuredHeight();
            int viewLeft = mGalleryView.getWidth() / 2 - viewWidth / 2;
            int viewTop = mGalleryView.getHeight() / 2 - viewHeight / 2;
            view.layout(viewLeft, viewTop, viewLeft + viewWidth, viewTop + viewHeight);
        }
    }

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
