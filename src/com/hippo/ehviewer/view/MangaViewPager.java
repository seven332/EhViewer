package com.hippo.ehviewer.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public class MangaViewPager extends ViewPager {
    private GestureDetector mGestureDetector;
    public float distanceX;
    public MangaViewPager(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(this.getContext()
                , new OnScrollListener());
    }
    
    public MangaViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(this.getContext()
                , new OnScrollListener());
    }
    
    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof MangaImage) {
            return ((MangaImage) v).canScroll(dx);
        } else {
            return super.canScroll(v, checkV, dx, x, y);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean re = super.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        return re;
    }
    
    class OnScrollListener extends SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            MangaViewPager.this.distanceX = distanceX;
            return true;
        }
    }
}
