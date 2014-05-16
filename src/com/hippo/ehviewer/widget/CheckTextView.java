package com.hippo.ehviewer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

public class CheckTextView extends TextView implements OnClickListener{
    @SuppressWarnings("unused")
    private static String TAG = "CheckImage";
    
    private static int MASK = 0xbb000000;
    
    private Paint mPaint;
    
    private boolean mChecked = false;
    
    public CheckTextView(Context context) {
        super(context);
        init();
    }
    
    public CheckTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public CheckTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    private void init() {
        mPaint = new Paint();
        mPaint.setColor(MASK);
        setOnClickListener(this);
    }
    
    @Override
    public void onClick(View v) {
        mChecked = !mChecked;
        invalidate();
    }
    
    @SuppressLint("DrawAllocation")
    @Override 
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mChecked)
            canvas.drawRect(canvas.getClipBounds(), mPaint);
    }
    
    public void setChecked(boolean checked) {
        mChecked = checked;
        invalidate();
    }
    
    public boolean isPressed() {
        return mChecked;
    }
}
