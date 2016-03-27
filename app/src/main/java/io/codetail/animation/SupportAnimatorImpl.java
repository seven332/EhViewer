package io.codetail.animation;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;

import java.lang.ref.WeakReference;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
final class SupportAnimatorImpl extends SupportAnimator{

    WeakReference<Animator> mAnimator;

    SupportAnimatorImpl(Animator animator, RevealAnimator target) {
        super(target);
        mAnimator = new WeakReference<>(animator);
    }

    @Override
    public boolean isNativeAnimator() {
        return true;
    }

    @Override
    public Object get() {
        return mAnimator.get();
    }


    @Override
    public void start() {
        Animator a = mAnimator.get();
        if(a != null) {
            a.start();
        }
    }

    @Override
    public Animator setDuration(long duration) {
        Animator a = mAnimator.get();
        if(a != null) {
            a.setDuration(duration);
        }
        return this;
    }

    @Override
    public void setInterpolator(TimeInterpolator value) {
        Animator a = mAnimator.get();
        if(a != null) {
            a.setInterpolator(value);
        }
    }

    @Override
    public void addListener(final AnimatorListener listener) {
        Animator a = mAnimator.get();
        if(a == null) {
            return;
        }

        if(listener == null){
            a.addListener(null);
            return;
        }

        a.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                listener.onAnimationStart();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                listener.onAnimationEnd();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                listener.onAnimationCancel();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                listener.onAnimationRepeat();
            }
        });
    }

    @Override
    public boolean isRunning() {
        Animator a = mAnimator.get();
        return a != null && a.isRunning();
    }

    @Override
    public void cancel() {
        Animator a = mAnimator.get();
        if(a != null){
            a.cancel();
        }
    }

    @Override
    public void end() {
        Animator a = mAnimator.get();
        if(a != null){
            a.end();
        }
    }

    @Override
    public long getStartDelay() {
        Animator a = mAnimator.get();
        if(a != null){
            return a.getStartDelay();
        }
        return 0;
    }

    @Override
    public void setStartDelay(long startDelay) {
        Animator a = mAnimator.get();
        if(a != null){
            a.setStartDelay(startDelay);
        }
    }

    @Override
    public long getDuration() {
        Animator a = mAnimator.get();
        if(a != null){
            return a.getDuration();
        }
        return 0;
    }

    @Override
    public void setupStartValues() {
        Animator a = mAnimator.get();
        if(a != null){
            a.setupStartValues();
        }
    }

    @Override
    public void setupEndValues() {
        Animator a = mAnimator.get();
        if(a != null){
            a.setupEndValues();
        }
    }
}
