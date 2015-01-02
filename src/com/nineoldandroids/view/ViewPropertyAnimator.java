/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nineoldandroids.view;

import java.util.WeakHashMap;
import android.os.Build;
import android.view.View;
import android.view.animation.Interpolator;
import com.nineoldandroids.animation.Animator;

/**
 * This class enables automatic and optimized animation of select properties on View objects.
 * If only one or two properties on a View object are being animated, then using an
 * {@link android.animation.ObjectAnimator} is fine; the property setters called by ObjectAnimator
 * are well equipped to do the right thing to set the property and invalidate the view
 * appropriately. But if several properties are animated simultaneously, or if you just want a
 * more convenient syntax to animate a specific property, then ViewPropertyAnimator might be
 * more well-suited to the task.
 *
 * <p>This class may provide better performance for several simultaneous animations, because
 * it will optimize invalidate calls to take place only once for several properties instead of each
 * animated property independently causing its own invalidation. Also, the syntax of using this
 * class could be easier to use because the caller need only tell the View object which
 * property to animate, and the value to animate either to or by, and this class handles the
 * details of configuring the underlying Animator class and starting it.</p>
 *
 * <p>This class is not constructed by the caller, but rather by the View whose properties
 * it will animate. Calls to {@link android.view.View#animate()} will return a reference
 * to the appropriate ViewPropertyAnimator object for that View.</p>
 *
 */
public abstract class ViewPropertyAnimator {
    private static final WeakHashMap<View, ViewPropertyAnimator> ANIMATORS =
            new WeakHashMap<View, ViewPropertyAnimator>(0);

    /**
     * This method returns a ViewPropertyAnimator object, which can be used to animate specific
     * properties on this View.
     *
     * @param view View to animate.
     * @return The ViewPropertyAnimator associated with this View.
     */
    public static ViewPropertyAnimator animate(View view) {
        ViewPropertyAnimator animator = ANIMATORS.get(view);
        if (animator == null) {
            final int version = Integer.valueOf(Build.VERSION.SDK);
            if (version >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                animator = new ViewPropertyAnimatorICS(view);
            } else if (version >= Build.VERSION_CODES.HONEYCOMB) {
                animator = new ViewPropertyAnimatorHC(view);
            } else {
                animator = new ViewPropertyAnimatorPreHC(view);
            }
            ANIMATORS.put(view, animator);
        }
        return animator;
    }


    /**
     * Sets the duration for the underlying animator that animates the requested properties.
     * By default, the animator uses the default value for ValueAnimator. Calling this method
     * will cause the declared value to be used instead.
     * @param duration The length of ensuing property animations, in milliseconds. The value
     * cannot be negative.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator setDuration(long duration);

    /**
     * Returns the current duration of property animations. If the duration was set on this
     * object, that value is returned. Otherwise, the default value of the underlying Animator
     * is returned.
     *
     * @see #setDuration(long)
     * @return The duration of animations, in milliseconds.
     */
    public abstract long getDuration();

    /**
     * Returns the current startDelay of property animations. If the startDelay was set on this
     * object, that value is returned. Otherwise, the default value of the underlying Animator
     * is returned.
     *
     * @see #setStartDelay(long)
     * @return The startDelay of animations, in milliseconds.
     */
    public abstract long getStartDelay();

    /**
     * Sets the startDelay for the underlying animator that animates the requested properties.
     * By default, the animator uses the default value for ValueAnimator. Calling this method
     * will cause the declared value to be used instead.
     * @param startDelay The delay of ensuing property animations, in milliseconds. The value
     * cannot be negative.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator setStartDelay(long startDelay);

    /**
     * Sets the interpolator for the underlying animator that animates the requested properties.
     * By default, the animator uses the default interpolator for ValueAnimator. Calling this method
     * will cause the declared object to be used instead.
     *
     * @param interpolator The TimeInterpolator to be used for ensuing property animations.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator setInterpolator(/*Time*/Interpolator interpolator);

    /**
     * Sets a listener for events in the underlying Animators that run the property
     * animations.
     *
     * @param listener The listener to be called with AnimatorListener events.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator setListener(Animator.AnimatorListener listener);

    /**
     * Starts the currently pending property animations immediately. Calling <code>start()</code>
     * is optional because all animations start automatically at the next opportunity. However,
     * if the animations are needed to start immediately and synchronously (not at the time when
     * the next event is processed by the hierarchy, which is when the animations would begin
     * otherwise), then this method can be used.
     */
    public abstract void start();

    /**
     * Cancels all property animations that are currently running or pending.
     */
    public abstract void cancel();

    /**
     * This method will cause the View's <code>x</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setX(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator x(float value);

    /**
     * This method will cause the View's <code>x</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setX(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator xBy(float value);

    /**
     * This method will cause the View's <code>y</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setY(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator y(float value);

    /**
     * This method will cause the View's <code>y</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setY(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator yBy(float value);

    /**
     * This method will cause the View's <code>rotation</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setRotation(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator rotation(float value);

    /**
     * This method will cause the View's <code>rotation</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setRotation(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator rotationBy(float value);

    /**
     * This method will cause the View's <code>rotationX</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setRotationX(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator rotationX(float value);

    /**
     * This method will cause the View's <code>rotationX</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setRotationX(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator rotationXBy(float value);

    /**
     * This method will cause the View's <code>rotationY</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setRotationY(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator rotationY(float value);

    /**
     * This method will cause the View's <code>rotationY</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setRotationY(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator rotationYBy(float value);

    /**
     * This method will cause the View's <code>translationX</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setTranslationX(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator translationX(float value);

    /**
     * This method will cause the View's <code>translationX</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setTranslationX(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator translationXBy(float value);

    /**
     * This method will cause the View's <code>translationY</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setTranslationY(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator translationY(float value);

    /**
     * This method will cause the View's <code>translationY</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setTranslationY(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator translationYBy(float value);

    /**
     * This method will cause the View's <code>scaleX</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setScaleX(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator scaleX(float value);

    /**
     * This method will cause the View's <code>scaleX</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setScaleX(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator scaleXBy(float value);

    /**
     * This method will cause the View's <code>scaleY</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setScaleY(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator scaleY(float value);

    /**
     * This method will cause the View's <code>scaleY</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setScaleY(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator scaleYBy(float value);

    /**
     * This method will cause the View's <code>alpha</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @see android.view.View#setAlpha(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator alpha(float value);

    /**
     * This method will cause the View's <code>alpha</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @see android.view.View#setAlpha(float)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public abstract ViewPropertyAnimator alphaBy(float value);
}
