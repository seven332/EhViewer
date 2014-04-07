package com.hippo.ehviewer.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.gallery.glrenderer.BitmapTexture;
import com.hippo.ehviewer.gallery.glrenderer.ColorTexture;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.GLPaint;
import com.hippo.ehviewer.gallery.glrenderer.MovieTexture;
import com.hippo.ehviewer.gallery.glrenderer.StringTexture;
import com.hippo.ehviewer.gallery.glrenderer.UploadedTexture;
import com.hippo.ehviewer.gallery.ui.GLView;
import com.hippo.ehviewer.gallery.ui.GestureRecognizer;
import com.hippo.ehviewer.gallery.util.GalleryUtils;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.ThreadPool;
import com.hippo.ehviewer.util.ThreadPool.Job;
import com.hippo.ehviewer.util.ThreadPool.JobContext;
import com.hippo.ehviewer.util.TimeRunner;
import com.hippo.ehviewer.util.Ui;

// TODO 双击加载该页
// TODO 长按从该页开始加载


// TODO 手动触屏滑动可能闪烁
public class GalleryView extends GLView {
    @SuppressWarnings("unused")
    private static final String TAG = "ImagesView";
    
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
    private static final float[] LEFT_AREA_SCALE   = {0,      0,      2/7.0f, 1};
    private static final float[] TOP_AREA_SCALE    = {2/7.0f, 0,      5/7.0f, 3/8.0f};
    private static final float[] RIGHT_AREA_SCALE  = {5/7.0f, 0,      1,      1};
    private static final float[] BOTTOM_AREA_SCALE = {2/7.0f, 5/8.0f, 5/7.0f, 1};
    private static final float[] CENTER_AREA_SCALE = {2/7.0f, 3/8.0f, 5/7.0f, 5/8.0f};
    
    private static final float MILLSEC_PER_DIX = 0.2f;
    private static final float CHANGE_PAGE_PROPORTION = 0.05f;
    
    // TODO in dip
    private static final float CHANGE_PAGE_OFFSET = 20;
    
    // TODO 根据界面的大小调整缩放界限
    private static final float SCALE_MIN = 1/4.0f;
    private static final float SCALE_MAX = 4;
    
    private static final float LINE_WIDTH = Ui.dp2pix(3);
    private static final int LINE_COLOR = -1; // White
    private static final int TAP_AREA_TEXT_COLOR = -1; // White
    private static final int TAP_AREA_TEXT_SIZE = Ui.dp2pix(24);
    private static final int TAP_AREA_MASK_COLOR = 0x88000000;
    
    // TODO
    private static final int BACKGROUND_COLOR = Color.BLACK;
    @SuppressWarnings("unused")
    private static final int MASK_COLOR = 0x88000000;
    
    
    
    private final GestureRecognizer mGestureRecognizer;
    private Context mContext;
    
    private ImageSet mImageSet;
    private int mState;
    // scale and scroll only can choose one
    private boolean isScale = false;
    private int scaleMode;
    private int startMode;
    private int mScrollState = SCROLL_NONE;
    private int scrollXOffset = 0;
    private int scrollYOffset = 0;
    private int stopScrollXOffset = 0;
    @SuppressWarnings("unused")
    private int stopScrollYOffset = 0;
    private float mScale = 1;
    
    private boolean mShowEdegTip = true;
    
    private int mWidth = -1;
    private int mHeight = -1;
    
    // at most keep three item
    private ShowItem[] showItems;
    private int mCurIndex;
    private int mSize;
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
    
    private boolean mShowTapArea = false;
    private boolean mShowTapAreaTurn = false;
    
    private OnEdgeListener mOnEdgeListener;
    private OnTapTextListener mOnTapTextListener;
    private OnScrollPageListener mOnScrollPageListener;
    
    private ThreadPool mMovieWait;
    
    public interface OnEdgeListener {
        public void onFirstPageEdge();
        public void onLastPageEdge();
    }
    
    public interface OnTapTextListener {
        public void onTapText(int index);
        public void onTapDoubleText(int index);
    }
    
    public interface OnScrollPageListener {
        public void onScrollPage(int index);
    }
    
    public void setOnEdgeListener(OnEdgeListener l) {
        mOnEdgeListener = l;
    }
    
    public void setOnTapTextListener(OnTapTextListener l) {
        mOnTapTextListener = l;
    }
    
    public void setOnScrollPageListener(OnScrollPageListener l) {
        mOnScrollPageListener = l;
    }
    
    public GalleryView(Context context, ImageSet imageSet, int startIndex) {
        mContext = context;
        mImageSet = imageSet;
        mSize = imageSet.getSize();
        mCurIndex = startIndex;
        
        // adjust mCurIndex
        if (mCurIndex < 0 || mCurIndex >= mSize)
            mCurIndex = 0;
        
        setState();
        
        mMovieWait = new ThreadPool(1, 1);
        
        mGestureRecognizer = new GestureRecognizer(mContext, new MyGestureListener());
        setBackgroundColor(GalleryUtils.intColorToFloatARGBArray(BACKGROUND_COLOR));
        
        showItems = new ShowItem[TARGET_INDEX_SIZE];
        
        // Init config
        scaleMode = Config.getPageScalingMode();
        startMode = Config.getStartPosition();
        
        //
        imageSet.setOnStateChangeListener(new ImageSet.OnStateChangeListener() {
            @Override
            public void onStateChange(int index, int state) {
                int targetIndex = index - mCurIndex + 1;
                if (targetIndex >= PRE_TARGET_INDEX
                        && targetIndex <= NEXT_TARGET_INDEX) {
                    loadImage(index);
                }
            }
        });
    }
    
    private void setState() {
        mState = STATE_NONE;
        if (mCurIndex == 0)
            mState |= STATE_FIRST;
        if (mCurIndex == mSize - 1)
            mState |= STATE_LAST;
    }
    
    private void drawTapArea(GLCanvas canvas) {
        // Background
        canvas.fillRect(0, 0, mWidth, mHeight, TAP_AREA_MASK_COLOR);
        
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
    protected void render(GLCanvas canvas) {
        super.render(canvas);
        
        boolean hasMovie = false;
        ShowItem item;
        switch (mScrollState) {
        case SCROLL_NONE:
            item = showItems[CUR_TARGET_INDEX];
            if (item != null)
                item.draw(canvas);
            if (item instanceof MovieImage)
                hasMovie |= true;
            break;
            
        case SCROLL_LEFT:
        case SCROLL_ANIME_LEFT:
            item = showItems[PRE_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            if (item instanceof MovieImage)
                hasMovie |= true;
            
            item = showItems[CUR_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            if (item instanceof MovieImage)
                hasMovie |= true;
            break;
            
        case SCROLL_RIGHT:
        case SCROLL_ANIME_RIGHT:
            item = showItems[CUR_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            if (item instanceof MovieImage)
                hasMovie |= true;
            
            item = showItems[NEXT_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            if (item instanceof MovieImage)
                hasMovie |= true;
            break;
        }
        
        // Add mInit to make show the leftArea or other is not null
        if (mShowTapArea && mInit)
            drawTapArea(canvas);
        
        // TODO Mask to reduce brightness
        //canvas.fillRect(0, 0, mWidth, mHeight, MASK_COLOR);
        
        
        if (hasMovie)
            mMovieWait.submit(mMovieShowJob);
    }
    
    private Job<Object> mMovieShowJob = new Job<Object>() {
        @Override
        public Object run(JobContext jc) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                invalidate();
            }
            return null;
        }
    };
    
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
            sumXOffset = -mWidth;
            break;
        case NEXT_TARGET_INDEX:
            sumXOffset = mWidth;
            break;
        case CUR_TARGET_INDEX:
        default:
            sumXOffset = 0;
            break;
        }
        
        if (showItem instanceof Text) {
            Text text = (Text)showItem;
            int xOffset;
            int yOffset;
            Rect rect = text.mRect;
            xOffset = (mWidth - text.width)/2;
            yOffset = (mHeight - text.height)/2;
            rect.left = sumXOffset + xOffset;
            rect.top = yOffset;
            rect.right = rect.left + text.width;
            rect.bottom = rect.top + text.height;
            return;
        }
        
        if (showItem instanceof Image) {
            Image image = (Image)showItem;
            
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
                image.imageScale = (float)mWidth/image.width;
                showWidth = mWidth;
                showHeight = (int)(image.height * image.imageScale);
                break;
            case FIT_HEIGHT:
                image.imageScale = (float)mHeight/image.height;
                showWidth = (int)(image.width * image.imageScale);
                showHeight = mHeight;
                break;
            case FIT:
                float scaleX = (float)mWidth/image.width;
                float scaleY = (float)mHeight/image.height;
                if (scaleX < scaleY) {
                    image.imageScale = scaleX;
                    showWidth = mWidth;
                    showHeight = (int)(image.height * image.imageScale);
                } else {
                    image.imageScale = scaleY;
                    showWidth = (int)(image.width * image.imageScale);
                    showHeight = mHeight;
                    break;
                }
                break;
            case FIXED:
            default:
                image.imageScale = mScale;
                showWidth = (int)(image.width * mScale);
                showHeight = (int)(image.height * mScale);
                break;
            }
            
            // adjust scale
            if (image.imageScale < SCALE_MIN) {
                image.imageScale = SCALE_MIN;
                showWidth = (int)(image.width * SCALE_MIN);
                showHeight = (int)(image.height * SCALE_MIN);
            } else if (image.imageScale > SCALE_MAX) {
                image.imageScale = SCALE_MAX;
                showWidth = (int)(image.width * SCALE_MAX);
                showHeight = (int)(image.height * SCALE_MAX);
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
                xOffset = mWidth - showWidth;
                yOffset = 0;
                break;
            case BOTTOM_LEFT:
                xOffset = 0;
                yOffset = mHeight - showHeight;
                break;
            case BOTTOM_RIGHT:
                xOffset = mWidth - showWidth;
                yOffset = mHeight - showHeight;
                break;
            case CENTER:
            default:
                xOffset = (mWidth - showWidth)/2;
                yOffset = (mHeight - showHeight)/2;
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
        int xOffset = ((area[2] - area[0]) - showItem.width)/2;
        int yOffset = ((area[3] - area[1]) - showItem.height)/2;
        
        Rect rect = showItem.mRect;
        
        rect.left = area[0] + xOffset;
        rect.right = rect.left + showItem.width;
        rect.top = area[1] + yOffset;
        rect.bottom = rect.top + showItem.height;
    }
    
    private void setTapArea() {
        leftArea = new int[]{
                (int)(LEFT_AREA_SCALE[0] * mWidth),
                (int)(LEFT_AREA_SCALE[1] * mHeight),
                (int)(LEFT_AREA_SCALE[2] * mWidth),
                (int)(LEFT_AREA_SCALE[3] * mHeight)};
        topArea = new int[]{
                (int)(TOP_AREA_SCALE[0] * mWidth),
                (int)(TOP_AREA_SCALE[1] * mHeight),
                (int)(TOP_AREA_SCALE[2] * mWidth),
                (int)(TOP_AREA_SCALE[3] * mHeight)};
        rightArea = new int[]{
                (int)(RIGHT_AREA_SCALE[0] * mWidth),
                (int)(RIGHT_AREA_SCALE[1] * mHeight),
                (int)(RIGHT_AREA_SCALE[2] * mWidth),
                (int)(RIGHT_AREA_SCALE[3] * mHeight)};
        bottomArea = new int[]{
                (int)(BOTTOM_AREA_SCALE[0] * mWidth),
                (int)(BOTTOM_AREA_SCALE[1] * mHeight),
                (int)(BOTTOM_AREA_SCALE[2] * mWidth),
                (int)(BOTTOM_AREA_SCALE[3] * mHeight)};
        centerArea = new int[]{
                (int)(CENTER_AREA_SCALE[0] * mWidth),
                (int)(CENTER_AREA_SCALE[1] * mHeight),
                (int)(CENTER_AREA_SCALE[2] * mWidth),
                (int)(CENTER_AREA_SCALE[3] * mHeight)};
        
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
    protected void onLayout(
            boolean changeSize, int left, int top, int right, int bottom) {
        mWidth = right - left;
        mHeight = bottom - top;
        
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
        setTapArea();
        invalidate();
    }
    
    private void loadImage(int index) {
        int targetIndex = index - mCurIndex + 1;
        int state = mImageSet.getImage(index, new DecodeImageListener());
        ShowItem showItem = showItems[targetIndex];
        if (showItem != null)
            showItem.recycle();
        if (state == ImageSet.STATE_LOADED)
            showItems[targetIndex] = new EmptyItem();
        else {
            showItems[targetIndex] = new Text(getErrorStateString(state));
        }
        resetSizePosition(targetIndex);
    }
    
    private String getErrorStateString(int state) {
        switch (state) {
        case ImageSet.INVALID_ID:
            return "无效的 index";
        case ImageSet.STATE_NONE:
            return "尚未加载";
        case ImageSet.STATE_LOADING:
            return "正在加载";
        case ImageSet.STATE_FAIL:
            return "加载失败";
        default:
            return "错误的代码";
        }
    }
    
    /**
     * If cur page is first page return true
     * @return
     */
    private boolean isFirstPage() {
        return (mState & STATE_FIRST) != 0;
    }
    
    /**
     * If cur page is last page return true
     * @return
     */
    private boolean isLastPage() {
        return (mState & STATE_LAST) != 0;
    }
    
    class DecodeImageListener implements ImageSet.OnDecodeOverListener {
        @Override
        public void onDecodeOver(Object res, int index) {
            int targetIndex = index - mCurIndex + 1;
            
            if (targetIndex < 0 || targetIndex > 2
                    || !(showItems[targetIndex] instanceof EmptyItem)) {// If it do not need any more
                if (res != null && res instanceof Bitmap)
                    ((Bitmap)res).recycle();
            } else {
                if (res == null) {
                    showItems[targetIndex] = new Text("读取图片错误"); // TODO
                } else {
                    if (res instanceof Bitmap) {
                        BitmapImage bi = new BitmapImage();
                        bi.load((Bitmap)res);
                        showItems[targetIndex] = bi;
                    } else {
                        MovieImage mi = new MovieImage();
                        mi.load((Movie)res);
                        showItems[targetIndex] = mi;
                    }
                }
                resetSizePosition(targetIndex);
            }
        }
    }
    
    /**
     * 
     * @param mode
     * true zoom in, false zoom out
     * @return
     */
    private boolean zoom(boolean mode) {
        ShowItem curShowItem;
        curShowItem = showItems[CUR_TARGET_INDEX];
        
        if (curShowItem == null || !(curShowItem instanceof Image))
            return false;
        Image image = (Image)curShowItem;
        float newScale;
        if (mode) {
            newScale = image.imageScale * 1.1f;
            if (newScale == SCALE_MAX)
                return false;
            if (newScale > SCALE_MAX)
                newScale = SCALE_MAX;
        }
        else {
            newScale = image.imageScale * 0.9f;
            if (newScale == SCALE_MIN)
                return false;
            if (newScale < SCALE_MIN)
                newScale = SCALE_MIN;
        }
        image.imageScale = newScale;
        mScale = newScale;
        
        Rect rect = image.mRect;
        int width = (int)(image.width * newScale);
        int height = (int)(image.height * newScale);
        int xOffset = (width - rect.width())/2;
        int yOffset = (height - rect.height())/2;
        
        rect.set(rect.left - xOffset, rect.top - yOffset, rect.right + xOffset, rect.bottom + yOffset);
        
        adjustPosition(curShowItem);
        
        invalidate();
        return true;
    }
    
    /**
     * You'd better resetSizePosition(PRE_TARGET_INDEX) before
     * @return
     */
    private boolean goToPrePage() {
        if (isFirstPage())
            return false;
        
        ShowItem showItem;
        showItem = showItems[NEXT_TARGET_INDEX];
        if (showItem != null)
            showItem.recycle();
        showItems[NEXT_TARGET_INDEX] = showItems[CUR_TARGET_INDEX];
        showItems[CUR_TARGET_INDEX] = showItems[PRE_TARGET_INDEX];
        showItems[PRE_TARGET_INDEX] = null;
        
        // adjust rect
        showItem = showItems[NEXT_TARGET_INDEX];
        if (showItem != null) {
            showItem.mRect.offset(mWidth, 0);
        }
        showItem = showItems[CUR_TARGET_INDEX];
        if (showItem != null) {
            showItem.mRect.offset(mWidth, 0);
        }
        
        mCurIndex--;
        setState();
        loadImage(mCurIndex-1);
        
        if (mOnScrollPageListener != null) {
            mOnScrollPageListener.onScrollPage(mCurIndex);
        }
        
        invalidate();
        return true;
    }
    
    /**
     * You'd better resetSizePosition(NEXT_TARGET_INDEX) before
     * @return
     */
    private boolean goToNextPage() {
        if (isLastPage())
            return false;
        
        ShowItem showItem;
        showItem = showItems[PRE_TARGET_INDEX];
        if (showItem != null)
            showItem.recycle();
        showItems[PRE_TARGET_INDEX] = showItems[CUR_TARGET_INDEX];
        showItems[CUR_TARGET_INDEX] = showItems[NEXT_TARGET_INDEX];
        showItems[NEXT_TARGET_INDEX] = null;
        // adjust rect
        showItem = showItems[PRE_TARGET_INDEX];
        if (showItem != null) {
            showItem.mRect.offset(-mWidth, 0);
        }
        showItem = showItems[CUR_TARGET_INDEX];
        if (showItem != null) {
            showItem.mRect.offset(-mWidth, 0);
        }
        
        mCurIndex++;
        setState();
        loadImage(mCurIndex+1);
        
        if (mOnScrollPageListener != null) {
            mOnScrollPageListener.onScrollPage(mCurIndex);
        }
        
        invalidate();
        return true;
    }
    
    @Override
    protected boolean onTouch(MotionEvent event) {
        mGestureRecognizer.onTouchEvent(event);
        return true;
    }
    
    private class MyGestureListener implements GestureRecognizer.Listener {
        
        public MyGestureListener() {
            mToPreTimeRunner.setOnTimerListener(new TimeRunner.OnTimeListener() {
                @Override
                public void onStart() {}
                @Override
                public void onEnd() {
                    scrollXOffset = 0;
                    goToPrePage();
                    mScrollState = SCROLL_NONE;
                }
            });
            mToNextTimeRunner.setOnTimerListener(new TimeRunner.OnTimeListener() {
                @Override
                public void onStart() {}
                @Override
                public void onEnd() {
                    scrollXOffset = 0;
                    goToNextPage();
                    mScrollState = SCROLL_NONE;
                }
            });
            mReturnTimeRunner.setOnTimerListener(new TimeRunner.OnTimeListener() {
                @Override
                public void onStart() {}
                @Override
                public void onEnd() {
                    scrollXOffset = 0;
                    mScrollState = SCROLL_NONE;
                    invalidate();
                }
            });
        }
        
        private boolean isInArea(int[] area, int x, int y) {
            if (area.length != 4)
                throw new IllegalArgumentException(
                        "area's length should be 4, but it's length is " + area.length);
            if (x >= area[0] && x < area[2] && y >= area[1] && y < area[3])
                return true;
            else
                return false;
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
            
            if ( showItems[CUR_TARGET_INDEX] == null
                    || showItems[CUR_TARGET_INDEX] instanceof Text) {
                if (mOnTapTextListener != null)
                    mOnTapTextListener.onTapText(mCurIndex);
                //return true;
            }
            
            if (isInArea(leftArea, (int)x, (int)y)) {
                // TODO goto bottom first the to pre page
                resetSizePosition(PRE_TARGET_INDEX);
                if (!goToPrePage() && mOnEdgeListener != null)
                    mOnEdgeListener.onFirstPageEdge();
            } else if (isInArea(topArea, (int)x, (int)y)) {
                zoom(true);
            } else if (isInArea(rightArea, (int)x, (int)y)) {
                // TODO goto bottom first the to pre page
                resetSizePosition(NEXT_TARGET_INDEX);
                if (!goToNextPage() && mOnEdgeListener != null)
                    mOnEdgeListener.onLastPageEdge();
            } else if (isInArea(bottomArea, (int)x, (int)y)) {
                zoom(false);
            } else if (isInArea(centerArea, (int)x, (int)y)) {
                mShowTapArea = true;
                mShowTapAreaTurn = true;
                invalidate();
            } else {
                // Can't catch tap
            }
            
            return true;
        }
        
        @Override
        public boolean onDoubleTap(float x, float y) {
            Log.d(TAG, "onDoubleTap");
            return true;
        }
        
        @Override
        public void onLongPress(MotionEvent e) {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public boolean onScrollBegin(float dx, float dy, float totalX,
                float totalY) {
            return this.onScroll(dx, dy, totalX, totalY);
        }
        
        /**
         * dx 和 totalX 符号相反，为啥
         */
        @Override
        public boolean onScroll(float dx, float dy, float totalX, float totalY) {
            
            if (isScale)
                return false;
            
            ShowItem curShowItem = showItems[CUR_TARGET_INDEX];
            
            switch(mScrollState) {
            case SCROLL_ANIME_LEFT:
            case SCROLL_ANIME_RIGHT:
                return false;
            case SCROLL_NONE:
                boolean changePage = false;
                Rect rect = curShowItem.mRect;
                
                // Check change page or not
                if (curShowItem == null || !(curShowItem instanceof Image))
                    changePage = true;
                else{
                    if ( Math.abs(totalX/totalY) > 1 && ((totalX > CHANGE_PAGE_OFFSET && dx < 0 && rect.left >= 0)
                            || (totalX < -CHANGE_PAGE_OFFSET && dx > 0 && rect.right <= mWidth)))
                        changePage = true;
                }
                if (changePage) { // If change page
                    if (dx < 0) { // Go to left
                        if (!isFirstPage()) { // Not first page
                            scrollXOffset = 0;
                            mScrollState = SCROLL_LEFT;
                            resetSizePosition(PRE_TARGET_INDEX);
                        } else {
                            changePage = false;
                            if (mShowEdegTip) { // First page
                                mShowEdegTip = false;
                                if (mOnEdgeListener != null)
                                    mOnEdgeListener.onFirstPageEdge();
                            }
                        }
                    }
                    else { // Go to righ
                        if (!isLastPage()) { // Not last page
                            scrollXOffset = 0;
                            mScrollState = SCROLL_RIGHT;
                            resetSizePosition(NEXT_TARGET_INDEX);
                        } else {
                            changePage = false;
                            if (mShowEdegTip) { // last page
                                mShowEdegTip = false;
                                if (mOnEdgeListener != null)
                                    mOnEdgeListener.onLastPageEdge();
                            }
                        }
                    }
                }
                if (!changePage){ // Move cur image
                    int actDx = -(int)dx;
                    int actDy = -(int)dy;
                    if (rect.width() <= mWidth)
                        actDx = 0;
                    if (rect.height() <= mHeight)
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
        public boolean onScrollEnd() {
            
            if (isScale)
                return false;
            
            switch(mScrollState) {
            case SCROLL_ANIME_LEFT:
            case SCROLL_ANIME_RIGHT:
                return false;
                
            case SCROLL_LEFT:
                stopScrollXOffset = scrollXOffset;
                stopScrollYOffset = scrollYOffset;
                
                mScrollState = SCROLL_ANIME_LEFT;
                if (stopScrollXOffset > mWidth * CHANGE_PAGE_PROPORTION) { // Go to pre page
                    mToPreTimeRunner.setDuration((int)(MILLSEC_PER_DIX * (mWidth-stopScrollXOffset)));
                    mToPreTimeRunner.start();
                } else {
                    mReturnTimeRunner.setDuration((int)(MILLSEC_PER_DIX * stopScrollXOffset));
                    mReturnTimeRunner.start();
                }
                break;
            case SCROLL_RIGHT:
                stopScrollXOffset = scrollXOffset;
                stopScrollYOffset = scrollYOffset;
                
                mScrollState = SCROLL_ANIME_RIGHT;
                if (-stopScrollXOffset > mWidth * CHANGE_PAGE_PROPORTION) { // Go to next page
                    mToNextTimeRunner.setDuration((int)(MILLSEC_PER_DIX * (mWidth+stopScrollXOffset)));
                    mToNextTimeRunner.start();
                } else {
                    mReturnTimeRunner.setDuration((int)(MILLSEC_PER_DIX * -stopScrollXOffset));
                    mReturnTimeRunner.start();
                }
                break;
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
            isScale = true;
            return true;
        }

        @Override
        public boolean onScale(float focusX, float focusY, float scale) {
            if (mScrollState == SCROLL_ANIME_LEFT || mScrollState == SCROLL_ANIME_RIGHT)
                return true;
            
            if (mScrollState == SCROLL_LEFT || mScrollState == SCROLL_RIGHT) {
                scrollXOffset = 0;
                scrollYOffset = 0;
                mScrollState = SCROLL_NONE;
            }
            
            ShowItem curShowItem = showItems[CUR_TARGET_INDEX];
            if (curShowItem == null || !(curShowItem instanceof Image))
                return true;
            
            Image image = (Image)curShowItem;
            
            float newScale = image.imageScale * scale;
            if (newScale > SCALE_MAX || newScale < SCALE_MIN)
                return true;
            image.imageScale = newScale;
            mScale = newScale;
            
            Rect rect = image.mRect;
            int left = rect.left;
            int top = rect.top;
            
            left = (int)(focusX - ((focusX - left) * scale));
            top = (int)(focusY - ((focusY - top) * scale));
            
            rect.set(left, top, (int)(left + (image.width * image.imageScale)), (int)(top + (image.height * image.imageScale)));
            
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
            mShowEdegTip = true;
            
            
            //  if use onSingleTapUp, use below
            /*
             * if (mShowTapAreaTurn) {
                mShowTapAreaTurn = false;
             */
        }
        
        // *** TimeRunner *** //
        TimeRunner mToPreTimeRunner = new TimeRunner() {
            @Override
            protected void run(float interpolatedTime, int runningTime) {
                scrollXOffset = (int)(stopScrollXOffset + ((mWidth - stopScrollXOffset) * interpolatedTime));
                invalidate();
            }
        };
        
        TimeRunner mToNextTimeRunner = new TimeRunner() {
            @Override
            protected void run(float interpolatedTime, int runningTime) {
                scrollXOffset = (int)(stopScrollXOffset + ((-mWidth - stopScrollXOffset) * interpolatedTime));
                invalidate();
            }
        };
        
        TimeRunner mReturnTimeRunner = new TimeRunner() {
            @Override
            protected void run(float interpolatedTime, int runningTime) {
                scrollXOffset = (int)((1 - interpolatedTime) * stopScrollXOffset);
                invalidate();
            }
        };
    }
    
    /**
     * If side is shorter then parent's, make it in parent's center
     * If side is longer then parent's, make sure it fill parent
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
            sumXOffset = -mWidth;
            break;
        case NEXT_TARGET_INDEX:
            sumXOffset = mWidth;
            break;
        case CUR_TARGET_INDEX:
        default:
            sumXOffset = 0;
        }
        
        if (showWidth > mWidth) {
            int fixXOffset = rect.left - sumXOffset;
            if (fixXOffset > 0) {
                rect.left -= fixXOffset;
                rect.right -= fixXOffset;
            } else if ((fixXOffset = sumXOffset + mWidth - rect.right) > 0) {
                rect.left += fixXOffset;
                rect.right += fixXOffset;
            }
        } else {
            int left = sumXOffset + (mWidth - showWidth) / 2;
            rect.offsetTo(left, rect.top);
        }
        if (showHeight > mHeight) {
            int fixYOffset = rect.top - sumYOffset;
            if (fixYOffset > 0) {
                rect.top -= fixYOffset;
                rect.bottom -= fixYOffset;
            } else if ((fixYOffset = sumYOffset + mHeight - rect.bottom) > 0) {
                rect.top += fixYOffset;
                rect.bottom += fixYOffset;
            }
        } else {
            int top = sumYOffset + (mHeight - showHeight) / 2;
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
        protected int width = 1;
        protected int height = 1;
        public Rect mRect = new Rect();
        public void draw(GLCanvas canvas) {
            this.draw(canvas, 0, 0);
        }
        public abstract void draw(GLCanvas canvas, int xOffset, int yOffset);
        public abstract void recycle();
    }
    
    private class EmptyItem extends ShowItem{
        
        private ColorTexture mTexture;
        
        public EmptyItem() {
            mTexture = new ColorTexture(BACKGROUND_COLOR);
        }
        
        @Override
        public void recycle() {
            mTexture = null;
        }

        @Override
        public void draw(GLCanvas canvas, int xOffset, int yOffset) {
            mTexture.draw(canvas, xOffset, yOffset);
        }
    }
    
    
    private abstract class Image extends ShowItem{
        private UploadedTexture mTexture;
        public float imageScale = 1;
        
        /**
         * You must call init before draw
         * @param texture
         */
        public void init(UploadedTexture texture) {
            mTexture = texture;
            imageScale = 1;
        }
        
        @Override
        public void draw(GLCanvas canvas, int xOffset, int yOffset) {
            if (mTexture != null) {
                int targetIndex;
                if ((targetIndex = getTargetIndex(this)) == INVALID_ID)
                    mTexture.draw(canvas, mRect.left + xOffset, mRect.top + yOffset, mRect.width(), mRect.height());
                else {
                    
                    int leftBound = mWidth;
                    int topBound = 0;
                    int rightBound = 2 * mWidth;
                    int bottomBound = mHeight;
                    
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
                    if (left < leftBound || top < topBound || right > rightBound || bottom > bottomBound) {
                        RectF source = new RectF();
                        RectF target = new RectF();
                        
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
                        
                        target.left += xOffset;
                        target.top += yOffset;
                        target.right += xOffset;
                        target.bottom += yOffset;
                        
                        mTexture.draw(canvas, source, target);
                    } else {
                        mTexture.draw(canvas, left + xOffset, top + yOffset, right - left, bottom - top);
                    }
                }
            }
        }
    }
    
    private class BitmapImage extends Image{
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
    
    private class MovieImage extends Image{
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
    
    private class Text extends ShowItem{
        private StringTexture mTexture;
        
        public Text(String str) {
            load(str);
        }
        
        public Text(String str, float size, int color) {
            load(str, size, color);
        }
        
        public void load(String str) {
            load(str, 100, -1);
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



