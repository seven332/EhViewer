package com.hippo.ehviewer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.AbsSeekBar;
import android.widget.RatingBar;

public class ProgressiveRatingBar extends RatingBar {
    
    private OnDrawListener mListener;
    
    public ProgressiveRatingBar(Context context) {
        super(context);
    }
    public ProgressiveRatingBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ProgressiveRatingBar(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    @SuppressLint("WrongCall")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (mListener != null)
            mListener.onDraw(getRating());
    }
    
    public void setOnDrawListener(OnDrawListener l) {
        mListener = l;
    }
    
    public interface OnDrawListener {
        void onDraw(float rating);
    }
}
