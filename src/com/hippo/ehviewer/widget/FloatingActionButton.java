package com.hippo.ehviewer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.hippo.ehviewer.R;

public class FloatingActionButton extends View {

    @SuppressWarnings("unused")
    private static final String TAG = FloatingActionButton.class.getSimpleName();

    private final Paint mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDrawablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap mBitmap;
    private int mColor;
    private final int mDrawableSide;
    private final float mShadowRadius;
    private final float mShadowDx;
    private final float mShadowDy;

    private int mCircleX;
    private int mCircleY;
    private int mCircleRadius;
    private final Rect mSrc = new Rect();
    private final RectF mDst = new RectF();


    public FloatingActionButton(Context context) {
        this(context, null);
    }

    public FloatingActionButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionButton);
        mDrawableSide = a.getDimensionPixelSize(R.styleable.FloatingActionButton_fabDrawableSide, 72);
        mColor = a.getColor(R.styleable.FloatingActionButton_fabColor, Color.WHITE);
        mButtonPaint.setStyle(Paint.Style.FILL);
        mButtonPaint.setColor(mColor);
        mShadowRadius = a.getDimension(R.styleable.FloatingActionButton_fabShadowRadius, 10.0f);
        mShadowDx = a.getDimension(R.styleable.FloatingActionButton_fabShadowDx, 0.0f);
        mShadowDy = a.getDimension(R.styleable.FloatingActionButton_fabShadowDy, 3.0f);
        int shadowColor = a.getColor(R.styleable.FloatingActionButton_fabShadowColor, 0x8a000000);
        mButtonPaint.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, shadowColor);
        Drawable drawable = a.getDrawable(R.styleable.FloatingActionButton_fabDrawable);
        if (drawable != null && drawable instanceof BitmapDrawable)
            mBitmap = ((BitmapDrawable) drawable).getBitmap();
        a.recycle();

        setWillNotDraw(false);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setColor(int color) {
        mColor = color;
        mButtonPaint.setColor(mColor);
        invalidate();
    }

    public void setDrawable(Drawable drawable) {
        mBitmap = ((BitmapDrawable) drawable).getBitmap();
        updateValue();
        invalidate();
    }

    public static int getDefaultSize(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
        case MeasureSpec.UNSPECIFIED:
        case MeasureSpec.AT_MOST:
            result = size;
            break;
        case MeasureSpec.EXACTLY:
            result = specSize;
            break;
        }
        return result;
    }

    private void updateValue() {
        float half = Math.min(getWidth(), getHeight()) / 2;
        mCircleX = (int) (half - mShadowDx);
        mCircleY = (int) (half - mShadowDy);
        mCircleRadius = (int) (half - mShadowRadius);
        float halfDrawableSide = mDrawableSide / 2.0f;
        if (mBitmap != null) {
            mSrc.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            mDst.set(mCircleX - halfDrawableSide, mCircleY - halfDrawableSide, mCircleX + halfDrawableSide, mCircleY + halfDrawableSide);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateValue();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(mCircleX, mCircleY, mCircleRadius, mButtonPaint);
        if (null != mBitmap)
            canvas.drawBitmap(mBitmap, mSrc, mDst, mDrawablePaint);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        int color;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            color = mColor;
        } else {
            color = darkenColor(mColor);
        }
        mButtonPaint.setColor(color);
        invalidate();
        return super.onTouchEvent(event);
    }

    public static int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }
}