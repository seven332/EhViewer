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
import android.graphics.RectF;
import android.hardware.SensorManager;
import android.support.annotation.IntDef;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import com.hippo.gl.anim.Animation;
import com.hippo.gl.glrenderer.GLCanvas;
import com.hippo.gl.glrenderer.ImageTexture;
import com.hippo.gl.util.GalleryUtils;
import com.hippo.gl.view.AnimationTime;
import com.hippo.gl.view.GLRoot;
import com.hippo.gl.view.GLView;
import com.hippo.gl.widget.EdgeView;
import com.hippo.gl.widget.ScaledImageView;
import com.hippo.yorozuya.IntArray;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.MathUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
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

    private static final Interpolator SMOOTH_SCROLLER_INTERPOLATOR = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    private static final Interpolator SMOOTH_SCALER_INTERPOLATOR = new OvershootInterpolator(0.7f);

    private static final float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
    private static final float START_TENSION = 0.5f;
    private static final float END_TENSION = 1.0f;
    private static final float P1 = START_TENSION * INFLEXION;
    private static final float P2 = 1.0f - END_TENSION * (1.0f - INFLEXION);
    private static final float FLING_FRICTION = ViewConfiguration.getScrollFriction();

    private static final int NB_SAMPLES = 100;
    private static final float[] SPLINE_POSITION = new float[NB_SAMPLES + 1];
    private static final float[] SPLINE_TIME = new float[NB_SAMPLES + 1];

    static {
        float x_min = 0.0f;
        float y_min = 0.0f;
        for (int i = 0; i < NB_SAMPLES; i++) {
            final float alpha = (float) i / NB_SAMPLES;

            float x_max = 1.0f;
            float x, tx, coef;
            while (true) {
                x = x_min + (x_max - x_min) / 2.0f;
                coef = 3.0f * x * (1.0f - x);
                tx = coef * ((1.0f - x) * P1 + x * P2) + x * x * x;
                if (Math.abs(tx - alpha) < 1E-5) break;
                if (tx > alpha) x_max = x;
                else x_min = x;
            }
            SPLINE_POSITION[i] = coef * ((1.0f - x) * START_TENSION + x) + x * x * x;

            float y_max = 1.0f;
            float y, dy;
            while (true) {
                y = y_min + (y_max - y_min) / 2.0f;
                coef = 3.0f * y * (1.0f - y);
                dy = coef * ((1.0f - y) * START_TENSION + y) + y * y * y;
                if (Math.abs(dy - alpha) < 1E-5) break;
                if (dy > alpha) y_max = y;
                else y_min = y;
            }
            SPLINE_TIME[i] = coef * ((1.0f - y) * P1 + y * P2) + y * y * y;
        }
        SPLINE_POSITION[NB_SAMPLES] = SPLINE_TIME[NB_SAMPLES] = 1.0f;
    }

    private static final Interpolator FLING_INTERPOLATOR = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            final int index = (int) (NB_SAMPLES * input);
            float distanceCoef = 1.f;
            float velocityCoef = 0.f;
            if (index < NB_SAMPLES) {
                final float t_inf = (float) index / NB_SAMPLES;
                final float t_sup = (float) (index + 1) / NB_SAMPLES;
                final float d_inf = SPLINE_POSITION[index];
                final float d_sup = SPLINE_POSITION[index + 1];
                velocityCoef = (d_sup - d_inf) / (t_sup - t_inf);
                distanceCoef = d_inf + (input - t_inf) * velocityCoef;
            }
            return distanceCoef;
        }
    };

    private static final float[] LEFT_AREA = {0.0f, 0.0f, 1.0f / 3.0f, 1f};
    private static final float[] RIGHT_AREA = {2.0f / 3.0f, 0.0f, 1.0f, 1f};
    private static final float[] CENTER_AREA = {1.0f / 3.0f, 2.0f / 5.0f, 2.0f / 3.0f, 3.0f / 5.0f};

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
    private boolean mFirstScroll = false;
    private boolean mTouched = false;

    private boolean mCanScrollBetweenPagesHMode;

    private Rect mLeftArea = new Rect();
    private Rect mRightArea = new Rect();
    private Rect mCenterArea = new Rect();

    private int mDeltaX;
    private int mDeltaY;
    private int[] mScrollRemain = new int[2];

    private boolean mHaveChangePage = false;

    @LayoutMode
    private int mLayoutMode = LAYOUT_MODE_RIGHT_TO_LEFT;
    @LayoutMode
    private int mLastLayoutMode = mLayoutMode;

    @Scale
    private int mScaleMode = SCALE_FIT;
    @StartPosition
    private int mStartPosition = START_POSITION_TOP_LEFT;
    private float mLastScale = 1f;

    public int mInterval;

    private Fling mFling;
    private SmoothScroller mSmoothScroller;
    private SmoothScaler mSmoothScaler;
    private Recycler mRecycler;

    private boolean mRequestFill = false;

    private ActionListener mListener;

    public GalleryView(Context context, int firstShownIndex, ActionListener listener) {
        mFirstShownIndex = firstShownIndex;
        mListener = listener;
        mEdgeView = new EdgeView(context);
        mGestureRecognizer = new GestureRecognizer(context, this);
        mSmoothScroller = new SmoothScroller();
        mSmoothScroller.setInterpolator(SMOOTH_SCROLLER_INTERPOLATOR);
        mSmoothScaler = new SmoothScaler();
        mSmoothScaler.setInterpolator(SMOOTH_SCALER_INTERPOLATOR);
        mFling = new Fling(context);
        mFling.setInterpolator(FLING_INTERPOLATOR);
        mRecycler = new Recycler();
        mInterval = LayoutUtils.dp2pix(context, INTERVAL_IN_DP);

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

    private void resetPageMoving() {
        mSmoothScroller.forceStop();
        mSmoothScaler.forceStop();
        mFling.forceStop();
        mLayoutOffset = 0;
        mHaveChangePage = false;
    }

    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;
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

    public void scrollToPrevious() {
        scrollToPage(mFirstShownIndex - 1);
    }

    public void scrollToNext() {
        scrollToPage(mFirstShownIndex + 1);
    }

    public void scrollToLeft() {
        switch (mLastLayoutMode) {
            case LAYOUT_MODE_LEFT_TO_RIGHT:
                scrollToPrevious();
                break;
            case LAYOUT_MODE_RIGHT_TO_LEFT:
                scrollToNext();
                break;
            case LAYOUT_MODE_TOP_TO_BOTTOM:
                // TODO What about top to bottom
                break;
        }
    }

    public void scrollToRight() {
        switch (mLastLayoutMode) {
            case LAYOUT_MODE_LEFT_TO_RIGHT:
                scrollToNext();
                break;
            case LAYOUT_MODE_RIGHT_TO_LEFT:
                scrollToPrevious();
                break;
            case LAYOUT_MODE_TOP_TO_BOTTOM:
                // TODO What about top to bottom
                break;
        }
    }

    public void scrollToPage(int page) {
        scrollToPageInternal(page, false);
    }

    private void scrollToPageInternal(int page, boolean internal) {
        if (mAdapter == null || mAdapter.getPages() <= 0) {
            return;
        }

        if (page < 0) {
            switch (mLastLayoutMode) {
                case LAYOUT_MODE_LEFT_TO_RIGHT:
                    mEdgeView.onPull(getWidth() * 2, EdgeView.LEFT);
                    break;
                case LAYOUT_MODE_RIGHT_TO_LEFT:
                    mEdgeView.onPull(getWidth() * 2, EdgeView.RIGHT);
                    break;
                case LAYOUT_MODE_TOP_TO_BOTTOM:
                    // TODO What about top to bottom
                    break;
            }
            return;
        }

        if (page >= mAdapter.getPages()) {
            switch (mLastLayoutMode) {
                case LAYOUT_MODE_LEFT_TO_RIGHT:
                    mEdgeView.onPull(getWidth() * 2, EdgeView.RIGHT);
                    break;
                case LAYOUT_MODE_RIGHT_TO_LEFT:
                    mEdgeView.onPull(getWidth() * 2, EdgeView.LEFT);
                    break;
                case LAYOUT_MODE_TOP_TO_BOTTOM:
                    // TODO What about top to bottom
                    break;
            }
            return;
        }

        mSmoothScroller.forceStop();
        mSmoothScaler.forceStop();
        mFling.forceStop();

        mFirstShownIndex = page;
        mLayoutOffset = 0;
        requestFill();

        mListener.onScrollToPage(page, internal);
    }

    public void smoothScrollToPage(int page) {
        mSmoothScroller.forceStop();
        mSmoothScaler.forceStop();
        mFling.forceStop();

        // TODO
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
        // TODO Let outside decide action
        if ((mLayoutMode == LAYOUT_MODE_LEFT_TO_RIGHT || mLayoutMode == LAYOUT_MODE_RIGHT_TO_LEFT) && mLayoutOffset == 0) {
            if (mLeftArea.contains((int) x, (int) y)) {
                if (mLayoutMode == LAYOUT_MODE_LEFT_TO_RIGHT) {
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
                if (mLayoutMode == LAYOUT_MODE_LEFT_TO_RIGHT) {
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

        // TODO Let outside decide action
        if ((mLayoutMode == LAYOUT_MODE_LEFT_TO_RIGHT || mLayoutMode == LAYOUT_MODE_RIGHT_TO_LEFT) &&
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

    private int doScrollBetweenPagesHMode(int dx) {
        boolean reverse = mLastLayoutMode == LAYOUT_MODE_RIGHT_TO_LEFT;

        int remain;
        int width = getWidth();
        int newLayoutOffset = mLayoutOffset + dx;
        if (!mHaveChangePage) {
            if (mLayoutOffset == 0) {
                if (dx > mInterval) {
                    mHaveChangePage = true;
                    mFirstShownIndex += reverse ? 1 : -1;
                    mLayoutOffset = -width;
                    remain = dx - mInterval;
                } else if (dx < -mInterval) {
                    mHaveChangePage = true;
                    mFirstShownIndex += reverse ? -1 : 1;
                    mLayoutOffset = width;
                    remain = dx + mInterval;
                } else {
                    mLayoutOffset = newLayoutOffset;
                    remain = 0;
                }
            } else if (mLayoutOffset > 0) {
                if (newLayoutOffset > mInterval) {
                    mHaveChangePage = true;
                    mFirstShownIndex += reverse ? 1 : -1;
                    mLayoutOffset = -width;
                    remain = newLayoutOffset - mInterval;
                } else if (newLayoutOffset < 0) {
                    mLayoutOffset = 0;
                    remain = newLayoutOffset;
                } else {
                    mLayoutOffset = newLayoutOffset;
                    remain = 0;
                }
            } else { // mLayoutOffset < 0
                if (newLayoutOffset < -mInterval) {
                    mHaveChangePage = true;
                    mFirstShownIndex += reverse ? -1 : 1;
                    mLayoutOffset = width;
                    remain = newLayoutOffset + mInterval;
                } else if (mLayoutOffset + dx > 0) {
                    mLayoutOffset = 0;
                    remain = newLayoutOffset;
                } else {
                    mLayoutOffset = newLayoutOffset;
                    remain = 0;
                }
            }

            // Fix mHaveChangePage
            if (mLayoutOffset < mInterval && mLayoutOffset > -mInterval) {
                mHaveChangePage = false;
            }
        } else {
            if (mLayoutOffset >= 0) {
                if (newLayoutOffset > width) {
                    mHaveChangePage = false;
                    mFirstShownIndex += reverse ? 1 : -1;
                    mLayoutOffset = -mInterval;
                    remain = newLayoutOffset - width;
                } else if (newLayoutOffset < mInterval) {
                    mHaveChangePage = false;
                    mLayoutOffset = mInterval;
                    remain = newLayoutOffset - mInterval;
                } else {
                    mLayoutOffset = newLayoutOffset;
                    remain = 0;
                }
            } else { // mLayoutOffset < 0
                if (newLayoutOffset < -width) {
                    mHaveChangePage = false;
                    mFirstShownIndex += reverse ? -1 : 1;
                    mLayoutOffset = mInterval;
                    remain = newLayoutOffset + width;
                } else if (newLayoutOffset > -mInterval) {
                    mHaveChangePage = false;
                    mLayoutOffset = -mInterval;
                    remain = newLayoutOffset + mInterval;
                } else {
                    mLayoutOffset = newLayoutOffset;
                    remain = 0;
                }
            }
        }

        return remain;
    }

    private int scrollBetweenPagesHMode(int dx) {
        int remain;
        if (mLayoutMode == LAYOUT_MODE_LEFT_TO_RIGHT) {
            if (mFirstShownIndex == 0 && mLayoutOffset - dx > 0) { // Start
                doScrollBetweenPagesHMode(-mLayoutOffset);
                remain = dx - mLayoutOffset;
            } else if (mFirstShownIndex == mAdapter.getPages() - 1 && mLayoutOffset - dx < 0) { // end
                doScrollBetweenPagesHMode(-mLayoutOffset);
                remain = dx - mLayoutOffset;
            } else {
                remain = -doScrollBetweenPagesHMode(-dx);
            }
        } else if (mLayoutMode == LAYOUT_MODE_RIGHT_TO_LEFT) {
            if (mFirstShownIndex == 0 && mLayoutOffset - dx < 0) { // Start
                doScrollBetweenPagesHMode(-mLayoutOffset);
                remain = dx - mLayoutOffset;
            } else if (mFirstShownIndex == mAdapter.getPages() - 1 && mLayoutOffset - dx > 0) { // end
                doScrollBetweenPagesHMode(-mLayoutOffset);
                remain = dx - mLayoutOffset;
            } else {
                remain = -doScrollBetweenPagesHMode(-dx);
            }
        } else {
            throw new IllegalStateException("mLayoutMode can only be " +
                    "LAYOUT_MODE_LEFT_TO_RIGHT or LAYOUT_MODE_LEFT_TO_RIGHT");
        }

        return remain;
    }


    private void overScrollEdgeForHMode(float x, float y, int dx, int dy) {
        if (dx == 0) {
            if (mDeltaX != 0) {
                mDeltaX = 0;
                mEdgeView.onRelease(EdgeView.LEFT);
                mEdgeView.onRelease(EdgeView.RIGHT);
            }
        } else if (dx < 0) {
            if (mDeltaX > 0) {
                mDeltaX = 0;
                mEdgeView.onRelease(EdgeView.LEFT);
                mEdgeView.onRelease(EdgeView.RIGHT);
            }
            mDeltaX += dx;
            mEdgeView.onPull(-mDeltaX, y, EdgeView.LEFT);
        } else { // dx > 0
            if (mDeltaX < 0) {
                mDeltaX = 0;
                mEdgeView.onRelease(EdgeView.LEFT);
                mEdgeView.onRelease(EdgeView.RIGHT);
            }
            mDeltaX += dx;
            mEdgeView.onPull(mDeltaX, y, EdgeView.RIGHT);
        }

        // Only over scroll top bottom edge when image full cover vertically
        if (mLayoutOffset != 0) {
            return;
        }
        GalleryPageView page = mPageMap.get(mFirstShownIndex);
        if (page == null) {
            return;
        }
        ScaledImageView imageView = page.getImageView();
        if (imageView == null) {
            return;
        }
        ImageTexture imageTexture = imageView.getImageTexture();
        if (imageTexture == null) {
            return;
        }
        if (imageView.getScale() * imageTexture.getHeight() <= getHeight()) {
            return;
        }
        if (dy == 0) {
            if (mDeltaY != 0) {
                mDeltaY = 0;
                mEdgeView.onRelease(EdgeView.TOP);
                mEdgeView.onRelease(EdgeView.BOTTOM);
            }
        } else if (dy < 0) {
            if (mDeltaY > 0) {
                mDeltaY = 0;
                mEdgeView.onRelease(EdgeView.TOP);
                mEdgeView.onRelease(EdgeView.BOTTOM);
            }
            mDeltaY += dy;
            mEdgeView.onPull(-mDeltaY, x, EdgeView.TOP);
        } else {
            if (mDeltaY < 0) {
                mDeltaY = 0;
                mEdgeView.onRelease(EdgeView.TOP);
                mEdgeView.onRelease(EdgeView.BOTTOM);
            }
            mDeltaY += dy;
            mEdgeView.onPull(mDeltaY, x, EdgeView.BOTTOM);
        }
    }

    @Override
    public boolean onScroll(float dx, float dy, float totalX, float totalY, float x, float y) {
        if (mScale) {
            return false;
        }
        mScroll = true;

        if (mAdapter == null || mAdapter.getPages() <= 0) {
            return true;
        }

        if (mFirstScroll) {
            mFirstScroll = false;
            mCanScrollBetweenPagesHMode = Math.abs(dx) > Math.abs(dy) * 1.5;
        }

        int dxI = (int) dx;
        int dyI = (int) dy;
        switch (mLayoutMode) {
            case LAYOUT_MODE_LEFT_TO_RIGHT:
            case LAYOUT_MODE_RIGHT_TO_LEFT: {
                boolean canImageScroll = true;
                boolean needFill = false;
                boolean reverse = mLastLayoutMode == LAYOUT_MODE_RIGHT_TO_LEFT;
                int remainX = dxI;
                int remainY = dyI;
                while (remainX != 0 || remainY != 0) {
                    if (mLayoutOffset != 0 || !canImageScroll) {
                        if (remainX == 0 || (mFirstShownIndex == 0 && mLayoutOffset == 0 &&
                                (reverse ? -remainX < 0 : -remainX > 0)) ||
                                (mFirstShownIndex == mAdapter.getPages() - 1 && mLayoutOffset == 0 &&
                                        (reverse ? -remainX > 0 : -remainX < 0))) {
                            // On edge
                            overScrollEdgeForHMode(x, y, remainX, remainY);
                            remainX = 0;
                            remainY = 0;
                        } else if (mCanScrollBetweenPagesHMode) {
                            remainX = scrollBetweenPagesHMode(remainX);
                            canImageScroll = true;
                            needFill = true;


                        } else {
                            remainX = 0;
                            remainY = 0;
                        }
                    } else {
                        GalleryPageView page = mPageMap.get(mFirstShownIndex);
                        if (page != null) {
                            page.scroll(remainX, remainY, mScrollRemain);
                            remainX = mScrollRemain[0];
                            remainY = mScrollRemain[1];
                        }
                        canImageScroll = false;
                    }
                }

                if (needFill) {
                    requestFill();
                }

                break;
            }

            case LAYOUT_MODE_TOP_TO_BOTTOM: {
                // TODO What about top to bottom
                break;
            }
        }

        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // Only over scroll top bottom edge when image full cover vertically
        if (mLayoutOffset != 0) {
            return false;
        }
        GalleryPageView page = mPageMap.get(mFirstShownIndex);
        if (page == null) {
            return false;
        }
        ScaledImageView imageView = page.getImageView();
        if (imageView == null) {
            return false;
        }
        ImageTexture imageTexture = imageView.getImageTexture();
        if (imageTexture == null) {
            return false;
        }

        RectF target = imageView.getTargetRect();
        mFling.startFling(page, (int) velocityX, getWidth() - (int) target.right, (int) -target.left,
                (int) velocityY, getHeight() - (int) target.bottom, (int) -target.top);
        return true;
    }

    @Override
    public boolean onScaleBegin(float focusX, float focusY) {
        return onScale(focusX, focusY, 1.0f);
    }

    private boolean canScale() {
        if (mLastLayoutMode != LAYOUT_MODE_LEFT_TO_RIGHT && mLastLayoutMode != LAYOUT_MODE_RIGHT_TO_LEFT) {
            return false;
        }
        if (mLayoutOffset != 0) {
            return false;
        }
        return !mScroll;
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
        mSmoothScroller.forceStop();
        mSmoothScaler.forceStop();
        mFling.forceStop();
        mTouched = true;
        mScale = false;
        mScroll = false;
        mFirstScroll = true;
        mDeltaX = 0;
        mDeltaY = 0;
    }

    @Override
    public void onUp() {
        mTouched = false;
        mDeltaX = 0;
        mDeltaY = 0;
        mEdgeView.onRelease();

        if ((mLayoutMode == LAYOUT_MODE_LEFT_TO_RIGHT || mLayoutMode == LAYOUT_MODE_RIGHT_TO_LEFT)) {
            if (mLayoutOffset != 0) {
                // Scroll
                final float pageDelta = 7 * (float) Math.abs(mLayoutOffset) / (getWidth() + mInterval);
                int duration = (int) ((pageDelta + 1) * 100);
                duration = Math.min(duration, MAX_SETTLE_DURATION);
                mSmoothScroller.startSmoothScroll(mLayoutOffset, 0, duration);
            } else {
                // Update AnimateState
                GalleryPageView page = mPageMap.get(mFirstShownIndex);
                if (page != null) {
                    page.setAnimateState(ScaledImageView.ANIMATE_STATE_RUN);
                }
            }
        } else if (mLayoutMode == LAYOUT_MODE_TOP_TO_BOTTOM) {
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

        mEdgeView.layout(left, top, right, bottom);

        //TODO set all seen UNKNOWN_SEEN in order to setScaleAndOffset
        //if (mLastLayoutMode != mMode) {
        //    mLastLayoutMode = mMode;
        //    for () {

        //    }
        //}


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
            ((LayoutParams) page.getLayoutParams()).seen = PAGE_STATE_UNKNOWN_SEEN;
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
            int seen = page.bounds().intersects(0, 0, width, height) ? PAGE_STATE_SEEN : PAGE_STATE_UNSEEN;
            lp.seen = seen;
            if (oldSeen == PAGE_STATE_UNKNOWN_SEEN || (oldSeen == PAGE_STATE_SEEN && seen == PAGE_STATE_UNSEEN)) {
                page.setScaleOffset(mScaleMode, mStartPosition, mLastScale);
            }

            // Update AnimateState
            if (seen == PAGE_STATE_UNSEEN) {
                page.setAnimateState(ScaledImageView.ANIMATE_STATE_STOP);
            } else if (!mTouched && mLayoutOffset == 0 && mFirstShownIndex == lp.index) {
                page.setAnimateState(ScaledImageView.ANIMATE_STATE_RUN);
            } else {
                //page.setAnimateState(ScaledImageView.ANIMATE_STATE_DEFALUE);
            }
        }
    }

    private void fillY(int start, int end, int startIndex, boolean reverse) {
    }

    private void fill() {
        // Must be in render thread
        GalleryUtils.assertInRenderThread();

        // Disable request layout
        mEnableRequestLayout = false;

        if (mAdapter == null) {
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
        } else if (mAdapter.getPages() == 0) {
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
            // TODO add

        } else if (mLayoutMode == LAYOUT_MODE_LEFT_TO_RIGHT || mLayoutMode == LAYOUT_MODE_RIGHT_TO_LEFT) {
            int width = getWidth();
            int start;
            int end;
            int startIndex;
            boolean reverse = mLayoutMode == LAYOUT_MODE_RIGHT_TO_LEFT;

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
                    end = -width - mInterval;
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
                    end = width * 2 + mInterval;
                }
            }

            // Set offset
            start += mLayoutOffset;
            end += mLayoutOffset;

            fillX(start, end, startIndex, reverse);
        } else { // mLayoutMode == LAYOUT_MODE_TOP_TO_BOTTOM

        }

        mEnableRequestLayout = true;
    }

    @Override
    public void onRender(GLCanvas canvas) {
        long time = AnimationTime.get();
        boolean invalidate = mSmoothScroller.calculate(time);
        invalidate |= mSmoothScaler.calculate(time);
        invalidate |= mFling.calculate(time);
        if (invalidate) {
            invalidate();
        }

        // mSmoothScroller may do fill too, so check fill request after scroll animator
        if (mRequestFill) {
            mRequestFill = false;
            fill();
        }

        mEdgeView.onRender(canvas);
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
        @PageState
        private int seen = PAGE_STATE_UNKNOWN_SEEN;

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

        public void notifySizeChange() {
            // TODO
        }
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
            int offsetX = x - mLastX;
            while (offsetX != 0) {
                int oldOffsetX = offsetX;
                offsetX = scrollBetweenPagesHMode(offsetX);
                // Avoid loop infinitely
                if (offsetX == oldOffsetX) {
                    break;
                } else {
                    mRequestFill = true;
                }
            }
            mLastX = x;
            mLastY = y;
        }
    }

    class Fling extends Animation {

        private float mPhysicalCoeff;

        private GalleryPageView mPage;
        private int mDx;
        private int mDy;
        private int mLastX;
        private int mLastY;
        private int[] mTemp = new int[2];

        public Fling(Context context) {
            final float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
            mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                    * 39.37f // inch/meter
                    * ppi
                    * 0.84f; // look and feel tuning
        }

        private double getSplineDeceleration(int velocity) {
            return Math.log(INFLEXION * Math.abs(velocity) / (FLING_FRICTION * mPhysicalCoeff));
        }

        /* Returns the duration, expressed in milliseconds */
        private int getSplineFlingDuration(int velocity) {
            final double l = getSplineDeceleration(velocity);
            final double decelMinusOne = DECELERATION_RATE - 1.0;
            return (int) (1000.0 * Math.exp(l / decelMinusOne));
        }

        private double getSplineFlingDistance(int velocity) {
            final double l = getSplineDeceleration(velocity);
            final double decelMinusOne = DECELERATION_RATE - 1.0;
            return FLING_FRICTION * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
        }

        /**
         *  Modifies mDuration to the duration it takes to get from start to newFinal using the
         *  spline interpolation. The previous duration was needed to get to oldFinal.
         **/
        private int adjustDuration(int start, int oldFinal, int newFinal, int duration) {
            final int oldDistance = oldFinal - start;
            final int newDistance = newFinal - start;
            final float x = Math.abs((float) newDistance / oldDistance);
            final int index = (int) (NB_SAMPLES * x);
            if (index < NB_SAMPLES) {
                final float x_inf = (float) index / NB_SAMPLES;
                final float x_sup = (float) (index + 1) / NB_SAMPLES;
                final float t_inf = SPLINE_TIME[index];
                final float t_sup = SPLINE_TIME[index + 1];
                final float timeCoef = t_inf + (x - x_inf) / (x_sup - x_inf) * (t_sup - t_inf);
                duration *= timeCoef;
            }
            return duration;
        }

        public void startFling(GalleryPageView page, int velocityX, int minX, int maxX,
                int velocityY, int minY, int maxY) {
            mPage = page;
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

            setDuration(Math.max(durationX, durationY));
            start();
            invalidate();
        }

        @Override
        protected void onCalculate(float progress) {
            int x = (int) (mDx * progress);
            int y = (int) (mDy * progress);
            int offsetX = x - mLastX;
            int offsetY = y - mLastY;
            if (offsetX != 0 || offsetY != 0) {
                mPage.scroll(-offsetX, -offsetY, mTemp);
            }
            mLastX = x;
            mLastY = y;
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

        void onScrollToPage(int page, boolean internal);
    }
}
