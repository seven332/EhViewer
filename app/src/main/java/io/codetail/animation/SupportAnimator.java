package io.codetail.animation;

import android.animation.Animator;

import java.lang.ref.WeakReference;

public abstract class SupportAnimator extends Animator{

    WeakReference<RevealAnimator> mTarget;

    public SupportAnimator(RevealAnimator target) {
        mTarget = new WeakReference<>(target);
    }

    /**
     * @return true if using native android animation framework, otherwise is
     * nineoldandroids
     */
    public abstract boolean isNativeAnimator();

    /**
     * @return depends from {@link android.os.Build.VERSION} if sdk version
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} and greater will return
     * {@link android.animation.Animator}
     */
    public abstract Object get();

    /**
     * Starts this animation. If the animation has a nonzero startDelay, the animation will start
     * running after that delay elapses. A non-delayed animation will have its initial
     * value(s) set immediately, followed by calls to
     * {@link android.animation.Animator.AnimatorListener#onAnimationStart(android.animation.Animator)}
     * for any listeners of this animator.
     *
     * <p>The animation started by calling this method will be run on the thread that called
     * this method. This thread should have a Looper on it (a runtime exception will be thrown if
     * this is not the case). Also, if the animation will animate
     * properties of objects in the view hierarchy, then the calling thread should be the UI
     * thread for that view hierarchy.</p>
     *
     */
    @Override
    public abstract void start();


    /**
     * Adds a listener to the set of listeners that are sent events through the life of an
     * animation, such as start, repeat, and end.
     *
     * @param listener the listener to be added to the current set of listeners for this animation.
     */
    public abstract void addListener(AnimatorListener listener);


    /**
     * Returns whether this Animator is currently running (having been started and gone past any
     * initial startDelay period and not yet ended).
     *
     * @return Whether the Animator is running.
     */
    @Override
    public abstract boolean isRunning();


    /**
     * Cancels the animation. Unlike {@link #end()}, <code>cancel()</code> causes the animation to
     * stop in its tracks, sending an
     * {@link AnimatorListener#onAnimationCancel()} to
     * its listeners, followed by an
     * {@link AnimatorListener#onAnimationEnd()} message.
     *
     * <p>This method must be called on the thread that is running the animation.</p>
     */
    @Override
    public abstract void cancel();

    /**
     * Ends the animation. This causes the animation to assign the end value of the property being
     * animated, then calling the
     * {@link AnimatorListener#onAnimationEnd()} method on
     * its listeners.
     *
     * <p>This method must be called on the thread that is running the animation.</p>
     */
    @Override
    public void end() {
    }

    /**
     * This method tells the object to use appropriate information to extract
     * starting values for the animation. For example, a AnimatorSet object will pass
     * this call to its child objects to tell them to set up the values. A
     * ObjectAnimator object will use the information it has about its target object
     * and PropertyValuesHolder objects to get the start values for its properties.
     * A ValueAnimator object will ignore the request since it does not have enough
     * information (such as a target object) to gather these values.
     */
    @Override
    public void setupStartValues() {
    }

    /**
     * This method tells the object to use appropriate information to extract
     * ending values for the animation. For example, a AnimatorSet object will pass
     * this call to its child objects to tell them to set up the values. A
     * ObjectAnimator object will use the information it has about its target object
     * and PropertyValuesHolder objects to get the start values for its properties.
     * A ValueAnimator object will ignore the request since it does not have enough
     * information (such as a target object) to gather these values.
     */
    @Override
    public void setupEndValues() {
    }

    /**
     * Experimental feature
     */
    public SupportAnimator reverse() {
        if(isRunning()){
            return null;
        }

        RevealAnimator target = mTarget.get();
        if(target != null){
            return target.startReverseAnimation();
        }

        return null;
    }

    /**
     * <p>An animation listener receives notifications from an animation.
     * Notifications indicate animation related events, such as the end or the
     * repetition of the animation.</p>
     */
    public interface AnimatorListener {
        /**
         * <p>Notifies the start of the animation.</p>
         */
        void onAnimationStart();

        /**
         * <p>Notifies the end of the animation. This callback is not invoked
         * for animations with repeat count set to INFINITE.</p>
         */
        void onAnimationEnd();

        /**
         * <p>Notifies the cancellation of the animation. This callback is not invoked
         * for animations with repeat count set to INFINITE.</p>
         */
        void onAnimationCancel();

        /**
         * <p>Notifies the repetition of the animation.</p>
         */
        void onAnimationRepeat();
    }

    /**
     * <p>Provides default implementation for AnimatorListener.</p>
     */
    public static abstract class SimpleAnimatorListener implements AnimatorListener {

        @Override
        public void onAnimationStart() {

        }

        @Override
        public void onAnimationEnd() {

        }

        @Override
        public void onAnimationCancel() {

        }

        @Override
        public void onAnimationRepeat() {

        }
    }

}
