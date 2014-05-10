package com.hippo.ehviewer.widget;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.R.styleable;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class CheckImage extends ImageView implements OnClickListener{
    private static String TAG = "CheckImage";
    
    private Bitmap mask;
    private boolean mPressed = false;
    
    public CheckImage(Context context) {
        super(context);
    }
    
    public CheckImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs,  
                R.styleable.CheckImage);
        mask = ((BitmapDrawable)ta.getDrawable(R.styleable.CheckImage_mask)).getBitmap();
        ta.recycle();
        // Set listener
        setOnClickListener(this);
    }
    
    public CheckImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray ta = context.obtainStyledAttributes(attrs,  
                R.styleable.CheckImage);
        mask = ((BitmapDrawable)ta.getDrawable(R.styleable.CheckImage_mask)).getBitmap();
        ta.recycle();
        // Set listener
        setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        mPressed = !mPressed;
        invalidate();
    }
    
    @Override 
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPressed)
            canvas.drawBitmap(mask, null, canvas.getClipBounds(), null);
    }
    
    public void pressed() {
        mPressed = true;
        invalidate();
    }
    
    public void unpressed() {
        mPressed = false;
        invalidate();
    }
    
    public boolean isPressed() {
        return mPressed;
    }
}
