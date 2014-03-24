package com.hippo.ehviewer.widget;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
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
    
    public AutoWrapLayout(Context context) {
        super(context);
    }
    public AutoWrapLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public AutoWrapLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
        
        
        Log.d(TAG, "widthMode = " + widthMode);
        Log.d(TAG, "heightMode = " + heightMode);
        Log.d(TAG, "maxWidth = " + maxWidth);
        Log.d(TAG, "maxHeight = " + maxHeight);
        
        Log.d(TAG, "getChildCount() = " + getChildCount());
        
        if (widthMode == MeasureSpec.UNSPECIFIED)
            maxWidth = Integer.MAX_VALUE;
        if (heightMode == MeasureSpec.UNSPECIFIED)
            maxHeight = Integer.MAX_VALUE;
        
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        
        int maxRightBound = maxWidth - paddingRight;
        int maxBottomBound = maxWidth - paddingBottom;
        
        int left;
        int top;
        int right;
        int bottom;
        int rightBound = paddingLeft;
        int maxRightNoPadding = rightBound;
        int bottomBound;
        int lastMaxBottom = paddingTop;
        int maxBottom = lastMaxBottom; // maxBottom is maxBottomNoPadding
        //int maxBottomNoPadding = maxBottom;
        int childWidth;
        int childHeight;
        
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
                // If child can't show in parent
                if (maxBottom >= maxBottomBound)
                    break;
                left = paddingLeft + lp.leftMargin;
                right = left + childWidth;
                rightBound = right + lp.rightMargin;
                
                lastMaxBottom = maxBottom;
                top = lastMaxBottom + lp.topMargin;
                bottom = top + childHeight;
                bottomBound = bottom + lp.bottomMargin;
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
        
        int measuredWidth;
        int measuredHeight;
        
        if (widthMode == MeasureSpec.EXACTLY)
            measuredWidth = maxWidth;
        else {
            measuredWidth = maxRightNoPadding + paddingRight;
            if (widthMode == MeasureSpec.AT_MOST)
                measuredWidth =  measuredWidth > maxWidth ? maxWidth : measuredWidth;
        }
        if (heightMode == MeasureSpec.EXACTLY)
            measuredHeight = maxHeight;
        else {
            measuredHeight = maxBottom + paddingBottom;
            if (heightMode == MeasureSpec.AT_MOST)
                measuredHeight = measuredHeight > maxHeight ? maxHeight : measuredHeight;
        }
        
        Log.d(TAG, "measuredWidth = " + measuredWidth);
        Log.d(TAG, "measuredHeight = " + measuredHeight);
        
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
