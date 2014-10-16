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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Movie;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.animation.AnticipateInterpolator;

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
import com.hippo.ehviewer.gallery.ui.GLView;
import com.hippo.ehviewer.gallery.ui.GestureRecognizer;
import com.hippo.ehviewer.gallery.util.GalleryUtils;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.MathUtils;
import com.hippo.ehviewer.util.TimeRunner;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.widget.MaterialToast;

public class GalleryView extends GLView implements ImageSet.ImageListener,
        Runnable {
    @SuppressWarnings("unused")
    private static final String TAG = GalleryView.class.getSimpleName();

    public static final int INVALID_ID = -1;

    private static final int STATE_FIRST = 0x1;
    private static final int STATE_LAST = 0x2;
    private static final int STATE_NONE = 0x0;

    // scale mode
    private static final int ORGIN = 0x0;
    private static final int FIT_WIDTH = 0x1;
    private static final int FIT_HEIGHT = 0x2;
    private static final int FIT = 0x3;
    private static final int FIXED = 0x4;

    // start position mode
    private static final int TOP_LEFT = 0x0;
    private static final int TOP_RIGHT = 0x1;
    private static final int BOTTOM_LEFT = 0x2;
    private static final int BOTTOM_RIGHT = 0x3;
    private static final int CENTER = 0x4;

    // Scroll state
    private static final int SCROLL_NONE = 0x0;
    private static final int SCROLL_LEFT = 0x1;
    private static final int SCROLL_RIGHT = 0x2;
    private static final int SCROLL_ANIME_LEFT = 0x3;
    private static final int SCROLL_ANIME_RIGHT = 0x4;

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

    private static final float MILLSEC_PER_DIX = 0.2f;
    private static final float CHANGE_PAGE_PROPORTION = 0.1f;
    private static final float SLIDE_SENSITIVITY = 0;
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

    private final GestureRecognizer mGestureRecognizer;
    private final Context mContext;

    private final ImageSet mImageSet;
    private int mState;
    // scale and scroll only can choose one
    private final int scaleMode;
    private final int startMode;
    private int mScrollState = SCROLL_NONE;
    private int scrollXOffset = 0;
    private int scrollYOffset = 0;
    private int stopScrollXOffset = 0;
    @SuppressWarnings("unused")
    private int stopScrollYOffset = 0;
    private float mScale = 1;

    private int mScreenWidth = -1;
    private int mScreenHeight = -1;

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

    private Text leftText;
    private Text topText;
    private Text rightText;
    private Text bottomText;
    private Text centerText;

    private boolean mDoubleTapAnimating = false;

    private boolean mShowTapArea = false;
    private boolean mShowTapAreaTurn = false;

    // private boolean mLocked = false;
    // private boolean mLocked = false;

    private OnTapTextListener mOnTapTextListener;
    private GalleryViewListener mGalleryViewListener;

    public interface GalleryViewListener {
        public void onTapCenter();

        public void onPageChanged(int index);

        public void onSizeUpdate(int size);
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

    public GalleryView(Context context, ImageSet imageSet, int startIndex) {
        mContext = context;
        mImageSet = imageSet;
        mCurIndex = startIndex;

        showItems = new ShowItem[TARGET_INDEX_SIZE];

        checkSize();

        // adjust mCurIndex
        mCurIndex = MathUtils.clamp(mCurIndex, 0, mSize - 1);

        setState();

        mGestureRecognizer = new GestureRecognizer(mContext,
                new MyGestureListener());
        setBackgroundColor(GalleryUtils
                .intColorToFloatARGBArray(BACKGROUND_COLOR));

        // Init config
        scaleMode = Config.getPageScalingMode();
        startMode = Config.getStartPosition();

        mImageSet.setImageListener(this);

        // Start task to check size
        AppHandler.getInstance().postDelayed(this, 500);

        // Try to make current index show image
        if (mCurIndex != startIndex)
            mImageSet.setTargetIndex(mCurIndex);

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

        case SCROLL_LEFT:
        case SCROLL_ANIME_LEFT:
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

        case SCROLL_RIGHT:
        case SCROLL_ANIME_RIGHT:
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

        // Add mInit to make show the leftArea or other is not null
        if (mShowTapArea && mInit)
            drawTapArea(canvas);

        // TODO Mask to reduce brightness

        if (needRefresh)
            invalidate();
    }

    private void resetSizePosition() {
        resetSizePosition(PRE_TARGET_INDEX);
        resetSizePosition(CUR_TARGET_INDEX);
        resetSizePosition(NEXT_TARGET_INDEX);
    }

    private void resetSizePosition(int targeIndex) {
        ShowItem showItem = showItems[targeIndex];
        if (showItem == null)
            return;

        int sumXOffset;
        switch (targeIndex) {
        case PRE_TARGET_INDEX:
            sumXOffset = -mScreenWidth;
            break;
        case NEXT_TARGET_INDEX:
            sumXOffset = mScreenWidth;
            break;
        case CUR_TARGET_INDEX:
        default:
            sumXOffset = 0;
            break;
        }

        if (showItem instanceof Text) {
            Text text = (Text) showItem;
            int xOffset;
            int yOffset;
            Rect rect = text.mRect;
            xOffset = (mScreenWidth - text.width) / 2;
            yOffset = (mScreenHeight - text.height) / 2;
            rect.left = sumXOffset + xOffset;
            rect.top = yOffset;
            rect.right = rect.left + text.width;
            rect.bottom = rect.top + text.height;
        } else if (showItem instanceof BasicItem) {
            BasicItem image = (BasicItem) showItem;

            // Set scale
            int showWidth = 0;
            int showHeight = 0;
            Rect rect = image.mRect;
            switch (scaleMode) {
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
            switch (startMode) {
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
            rect.top = yOffset;
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
        if (showItem != null) {
            showItem.mRect.offset(mScreenWidth, 0);
        }
        showItem = showItems[CUR_TARGET_INDEX];
        if (showItem != null) {
            showItem.mRect.offset(mScreenWidth, 0);
        }

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
        if (showItem != null) {
            showItem.mRect.offset(-mScreenWidth, 0);
        }
        showItem = showItems[CUR_TARGET_INDEX];
        if (showItem != null) {
            showItem.mRect.offset(-mScreenWidth, 0);
        }

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
    protected boolean onTouch(MotionEvent event) {
        mGestureRecognizer.onTouchEvent(event);
        return true;
    }

    private class MyGestureListener implements GestureRecognizer.Listener {

        private boolean isScale = false;

        private final TimeRunner mToPreTimeRunner = new TimeRunner() {
            @Override
            protected void onStart() {
            }

            @Override
            protected void onRun(float interpolatedTime, int runningTime) {
                scrollXOffset = (int) (stopScrollXOffset + ((mScreenWidth - stopScrollXOffset) * interpolatedTime));
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
                goToPrePage();
                mScrollState = SCROLL_NONE;
            }

            @Override
            protected void onCancel() {
            }
        };

        private final TimeRunner mToNextTimeRunner = new TimeRunner() {
            @Override
            protected void onStart() {
            }

            @Override
            protected void onRun(float interpolatedTime, int runningTime) {
                scrollXOffset = (int) (stopScrollXOffset + ((-mScreenWidth - stopScrollXOffset) * interpolatedTime));
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
                goToNextPage();
                mScrollState = SCROLL_NONE;
            }

            @Override
            protected void onCancel() {
            }
        };

        private final TimeRunner mReturnTimeRunner = new TimeRunner() {
            @Override
            protected void onStart() {
            }

            @Override
            protected void onRun(float interpolatedTime, int runningTime) {
                scrollXOffset = (int) ((1 - interpolatedTime) * stopScrollXOffset);
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
                mScrollState = SCROLL_NONE;
                invalidate();
            }

            @Override
            protected void onCancel() {
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
            mDoubleTapRunner.setDuration(Constants.ANIMATE_TIME);
            mDoubleTapRunner.setInterpolator(new AnticipateInterpolator());
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
                // TODO goto bottom first the to pre page
                resetSizePosition(PRE_TARGET_INDEX);
                if (!goToPrePage()) {
                    // Get to first page
                    MaterialToast.showToast(mContext
                            .getString(R.string.first_page));
                }
            } else if (Utils.isInArea(topArea, (int) x, (int) y)) {
                zoom(true);
            } else if (Utils.isInArea(rightArea, (int) x, (int) y)) {
                // TODO goto bottom first the to pre page
                resetSizePosition(NEXT_TARGET_INDEX);
                if (!goToNextPage()) {
                    // Get to last page
                    MaterialToast.showToast(mContext
                            .getString(isEnsureSize ? R.string.the_last_page
                                    : R.string.wait_for_more));
                }
            } else if (Utils.isInArea(bottomArea, (int) x, (int) y)) {
                zoom(false);
            } else if (Utils.isInArea(centerArea, (int) x, (int) y)) {
                if (mGalleryViewListener != null)
                    mGalleryViewListener.onTapCenter();

                // mShowTapArea = true;
                // mShowTapAreaTurn = true;
                // invalidate();
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
        public boolean onScrollBegin(float dx, float dy, float totalX,
                float totalY) {
            return onScroll(dx, dy, totalX, totalY);
        }

        /**
         * dx 和 totalX 符号相反，为啥
         */
        @Override
        public boolean onScroll(float dx, float dy, float totalX, float totalY) {

            if (isScale)
                return false;

            ShowItem curShowItem = showItems[CUR_TARGET_INDEX];
            // TODO what if current show item is always null
            if (curShowItem == null)
                return true;

            if (mDoubleTapAnimating)
                return false;

            switch (mScrollState) {
            case SCROLL_ANIME_LEFT:
            case SCROLL_ANIME_RIGHT:
                return false;
            case SCROLL_NONE:
                // Stop animated image
                ShowItem showItem;
                for (int i = PRE_TARGET_INDEX; i < TARGET_INDEX_SIZE; i++) {
                    showItem = showItems[i];
                    if (showItem instanceof ImageItem)
                        ((ImageItem) showItem).stop();
                }

                // If it is true, page will move
                boolean movePage = false;
                Rect rect = curShowItem.mRect;

                // If show item is null or TextItem, just move
                // Check finger move left or right
                // Check min sensitivity
                if (curShowItem == null
                        || !(curShowItem instanceof BasicItem)
                        || (Math.abs(totalX / totalY) > 1 && ((totalX > SLIDE_SENSITIVITY
                                && dx < 0 && rect.left >= 0) || (totalX < -SLIDE_SENSITIVITY
                                && dx > 0 && rect.right <= mScreenWidth)))) {
                    movePage = true;
                }
                if (movePage) { // If change page
                    if (dx < 0) { // Go to left
                        if (!isFirstPage()) { // Not first page
                            scrollXOffset = 0;
                            mScrollState = SCROLL_LEFT;
                            // Prepare previous show item
                            resetSizePosition(PRE_TARGET_INDEX);
                        } else {
                            movePage = false;
                        }
                    } else { // Go to righ
                        if (!isLastPage()) { // Not last page
                            scrollXOffset = 0;
                            mScrollState = SCROLL_RIGHT;
                            // Prepare next show item
                            resetSizePosition(NEXT_TARGET_INDEX);
                        } else {
                            movePage = false;
                        }
                    }
                }
                if (!movePage) { // Move cur image
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

            case SCROLL_LEFT:
                scrollXOffset -= dx;
                if (scrollXOffset <= 0) {
                    scrollXOffset = 0;
                    mScrollState = SCROLL_NONE;
                }
                break;

            case SCROLL_RIGHT:
                scrollXOffset -= dx;
                if (scrollXOffset >= 0) {
                    scrollXOffset = 0;
                    mScrollState = SCROLL_NONE;
                }
                break;
            }

            invalidate();
            return true;
        }

        @Override
        public boolean onScrollEnd(float totalX, float totalY) {

            if (isScale)
                return false;
            if (mDoubleTapAnimating)
                return false;

            switch (mScrollState) {
            case SCROLL_ANIME_LEFT:
            case SCROLL_ANIME_RIGHT:
                return false;

            case SCROLL_LEFT:
                stopScrollXOffset = scrollXOffset;
                stopScrollYOffset = scrollYOffset;

                mScrollState = SCROLL_ANIME_LEFT;
                if (stopScrollXOffset > mScreenWidth * CHANGE_PAGE_PROPORTION) { // Go
                                                                                 // to
                                                                                 // pre
                                                                                 // page
                    mToPreTimeRunner
                            .setDuration(Math
                                    .max((int) (MILLSEC_PER_DIX * (mScreenWidth - stopScrollXOffset)),
                                            ANIMATE_MIN));
                    mToPreTimeRunner.start();
                } else {
                    mReturnTimeRunner.setDuration(Math.max(
                            (int) (MILLSEC_PER_DIX * stopScrollXOffset),
                            ANIMATE_MIN));
                    mReturnTimeRunner.start();
                }
                break;
            case SCROLL_RIGHT:
                stopScrollXOffset = scrollXOffset;
                stopScrollYOffset = scrollYOffset;

                mScrollState = SCROLL_ANIME_RIGHT;
                if (-stopScrollXOffset > mScreenWidth * CHANGE_PAGE_PROPORTION) { // Go
                                                                                  // to
                                                                                  // next
                                                                                  // page
                    mToNextTimeRunner
                            .setDuration(Math
                                    .max((int) (MILLSEC_PER_DIX * (mScreenWidth + stopScrollXOffset)),
                                            ANIMATE_MIN));
                    mToNextTimeRunner.start();
                } else {
                    mReturnTimeRunner.setDuration(Math.max(
                            (int) (MILLSEC_PER_DIX * (-stopScrollXOffset)),
                            ANIMATE_MIN));
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
                if (showItem != null
                        && Math.abs(totalX) - BORDER_TIP_SENSITIVITY > 0) {
                    if (isFirstPage() && showItem.mRect.left >= 0)
                        MaterialToast.showToast(mContext
                                .getString(R.string.first_page));
                    else if (isLastPage()
                            && showItem.mRect.right <= mScreenWidth)
                        MaterialToast
                                .showToast(mContext
                                        .getString(isEnsureSize ? R.string.the_last_page
                                                : R.string.wait_for_more));
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

            if (mScrollState == SCROLL_ANIME_LEFT
                    || mScrollState == SCROLL_ANIME_RIGHT)
                return true;

            if (mScrollState == SCROLL_LEFT || mScrollState == SCROLL_RIGHT) {
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
            mShowTapArea = false;
            invalidate();
        }

        @Override
        public void onUp() {

            // if use onSingleTapUp, use below
            /*
             * if (mShowTapAreaTurn) { mShowTapAreaTurn = false;
             */
        }
    }

    /**
     * If side is shorter then parent's, make it in parent's center If side is
     * longer then parent's, make sure it fill parent
     *
     * @param showItem
     */
    private void adjustPosition(ShowItem showItem) {
        Rect rect = showItem.mRect;
        int showWidth = rect.width();
        int showHeight = rect.height();

        int sumXOffset;
        int sumYOffset = 0;

        int targetIndex = getTargetIndex(showItem);
        switch (targetIndex) {
        case PRE_TARGET_INDEX:
            sumXOffset = -mScreenWidth;
            break;
        case NEXT_TARGET_INDEX:
            sumXOffset = mScreenWidth;
            break;
        case CUR_TARGET_INDEX:
        default:
            sumXOffset = 0;
        }

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

            int targetIndex;
            if ((targetIndex = getTargetIndex(this)) == INVALID_ID)
                mTexture.draw(canvas, mRect.left + xOffset,
                        mRect.top + yOffset, mRect.width(), mRect.height());
            else {
                int leftBound = mScreenWidth;
                int topBound = 0;
                int rightBound = 2 * mScreenWidth;
                int bottomBound = mScreenHeight;

                switch (targetIndex) {
                case PRE_TARGET_INDEX:
                    leftBound = -leftBound;
                    rightBound = 0;
                    break;
                case CUR_TARGET_INDEX:
                    rightBound = leftBound;
                    leftBound = 0;
                    break;
                case NEXT_TARGET_INDEX:
                default:
                    break;
                }

                int left = mRect.left;
                int top = mRect.top;
                int right = mRect.right;
                int bottom = mRect.bottom;

                // Only show what in the own box
                // TODO only what can be seen
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
                        source.right = width
                                - ((right - rightBound) / imageScale);
                    } else {
                        target.right = right;
                        source.right = width;
                    }
                    if (bottom > bottomBound) {
                        target.bottom = bottomBound;
                        source.bottom = height
                                - ((bottom - bottomBound) / imageScale);
                    } else {
                        target.bottom = bottom;
                        source.bottom = height;
                    }

                    target.left += xOffset;
                    target.top += yOffset;
                    target.right += xOffset;
                    target.bottom += yOffset;

                    mTexture.draw(canvas, source, target);
                } else {
                    mTexture.draw(canvas, left + xOffset, top + yOffset, right
                            - left, bottom - top);
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
        private StringTexture mTexture;

        public Text(String str) {
            load(str);
        }

        public Text(String str, float size, int color) {
            load(str, size, color);
        }

        public void load(String str) {
            load(str, 100, 0xdeffffff);
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
