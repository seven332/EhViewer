package com.hippo.ehviewer.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

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

    static int adjust(int measureSpec, int delta) {
        return MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(measureSpec + delta),  MeasureSpec.getMode(measureSpec));
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightSpec) {
        final int mWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        final int childCount = getChildCount();
        
        int left;
        int top;
        int right;
        int bottom;
        int rightBound = 0;
        int bottomBound;
        int lastMaxBottom = 0;
        int maxBottom = lastMaxBottom;
        int width;
        int height;
        
        rectList.clear();
        
        for(int i = 0; i < childCount; i++){
            final View child = this.getChildAt(i);
            child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            final AutoWrapLayout.LayoutParams lp =
                    (AutoWrapLayout.LayoutParams)child.getLayoutParams();
            width = child.getMeasuredWidth();
            height = child.getMeasuredHeight();
            
            left = rightBound + lp.leftMargin;
            right = left + width;
            rightBound = right + lp.rightMargin;
            //if it can't drawing on a same line , skip to next line
            if(rightBound > mWidth) {
                left = 0 + lp.leftMargin;
                right = left + width;
                rightBound = right + lp.rightMargin;
                
                lastMaxBottom = maxBottom;
                
                top = lastMaxBottom + lp.topMargin;
                bottom = top + height;
                bottomBound = bottom + lp.bottomMargin;
                
                maxBottom = bottomBound;
            } else {
                top = lastMaxBottom + lp.topMargin;
                bottom = top + height;
                bottomBound = bottom + lp.bottomMargin;
            }
            // Update max bottom
            if (bottomBound > maxBottom)
                maxBottom = bottomBound;
            Rect rect = new Rect();
            rect.left = left;
            rect.top = top;
            rect.right = right;
            rect.bottom = bottom;
            rectList.add(rect);
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), maxBottom + getPaddingTop()  + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount() > rectList.size() ? rectList.size() : getChildCount();
        
        for(int i = 0; i < count; i++){
            final View child = this.getChildAt(i);
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
