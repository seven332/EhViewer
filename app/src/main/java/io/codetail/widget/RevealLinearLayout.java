package io.codetail.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import io.codetail.animation.RevealAnimator;
import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

public class RevealLinearLayout extends LinearLayout implements RevealAnimator{

    private final Path mRevealPath;
    private final Rect mTargetBounds = new Rect();
    private RevealInfo mRevealInfo;
    private boolean mRunning;
    private float mRadius;

    public RevealLinearLayout(Context context) {
        this(context, null);
    }

    public RevealLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RevealLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        mRevealPath = new Path();
    }

    @Override
    public void onRevealAnimationStart() {
        mRunning = true;
    }

    @Override
    public void onRevealAnimationEnd() {
        mRunning = false;
        invalidate(mTargetBounds);
    }

    @Override
    public void onRevealAnimationCancel() {
        onRevealAnimationEnd();
    }

    /**
     * Circle radius size
     *
     * @hide
     */
    @Override
    public void setRevealRadius(float radius){
        mRadius = radius;
        mRevealInfo.getTarget().getHitRect(mTargetBounds);
        invalidate(mTargetBounds);
    }

    /**
     * Circle radius size
     *
     * @hide
     */
    @Override
    public float getRevealRadius(){
        return mRadius;
    }

    /**
     * @hide
     */
    @Override
    public void attachRevealInfo(RevealInfo info) {
        mRevealInfo = info;
    }

    /**
     * @hide
     */
    @Override
    public SupportAnimator startReverseAnimation() {
        if(mRevealInfo != null && mRevealInfo.hasTarget() && !mRunning) {
            return ViewAnimationUtils.createCircularReveal(mRevealInfo.getTarget(),
                    mRevealInfo.centerX, mRevealInfo.centerY,
                    mRevealInfo.endRadius, mRevealInfo.startRadius);
        }
        return null;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if(mRunning && child == mRevealInfo.getTarget()){
            final int state = canvas.save();

            mRevealPath.reset();
            mRevealPath.addCircle(mRevealInfo.centerX, mRevealInfo.centerY, mRadius, Path.Direction.CW);

            canvas.clipPath(mRevealPath);

            boolean isInvalided = super.drawChild(canvas, child, drawingTime);

            canvas.restoreToCount(state);

            return isInvalided;
        }

        return super.drawChild(canvas, child, drawingTime);
    }

}
