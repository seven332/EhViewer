package com.hippo.ehviewer.drawable;

import java.util.ArrayList;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

// Get from android.graphics.drawable.MaterialProgressDrawable
public class MaterialProgressDrawable extends Drawable implements Animatable {
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final TimeInterpolator END_CURVE_INTERPOLATOR = new EndCurveInterpolator();
    private static final TimeInterpolator START_CURVE_INTERPOLATOR = new StartCurveInterpolator();

    private static final long ANIMATION_DURATION = 1333;

    private static final int NUM_POINTS = 5;

    private final ArrayList<Animator> mAnimators = new ArrayList<Animator>();

    private final Ring mRing;

    private MaterialProgressState mState;

    private float mRotation;

    private boolean mMutated;

    public MaterialProgressDrawable() {
        this(new MaterialProgressState(null));
    }

    public MaterialProgressDrawable(int width, int height, int color, float strokeWidth, float innerRadius) {
        this(new MaterialProgressState(width, height, color, strokeWidth, innerRadius));
    }

    private MaterialProgressDrawable(MaterialProgressState state) {
        mState = state;
        mRing = new Ring(mCallback);
        mMutated = false;

        initializeFromState();
        setupAnimators();
    }

    private void initializeFromState() {
        MaterialProgressState state = mState;

        Ring ring = mRing;
        ring.setStrokeWidth(state.mStrokeWidth);

        int color = state.mColor.getColorForState(getState(), 0);
        ring.setColor(color);

        float minEdge = Math.min(state.mWidth, state.mHeight);
        if ((state.mInnerRadius <= 0.0F) || (minEdge < 0.0F)) {
            ring.setInsets((int) Math.ceil(state.mStrokeWidth / 2.0F));
        } else {
            float insets = minEdge / 2.0F - state.mInnerRadius;
            ring.setInsets(insets);
        }
    }

    @Override
    public Drawable mutate() {
        if ((!mMutated) && (super.mutate() == this)) {
            mState = new MaterialProgressState(mState);
            mMutated = true;
        }
        return this;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = super.onStateChange(state);

        int color = mState.mColor.getColorForState(state, 0);
        if (color != mRing.getColor()) {
            mRing.setColor(color);
            changed = true;
        }

        return changed;
    }

    @Override
    public boolean isStateful() {
        return (super.isStateful()) || (mState.mColor.isStateful());
    }

    @Override
    public int getIntrinsicHeight() {
        return mState.mHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return mState.mWidth;
    }

    @Override
    public void draw(Canvas c) {
        Rect bounds = getBounds();
        int saveCount = c.save();
        c.rotate(mRotation, bounds.exactCenterX(), bounds.exactCenterY());
        mRing.draw(c, bounds);
        c.restoreToCount(saveCount);
    }

    public void setColor(int color) {
        mState.mColor = ColorStateList.valueOf(color);
        mRing.setColor(color);
    }

    @Override
    public void setAlpha(int alpha) {
        mRing.setAlpha(alpha);
    }

    @Override
    public int getAlpha() {
        return mRing.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mRing.setColorFilter(colorFilter);
    }

    private void setRotation(float rotation) {
        mRotation = rotation;
        invalidateSelf();
    }

    private float getRotation() {
        return mRotation;
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (visible) {
            if ((changed) || (restart)) {
                start();
            }
        } else {
            stop();
        }
        return changed;
    }

    @Override
    public boolean isRunning() {
        ArrayList<Animator> animators = mAnimators;
        int N = animators.size();
        for (int i = 0; i < N; i++) {
            Animator animator = animators.get(i);
            if (animator.isRunning()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void start() {
        ArrayList<Animator> animators = mAnimators;
        int N = animators.size();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (int i = 0; i < N; i++) {
                Animator animator = animators.get(i);
                if (animator.isPaused())
                    animator.resume();
                else if (!animator.isRunning())
                    animator.start();
            }
        } else {
            for (int i = 0; i < N; i++) {
                Animator animator = animators.get(i);
                if (!animator.isRunning())
                    animator.start();
            }
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void stop() {
        ArrayList<Animator> animators = mAnimators;
        int N = animators.size();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (int i = 0; i < N; i++) {
                animators.get(i).pause();
            }
        } else {
            for (int i = 0; i < N; i++) {
                animators.get(i).cancel();
            }
        }
    }

    private void setupAnimators() {
        Ring ring = mRing;

        ObjectAnimator endTrim = ObjectAnimator.ofFloat(ring, "endTrim",
                new float[] { 0.0F, 0.75F });
        endTrim.setDuration(ANIMATION_DURATION);
        endTrim.setInterpolator(START_CURVE_INTERPOLATOR);
        endTrim.setRepeatCount(-1);
        endTrim.setRepeatMode(1);

        ObjectAnimator startTrim = ObjectAnimator.ofFloat(ring, "startTrim",
                new float[] { 0.0F, 0.75F });
        startTrim.setDuration(ANIMATION_DURATION);
        startTrim.setInterpolator(END_CURVE_INTERPOLATOR);
        startTrim.setRepeatCount(-1);
        startTrim.setRepeatMode(1);

        ObjectAnimator rotation = ObjectAnimator.ofFloat(ring, "rotation",
                new float[] { 0.0F, 0.25F });
        rotation.setDuration(ANIMATION_DURATION);
        rotation.setInterpolator(LINEAR_INTERPOLATOR);
        rotation.setRepeatCount(-1);
        rotation.setRepeatMode(1);

        ObjectAnimator groupRotation = ObjectAnimator.ofFloat(this, "rotation",
                new float[] { 0.0F, 360.0F });
        groupRotation.setDuration(6665L);
        groupRotation.setInterpolator(LINEAR_INTERPOLATOR);
        groupRotation.setRepeatCount(-1);
        groupRotation.setRepeatMode(1);

        mAnimators.add(endTrim);
        mAnimators.add(startTrim);
        mAnimators.add(rotation);
        mAnimators.add(groupRotation);
    }

    private final Drawable.Callback mCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable d) {
            invalidateSelf();
        }

        @Override
        public void scheduleDrawable(Drawable d, Runnable what, long when) {
            scheduleSelf(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable d, Runnable what) {
            unscheduleSelf(what);
        }
    };

    private static class MaterialProgressState extends Drawable.ConstantState {
        private float mStrokeWidth = 5.0F;
        private float mInnerRadius = -1.0F;
        private int mWidth = -1;
        private int mHeight = -1;
        private ColorStateList mColor = ColorStateList.valueOf(0);

        public MaterialProgressState(int width, int height, int color, float strokeWidth, float innerRadius) {
            mWidth = width;
            mHeight = height;
            mColor = ColorStateList.valueOf(color);
            mStrokeWidth = strokeWidth;
            mInnerRadius = innerRadius;
        }

        public MaterialProgressState(MaterialProgressState orig) {
            if (orig != null) {
                mStrokeWidth = orig.mStrokeWidth;
                mInnerRadius = orig.mInnerRadius;
                mWidth = orig.mWidth;
                mHeight = orig.mHeight;
                mColor = orig.mColor;
            }
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }

        @Override
        public Drawable newDrawable() {
            return new MaterialProgressDrawable(this);
        }
    }

    private static class Ring {
        private final RectF mTempBounds = new RectF();
        private final Paint mPaint = new Paint();

        private final Drawable.Callback mCallback;

        private float mStartTrim = 0.0F;
        private float mEndTrim = 0.0F;
        private float mRotation = 0.0F;
        private float mStrokeWidth = 5.0F;
        private float mStrokeInset = 2.5F;

        private int mAlpha = 255;
        private int mColor = -16777216;

        public Ring(Drawable.Callback callback) {
            mCallback = callback;

            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);
        }

        public void draw(Canvas c, Rect bounds) {
            RectF arcBounds = mTempBounds;
            arcBounds.set(bounds);
            arcBounds.inset(mStrokeInset, mStrokeInset);

            float startAngle = (mStartTrim + mRotation) * 360.0F;
            float endAngle = (mEndTrim + mRotation) * 360.0F;
            float sweepAngle = endAngle - startAngle;

            float diameter = Math.min(arcBounds.width(), arcBounds.height());
            float minAngle = (float) (360.0D / (diameter * 3.141592653589793D));
            if ((sweepAngle < minAngle) && (sweepAngle > -minAngle)) {
                sweepAngle = Math.signum(sweepAngle) * minAngle;
            }

            c.drawArc(arcBounds, startAngle, sweepAngle, false, mPaint);
        }

        public void setColorFilter(ColorFilter filter) {
            mPaint.setColorFilter(filter);
            invalidateSelf();
        }

        public ColorFilter getColorFilter() {
            return mPaint.getColorFilter();
        }

        public void setAlpha(int alpha) {
            mAlpha = alpha;
            mPaint.setColor(mColor & 0xFFFFFF | alpha << 24);
            invalidateSelf();
        }

        public int getAlpha() {
            return mAlpha;
        }

        public void setColor(int color) {
            mColor = color;
            mPaint.setColor(color & 0xFFFFFF | mAlpha << 24);
            invalidateSelf();
        }

        public int getColor() {
            return mColor;
        }

        public void setStrokeWidth(float strokeWidth) {
            mStrokeWidth = strokeWidth;
            mPaint.setStrokeWidth(strokeWidth);
            invalidateSelf();
        }

        public float getStrokeWidth() {
            return mStrokeWidth;
        }

        public void setStartTrim(float startTrim) {
            mStartTrim = startTrim;
            invalidateSelf();
        }

        public float getStartTrim() {
            return mStartTrim;
        }

        public void setEndTrim(float endTrim) {
            mEndTrim = endTrim;
            invalidateSelf();
        }

        public float getEndTrim() {
            return mEndTrim;
        }

        public void setRotation(float rotation) {
            mRotation = rotation;
            invalidateSelf();
        }

        public float getRotation() {
            return mRotation;
        }

        public void setInsets(float insets) {
            mStrokeInset = insets;
        }

        public float getInsets() {
            return mStrokeInset;
        }

        private void invalidateSelf() {
            mCallback.invalidateDrawable(null);
        }
    }

    private static class EndCurveInterpolator extends
            AccelerateDecelerateInterpolator {
        @Override
        public float getInterpolation(float input) {
            return super
                    .getInterpolation(Math.max(0.0F, (input - 0.5F) * 2.0F));
        }
    }

    private static class StartCurveInterpolator extends
            AccelerateDecelerateInterpolator {
        @Override
        public float getInterpolation(float input) {
            return super.getInterpolation(Math.min(1.0F, input * 2.0F));
        }
    }
}
