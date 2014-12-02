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

package com.hippo.ehviewer.gallery;

import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Movie;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.gallery.glrenderer.BasicTexture;
import com.hippo.ehviewer.gallery.glrenderer.BitmapTexture;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.GLPaint;
import com.hippo.ehviewer.gallery.glrenderer.ImageTexture;
import com.hippo.ehviewer.gallery.glrenderer.MovieTexture;
import com.hippo.ehviewer.gallery.glrenderer.StringTexture;
import com.hippo.ehviewer.gallery.image.Image;
import com.hippo.ehviewer.gallery.ui.GLRoot;
import com.hippo.ehviewer.gallery.ui.GLView;
import com.hippo.ehviewer.gallery.ui.GestureRecognizer;
import com.hippo.ehviewer.gallery.ui.SynchronizedHandler;
import com.hippo.ehviewer.gallery.ui.TimeRunner;
import com.hippo.ehviewer.gallery.util.GalleryUtils;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.MathUtils;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.util.ZInterpolator;
import com.hippo.ehviewer.widget.MaterialToast;

public class GalleryView extends GLView implements ImageSet.ImageListener,
        Runnable, Config.OnGallerySettingsChangedListener {
    @SuppressWarnings("unused")
    private static final String TAG = GalleryView.class.getSimpleName();

    public static final int INVALID_ID = -1;

    private static final int STATE_FIRST = 0x1;
    private static final int STATE_LAST = 0x2;
    private static final int STATE_NONE = 0x0;

    // scale mode
    private static final int ORGIN = 0;
    private static final int FIT_WIDTH = 1;
    private static final int FIT_HEIGHT = 2;
    private static final int FIT = 3;
    private static final int FIXED = 4;

    // Reading direction
    private static final int DIRECTION_LEFT_RIGHT = 0;
    private static final int DIRECTION_RIGHT_LEFT = 1;
    private static final int DIRECTION_TOP_BOTTOM = 2;
    private static final int DIRECTION_BOTTOM_TOP = 3;

    // start position mode
    private static final int TOP_LEFT = 0;
    private static final int TOP_RIGHT = 1;
    private static final int BOTTOM_LEFT = 2;
    private static final int BOTTOM_RIGHT = 3;
    private static final int CENTER = 4;

    // Scroll state
    private static final int SCROLL_NONE = 0;
    private static final int SCROLL_PRE = 1;
    private static final int SCROLL_NEXT = 2;
    private static final int SCROLL_ANIME_PRE = 3;
    private static final int SCROLL_ANIME_NEXT = 4;

    private static final int PRE_TARGET_INDEX = 0;
    private static final int CUR_TARGET_INDEX = 1;
    private static final int NEXT_TARGET_INDEX = 2;
    private static final int TARGET_INDEX_SIZE = 3;

    // Area
    private static final float[] LEFT_AREA_SCALE = { 0, 0, 2 / 7.0f, 1 };
    private static final float[] TOP_AREA_SCALE = { 2 / 7.0f, 0, 5 / 7.0f,
            3 / 8.0f };
    private static final float[] RIGHT_AREA_SCALE = { 5 / 7.0f, 0, 1, 1 };
    private static final float[] BOTTOM_AREA_SCALE = { 2 / 7.0f, 5 / 8.0f,
            5 / 7.0f, 1 };
    private static final float[] CENTER_AREA_SCALE = { 2 / 7.0f, 3 / 8.0f,
            5 / 7.0f, 5 / 8.0f };
    private static final float[] LIGHTNESS_SLIDING_AREA = {
        0.0f, 0.0f, 0.166f, 1.0f };

    private static final float MILLSEC_PER_DIX = 0.2f;
    private static final float CHANGE_PAGE_PROPORTION = 0.1f;
    private static final float BORDER_TIP_SENSITIVITY = Ui.dp2pix(48);

    private static final int ANIMATE_MIN = 200;

    // TODO 根据图片的大小调整缩放界限
    private static final float SCALE_MIN = 1 / 10.0f;
    private static final float SCALE_MAX = 10.0f;

    private static final float LINE_WIDTH = Ui.dp2pix(3);
    private static final int LINE_COLOR = -1; // White
    private static final int TAP_AREA_TEXT_COLOR = -1; // White
    private static final int TAP_AREA_TEXT_SIZE = Ui.dp2pix(24);
    private static final int TAP_AREA_MASK_COLOR = 0x88000000;

    private static final int BACKGROUND_COLOR = 0xff212121;
    @SuppressWarnings("unused")
    private static final int MASK_COLOR = 0x88000000;

    private GestureRecognizer mGestureRecognizer;
    private final Context mContext;

    private final ImageSet mImageSet;
    private int mState;

    private int mReadingDirection;
    private int mPageScaling;
    private int mStartPosition;

    private int mScrollState = SCROLL_NONE;
    private int scrollXOffset = 0;
    private int scrollYOffset = 0;
    private int stopScrollXOffset = 0;
    private int stopScrollYOffset = 0;
    private float mScale = 1;

    private int mScreenWidth = -1;
    private int mScreenHeight = -1;

    private final int mSumOffsetXStore[];
    private final int mSumOffsetYStore[];
    private final int mToNextOffsetXStore[];
    private final int mToNextOffsetYStore[];

    // at most keep three item
    private volatile ShowItem[] showItems;
    private int mCurIndex;
    private int mSize;
    private boolean isEnsureSize;
    private boolean mInit = false;

    private int[] leftArea;
    private int[] topArea;
    private int[] rightArea;
    private int[] bottomArea;
    private int[] centerArea;
    private int[] mLightnessSlidingArea;

    private Text leftText;
    private Text topText;
    private Text rightText;
    private Text bottomText;
    private Text centerText;

    private boolean mDoubleTapAnimating = false;

    private boolean mLightnessSliding = false;

    private final boolean mShowTapArea = false;
    private boolean mShowTapAreaTurn = false;

    private OnTapTextListener mOnTapTextListener;
    private GalleryViewListener mGalleryViewListener;

    public interface GalleryViewListener {
        public void onTapCenter();

        public void onPageChanged(int index);

        public void onSizeUpdate(int size);

        public void onSlideBottom(float dx);

        public void onSlideBottomOver();

        public void onRightToLeftChanged(boolean value);
    }

    public interface OnTapTextListener {
        public void onTapText(int index);

        public void onTapDoubleText(int index);
    }

    public void setOnTapTextListener(OnTapTextListener l) {
        mOnTapTextListener = l;
    }

    public void setGalleryViewListener(GalleryViewListener l) {
        mGalleryViewListener = l;
    }

    @Override
    public void onReadingDirectionChanged(int value) {
        // TODO It is not safe when scroll
        boolean oldIsRTL = isRightToLeft();
        mReadingDirection = value;
        boolean newIsRTL = isRightToLeft();
        if (oldIsRTL != newIsRTL && mGalleryViewListener != null)
            mGalleryViewListener.onRightToLeftChanged(newIsRTL);
    }

    @Override
    public void onPageScalingChanged(int value) {
        mPageScaling = value;
    }

    @Override
    public void onStartPositionChanged(int value) {
        mStartPosition = value;
    }

    public GalleryView(Context context, ImageSet imageSet, int startIndex) {

        mSumOffsetXStore = new int[12];
        mSumOffsetYStore = new int[12];
        mToNextOffsetXStore = new int[4];
        mToNextOffsetYStore = new int[4];
        Arrays.fill(mSumOffsetXStore, 0, 12, 0);
        Arrays.fill(mSumOffsetYStore, 0, 12, 0);
        Arrays.fill(mToNextOffsetXStore, 0, 4, 0);
        Arrays.fill(mToNextOffsetYStore, 0, 4, 0);

        mContext = context;
        mImageSet = imageSet;
        mCurIndex = startIndex;

        showItems = new ShowItem[TARGET_INDEX_SIZE];

        checkSize();

        // adjust mCurIndex
        mCurIndex = MathUtils.clamp(mCurIndex, 0, mSize - 1);

        setState();

        setBackgroundColor(GalleryUtils
                .intColorToFloatARGBArray(BACKGROUND_COLOR));

        // Init config
        mReadingDirection = Config.getReadingDirection();
        mPageScaling = Config.getPageScaling();
        mStartPosition = Config.getStartPosition();
        Config.setOnGallerySettingsChangedListener(this);

        mImageSet.setImageListener(this);

        // Start task to check size
        AppHandler.getInstance().postDelayed(this, 500);

        // Try to make current index show image
        if (mCurIndex != startIndex)
            mImageSet.addTargetIndex(mCurIndex);

        // First time to load image in onLayout()
    }

    // The task to update size
    @Override
    public void run() {
        checkSize();
        setState();
        if (!isEnsureSize && !mImageSet.isStop())
            AppHandler.getInstance().postDelayed(this, 500);
    }

    /**
     * Check size
     */
    private void checkSize() {
        int oldSize = mSize;
        int size = mImageSet.getSize();
        if (size == -1) {
            isEnsureSize = false;
            mSize = mImageSet.getEnsureSize();
        } else {
            isEnsureSize = true;
            mSize = size;
        }

        if (mSize > oldSize && mCurIndex == oldSize - 1
                && showItems[NEXT_TARGET_INDEX] == null) {
            mState = STATE_NONE;
            loadImage(mCurIndex + 1);
        }

        if (mSize != oldSize && mGalleryViewListener != null)
            mGalleryViewListener.onSizeUpdate(mSize);
    }

    /**
     * Set current page state, first page or last page
     */
    private void setState() {
        mState = STATE_NONE;
        if (mCurIndex == 0)
            mState = STATE_FIRST;
        if (mCurIndex == mSize - 1)
            mState = STATE_LAST;
    }

    private boolean isIndexLoadable(int index) {
        int targetIndex = index - mCurIndex + 1;
        return targetIndex == CUR_TARGET_INDEX
                || (mState != STATE_FIRST && targetIndex == PRE_TARGET_INDEX)
                || (mState != STATE_LAST && targetIndex == NEXT_TARGET_INDEX);
    }

    private boolean isIndexValid(int index) {
        return index >= 0 && index < mSize;
    }

    @Override
    public void onGetImage(int index) {
        if (isIndexLoadable(index)) {
            loadImage(index);
        }
    }

    @Override
    public synchronized void onDownloading(int index, float percent) {
        int targetIndex = index - mCurIndex + 1;
        if (isIndexLoadable(index)) {
            // Free what do not need
            if (showItems[targetIndex] != null)
                showItems[targetIndex].recycle();

            showItems[targetIndex] = new Text(String.format(
                    mContext.getString(R.string.loading_percent),
                    Math.round(percent * 100)));
            resetSizePosition(targetIndex);
        }
    }

    @Override
    public synchronized void onDecodeOver(int index, Object res) {
        int targetIndex = index - mCurIndex + 1;

        if (!isIndexLoadable(index)) {
            // If it do not need any more, free
            if (res != null && res instanceof Bitmap)
                ((Bitmap) res).recycle();
        } else {
            // Free what do not need
            if (showItems[targetIndex] != null)
                showItems[targetIndex].recycle();

            if (res == null) {
                showItems[targetIndex] = new Text(
                        mContext.getString(R.string.read_image_error));
            } else {
                if (res instanceof Bitmap) {
                    BitmapItem bi = new BitmapItem();
                    bi.load((Bitmap) res);
                    showItems[targetIndex] = bi;
                } else if (res instanceof Image) {
                    ImageItem ii = new ImageItem();
                    ii.load((Image) res);
                    showItems[targetIndex] = ii;
                } else {
                    MovieItem mi = new MovieItem();
                    mi.load((Movie) res);
                    showItems[targetIndex] = mi;
                }
            }
            resetSizePosition(targetIndex);
        }
    }

    public int getCurIndex() {
        return mCurIndex;
    }

    public int getSize() {
        return mSize;
    }

    private void drawTapArea(GLCanvas canvas) {
        // Background
        canvas.fillRect(0, 0, mScreenWidth, mScreenHeight, TAP_AREA_MASK_COLOR);

        drawRect(canvas, leftArea);
        drawRect(canvas, topArea);
        drawRect(canvas, rightArea);
        drawRect(canvas, bottomArea);
        drawRect(canvas, centerArea);
        leftText.draw(canvas);
        topText.draw(canvas);
        rightText.draw(canvas);
        bottomText.draw(canvas);
        centerText.draw(canvas);
    }

    private void drawRect(GLCanvas canvas, int[] rect) {

        int left = rect[0];
        int top = rect[1];
        int right = rect[2];
        int bottom = rect[3];

        GLPaint paint = new GLPaint(LINE_COLOR, LINE_WIDTH);
        canvas.drawLine(left, top, right, top, paint);
        canvas.drawLine(left, bottom, right, bottom, paint);
        canvas.drawLine(left, top, left, bottom, paint);
        canvas.drawLine(right, top, right, bottom, paint);
    }

    @Override
    protected synchronized void render(GLCanvas canvas) {
        // If it is not render thread, do not render
        super.render(canvas);

        boolean needRefresh = false;
        ShowItem item;
        switch (mScrollState) {
        case SCROLL_NONE:
            item = showItems[CUR_TARGET_INDEX];
            if (item != null)
                item.draw(canvas);
            if (item instanceof ImageItem && ((ImageItem) item).isAnimated())
                needRefresh |= true;
            if (item instanceof MovieItem)
                needRefresh |= true;
            break;

        case SCROLL_PRE:
        case SCROLL_ANIME_PRE:
            item = showItems[PRE_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            if (item instanceof ImageItem && ((ImageItem) item).isAnimated())
                needRefresh |= true;
            if (item instanceof MovieItem)
                needRefresh |= true;

            item = showItems[CUR_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            if (item instanceof ImageItem && ((ImageItem) item).isAnimated())
                needRefresh |= true;
            if (item instanceof MovieItem)
                needRefresh |= true;
            break;

        case SCROLL_NEXT:
        case SCROLL_ANIME_NEXT:
            item = showItems[CUR_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            if (item instanceof ImageItem && ((ImageItem) item).isAnimated())
                needRefresh |= true;
            if (item instanceof MovieItem)
                needRefresh |= true;

            item = showItems[NEXT_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            if (item instanceof ImageItem && ((ImageItem) item).isAnimated())
                needRefresh |= true;
            if (item instanceof MovieItem)
                needRefresh |= true;
            break;
        }

        // TODO Mask to reduce brightness

        if (needRefresh)
            invalidate();
    }

    private void resetSizePosition() {
        resetSizePosition(PRE_TARGET_INDEX);
        resetSizePosition(CUR_TARGET_INDEX);
        resetSizePosition(NEXT_TARGET_INDEX);
    }

    /**
     * Reset target index show item position base on mReadingDirection
     * or current next previous
     *
     * @param targetIndex
     */
    private void resetSizePosition(int targetIndex) {
        ShowItem showItem = showItems[targetIndex];
        if (showItem == null)
            return;

        int index = mReadingDirection * TARGET_INDEX_SIZE + targetIndex;
        int sumXOffset = mSumOffsetXStore[index];
        int sumYOffset = mSumOffsetYStore[index];

        if (showItem instanceof Text) {
            // Make text in center
            Text text = (Text) showItem;
            int xOffset;
            int yOffset;
            Rect rect = text.mRect;
            xOffset = (mScreenWidth - text.width) / 2;
            yOffset = (mScreenHeight - text.height) / 2;
            rect.left = sumXOffset + xOffset;
            rect.top = sumYOffset + yOffset;
            rect.right = rect.left + text.width;
            rect.bottom = rect.top + text.height;
        } else if (showItem instanceof BasicItem) {
            BasicItem image = (BasicItem) showItem;

            // Set scale
            int showWidth = 0;
            int showHeight = 0;
            Rect rect = image.mRect;
            switch (mPageScaling) {
            case ORGIN:
                image.imageScale = 1;
                showWidth = image.width;
                showHeight = image.height;
                break;
            case FIT_WIDTH:
                image.imageScale = (float) mScreenWidth / image.width;
                showWidth = mScreenWidth;
                showHeight = (int) (image.height * image.imageScale);
                break;
            case FIT_HEIGHT:
                image.imageScale = (float) mScreenHeight / image.height;
                showWidth = (int) (image.width * image.imageScale);
                showHeight = mScreenHeight;
                break;
            case FIT:
                float scaleX = (float) mScreenWidth / image.width;
                float scaleY = (float) mScreenHeight / image.height;
                if (scaleX < scaleY) {
                    image.imageScale = scaleX;
                    showWidth = mScreenWidth;
                    showHeight = (int) (image.height * image.imageScale);
                } else {
                    image.imageScale = scaleY;
                    showWidth = (int) (image.width * image.imageScale);
                    showHeight = mScreenHeight;
                    break;
                }
                break;
            case FIXED:
            default:
                image.imageScale = mScale;
                showWidth = (int) (image.width * mScale);
                showHeight = (int) (image.height * mScale);
                break;
            }

            // adjust scale
            if (image.imageScale < SCALE_MIN) {
                image.imageScale = SCALE_MIN;
                showWidth = (int) (image.width * SCALE_MIN);
                showHeight = (int) (image.height * SCALE_MIN);
            } else if (image.imageScale > SCALE_MAX) {
                image.imageScale = SCALE_MAX;
                showWidth = (int) (image.width * SCALE_MAX);
                showHeight = (int) (image.height * SCALE_MAX);
            }

            // set start position
            int xOffset;
            int yOffset;
            switch (mStartPosition) {
            case TOP_LEFT:
                xOffset = 0;
                yOffset = 0;
                break;
            case TOP_RIGHT:
                xOffset = mScreenWidth - showWidth;
                yOffset = 0;
                break;
            case BOTTOM_LEFT:
                xOffset = 0;
                yOffset = mScreenHeight - showHeight;
                break;
            case BOTTOM_RIGHT:
                xOffset = mScreenWidth - showWidth;
                yOffset = mScreenHeight - showHeight;
                break;
            case CENTER:
            default:
                xOffset = (mScreenWidth - showWidth) / 2;
                yOffset = (mScreenHeight - showHeight) / 2;
                break;
            }

            rect.left = sumXOffset + xOffset;
            rect.right = rect.left + showWidth;
            rect.top = sumYOffset + yOffset;
            rect.bottom = rect.top + showHeight;

            // adjust position
            adjustPosition(image);
        }

        invalidate();
    }

    private void setCenterInArea(int[] area, ShowItem showItem) {
        int xOffset = ((area[2] - area[0]) - showItem.width) / 2;
        int yOffset = ((area[3] - area[1]) - showItem.height) / 2;

        Rect rect = showItem.mRect;

        rect.left = area[0] + xOffset;
        rect.right = rect.left + showItem.width;
        rect.top = area[1] + yOffset;
        rect.bottom = rect.top + showItem.height;
    }

    private void setTapArea() {
        leftArea = new int[] { (int) (LEFT_AREA_SCALE[0] * mScreenWidth),
                (int) (LEFT_AREA_SCALE[1] * mScreenHeight),
                (int) (LEFT_AREA_SCALE[2] * mScreenWidth),
                (int) (LEFT_AREA_SCALE[3] * mScreenHeight) };
        topArea = new int[] { (int) (TOP_AREA_SCALE[0] * mScreenWidth),
                (int) (TOP_AREA_SCALE[1] * mScreenHeight),
                (int) (TOP_AREA_SCALE[2] * mScreenWidth),
                (int) (TOP_AREA_SCALE[3] * mScreenHeight) };
        rightArea = new int[] { (int) (RIGHT_AREA_SCALE[0] * mScreenWidth),
                (int) (RIGHT_AREA_SCALE[1] * mScreenHeight),
                (int) (RIGHT_AREA_SCALE[2] * mScreenWidth),
                (int) (RIGHT_AREA_SCALE[3] * mScreenHeight) };
        bottomArea = new int[] { (int) (BOTTOM_AREA_SCALE[0] * mScreenWidth),
                (int) (BOTTOM_AREA_SCALE[1] * mScreenHeight),
                (int) (BOTTOM_AREA_SCALE[2] * mScreenWidth),
                (int) (BOTTOM_AREA_SCALE[3] * mScreenHeight) };
        centerArea = new int[] { (int) (CENTER_AREA_SCALE[0] * mScreenWidth),
                (int) (CENTER_AREA_SCALE[1] * mScreenHeight),
                (int) (CENTER_AREA_SCALE[2] * mScreenWidth),
                (int) (CENTER_AREA_SCALE[3] * mScreenHeight) };
        mLightnessSlidingArea = new int[] { (int) (LIGHTNESS_SLIDING_AREA[0] * mScreenWidth),
                (int) (LIGHTNESS_SLIDING_AREA[1] * mScreenHeight),
                (int) (LIGHTNESS_SLIDING_AREA[2] * mScreenWidth),
                (int) (LIGHTNESS_SLIDING_AREA[3] * mScreenHeight) };

        if (leftText == null)
            leftText = new Text(mContext.getString(R.string.pre_page),
                    TAP_AREA_TEXT_SIZE, TAP_AREA_TEXT_COLOR);
        if (topText == null)
            topText = new Text(mContext.getString(R.string.zoom_in),
                    TAP_AREA_TEXT_SIZE, TAP_AREA_TEXT_COLOR);
        if (rightText == null)
            rightText = new Text(mContext.getString(R.string.next_page),
                    TAP_AREA_TEXT_SIZE, TAP_AREA_TEXT_COLOR);
        if (bottomText == null)
            bottomText = new Text(mContext.getString(R.string.zoom_out),
                    TAP_AREA_TEXT_SIZE, TAP_AREA_TEXT_COLOR);
        if (centerText == null)
            centerText = new Text(mContext.getString(R.string.menu),
                    TAP_AREA_TEXT_SIZE, TAP_AREA_TEXT_COLOR);

        setCenterInArea(leftArea, leftText);
        setCenterInArea(topArea, topText);
        setCenterInArea(rightArea, rightText);
        setCenterInArea(bottomArea, bottomText);
        setCenterInArea(centerArea, centerText);
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right,
            int bottom) {
        // Get View size
        mScreenWidth = right - left;
        mScreenHeight = bottom - top;

        // Fill store
        mSumOffsetXStore[0] = -mScreenWidth;
        mSumOffsetXStore[1] = 0;
        mSumOffsetXStore[2] = mScreenWidth;
        mSumOffsetXStore[3] = mScreenWidth;
        mSumOffsetXStore[4] = 0;
        mSumOffsetXStore[5] = -mScreenWidth;
        mSumOffsetXStore[6] = 0;
        mSumOffsetXStore[7] = 0;
        mSumOffsetXStore[8] = 0;
        mSumOffsetXStore[9] = 0;
        mSumOffsetXStore[10] = 0;
        mSumOffsetXStore[11] = 0;

        mSumOffsetYStore[0] = 0;
        mSumOffsetYStore[1] = 0;
        mSumOffsetYStore[2] = 0;
        mSumOffsetYStore[3] = 0;
        mSumOffsetYStore[4] = 0;
        mSumOffsetYStore[5] = 0;
        mSumOffsetYStore[6] = -mScreenHeight;
        mSumOffsetYStore[7] = 0;
        mSumOffsetYStore[8] = mScreenHeight;
        mSumOffsetYStore[9] = mScreenHeight;
        mSumOffsetYStore[10] = 0;
        mSumOffsetYStore[11] = -mScreenHeight;

        mToNextOffsetXStore[DIRECTION_LEFT_RIGHT] = mScreenWidth;
        mToNextOffsetXStore[DIRECTION_RIGHT_LEFT] = -mScreenWidth;
        mToNextOffsetYStore[DIRECTION_TOP_BOTTOM] = mScreenHeight;
        mToNextOffsetYStore[DIRECTION_BOTTOM_TOP] = -mScreenHeight;

        if (!mInit) {
            mInit = true;
            loadImage(mCurIndex);
            if ((mState & STATE_LAST) == 0)
                loadImage(mCurIndex + 1);
            if ((mState & STATE_FIRST) == 0)
                loadImage(mCurIndex - 1);
        } else {
            resetSizePosition();
        }

        // When screen orientation change, need to reset tap area
        setTapArea();

        invalidate();
    }

    /**
     * Requset target image from ImageSet,
     *
     * @param index
     */
    private synchronized void loadImage(int index) {
        if (!isIndexLoadable(index))
            return;

        int targetIndex = index - mCurIndex + 1;
        Object obj = mImageSet.getImage(index);

        if (showItems[targetIndex] != null)
            showItems[targetIndex].recycle();

        if (obj instanceof Integer) {
            int state = (Integer) obj;
            if (state == ImageSet.RESULT_DECODE) {
                showItems[targetIndex] = null;
            } else if (state == ImageSet.RESULT_DOWNLOADING) {
                showItems[targetIndex] = new Text(
                        mContext.getString(R.string.downloading));
            } else if (state == ImageSet.RESULT_NONE) {
                showItems[targetIndex] = new Text(
                        mContext.getString(R.string.not_loaded));
            }
        } else if (obj instanceof Float) {
            showItems[targetIndex] = new Text(String.format(
                    mContext.getString(R.string.loading_percent),
                    Math.round((Float) obj * 100)));
        }

        resetSizePosition(targetIndex);
    }

    /**
     * If cur page is first page return true
     *
     * @return
     */
    private boolean isFirstPage() {
        return (mState & STATE_FIRST) != 0;
    }

    /**
     * If cur page is last page return true
     *
     * @return
     */
    private boolean isLastPage() {
        return (mState & STATE_LAST) != 0;
    }

    /**
     *
     * @param mode
     *            true zoom in, false zoom out
     * @return
     */
    private boolean zoom(boolean mode) {
        ShowItem curShowItem;
        curShowItem = showItems[CUR_TARGET_INDEX];

        if (curShowItem == null || !(curShowItem instanceof BasicItem))
            return false;
        BasicItem image = (BasicItem) curShowItem;
        float newScale;
        if (mode) {
            newScale = image.imageScale * 1.1f;
            if (newScale == SCALE_MAX)
                return false;
            if (newScale > SCALE_MAX)
                newScale = SCALE_MAX;
        } else {
            newScale = image.imageScale * 0.9f;
            if (newScale == SCALE_MIN)
                return false;
            if (newScale < SCALE_MIN)
                newScale = SCALE_MIN;
        }
        image.imageScale = newScale;
        mScale = newScale;

        Rect rect = image.mRect;
        int width = (int) (image.width * newScale);
        int height = (int) (image.height * newScale);
        int xOffset = (width - rect.width()) / 2;
        int yOffset = (height - rect.height()) / 2;

        rect.set(rect.left - xOffset, rect.top - yOffset, rect.right + xOffset,
                rect.bottom + yOffset);

        adjustPosition(curShowItem);

        invalidate();
        return true;
    }

    /**
     * You'd better resetSizePosition(PRE_TARGET_INDEX) before
     *
     * @return
     */
    private synchronized boolean goToPrePage() {
        if (isFirstPage())
            return false;

        ShowItem showItem;
        if (showItems[NEXT_TARGET_INDEX] != null) {
            showItems[NEXT_TARGET_INDEX].recycle();
            showItems[NEXT_TARGET_INDEX] = null;
        }
        showItems[NEXT_TARGET_INDEX] = showItems[CUR_TARGET_INDEX];
        showItems[CUR_TARGET_INDEX] = showItems[PRE_TARGET_INDEX];
        showItems[PRE_TARGET_INDEX] = null;

        // adjust rect
        showItem = showItems[NEXT_TARGET_INDEX];
        if (showItem != null)
            showItem.mRect.offset(mToNextOffsetXStore[mReadingDirection], mToNextOffsetYStore[mReadingDirection]);
        showItem = showItems[CUR_TARGET_INDEX];
        if (showItem != null)
            showItem.mRect.offset(mToNextOffsetXStore[mReadingDirection], mToNextOffsetYStore[mReadingDirection]);

        mCurIndex--;
        setState();
        loadImage(mCurIndex - 1);

        mImageSet.setCurReadIndex(mCurIndex);
        if (mGalleryViewListener != null) {
            mGalleryViewListener.onPageChanged(mCurIndex);
        }

        invalidate();
        return true;
    }

    /**
     * You'd better resetSizePosition(NEXT_TARGET_INDEX) before
     *
     * @return
     */
    private synchronized boolean goToNextPage() {
        if (isLastPage())
            return false;

        ShowItem showItem;
        if (showItems[PRE_TARGET_INDEX] != null) {
            showItems[PRE_TARGET_INDEX].recycle();
            showItems[PRE_TARGET_INDEX] = null;
        }
        showItems[PRE_TARGET_INDEX] = showItems[CUR_TARGET_INDEX];
        showItems[CUR_TARGET_INDEX] = showItems[NEXT_TARGET_INDEX];
        showItems[NEXT_TARGET_INDEX] = null;
        // adjust rect
        showItem = showItems[PRE_TARGET_INDEX];
        if (showItem != null)
            showItem.mRect.offset(-mToNextOffsetXStore[mReadingDirection], -mToNextOffsetYStore[mReadingDirection]);
        showItem = showItems[CUR_TARGET_INDEX];
        if (showItem != null)
            showItem.mRect.offset(-mToNextOffsetXStore[mReadingDirection], -mToNextOffsetYStore[mReadingDirection]);

        mCurIndex++;
        setState();
        loadImage(mCurIndex + 1);

        mImageSet.setCurReadIndex(mCurIndex);
        if (mGalleryViewListener != null) {
            mGalleryViewListener.onPageChanged(mCurIndex);
        }

        invalidate();
        return true;
    }

    public synchronized boolean goToPage(int index) {
        if (!isIndexValid(index))
            return false;

        if (index == mCurIndex) {
            return true;
        } else if (index == mCurIndex - 1) {
            return goToPrePage();
        } else if (index == mCurIndex + 1) {
            return goToNextPage();
        } else {
            // Free
            if (showItems[PRE_TARGET_INDEX] != null) {
                showItems[PRE_TARGET_INDEX].recycle();
                showItems[PRE_TARGET_INDEX] = null;
            }
            if (showItems[CUR_TARGET_INDEX] != null) {
                showItems[CUR_TARGET_INDEX].recycle();
                showItems[CUR_TARGET_INDEX] = null;
            }
            if (showItems[NEXT_TARGET_INDEX] != null) {
                showItems[NEXT_TARGET_INDEX].recycle();
                showItems[NEXT_TARGET_INDEX] = null;
            }

            mCurIndex = index;
            setState();
            loadImage(mCurIndex - 1);
            loadImage(mCurIndex);
            loadImage(mCurIndex + 1);

            mImageSet.setCurReadIndex(mCurIndex);
            if (mGalleryViewListener != null) {
                mGalleryViewListener.onPageChanged(mCurIndex);
            }

            invalidate();
            return true;
        }
    }

    public synchronized void free() {
        for (int i = 0; i < TARGET_INDEX_SIZE; i++) {
            if (showItems[i] != null) {
                showItems[i].recycle();
                showItems[i] = null;
            }
        }
    }

    @Override
    protected void onAttachToRoot(GLRoot root) {
        super.onAttachToRoot(root);
        mGestureRecognizer = new GestureRecognizer(mContext,
                new MyGestureListener());
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        if (mGestureRecognizer != null)
            mGestureRecognizer.onTouchEvent(event);
        return true;
    }

    public boolean isRightToLeft() {
        if (mReadingDirection == DIRECTION_RIGHT_LEFT || mReadingDirection == DIRECTION_BOTTOM_TOP)
            return true;
        else
            return false;
    }

    private class MyGestureListener implements GestureRecognizer.Listener {

        private boolean isScale = false;
        private final SynchronizedHandler mSynchronizedHandler;

        private final TimeRunner mToPreTimeRunner = new TimeRunner() {
            @Override
            protected void onStart() {
                // Empty
            }

            @Override
            protected void onRun(float interpolatedTime, int runningTime) {
                scrollXOffset = (int) (stopScrollXOffset + ((mToNextOffsetXStore[mReadingDirection] - stopScrollXOffset) * interpolatedTime));
                scrollYOffset = (int) (stopScrollYOffset + ((mToNextOffsetYStore[mReadingDirection] - stopScrollYOffset) * interpolatedTime));
                invalidate();
            }

            @Override
            protected void onEnd() {
                // Start animated image
                ShowItem showItem;
                for (int i = PRE_TARGET_INDEX; i < TARGET_INDEX_SIZE; i++) {
                    showItem = showItems[i];
                    if (showItem instanceof ImageItem)
                        ((ImageItem) showItem).start();
                }

                scrollXOffset = 0;
                scrollYOffset = 0;
                goToPrePage();
                mScrollState = SCROLL_NONE;
            }

            @Override
            protected void onCancel() {
                // Empty
            }
        };

        private final TimeRunner mToNextTimeRunner = new TimeRunner() {
            @Override
            protected void onStart() {
                // Empty
            }

            @Override
            protected void onRun(float interpolatedTime, int runningTime) {
                scrollXOffset = (int) (stopScrollXOffset + ((-mToNextOffsetXStore[mReadingDirection] - stopScrollXOffset) * interpolatedTime));
                scrollYOffset = (int) (stopScrollYOffset + ((-mToNextOffsetYStore[mReadingDirection] - stopScrollYOffset) * interpolatedTime));
                invalidate();
            }

            @Override
            protected void onEnd() {
                // Start animated image
                ShowItem showItem;
                for (int i = PRE_TARGET_INDEX; i < TARGET_INDEX_SIZE; i++) {
                    showItem = showItems[i];
                    if (showItem instanceof ImageItem)
                        ((ImageItem) showItem).start();
                }

                scrollXOffset = 0;
                scrollYOffset = 0;
                goToNextPage();
                mScrollState = SCROLL_NONE;
            }

            @Override
            protected void onCancel() {
                // Empty
            }
        };

        private final TimeRunner mReturnTimeRunner = new TimeRunner() {
            @Override
            protected void onStart() {
                // Empty
            }

            @Override
            protected void onRun(float interpolatedTime, int runningTime) {
                scrollXOffset = (int) ((1 - interpolatedTime) * stopScrollXOffset);
                scrollYOffset = (int) ((1 - interpolatedTime) * stopScrollYOffset);
                invalidate();
            }

            @Override
            protected void onEnd() {
                // Start animated image
                ShowItem showItem;
                for (int i = PRE_TARGET_INDEX; i < TARGET_INDEX_SIZE; i++) {
                    showItem = showItems[i];
                    if (showItem instanceof ImageItem)
                        ((ImageItem) showItem).start();
                }

                scrollXOffset = 0;
                scrollYOffset = 0;
                mScrollState = SCROLL_NONE;
                invalidate();
            }

            @Override
            protected void onCancel() {
                // Empty
            }
        };

        private final TimeRunner mDoubleTapRunner = new TimeRunner() {

            private BasicItem image;
            private float startScale;
            private float endScale;
            private final Point startPosition = new Point();
            private final Point endPosition = new Point();

            private boolean chooseFirst(float a, float b, float standard) {
                float offset1 = standard - a;
                float offset2 = standard - b;
                if ((offset1 > 0.0f && offset2 > 0.0f)
                        || (offset1 < 0.0f && offset2 < 0.0f)) {
                    return Math.abs(offset1) < Math.abs(offset2);
                } else {
                    float aa = a / standard;
                    float bb = b / standard;
                    aa = aa > 1.0f ? 1 / aa : aa;
                    bb = bb > 1.0f ? 1 / bb : bb;
                    return aa < bb;
                }
            }

            @Override
            protected void onStart() {
                ShowItem showItem = showItems[CUR_TARGET_INDEX];
                if (showItem == null || !(showItem instanceof BasicItem)) {
                    mDoubleTapRunner.cancel();
                    return;
                }
                image = (BasicItem) showItem;
                int width = image.width;
                int height = image.height;
                if (width == -1 || height == -1) {
                    mDoubleTapRunner.cancel();
                    return;
                }

                // Init start info
                Rect rect = image.mRect;
                startScale = image.imageScale;
                startPosition.x = (rect.left + rect.right) / 2;
                startPosition.y = (rect.top + rect.bottom) / 2;

                float fitScreenScale;
                float scaleX = (float) mScreenWidth / width;
                float scaleY = (float) mScreenHeight / height;
                if (scaleX < scaleY)
                    fitScreenScale = scaleX;
                else
                    fitScreenScale = scaleY;

                if (chooseFirst(fitScreenScale, 1, startScale))
                    // To fix screen
                    endScale = fitScreenScale;
                else
                    // To origin
                    endScale = 1;
                endPosition.x = mScreenWidth / 2;
                endPosition.y = mScreenHeight / 2;
            }

            @Override
            protected void onRun(float interpolatedTime, int runningTime) {
                int x = (int) (startPosition.x + (endPosition.x - startPosition.x)
                        * interpolatedTime);
                int y = (int) (startPosition.y + (endPosition.y - startPosition.y)
                        * interpolatedTime);
                float scale = startScale + (endScale - startScale)
                        * interpolatedTime;
                setImageRect(x, y, scale);
                invalidate();
            }

            @Override
            protected void onEnd() {
                mDoubleTapAnimating = false;
                setImageRect(mScreenWidth / 2, mScreenHeight / 2, endScale);
                invalidate();
            }

            @Override
            protected void onCancel() {
                mDoubleTapAnimating = false;
            }

            private void setImageRect(int x, int y, float scale) {
                image.imageScale = scale;
                int showWidth = (int) (image.width * scale);
                int showHeight = (int) (image.height * scale);
                Rect rect = image.mRect;
                rect.left = x - showWidth / 2;
                rect.top = y - showHeight / 2;
                rect.right = x + showWidth / 2;
                rect.bottom = y + showHeight / 2;
            }
        };

        public MyGestureListener() {

            mSynchronizedHandler = new SynchronizedHandler(getGLRoot());

            mToPreTimeRunner.setHandler(mSynchronizedHandler);
            mToNextTimeRunner.setHandler(mSynchronizedHandler);
            mReturnTimeRunner.setHandler(mSynchronizedHandler);
            mDoubleTapRunner.setHandler(mSynchronizedHandler);
            mDoubleTapRunner.setDuration(Constants.ANIMATE_TIME);
            mDoubleTapRunner.setInterpolator(new ZInterpolator(0.5f));
        }

        private void onTapPre() {
            // TODO goto bottom first the to pre page
            resetSizePosition(PRE_TARGET_INDEX);
            if (!goToPrePage()) {
                // Get to first page
                MaterialToast.showToast(mContext.getString(R.string.first_page));
            }
        }

        private void onTapNext() {
            // TODO goto bottom first the to pre page
            resetSizePosition(NEXT_TARGET_INDEX);
            if (!goToNextPage()) {
                // Get to last page
                MaterialToast.showToast(mContext.getString(isEnsureSize ?
                        R.string.the_last_page : R.string.wait_for_more));
            }
        }

        @Override
        public boolean onSingleTapConfirmed(float x, float y) {
            if (mScrollState != SCROLL_NONE) {
                return false;
            }
            if (mShowTapAreaTurn) {
                mShowTapAreaTurn = false;
                return false;
            }
            if (mDoubleTapAnimating)
                return false;

            if (showItems[CUR_TARGET_INDEX] == null
                    || showItems[CUR_TARGET_INDEX] instanceof Text) {
                if (mOnTapTextListener != null)
                    mOnTapTextListener.onTapText(mCurIndex);
                // return true;
            }

            if (leftArea == null || topArea == null || rightArea == null
                    || bottomArea == null || centerArea == null)
                return true;

            if (Utils.isInArea(leftArea, (int) x, (int) y)) {
                if (isRightToLeft())
                    onTapNext();
                else
                    onTapPre();

            } else if (Utils.isInArea(topArea, (int) x, (int) y)) {
                zoom(true);

            } else if (Utils.isInArea(rightArea, (int) x, (int) y)) {
                if (isRightToLeft())
                    onTapPre();
                else
                    onTapNext();

            } else if (Utils.isInArea(bottomArea, (int) x, (int) y)) {
                zoom(false);

            } else if (Utils.isInArea(centerArea, (int) x, (int) y)) {
                if (mGalleryViewListener != null)
                    mGalleryViewListener.onTapCenter();

            } else {
                // Can't catch tap
            }

            return true;
        }

        @Override
        public boolean onDoubleTap(float x, float y) {
            return false;
        }

        @Override
        public boolean onDoubleTapConfirmed(float x, float y) {
            if (isScale)
                return true;

            ShowItem curShowItem = showItems[CUR_TARGET_INDEX];
            if (curShowItem == null || !(curShowItem instanceof BasicItem))
                return true;

            mDoubleTapAnimating = true;
            mDoubleTapRunner.start();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (isScale)
                return;

            Utils.vibrator(mContext, 100);
            mImageSet.setStartIndex(mCurIndex);
        }

        @Override
        public boolean onScrollBegin(float dx, float dy, MotionEvent e1, MotionEvent e2) {
            mLightnessSliding = Utils.isInArea(mLightnessSlidingArea,
                    (int) e2.getX(), (int) e2.getY()) && Math.abs(dy / dx) > 1.0f;
            return onScroll(dx, dy, e1, e2);
        }

        /**
         * SCROLL_NONE, SCROLL_PRE or SCROLL_NEXT
         *
         * @param showItem
         * @param dx
         * @param dy
         * @param totalX
         * @param totalY
         * @return
         */
        private int getScrollState(ShowItem showItem, float dx, float dy, float totalX, float totalY) {
            Rect rect = showItem.mRect;
            boolean needCheckBound = showItem instanceof BasicItem;

            switch (mReadingDirection) {
            case DIRECTION_LEFT_RIGHT:
            case DIRECTION_RIGHT_LEFT:
                if (Math.abs(totalX / totalY) > 1) {
                    if (dx < 0 && (rect.left >= 0 || !needCheckBound))
                        return mReadingDirection == DIRECTION_LEFT_RIGHT ? SCROLL_PRE : SCROLL_NEXT;
                    else if (dx > 0 && (rect.right <= mScreenWidth || !needCheckBound))
                        return mReadingDirection == DIRECTION_LEFT_RIGHT ? SCROLL_NEXT : SCROLL_PRE;
                    else
                        return SCROLL_NONE;
                } else {
                    return SCROLL_NONE;
                }

            case DIRECTION_TOP_BOTTOM:
            case DIRECTION_BOTTOM_TOP:
                if (Math.abs(totalY / totalX) > 1) {
                    if (dy < 0 && (rect.top >= 0 || !needCheckBound))
                        return mReadingDirection == DIRECTION_TOP_BOTTOM ? SCROLL_PRE : SCROLL_NEXT;
                    else if (dy > 0 && (rect.bottom <= mScreenHeight || !needCheckBound))
                        return mReadingDirection == DIRECTION_TOP_BOTTOM ? SCROLL_NEXT : SCROLL_PRE;
                    else
                        return SCROLL_NONE;
                } else {
                    return SCROLL_NONE;
                }

            default:
                return SCROLL_NONE;
            }
        }

        /**
         * dx 和 totalX 符号相反，为啥
         */
        @Override
        public boolean onScroll(float dx, float dy, MotionEvent e1, MotionEvent e2) {

            if (isScale)
                return false;

            ShowItem curShowItem = showItems[CUR_TARGET_INDEX];
            // TODO what if current show item is always null
            if (curShowItem == null)
                return true;

            if (mDoubleTapAnimating)
                return false;

            if (mLightnessSliding) {
                if (mGalleryViewListener != null)
                    mGalleryViewListener.onSlideBottom(dy);
                return true;
            }

            float totalX = e2.getX() - e1.getX();
            float totalY = e2.getY() - e1.getY();

            switch (mScrollState) {
            case SCROLL_ANIME_PRE:
            case SCROLL_ANIME_NEXT:
                return false;
            case SCROLL_NONE:
                // Stop animated image
                ShowItem showItem;
                for (int i = PRE_TARGET_INDEX; i < TARGET_INDEX_SIZE; i++) {
                    showItem = showItems[i];
                    if (showItem instanceof ImageItem)
                        ((ImageItem) showItem).stop();
                }

                Rect rect = curShowItem.mRect;
                mScrollState = getScrollState(curShowItem, dx, dy, totalX, totalY);

                switch (mScrollState) {
                case SCROLL_PRE:
                    if (isFirstPage()) {
                        mScrollState = SCROLL_NONE;
                    } else {
                        scrollXOffset = 0;
                        scrollYOffset = 0;
                        // Prepare previous show item
                        resetSizePosition(PRE_TARGET_INDEX);
                    }
                    break;
                case SCROLL_NEXT:
                    if (isLastPage()) {
                        mScrollState = SCROLL_NONE;
                    } else {
                        scrollXOffset = 0;
                        scrollYOffset = 0;
                        // Prepare next show item
                        resetSizePosition(NEXT_TARGET_INDEX);
                    }
                    break;
                }
                if (mScrollState == SCROLL_NONE) {
                    int actDx = -(int) dx;
                    int actDy = -(int) dy;
                    if (rect.width() <= mScreenWidth)
                        actDx = 0;
                    if (rect.height() <= mScreenHeight)
                        actDy = 0;
                    rect.offset(actDx, actDy);

                    // Fix position
                    adjustPosition(curShowItem);
                }
                break;

            case SCROLL_PRE:
            case SCROLL_NEXT:
                if ((mScrollState == SCROLL_PRE && mReadingDirection == DIRECTION_LEFT_RIGHT) ||
                        (mScrollState == SCROLL_NEXT && mReadingDirection == DIRECTION_RIGHT_LEFT)) {
                    scrollXOffset -= dx;
                    if (scrollXOffset <= 0) {
                        scrollXOffset = 0;
                        mScrollState = SCROLL_NONE;
                    }
                } else if ((mScrollState == SCROLL_NEXT && mReadingDirection == DIRECTION_LEFT_RIGHT) ||
                        (mScrollState == SCROLL_PRE && mReadingDirection == DIRECTION_RIGHT_LEFT)) {
                    scrollXOffset -= dx;
                    if (scrollXOffset >= 0) {
                        scrollXOffset = 0;
                        mScrollState = SCROLL_NONE;
                    }
                } else if ((mScrollState == SCROLL_PRE && mReadingDirection == DIRECTION_TOP_BOTTOM) ||
                        (mScrollState == SCROLL_NEXT && mReadingDirection == DIRECTION_BOTTOM_TOP)) {
                    scrollYOffset -= dy;
                    if (scrollYOffset <= 0) {
                        scrollYOffset = 0;
                        mScrollState = SCROLL_NONE;
                    }
                } else if ((mScrollState == SCROLL_NEXT && mReadingDirection == DIRECTION_TOP_BOTTOM) ||
                        (mScrollState == SCROLL_PRE && mReadingDirection == DIRECTION_BOTTOM_TOP)) {
                    scrollYOffset -= dy;
                    if (scrollYOffset >= 0) {
                        scrollYOffset = 0;
                        mScrollState = SCROLL_NONE;
                    }
                }
                break;
            }

            invalidate();
            return true;
        }

        private boolean atPreEdge() {
            ShowItem showItem = showItems[CUR_TARGET_INDEX];
            if (showItem == null)
                return true;

            Rect rect = showItem.mRect;
            switch (mReadingDirection) {
            case DIRECTION_LEFT_RIGHT:
                if (rect.left >= 0)
                    return true;
                else
                    return false;
            case DIRECTION_RIGHT_LEFT:
                if (rect.right <= mScreenWidth)
                    return true;
                else
                    return false;
            case DIRECTION_TOP_BOTTOM:
                if (rect.top >= 0)
                    return true;
                else
                    return false;
            case DIRECTION_BOTTOM_TOP:
                if (rect.bottom <= mScreenHeight)
                    return true;
                else
                    return false;
            }
            return true;
        }

        private boolean atNextEdge() {
            ShowItem showItem = showItems[CUR_TARGET_INDEX];
            if (showItem == null)
                return true;

            Rect rect = showItem.mRect;
            switch (mReadingDirection) {
            case DIRECTION_LEFT_RIGHT:
                if (rect.right <= mScreenWidth)
                    return true;
                else
                    return false;
            case DIRECTION_RIGHT_LEFT:
                if (rect.left >= 0)
                    return true;
                else
                    return false;
            case DIRECTION_TOP_BOTTOM:
                if (rect.bottom <= mScreenHeight)
                    return true;
                else
                    return false;
            case DIRECTION_BOTTOM_TOP:
                if (rect.top >= 0)
                    return true;
                else
                    return false;
            }
            return true;
        }

        @Override
        public boolean onScrollEnd(float totalX, float totalY) {

            if (isScale)
                return false;
            if (mDoubleTapAnimating)
                return false;
            if (mLightnessSliding) {
                if (mGalleryViewListener != null)
                    mGalleryViewListener.onSlideBottomOver();
                return true;
            }

            boolean isHorizon = mReadingDirection == DIRECTION_TOP_BOTTOM || mReadingDirection == DIRECTION_BOTTOM_TOP;
            switch (mScrollState) {
            case SCROLL_ANIME_PRE:
            case SCROLL_ANIME_NEXT:
                return false;

            case SCROLL_PRE:
            case SCROLL_NEXT:
                stopScrollXOffset = scrollXOffset;
                stopScrollYOffset = scrollYOffset;
                mScrollState = mScrollState == SCROLL_PRE ? SCROLL_ANIME_PRE : SCROLL_ANIME_NEXT;
                int absStopScrollXOffset = Math.abs(stopScrollXOffset);
                int absStopScrollYOffset = Math.abs(stopScrollYOffset);

                if (Math.abs(stopScrollXOffset) > mScreenWidth * CHANGE_PAGE_PROPORTION ||
                        Math.abs(stopScrollYOffset) > mScreenHeight * CHANGE_PAGE_PROPORTION) {
                    TimeRunner runner = mScrollState == SCROLL_ANIME_PRE ? mToPreTimeRunner : mToNextTimeRunner;
                    if (isHorizon)
                        runner.setDuration(Math.max((int) (MILLSEC_PER_DIX * (mScreenHeight - absStopScrollYOffset)), ANIMATE_MIN));
                    else
                        runner.setDuration(Math.max((int) (MILLSEC_PER_DIX * (mScreenWidth - absStopScrollXOffset)), ANIMATE_MIN));
                    runner.start();
                } else {
                    mReturnTimeRunner.setDuration(Math.max((int) (MILLSEC_PER_DIX * (isHorizon ? absStopScrollYOffset : absStopScrollXOffset)), ANIMATE_MIN));
                    mReturnTimeRunner.start();
                }
                break;

            case SCROLL_NONE:
                // Start animated image
                ShowItem showItem;
                for (int i = PRE_TARGET_INDEX; i < TARGET_INDEX_SIZE; i++) {
                    showItem = showItems[i];
                    if (showItem instanceof ImageItem)
                        ((ImageItem) showItem).start();
                }

                showItem = showItems[CUR_TARGET_INDEX];
                if (isHorizon ? Math.abs(totalY) - BORDER_TIP_SENSITIVITY > 0 : Math.abs(totalX) - BORDER_TIP_SENSITIVITY > 0) {
                    if (isFirstPage() && atPreEdge())
                        MaterialToast.showToast(mContext.getString(R.string.first_page));
                    else if (isLastPage() && atNextEdge())
                        MaterialToast.showToast(mContext.getString(isEnsureSize ? R.string.the_last_page : R.string.wait_for_more));
                }
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            return true;
        }

        @Override
        public boolean onScaleBegin(float focusX, float focusY) {
            if (mDoubleTapAnimating)
                return false;

            isScale = true;
            return true;
        }

        @Override
        public boolean onScale(float focusX, float focusY, float scale) {
            if (mDoubleTapAnimating)
                return false;

            if (mScrollState == SCROLL_ANIME_PRE
                    || mScrollState == SCROLL_ANIME_NEXT)
                return true;

            if (mScrollState == SCROLL_PRE || mScrollState == SCROLL_NEXT) {
                scrollXOffset = 0;
                scrollYOffset = 0;
                mScrollState = SCROLL_NONE;
            }

            ShowItem curShowItem = showItems[CUR_TARGET_INDEX];
            if (curShowItem == null || !(curShowItem instanceof BasicItem))
                return true;

            BasicItem image = (BasicItem) curShowItem;

            float newScale = image.imageScale * scale;
            if (newScale > SCALE_MAX || newScale < SCALE_MIN)
                return true;
            image.imageScale = newScale;
            mScale = newScale;

            Rect rect = image.mRect;
            int left = rect.left;
            int top = rect.top;

            left = (int) (focusX - ((focusX - left) * scale));
            top = (int) (focusY - ((focusY - top) * scale));

            rect.set(left, top,
                    (int) (left + (image.width * image.imageScale)),
                    (int) (top + (image.height * image.imageScale)));

            // adjust
            adjustPosition(image);

            invalidate();

            return true;
        }

        @Override
        public void onScaleEnd() {
            isScale = false;
        }

        @Override
        public void onDown(float x, float y) {
            // Empty
        }

        @Override
        public void onUp() {
            // Empty
        }
    }

    /**
     *
     * If side is shorter then parent's, make it in parent's center If side is
     * longer then parent's, make sure it fill parent
     *
     * @param showItem
     */
    private void adjustPosition(ShowItem showItem) {
        int targetIndex = getTargetIndex(showItem);
        if (targetIndex == INVALID_ID)
            return;

        Rect rect = showItem.mRect;
        int showWidth = rect.width();
        int showHeight = rect.height();

        int index = mReadingDirection * TARGET_INDEX_SIZE + targetIndex;
        int sumXOffset = mSumOffsetXStore[index];
        int sumYOffset = mSumOffsetYStore[index];

        if (showWidth > mScreenWidth) {
            int fixXOffset = rect.left - sumXOffset;
            if (fixXOffset > 0) {
                rect.left -= fixXOffset;
                rect.right -= fixXOffset;
            } else if ((fixXOffset = sumXOffset + mScreenWidth - rect.right) > 0) {
                rect.left += fixXOffset;
                rect.right += fixXOffset;
            }
        } else {
            int left = sumXOffset + (mScreenWidth - showWidth) / 2;
            rect.offsetTo(left, rect.top);
        }
        if (showHeight > mScreenHeight) {
            int fixYOffset = rect.top - sumYOffset;
            if (fixYOffset > 0) {
                rect.top -= fixYOffset;
                rect.bottom -= fixYOffset;
            } else if ((fixYOffset = sumYOffset + mScreenHeight - rect.bottom) > 0) {
                rect.top += fixYOffset;
                rect.bottom += fixYOffset;
            }
        } else {
            int top = sumYOffset + (mScreenHeight - showHeight) / 2;
            rect.offsetTo(rect.left, top);
        }
    }

    public int getTargetIndex(ShowItem showItem) {
        if (mImageSet == null)
            return INVALID_ID;
        for (int i = 0; i < TARGET_INDEX_SIZE; i++) {
            if (showItem == showItems[i])
                return i;
        }
        return INVALID_ID;
    }

    private abstract class ShowItem {
        protected int width = -1;
        protected int height = -1;
        public Rect mRect = new Rect();

        public void draw(GLCanvas canvas) {
            this.draw(canvas, 0, 0);
        }

        public abstract void draw(GLCanvas canvas, int xOffset, int yOffset);

        public abstract void recycle();
    }

    private abstract class BasicItem extends ShowItem {
        private BasicTexture mTexture;
        public float imageScale = 1;
        private final RectF source = new RectF();
        private final RectF target = new RectF();

        /**
         * You must call init before draw
         *
         * @param texture
         */
        public void init(BasicTexture texture) {
            mTexture = texture;
            imageScale = 1;
        }

        @Override
        public void draw(GLCanvas canvas, int xOffset, int yOffset) {
            if (mTexture == null)
                return;

            if (getTargetIndex(this) == INVALID_ID)
                mTexture.draw(canvas, mRect.left + xOffset,
                        mRect.top + yOffset, mRect.width(), mRect.height());
            else {
                int leftBound = 0;
                int topBound = 0;
                int rightBound = mScreenWidth;
                int bottomBound = mScreenHeight;

                int left = mRect.left + xOffset;
                int top = mRect.top + yOffset;
                int right = mRect.right + xOffset;
                int bottom = mRect.bottom + yOffset;

                // Only show what in the own box
                // Only what can be seen
                if (left < leftBound || top < topBound || right > rightBound
                        || bottom > bottomBound) {

                    if (left < leftBound) {
                        target.left = leftBound;
                        source.left = (leftBound - left) / imageScale;
                    } else {
                        target.left = left;
                        source.left = 0;
                    }
                    if (top < topBound) {
                        target.top = topBound;
                        source.top = (topBound - top) / imageScale;
                    } else {
                        target.top = top;
                        source.top = 0;
                    }
                    if (right > rightBound) {
                        target.right = rightBound;
                        source.right = width - ((right - rightBound) / imageScale);
                    } else {
                        target.right = right;
                        source.right = width;
                    }
                    if (bottom > bottomBound) {
                        target.bottom = bottomBound;
                        source.bottom = height - ((bottom - bottomBound) / imageScale);
                    } else {
                        target.bottom = bottom;
                        source.bottom = height;
                    }

                    mTexture.draw(canvas, source, target);
                } else {
                    mTexture.draw(canvas, left, top, right - left, bottom - top);
                }
            }
        }
    }

    private class BitmapItem extends BasicItem {
        private BitmapTexture mTexture;
        private Bitmap mContextBmp;

        public void load(Bitmap bmp) {
            if (mTexture != null)
                recycle();
            mContextBmp = bmp;
            mTexture = new BitmapTexture(bmp);
            width = mContextBmp.getWidth();
            height = mContextBmp.getHeight();

            super.init(mTexture);
        }

        @Override
        public void recycle() {
            if (mTexture == null)
                return;

            mTexture.recycle();
            mContextBmp.recycle();

            mTexture = null;
            mContextBmp = null;
        }
    }

    private class ImageItem extends BasicItem {
        private ImageTexture mTexture;

        public void load(Image image) {
            mTexture = new ImageTexture(image);
            width = mTexture.getWidth();
            height = mTexture.getHeight();

            super.init(mTexture);
        }

        @Override
        public void draw(GLCanvas canvas, int xOffset, int yOffset) {
            super.draw(canvas, xOffset, yOffset);
        }

        @Override
        public void recycle() {
            mTexture.recycle();
        }

        public void start() {
            mTexture.start();
        }

        public void stop() {
            mTexture.stop();
        }

        public boolean isAnimated() {
            return mTexture.isAnimated();
        }
    }

    private class MovieItem extends BasicItem {
        private MovieTexture mTexture;
        private Movie mContextMovie;

        public void load(Movie movie) {
            if (mTexture != null)
                recycle();
            mContextMovie = movie;
            mTexture = MovieTexture.newInstance(mContextMovie);

            width = mContextMovie.width();
            height = mContextMovie.height();

            super.init(mTexture);
        }

        @Override
        public void recycle() {
            if (mTexture == null)
                return;

            mTexture.recycle();

            mTexture = null;
            mContextMovie = null;
        }
    }

    private class Text extends ShowItem {
        private final int mSize = Ui.sp2pix(32);

        private StringTexture mTexture;

        public Text(String str) {
            load(str);
        }

        public Text(String str, float size, int color) {
            load(str, size, color);
        }

        public void load(String str) {
            load(str, mSize, 0xdeffffff);
        }

        public void load(String str, float size, int color) {
            if (mTexture != null)
                recycle();
            mTexture = StringTexture.newInstance(str, size, color);
            width = mTexture.getWidth();
            height = mTexture.getHeight();
        }

        @Override
        public void draw(GLCanvas canvas, int xOffset, int yOffset) {
            if (mTexture != null)
                mTexture.draw(canvas, mRect.left + xOffset, mRect.top + yOffset);
        }

        @Override
        public void recycle() {
            if (mTexture == null)
                return;
            mTexture.recycle();
        }
    }
}
