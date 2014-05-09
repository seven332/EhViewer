package com.hippo.ehviewer.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class FswView extends View {
    private OnFitSystemWindowsListener mListener;
    
    public FswView(Context context) {
        super(context);
    }
    public FswView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public FswView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setOnFitSystemWindowsListener(OnFitSystemWindowsListener l) {
        mListener = l;
    }
    
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        boolean re = super.fitSystemWindows(insets);
        if (mListener != null)
            mListener.onfitSystemWindows(getPaddingLeft(), getPaddingTop(),
                    getPaddingRight(), getPaddingBottom());
        return re;
    }
}
