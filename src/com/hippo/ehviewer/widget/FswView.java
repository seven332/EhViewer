package com.hippo.ehviewer.widget;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class FswView extends View {
    private List<OnFitSystemWindowsListener> mListeners;
    
    public FswView(Context context) {
        super(context);
        init();
    }
    public FswView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public FswView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    public void init() {
        mListeners = new LinkedList<OnFitSystemWindowsListener>();
        setVisibility(View.GONE);
    }
    
    public void addOnFitSystemWindowsListener(OnFitSystemWindowsListener l) {
        mListeners.add(l);
    }
    
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        boolean re = super.fitSystemWindows(insets);
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        for (OnFitSystemWindowsListener l : mListeners)
            l.onfitSystemWindows(paddingLeft, paddingTop,
                    paddingRight, paddingBottom);
        return re;
    }
}
