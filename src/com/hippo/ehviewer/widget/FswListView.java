package com.hippo.ehviewer.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ListView;

public class FswListView extends ListView {
    
    private OnfitSystemWindowsListener mListener;
    
    public FswListView(Context context) {
        super(context);
    }
    public FswListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public FswListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setOnfitSystemWindowsListener(OnfitSystemWindowsListener l) {
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
