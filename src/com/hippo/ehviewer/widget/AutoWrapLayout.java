package com.hippo.ehviewer.widget;

import java.util.ArrayList;
import java.util.List;

import com.hippo.ehviewer.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import com.hippo.ehviewer.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * A ViewGroup that can layout views in line and auto wrap
 * 
 * @author Hippo
 *
 */
public class AutoWrapLayout extends ViewGroup {
    @SuppressWarnings("unused")
    private final static String TAG = "AutoWrapLayout";
    private List<Rect> rectList = new ArrayList<Rect>();
    
    private BaseLine mBaseLine;
    
    private static final BaseLine[] sBaseLineArray = { BaseLine.TOP,
        BaseLine.CENTER, BaseLine.BOTTOM };
    
    
    public enum BaseLine {
        TOP(0),
        CENTER(1),
        BOTTOM(2);

        BaseLine(int ni) {
            nativeInt = ni;
        }

        final int nativeInt;
    }
    
    public AutoWrapLayout(Context context) {
        super(context);
    }
    public AutoWrapLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public AutoWrapLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.AutoWrapLayout, defStyle, 0);
        
        int index = a.getInt(R.styleable.AutoWrapLayout_baseLine, -1);
        if (index >= 0)
            setBaseLine(sBaseLineArray[index]);
        
        a.recycle();
    }
    
    public void setBaseLine(BaseLine baseLine) {
        if (baseLine == null)
            return;

        if (mBaseLine != baseLine) {
            mBaseLine = baseLine;

            requestLayout();
            invalidate();
        }
    }
    
    public BaseLine getScaleType() {
        return mBaseLine;
    }
    
    private void adjustBaseLine(int lineHeight, int startIndex, int endIndex) {
        if (mBaseLine == BaseLine.TOP)
            return;
        
        for (int index = startIndex; index < endIndex; index++) {
            final View child = getChildAt(index);
            final AutoWrapLayout.LayoutParams lp =
                    (AutoWrapLayout.LayoutParams)child.getLayoutParams();
            Rect rect = rectList.get(index);
            int offsetRaw = lineHeight - rect.height() - lp.topMargin - lp.bottomMargin;
            if (mBaseLine == BaseLine.CENTER)
                rect.offset(0, offsetRaw/2);
            else if (mBaseLine == BaseLine.BOTTOM)
                rect.offset(0, offsetRaw);
        }
    }
    
    
    // TODO Take vertical mode
    /**
     * each row or line at least show one child
     * 
     * horizontal only show child can show or partly show in parent
     */
    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
        
        if (widthMode == MeasureSpec.UNSPECIFIED)
            maxWidth = Integer.MAX_VALUE;
        if (heightMode == MeasureSpec.UNSPECIFIED)
            maxHeight = Integer.MAX_VALUE;
        
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        
        int maxRightBound = maxWidth - paddingRight;
        int maxBottomBound = maxHeight - paddingBottom;
        
        int left;
        int top;
        int right;
        int bottom;
        int rightBound = paddingLeft;
        int maxRightNoPadding = rightBound;
        int bottomBound;
        int lastMaxBottom = paddingTop;
        int maxBottom = lastMaxBottom;
        int childWidth;
        int childHeight;
        
        int lineStartIndex = 0;
        int lineEndIndex = 0; // endIndex + 1
        
        rectList.clear();
        int childCount = getChildCount();
        for (int index = 0; index < childCount; index++) {
            final View child = getChildAt(index);
            child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            if (child.getVisibility() == View.GONE)
                continue;
            final AutoWrapLayout.LayoutParams lp =
                    (AutoWrapLayout.LayoutParams)child.getLayoutParams();
            childWidth = child.getMeasuredWidth();
            childHeight = child.getMeasuredHeight();
            
            left = rightBound + lp.leftMargin;
            right = left + childWidth;
            rightBound = right + lp.rightMargin;
            if (rightBound > maxRightBound) { // Go to next row
                lineEndIndex = index;
                // Adjust child position base on baseline
                adjustBaseLine(maxBottom - lastMaxBottom, lineStartIndex, lineEndIndex);
                
                // If child can't show in parent begin this line
                if (maxBottom >= maxBottomBound)
                    break;
                left = paddingLeft + lp.leftMargin;
                right = left + childWidth;
                rightBound = right + lp.rightMargin;
                
                lastMaxBottom = maxBottom;
                top = lastMaxBottom + lp.topMargin;
                bottom = top + childHeight;
                bottomBound = bottom + lp.bottomMargin;
                
                lineStartIndex = index;
            } else {
                top = lastMaxBottom + lp.topMargin;
                bottom = top + childHeight;
                bottomBound = bottom + lp.bottomMargin;
            }
            // Update max
            if (rightBound > maxRightNoPadding)
                maxRightNoPadding = rightBound;
            if (bottomBound > maxBottom)
                maxBottom = bottomBound;
            Rect rect = new Rect();
            rect.left = left;
            rect.top = top;
            rect.right = right;
            rect.bottom = bottom;
            rectList.add(rect);
        }
        
        // Handle last line baseline
        adjustBaseLine(maxBottom - lastMaxBottom, lineStartIndex, rectList.size());
        
        int measuredWidth;
        int measuredHeight;
        
        if (widthMode == MeasureSpec.EXACTLY)
            measuredWidth = maxWidth;
        else
            measuredWidth = maxRightNoPadding + paddingRight;
        if (heightMode == MeasureSpec.EXACTLY)
            measuredHeight = maxHeight;
        else {
            measuredHeight = maxBottom + paddingBottom;
            if (heightMode == MeasureSpec.AT_MOST)
                measuredHeight = measuredHeight > maxHeight ? maxHeight : measuredHeight;
        }
        
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = rectList.size();
        for(int i = 0; i < count; i++){
            final View child = this.getChildAt(i);
            if (child.getVisibility() == View.GONE)
                continue;
            Rect rect = rectList.get(i);
            child.layout(rect.left, rect.top, rect.right, rect.bottom);
        }
    }
    
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new AutoWrapLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new AutoWrapLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }
    
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public LayoutParams() {
            this(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }
        public LayoutParams(int width, int height) {
            super(width, height);
        }
        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }
}
