package com.hippo.ehviewer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class OlScrollView extends ScrollView {
    private OnLayoutListener mListener;
    
    public OlScrollView(Context context) {
        super(context);
    }
    public OlScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public OlScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @SuppressLint("WrongCall")
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mListener != null)
            mListener.onLayout();
    }
    
    public void setOnLayoutListener(OnLayoutListener l) {
        mListener = l;
    }
}
