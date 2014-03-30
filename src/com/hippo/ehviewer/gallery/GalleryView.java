package com.hippo.ehviewer.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.gallery.glrenderer.BitmapTexture;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.StringTexture;
import com.hippo.ehviewer.gallery.ui.GLView;
import com.hippo.ehviewer.gallery.ui.GestureRecognizer;
import com.hippo.ehviewer.gallery.ui.GestureRecognizer.Listener;
import com.hippo.ehviewer.gallery.util.GalleryUtils;
import com.hippo.ehviewer.util.TimeRunner;

public class GalleryView extends GLView {
    
    private static final String TAG = "ImagesView";
    
    public static final int INVALID_ID = -1;
    
    private static final int STATE_FIRST = 0x1;
    private static final int STATE_LAST = 0x2;
    private static final int STATE_NONE = 0x0;
    
    private static final int ORGIN = 0x0;
    private static final int FIT_WIDTH = 0x1;
    private static final int FIT_HEIGHT = 0x2;
    private static final int FIT = 0x3;
    private static final int FIXED = 0x4;
    
    private static final int TOP_LEFT = 0x0;
    private static final int TOP_RIGHT = 0x1;
    private static final int BOTTOM_LEFT = 0x2;
    private static final int BOTTOM_RIGHT = 0x3;
    private static final int CENTER = 0x4;
    
    private static final int SCROLL_NONE = 0x0;
    private static final int SCROLL_LEFT = 0x1;
    private static final int SCROLL_RIGHT = 0x2;
    private static final int SCROLL_ANIME_LEFT = 0x3;
    private static final int SCROLL_ANIME_RIGHT = 0x4;
    
    private static final int PRE_TARGET_INDEX = 0;
    private static final int CUR_TARGET_INDEX = 1;
    private static final int NEXT_TARGET_INDEX = 2;
    private static final int TARGET_INDEX_SIZE = 3;
    
    private static final float MILLSEC_PER_DIX = 0.2f;
    private static final float CHANGE_PAGE_PROPORTION = 0.1f;
    
    private final GestureRecognizer mGestureRecognizer;
    private Context mContext;
    
    private ImageSet mImageSet;
    private int mState;
    // scale and scroll only can choose one
    private boolean isScale = false;
    private int scaleMode = FIXED;
    private int startMode = TOP_LEFT;
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
    
    private static final float SCALE_MIN = 1/4.0f;
    private static final float SCALE_MAX = 4;
    
    // TODO 开页方向
    
    // at most keep three item
    private ShowItem[] showItems;
    private int mCurIndex;
    private int mSize;
    private boolean mInit = false;
    
    public GalleryView(Context context, ImageSet imageSet, int startIndex) {
        mContext = context;
        mImageSet = imageSet;
        mSize = imageSet.getSize();
        mCurIndex = startIndex;
        
        // adjust mCurIndex
        if (mCurIndex < 0 || mCurIndex >= mSize)
            mCurIndex = 0;
        
        setState();
        
        mGestureRecognizer = new GestureRecognizer(mContext, new MyGestureListener());
        setBackgroundColor(GalleryUtils.intColorToFloatARGBArray(Color.GRAY));
        
        showItems = new ShowItem[TARGET_INDEX_SIZE];
    }
    
    private void setState() {
        mState = STATE_NONE;
        if (mCurIndex == 0)
            mState |= STATE_FIRST;
        if (mCurIndex == mSize - 1)
            mState |= STATE_LAST;
    }
    
    @Override
    protected void render(GLCanvas canvas) {
        super.render(canvas);
        
        ShowItem item;
        switch (mScrollState) {
        case SCROLL_NONE:
            item = showItems[CUR_TARGET_INDEX];
            if (item != null) {
                item.draw(canvas);
            }
            break;
            
        case SCROLL_LEFT:
        case SCROLL_ANIME_LEFT:
            item = showItems[PRE_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            item = showItems[CUR_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            break;
            
        case SCROLL_RIGHT:
        case SCROLL_ANIME_RIGHT:
            item = showItems[CUR_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            item = showItems[NEXT_TARGET_INDEX];
            if (item != null)
                item.draw(canvas, scrollXOffset, scrollYOffset);
            break;
        }
    }
    
    private void resetSizePosition() {
        
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
            
            // TODO 检查缩放尺寸是否超界
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
            
            adjustPosition(image);
        }
        
        invalidate();
    }
    
    @Override
    protected void onLayout(
            boolean changeSize, int left, int top, int right, int bottom) {
        mWidth = right - left;
        mHeight = bottom - top;
        
        if (!mInit) {
            mInit = true;
            loadImage(mCurIndex, 1);
            if ((mState & STATE_LAST) == 0)
                loadImage(mCurIndex + 1, 2);
            if ((mState & STATE_FIRST) == 0)
                loadImage(mCurIndex - 1, 0);
        } else {
            resetSizePosition();
        }
        invalidate();
    }
    
    private void loadImage(int index, int targetIndex) {
        int state = mImageSet.getImage(index, new DecodeImageListener());
        ShowItem showItem = showItems[targetIndex];
        if (showItem != null)
            showItem.recycle();
        if (state == ImageSet.STATE_LOADED)
            showItems[targetIndex] = new Image();
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
            return "错误的错误代码";
        }
    }
    
    class DecodeImageListener implements ImageSet.OnDecodeOverListener {
        @Override
        public void onDecodeOver(Bitmap bmp, int index) {
            int targetIndex = index - mCurIndex + 1;
            ShowItem showItem;
            Image image;
            
            if (targetIndex < 0 || targetIndex > 2
                    || !((showItem = showItems[targetIndex]) instanceof Image)
                    || (image = (Image)showItem).isLoaded()) {// If it do not need any more
                if (bmp != null)
                    bmp.recycle();
            } else {
                if (bmp == null) {
                    showItems[targetIndex] = new Text("读取图片错误");
                } else {
                    image.load(bmp);
                }
                resetSizePosition(targetIndex);
            }
        }
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
                    loadImage(mCurIndex-1, PRE_TARGET_INDEX);
                    
                    mScrollState = SCROLL_NONE;
                    invalidate();
                }
            });
            mToNextTimeRunner.setOnTimerListener(new TimeRunner.OnTimeListener() {
                @Override
                public void onStart() {}
                @Override
                public void onEnd() {
                    scrollXOffset = 0;
                    
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
                    loadImage(mCurIndex+1, NEXT_TARGET_INDEX);
                    
                    mScrollState = SCROLL_NONE;
                    invalidate();
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
        
        @Override
        public boolean onSingleTapUp(float x, float y) {
            // TODO Auto-generated method stub
            return true;
        }

        @Override
        public boolean onDoubleTap(float x, float y) {
            // TODO Auto-generated method stub
            return true;
        }
        
        @Override
        public boolean onScrollBegin(float dx, float dy, float totalX,
                float totalY) {
            return this.onScroll(dx, dy, totalX, totalY);
        }
        
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
                //Image image = null;
                boolean changePage = false;
                Rect rect = null;
                
                // Check change page or not
                if (curShowItem == null || !(curShowItem instanceof Image))
                    changePage = true;
                else{
                    rect = curShowItem.mRect;
                    if ((dx < 0 && rect.left >= 0) || (dx > 0 && rect.right <= mWidth))
                        changePage = true;
                }
                
                if (changePage) { // If change page
                    if (dx < 0) { // Go to left
                        if ((mState & STATE_FIRST) == 0) { // Not first page
                            mScrollState = SCROLL_LEFT;
                            resetSizePosition(PRE_TARGET_INDEX);
                        } else if (mShowEdegTip) { // First page
                            mShowEdegTip = false;
                            Log.d(TAG, "First page");
                        }
                    }
                    else { // Go to righ
                        if ((mState & STATE_LAST) == 0) { // Not last page
                            mScrollState = SCROLL_RIGHT;
                            resetSizePosition(NEXT_TARGET_INDEX);
                        } else if (mShowEdegTip) { // last page
                            mShowEdegTip = false;
                            Log.d(TAG, "Last page");
                        }
                    }
                } else { // Move cur image
                    int actDx = -(int)dx;
                    int actDy = -(int)dy;
                    if (rect.width() <= mWidth)
                        actDx = 0;
                    if (rect.height() <= mHeight)
                        actDy = 0;
                    rect.offset(actDx, actDy);
                    
                    // Fix position
                    adjustPosition((Image)curShowItem);
                }
                break;
                
            case SCROLL_LEFT:
            case SCROLL_RIGHT:
                scrollXOffset -= dx;
                // adjust
                if ((mState & STATE_FIRST) != 0 && scrollXOffset > 0)
                    scrollXOffset = 0;
                if ((mState & STATE_LAST) != 0 && scrollXOffset < 0)
                    scrollXOffset = 0;
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
            // TODO Auto-generated method stub
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
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onUp() {
            mShowEdegTip = true;
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
    
    public void adjustPosition(ShowItem showItem) {
        Rect rect = showItem.mRect;
        int showWidth = rect.width();
        int showHeight = rect.height();
        
        int sumXOffset;
        int sumYOffset = 0; // TODO
        
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
    
    private class Image extends ShowItem{
        private BitmapTexture mTexture;
        private Bitmap mContextBmp;
        public float imageScale = 1;
        
        public void load(Bitmap bmp) {
            if (mTexture != null)
                recycle();
            mTexture = new BitmapTexture(bmp);
            mContextBmp = bmp;
            width = bmp.getWidth();
            height = bmp.getHeight();
            imageScale = 1;
        }
        
        public boolean isLoaded() {
            return mTexture != null;
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
    
    private class Text extends ShowItem{
        private StringTexture mTexture;
        
        public Text(String str) {
            load(str);
        }
        
        public void load(String str) {
            if (mTexture != null)
                recycle();
            mTexture = StringTexture.newInstance(str, 100, 0xffffffff);
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



