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
import android.graphics.Typeface;
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
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.Pool;
import com.hippo.yorozuya.ResourcesUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GalleryView extends GLView implements GestureRecognizer.Listener {

    @IntDef({LAYOUT_MODE_LEFT_TO_RIGHT, LAYOUT_MODE_RIGHT_TO_LEFT, LAYOUT_MODE_TOP_TO_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LayoutMode {}

    public static final int BACKGROUND_COLOR = 0xff212121;
    private static final int PROGRESS_SIZE_IN_DP = 48;

    public static final int LAYOUT_MODE_LEFT_TO_RIGHT = 0;
    public static final int LAYOUT_MODE_RIGHT_TO_LEFT = 1;
    public static final int LAYOUT_MODE_TOP_TO_BOTTOM = 2;

    private static final float[] LEFT_AREA = {0.0f, 0.0f, 1.0f / 3.0f, 1f};
    private static final float[] RIGHT_AREA = {2.0f / 3.0f, 0.0f, 1.0f, 1f};
    private static final float[] MENU_AREA = {1.0f / 3.0f, 0.0f, 2.0f / 3.0f, 3.0f / 4.0f};
    private static final float[] SLIDER_AREA = {1.0f / 3.0f, 3.0f / 4.0f, 2.0f / 3.0f, 1.0f};

    private static final int METHOD_ON_SINGLE_TAP_UP = 0;
    private static final int METHOD_ON_SINGLE_TAP_CONFIRMED = 1;
    private static final int METHOD_ON_DOUBLE_TAP = 2;
    private static final int METHOD_ON_DOUBLE_TAP_CONFIRMED = 3;
    private static final int METHOD_ON_LONG_PRESS = 4;
    private static final int METHOD_ON_SCROLL = 5;
    private static final int METHOD_ON_FLING = 6;
    private static final int METHOD_ON_SCALE_BEGIN = 7;
    private static final int METHOD_ON_SCALE = 8;
    private static final int METHOD_ON_SCALE_END = 9;
    private static final int METHOD_ON_DOWN = 10;
    private static final int METHOD_ON_UP = 11;
    private static final int METHOD_ON_POINTER_DOWN = 12;
    private static final int METHOD_ON_POINTER_UP = 13;
    private static final int METHOD_SET_LAYOUT_MODE = 14;
    private static final int METHOD_SET_CURRENT_PAGE = 15;
    private static final int METHOD_PAGE_LEFT = 16;
    private static final int METHOD_PAGE_RIGHT = 17;
    private static final int METHOD_SET_SCALE_MODE = 18;
    private static final int METHOD_SET_START_POSITION = 19;
    private static final int METHOD_ON_ATTACH_TO_ROOT = 20;
    private static final int METHOD_ON_DETACH_FROM_ROOT = 21;
    private static final int METHOD_PAUSE = 22;
    private static final int METHOD_RESUME = 23;

    private final Context mContext;
    private MovableTextTexture mPageTextTexture;
    private final GestureRecognizer mGestureRecognizer;
    private Adapter mAdapter;

    private PagerLayoutManager mPagerLayoutManager;
    private ScrollLayoutManager mScrollLayoutManager;
    @Nullable
    private LayoutManager mLayoutManager;

    private final Pool<GalleryPageView> mGalleryPageViewPool = new Pool<>(5);
    private final GLEdgeView mEdgeView;
    private GLProgressView mProgressCache;
    private GLTextureView mErrorViewCache;

    private final int mProgressColor;
    private final int mProgressSize;
    private final float mErrorSize;
    private final int mErrorColor;

    private boolean mEnableRequestFill = true;
    private boolean mRequestFill = false;

    private boolean mScale = false;
    private boolean mScroll = false;
    private boolean mFirstScroll = false;
    private boolean mTouched = false;

    private final Rect mLeftArea = new Rect();
    private final Rect mRightArea = new Rect();
    private final Rect mMenuArea = new Rect();
    private final Rect mSliderArea = new Rect();

    private final String mDefaultErrorStr;
    private final String mEmptyStr;

    @LayoutMode
    private int mLayoutMode = LAYOUT_MODE_RIGHT_TO_LEFT;
    @ImageView.Scale
    private int mScaleMode = ImageView.SCALE_FIT;
    @ImageView.StartPosition
    private int mStartPosition = ImageView.START_POSITION_TOP_LEFT;
    private int mIndex;

    private final Listener mListener;

    private final List<Integer> mMethodList = new ArrayList<>(5);
    private final List<Object[]> mArgsList = new ArrayList<>(5);
    private final List<Integer> mMethodListTemp = new ArrayList<>(5);
    private final List<Object[]> mArgsListTemp = new ArrayList<>(5);

    private final AtomicInteger mCurrentIndex = new AtomicInteger(GalleryPageView.INVALID_INDEX);
    private boolean mPause;

    @SuppressWarnings("deprecation")
    public GalleryView(@NonNull Context context, @NonNull Adapter adapter,
            Listener listener, @LayoutMode int layoutMode, @ImageView.Scale int scaleMode,
            @ImageView.StartPosition int startPosition, int startPage) {
        mContext = context;
        mAdapter = adapter;
        mListener = listener;
        mEdgeView = new GLEdgeView(context);
        mGestureRecognizer = new GestureRecognizer(context, this);

        adapter.setGalleryView(this);

        mLayoutMode = layoutMode;
        mScaleMode = scaleMode;
        mStartPosition = startPosition;
        mIndex = MathUtils.clamp(startPage, 0, Integer.MAX_VALUE);
        mProgressColor = ResourcesUtils.getAttrColor(context, R.attr.colorPrimary);
        mProgressSize = LayoutUtils.dp2pix(context, PROGRESS_SIZE_IN_DP);
        mErrorSize = context.getResources().getDimension(R.dimen.text_large);
        mErrorColor = context.getResources().getColor(R.color.red_500);

        mDefaultErrorStr = context.getResources().getString(R.string.error_unknown);
        mEmptyStr = context.getResources().getString(R.string.error_empty);

        setBackgroundColor(BACKGROUND_COLOR);
    }

    private void ensurePagerLayoutManager() {
        if (mPagerLayoutManager == null) {
            mPagerLayoutManager = new PagerLayoutManager(mContext, this, mScaleMode, mStartPosition, 1.0f);
        }
    }

    private void ensureScrollLayoutManager() {
        if (mScrollLayoutManager == null) {
            mScrollLayoutManager = new ScrollLayoutManager(mContext, this);
        }
    }

    private void attachLayoutManager() {
        if (null != mLayoutManager) {
            return;
        }

        switch (mLayoutMode) {
            case LAYOUT_MODE_LEFT_TO_RIGHT:
                ensurePagerLayoutManager();
                mPagerLayoutManager.setMode(PagerLayoutManager.MODE_LEFT_TO_RIGHT);
                mPagerLayoutManager.onAttach(mAdapter);
                mPagerLayoutManager.setCurrentIndex(mIndex);
                mAdapter = null;
                mLayoutManager = mPagerLayoutManager;
                break;
            case LAYOUT_MODE_RIGHT_TO_LEFT:
                ensurePagerLayoutManager();
                mPagerLayoutManager.setMode(PagerLayoutManager.MODE_RIGHT_TO_LEFT);
                mPagerLayoutManager.onAttach(mAdapter);
                mPagerLayoutManager.setCurrentIndex(mIndex);
                mAdapter = null;
                mLayoutManager = mPagerLayoutManager;
                break;
            case LAYOUT_MODE_TOP_TO_BOTTOM:
                ensureScrollLayoutManager();
                mScrollLayoutManager.onAttach(mAdapter);
                mScrollLayoutManager.setCurrentIndex(mIndex);
                mAdapter = null;
                mLayoutManager = mScrollLayoutManager;
                break;
        }

        requestFill();
    }

    private void detachLayoutManager() {
        if (null == mLayoutManager) {
            return;
        }

        mIndex = mLayoutManager.getInternalCurrentIndex();
        mAdapter = mLayoutManager.onDetach();
        mLayoutManager = null;
    }

    @SuppressWarnings("deprecation")
    private void onAttachToRootInternal() {
        if (null == mPageTextTexture) {
            mPageTextTexture = MovableTextTexture.create(Typeface.DEFAULT,
                    mContext.getResources().getDimensionPixelSize(R.dimen.gallery_page_text),
                    mContext.getResources().getColor(R.color.secondary_text_dark),
                    new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
        }
        attachLayoutManager();
    }

    @Override
    public void onAttachToRoot(GLRoot root) {
        super.onAttachToRoot(root);
        mEdgeView.onAttachToRoot(root);
        postMethod(METHOD_ON_ATTACH_TO_ROOT);
    }

    private void onDetachFromRootInternal() {
        detachLayoutManager();
        if (null != mPageTextTexture) {
            mPageTextTexture.recycle();
            mPageTextTexture = null;
        }
    }

    @Override
    public void onDetachFromRoot() {
        super.onDetachFromRoot();
        mEdgeView.onDetachFromRoot();
        postMethod(METHOD_ON_DETACH_FROM_ROOT);
    }

    private void pauseInternal() {
        mPause = true;
    }

    public void pause() {
        postMethod(METHOD_PAUSE);
    }

    private void resumeInternal() {
        if (mPause) {
            mPause = false;
            if (null != mLayoutManager) {
                mLayoutManager.bindUnloadedPage();
            }
        }
    }

    public void resume() {
        postMethod(METHOD_RESUME);
    }

    @LayoutMode
    public static int sanitizeLayoutMode(int layoutMode) {
        if (layoutMode != GalleryView.LAYOUT_MODE_LEFT_TO_RIGHT &&
                layoutMode != GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT &&
                layoutMode != GalleryView.LAYOUT_MODE_TOP_TO_BOTTOM) {
            return GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT;
        } else {
            return layoutMode;
        }
    }

    public int getLayoutMode() {
        return mLayoutMode;
    }

    public int getCurrentIndex() {
        return mCurrentIndex.get();
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

    GLEdgeView getEdgeView() {
        return mEdgeView;
    }

    String getDefaultErrorStr() {
        return mDefaultErrorStr;
    }

    String getEmptyStr() {
        return mEmptyStr;
    }

    boolean isFirstScroll() {
        boolean firstScroll = mFirstScroll;
        mFirstScroll = false;
        return firstScroll;
    }

    // Make sure method run in render thread to ensure thread safe
    private void postMethod(int method, Object... args) {
        synchronized (this) {
            mMethodList.add(method);
            mArgsList.add(args);
        }

        invalidate();
    }

    public void setLayoutMode(@LayoutMode int layoutMode) {
        postMethod(METHOD_SET_LAYOUT_MODE, layoutMode);
    }

    public void setCurrentPage(int page) {
        postMethod(METHOD_SET_CURRENT_PAGE, page);
    }

    public void pageLeft() {
        postMethod(METHOD_PAGE_LEFT);
    }

    public void pageRight() {
        postMethod(METHOD_PAGE_RIGHT);
    }

    public void setScaleMode(@ImageView.Scale int scaleMode) {
        postMethod(METHOD_SET_SCALE_MODE, scaleMode);
    }

    public void setStartPosition(@ImageView.StartPosition int startPosition) {
        postMethod(METHOD_SET_START_POSITION, startPosition);
    }

    @Override
    public boolean onSingleTapUp(float x, float y) {
        postMethod(METHOD_ON_SINGLE_TAP_UP, x, y);
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(float x, float y) {
        postMethod(METHOD_ON_SINGLE_TAP_CONFIRMED, x, y);
        return true;
    }

    @Override
    public boolean onDoubleTap(float x, float y) {
        postMethod(METHOD_ON_DOUBLE_TAP, x, y);
        return true;
    }

    @Override
    public boolean onDoubleTapConfirmed(float x, float y) {
        postMethod(METHOD_ON_DOUBLE_TAP_CONFIRMED, x, y);
        return true;
    }

    @Override
    public void onLongPress(float x, float y) {
        if (mLayoutManager != null && !mLayoutManager.isTapOrPressEnable()) {
            return;
        }

        postMethod(METHOD_ON_LONG_PRESS, x, y);
    }

    @Override
    public boolean onScroll(float dx, float dy, float totalX, float totalY, float x, float y) {
        postMethod(METHOD_ON_SCROLL, dx, dy, totalX, totalY, x, y);
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        postMethod(METHOD_ON_FLING, velocityX, velocityY);
        return true;
    }

    @Override
    public boolean onScaleBegin(float focusX, float focusY) {
        postMethod(METHOD_ON_SCALE_BEGIN, focusX, focusY);
        return true;
    }

    @Override
    public boolean onScale(float focusX, float focusY, float scale) {
        postMethod(METHOD_ON_SCALE, focusX, focusY, scale);
        return true;
    }

    @Override
    public void onScaleEnd() {
        postMethod(METHOD_ON_SCALE_END);
    }

    @Override
    public void onDown(float x, float y) {
        postMethod(METHOD_ON_DOWN, x, y);
    }

    @Override
    public void onUp() {
        postMethod(METHOD_ON_UP);
    }

    @Override
    public void onPointerDown(float x, float y) {
        postMethod(METHOD_ON_POINTER_DOWN, x, y);
    }

    @Override
    public void onPointerUp() {
        postMethod(METHOD_ON_POINTER_UP);
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
            mMenuArea.set((int) (MENU_AREA[0] * width), (int) (MENU_AREA[1] * height),
                    (int) (MENU_AREA[2] * width), (int) (MENU_AREA[3] * height));
            mSliderArea.set((int) (SLIDER_AREA[0] * width), (int) (SLIDER_AREA[1] * height),
                    (int) (SLIDER_AREA[2] * width), (int) (SLIDER_AREA[3] * height));
        }
    }

    @RenderThread
    public void onDataChanged() {
        GalleryUtils.assertInRenderThread();

        if (mLayoutManager != null){
            mLayoutManager.onDataChanged();
        }
    }

    private void onSingleTapUpInternal(float x, float y) {
    }

    private void onSingleTapConfirmedInternal(float x, float y) {
        if (mLayoutManager == null || !mLayoutManager.isTapOrPressEnable()) {
            return;
        }

        if (mSliderArea.contains((int) x, (int) y)) {
            mListener.onTapSliderArea();
        } else if (mMenuArea.contains((int) x, (int) y)) {
            mListener.onTapMenuArea();
        } else if (mLeftArea.contains((int) x, (int) y)) {
            mLayoutManager.onPageLeft();
        } else if (mRightArea.contains((int) x, (int) y)) {
            mLayoutManager.onPageRight();
        }
    }

    private void onDoubleTapInternal(float x, float y) {
    }

    private void onDoubleTapConfirmedInternal(float x, float y) {
        if (mScale) {
            return;
        }

        if (mLayoutManager != null) {
            mLayoutManager.onDoubleTapConfirmed(x, y);
        }
    }

    private void onLongPressInternal(float x, float y) {
        if (mScale) {
            return;
        }

        if (mLayoutManager == null) {
            return;
        }

        int index = mLayoutManager.getIndexUnder(x, y);
        if (index == GalleryPageView.INVALID_INDEX) {
            return;
        }

        mListener.onLongPressPage(index);
    }

    private void onScrollInternal(float dx, float dy, float totalX, float totalY, float x, float y) {
        if (mScale) {
            return;
        }
        mScroll = true;

        if (mLayoutManager != null) {
            mLayoutManager.onScroll(dx, dy, totalX, totalY, x, y);
        }
    }

    private void onFlingInternal(float velocityX, float velocityY) {
        if (mLayoutManager != null) {
            mLayoutManager.onFling(velocityX, velocityY);
        }
    }

    private void onScaleBeginInternal(float focusX, float focusY) {
        onScaleInternal(focusX, focusY, 1.0f);
    }

    private void onScaleInternal(float focusX, float focusY, float scale) {
        if (mScroll || (mLayoutManager != null && !mLayoutManager.canScale())) {
            return;
        }
        mScale = true;

        if (mLayoutManager != null) {
            mLayoutManager.onScale(focusX, focusY, scale);
        }
    }

    private void onScaleEndInternal() {
    }

    private void onDownInternal(float x, float y) {
        mTouched = true;
        mScale = false;
        mScroll = false;
        mFirstScroll = true;
        if (mLayoutManager != null) {
            mLayoutManager.onDown();
        }
    }

    private void onUpInternal() {
        mTouched = false;
        if (mLayoutManager != null) {
            mLayoutManager.onUp();
        }
    }

    private void onPointerDownInternal(float x, float y) {
        if (!mScroll && (mLayoutManager != null && mLayoutManager.canScale())) {
            mScale = true;
        }
    }

    private void onPointerUpInternal() {
    }

    private void setLayoutModeInternal(int layoutMode) {
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
                    int index = mLayoutManager.getInternalCurrentIndex();
                    mPagerLayoutManager.onAttach(mLayoutManager.onDetach());
                    mPagerLayoutManager.setCurrentIndex(index);
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
                    int index = mLayoutManager.getInternalCurrentIndex();
                    mPagerLayoutManager.onAttach(mLayoutManager.onDetach());
                    mPagerLayoutManager.setCurrentIndex(index);
                    mLayoutManager = mPagerLayoutManager;
                }
                break;
            case LAYOUT_MODE_TOP_TO_BOTTOM:
                ensureScrollLayoutManager();
                int index = mLayoutManager.getInternalCurrentIndex();
                mScrollLayoutManager.onAttach(mLayoutManager.onDetach());
                mScrollLayoutManager.setCurrentIndex(index);
                mLayoutManager = mScrollLayoutManager;
                break;
        }

        requestFill();
    }

    private void setCurrentPageInternal(int page) {
        if (mLayoutManager != null) {
            mLayoutManager.setCurrentIndex(page);
        } else {
            mIndex = page;
        }
    }

    private void pageLeftInternal() {
        if (mLayoutManager != null) {
            mLayoutManager.onPageLeft();
        }
    }

    private void pageRightInternal() {
        if (mLayoutManager != null) {
            mLayoutManager.onPageRight();
        }
    }

    private void setScaleModeInternal(int scaleMode) {
        mScaleMode = scaleMode;
        if (mPagerLayoutManager != null) {
            mPagerLayoutManager.setScaleMode(scaleMode);
        }
    }

    private void setStartPositionInternal(int startPosition) {
        mStartPosition = startPosition;
        if (mPagerLayoutManager != null) {
            mPagerLayoutManager.setStartPosition(startPosition);
        }
    }

    @RenderThread
    void forceFill() {
        mRequestFill = true;
        fill();
    }

    @RenderThread
    private void fill() {
        GalleryUtils.assertInRenderThread();

        if (!mRequestFill) {
            return;
        }

        // Disable request layout
        mEnableRequestFill = false;
        if (mLayoutManager != null) {
            mLayoutManager.onFill();
        }
        mEnableRequestFill = true;
        mRequestFill = false;
    }

    private void dispatchMethod() {
        List<Integer> methodListTemp = mMethodListTemp;
        List<Object[]> argsListTemp = mArgsListTemp;

        synchronized (this) {
            if (mMethodList.isEmpty()) {
                return;
            }

            methodListTemp.addAll(mMethodList);
            argsListTemp.addAll(mArgsList);
            mMethodList.clear();
            mArgsList.clear();
        }

        for (int i = 0, n = methodListTemp.size(); i < n; i++) {
            int method = methodListTemp.get(i);
            Object[] args = argsListTemp.get(i);

            switch (method) {
                case METHOD_ON_SINGLE_TAP_UP:
                    onSingleTapUpInternal((Float) args[0], (Float) args[1]);
                    break;
                case METHOD_ON_SINGLE_TAP_CONFIRMED:
                    onSingleTapConfirmedInternal((Float) args[0], (Float) args[1]);
                    break;
                case METHOD_ON_DOUBLE_TAP:
                    onDoubleTapInternal((Float) args[0], (Float) args[1]);
                    break;
                case METHOD_ON_DOUBLE_TAP_CONFIRMED:
                    onDoubleTapConfirmedInternal((Float) args[0], (Float) args[1]);
                    break;
                case METHOD_ON_LONG_PRESS:
                    onLongPressInternal((Float) args[0], (Float) args[1]);
                    break;
                case METHOD_ON_SCROLL:
                    onScrollInternal((Float) args[0], (Float) args[1], (Float) args[2],
                            (Float) args[3], (Float) args[4], (Float) args[5]);
                    break;
                case METHOD_ON_FLING:
                    onFlingInternal((Float) args[0], (Float) args[1]);
                    break;
                case METHOD_ON_SCALE_BEGIN:
                    onScaleBeginInternal((Float) args[0], (Float) args[1]);
                    break;
                case METHOD_ON_SCALE:
                    onScaleInternal((Float) args[0], (Float) args[1], (Float) args[2]);
                    break;
                case METHOD_ON_SCALE_END:
                    onScaleEndInternal();
                    break;
                case METHOD_ON_DOWN:
                    onDownInternal((Float) args[0], (Float) args[1]);
                    break;
                case METHOD_ON_UP:
                    onUpInternal();
                    break;
                case METHOD_ON_POINTER_DOWN:
                    onPointerDownInternal((Float) args[0], (Float) args[1]);
                    break;
                case METHOD_ON_POINTER_UP:
                    onPointerUpInternal();
                    break;
                case METHOD_SET_LAYOUT_MODE:
                    setLayoutModeInternal((Integer) args[0]);
                    break;
                case METHOD_SET_CURRENT_PAGE:
                    setCurrentPageInternal((Integer) args[0]);
                    break;
                case METHOD_PAGE_LEFT:
                    pageLeftInternal();
                    break;
                case METHOD_PAGE_RIGHT:
                    pageRightInternal();
                    break;
                case METHOD_SET_SCALE_MODE:
                    setScaleModeInternal((Integer) args[0]);
                    break;
                case METHOD_SET_START_POSITION:
                    setStartPositionInternal((Integer) args[0]);
                    break;
                case METHOD_ON_ATTACH_TO_ROOT:
                    onAttachToRootInternal();
                    break;
                case METHOD_ON_DETACH_FROM_ROOT:
                    onDetachFromRootInternal();
                    break;
                case METHOD_PAUSE:
                    pauseInternal();
                    break;
                case METHOD_RESUME:
                    resumeInternal();
                    break;
            }
        }

        methodListTemp.clear();
        argsListTemp.clear();
    }

    @Override
    public void render(GLCanvas canvas) {
        int oldCurrentIndex = mCurrentIndex.get();

        // Dispatch method
        dispatchMethod();

        long time = AnimationTime.get();
        if (mLayoutManager != null && mLayoutManager.onUpdateAnimation(time)) {
            invalidate();
        }

        fill();

        super.render(canvas);
        mEdgeView.render(canvas);

        int newCurrentIndex;
        if (mLayoutManager != null) {
            newCurrentIndex = mLayoutManager.getCurrentIndex();
        } else {
            newCurrentIndex = GalleryPageView.INVALID_INDEX;
        }
        mCurrentIndex.lazySet(newCurrentIndex);

        if (oldCurrentIndex != newCurrentIndex) {
            mListener.onUpdateCurrentIndex(newCurrentIndex);
        }
    }

    @RenderThread
    public GalleryPageView findPageByIndex(int id) {
        if (!mPause && mLayoutManager != null) {
            return mLayoutManager.findPageByIndex(id);
        } else {
            return null;
        }
    }

    GalleryPageView obtainPage() {
        GalleryPageView page = mGalleryPageViewPool.pop();
        if (page == null) {
            page = new GalleryPageView(mContext, mPageTextTexture, mProgressColor, mProgressSize);
        }
        return page;
    }

    void releasePage(GalleryPageView page) {
        mGalleryPageViewPool.push(page);
    }

    /**
     * Indeterminate GLProgressView
     */
    GLProgressView obtainProgress() {
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
    void releaseProgress(GLProgressView progress) {
        mProgressCache = progress;
    }

    GLTextureView obtainErrorView() {
        GLTextureView errorView;
        if (mErrorViewCache != null) {
            errorView = mErrorViewCache;
            mErrorViewCache = null;
        } else {
            errorView = new GLTextureView();
        }
        return errorView;
    }

    void unbindErrorView(GLTextureView errorView) {
        Texture texture = errorView.getTexture();
        if (texture != null) {
            errorView.setTexture(null);
            if (texture instanceof BasicTexture) {
                ((BasicTexture) texture).recycle();
            }
        }
    }

    void bindErrorView(GLTextureView errorView, String error) {
        unbindErrorView(errorView);

        Texture texture = StringTexture.newInstance(error, mErrorSize, mErrorColor);
        errorView.setTexture(texture);
    }

    void releaseErrorView(GLTextureView errorView) {
        unbindErrorView(errorView);
        mErrorViewCache = errorView;
    }

    public static abstract class Adapter {

        protected GalleryView mGalleryView;

        private void setGalleryView(@NonNull GalleryView galleryView) {
            mGalleryView = galleryView;
        }

        public void bind(GalleryPageView view, int index) {
            onBind(view, index);
            view.setIndex(index);
        }

        public void unbind(GalleryPageView view) {
            onUnbind(view, view.getIndex());
            view.setIndex(GalleryPageView.INVALID_INDEX);
        }

        public abstract void onBind(GalleryPageView view, int index);

        public abstract void onUnbind(GalleryPageView view, int index);

        /**
         * @return Null for no error
         */
        public abstract String getError();

        public abstract int size();
    }

    public static abstract class LayoutManager {

        protected GalleryView mGalleryView;

        public LayoutManager(@NonNull GalleryView galleryView) {
            mGalleryView = galleryView;
        }

        public abstract void onAttach(Adapter iterator);

        public abstract Adapter onDetach();

        public abstract void onFill();

        public abstract void onDown();

        public abstract void onUp();

        public abstract void onDoubleTapConfirmed(float x, float y);

        public abstract void onLongPress(float x, float y);

        public abstract void onScroll(float dx, float dy, float totalX, float totalY, float x, float y);

        public abstract void onFling(float velocityX, float velocityY);

        public abstract boolean canScale();

        public abstract void onScale(float focusX, float focusY, float scale);

        public abstract boolean onUpdateAnimation(long time);

        public abstract void onDataChanged();

        public abstract void bindUnloadedPage();

        public abstract void onPageLeft();

        public abstract void onPageRight();

        public abstract boolean isTapOrPressEnable();

        public abstract GalleryPageView findPageByIndex(int index);

        /**
         * @return {@link GalleryPageView#INVALID_INDEX} for error
         */
        public abstract int getCurrentIndex();

        public abstract void setCurrentIndex(int index);

        public abstract int getIndexUnder(float x, float y);

        abstract int getInternalCurrentIndex();

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

    public interface Listener {

        @RenderThread
        void onUpdateCurrentIndex(int index);

        @RenderThread
        void onTapSliderArea();

        @RenderThread
        void onTapMenuArea();

        @RenderThread
        void onLongPressPage(int index);
    }
}
