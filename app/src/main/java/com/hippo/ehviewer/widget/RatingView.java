package com.hippo.ehviewer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.hippo.ehviewer.R;
import com.hippo.yorozuya.ViewUtils;

public class RatingView extends View {

    private Drawable mStarDrawable;
    private Drawable mStarHalfDrawable;
    private Drawable mStarOutlineDrawable;

    private float mStarSize;
    private float mStarInterval;
    private int mStarCount;
    private boolean mIndicator;
    private float mMax;
    private float mRating;

    private int mShownStarCount;
    private int mShownStarHalfCount;
    private int mShownStarOutlineCount;

    private OnRatingChangeListener mOnRatingChangeListener;

    public RatingView(Context context) {
        super(context);
        init(context, null);
    }

    public RatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mStarDrawable = context.getResources().getDrawable(R.drawable.ic_star_theme_accent);
        mStarHalfDrawable = context.getResources().getDrawable(R.drawable.ic_star_half_theme_accent);
        mStarOutlineDrawable = context.getResources().getDrawable(R.drawable.ic_star_outline_theme_accent);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RatingView);
        mStarSize = a.getDimension(R.styleable.RatingView_starSize, 10);
        mStarInterval = a.getDimension(R.styleable.RatingView_starInterval, 10);
        mStarCount = a.getInt(R.styleable.RatingView_starCount, 5);
        mIndicator = a.getBoolean(R.styleable.RatingView_isIndicator, true);
        mMax = a.getFloat(R.styleable.RatingView_max, 10f);
        mRating = a.getFloat(R.styleable.RatingView_rating, 0f);

        a.recycle();

        updateBounds();
        updateShownCount();
    }

    private void updateBounds() {
        int starSize = (int) mStarSize;
        mStarDrawable.setBounds(0, 0, starSize, starSize);
        mStarHalfDrawable.setBounds(0, 0, starSize, starSize);
        mStarOutlineDrawable.setBounds(0, 0, starSize, starSize);
    }

    private void updateShownCount() {
        float step = mMax / mStarCount / 2;
        int shown = Math.round(mRating / step);
        if (shown >= mStarCount * 2) {
            mShownStarCount = mStarCount;
            mShownStarHalfCount = 0;
            mShownStarOutlineCount = 0;
        } else {
            mShownStarCount = shown / 2;
            mShownStarHalfCount = shown % 2;
            mShownStarOutlineCount = mStarCount - mShownStarCount - mShownStarHalfCount;
        }
    }

    public void setOnRatingChangeListener(OnRatingChangeListener listener) {
        mOnRatingChangeListener = listener;
    }

    public void setIndicator(boolean indicator) {
        mIndicator = indicator;
    }

    public boolean isIndicator() {
        return mIndicator;
    }

    public void setRating(float rating) {
        if (mRating != rating) {
            float oldRating = mRating;
            mRating = rating;
            updateShownCount();
            invalidate();
            if (mOnRatingChangeListener != null) {
                mOnRatingChangeListener.onRatingChange(oldRating, rating, false, true);
            }
        }
    }

    public float getRating() {
        return mRating;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return Math.max(super.getSuggestedMinimumWidth(),
                (int) ((mStarSize * mStarCount) + (mStarInterval * (mStarCount - 1))));
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return Math.max(super.getSuggestedMinimumHeight(), (int) mStarSize);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(ViewUtils.getSuitableSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                ViewUtils.getSuitableSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int saved = canvas.save();

        float moveStep = mStarSize + mStarInterval;
        int count = mShownStarCount;
        while (count-- > 0) {
            mStarDrawable.draw(canvas);
            canvas.translate(moveStep, 0);
        }
        count = mShownStarHalfCount;
        while (count-- > 0) {
            mStarHalfDrawable.draw(canvas);
            canvas.translate(moveStep, 0);
        }
        count = mShownStarOutlineCount;
        while (count-- > 0) {
            mStarOutlineDrawable.draw(canvas);
            canvas.translate(moveStep, 0);
        }

        canvas.restoreToCount(saved);
    }

    private float getRatingByX(float x) {
        float step = mStarSize + mStarInterval;
        float sum = x + mStarInterval;
        float ratingStep = mMax / mStarCount / 2;
        int count = (int) (sum / step) * 2;
        float remain = sum - (count / 2 * step);
        if (remain > mStarInterval + (mStarSize / 2)) {
            count += 2;
        } else if (remain > mStarInterval) {
            count += 1;
        }
        count = Math.min(mStarCount * 2, count);
        return ratingStep * count;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!mIndicator) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    float rating = getRatingByX(event.getX());
                    if (mRating != rating) {
                        float oldRating = mRating;
                        mRating = rating;
                        updateShownCount();
                        invalidate();
                        if (mOnRatingChangeListener != null) {
                            mOnRatingChangeListener.onRatingChange(oldRating, rating, true, false);
                        }
                    }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mOnRatingChangeListener != null) {
                        mOnRatingChangeListener.onRatingChange(mRating, mRating, true, true);
                    }
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    public interface OnRatingChangeListener {
        void onRatingChange(float oldRating, float newRating, boolean byUser, boolean confirm);
    }
}
