/*
 * Copyright (C) 2014 Balys Valentukevicius
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

package com.hippo.ehviewer.drawable;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Property;
import android.util.TypedValue;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;


public class MaterialIndicatorDrawable extends Drawable implements Animatable {

    private static final String STATE_KEY = "material_menu_icon_state";

    public enum IconState {
        BURGER, ARROW
    }

    public enum AnimationState {
        BURGER_ARROW;

        public IconState getFirstState() {
            switch (this) {
                case BURGER_ARROW:
                    return IconState.BURGER;
                default:
                    return null;
            }
        }

        public IconState getSecondState() {
            switch (this) {
                case BURGER_ARROW:
                    return IconState.ARROW;
                default:
                    return null;
            }
        }
    }

    public enum Stroke {
        /**
         * 3 dip
         */
        REGULAR(3),
        /**
         * 2 dip
         */
        THIN(2),
        /**
         * 1 dip
         */
        EXTRA_THIN(1);

        private final int strokeWidth;

        Stroke(int strokeWidth) {
            this.strokeWidth = strokeWidth;
        }

        protected static Stroke valueOf(int strokeWidth) {
            switch (strokeWidth) {
                case 3:
                    return REGULAR;
                case 2:
                    return THIN;
                case 1:
                    return EXTRA_THIN;
                default:
                    return THIN;
            }
        }
    }

    public static final int DEFAULT_COLOR              = Color.WHITE;
    public static final int DEFAULT_SCALE              = 1;
    public static final int DEFAULT_TRANSFORM_DURATION = 800;
    public static final int DEFAULT_PRESSED_DURATION   = 400;

    private static final int BASE_DRAWABLE_WIDTH  = 40;
    private static final int BASE_DRAWABLE_HEIGHT = 40;
    private static final int BASE_ICON_WIDTH      = 20;
    private static final int BASE_CIRCLE_RADIUS   = 18;

    private static final float ARROW_MID_LINE_ANGLE = 180;
    private static final float ARROW_TOP_LINE_ANGLE = 135;
    private static final float ARROW_BOT_LINE_ANGLE = 225;

    private static final float TRANSFORMATION_START = 0;
    private static final float TRANSFORMATION_MID   = 1.0f;
    private static final float TRANSFORMATION_END   = 2.0f;

    private static final int DEFAULT_CIRCLE_ALPHA = 200;

    private final float diph;
    private final float dip1;
    private final float dip2;
    private final float dip3;
    private final float dip4;

    private final int   width;
    private final int   height;
    private final float strokeWidth;
    private final float iconWidth;
    private final float topPadding;
    private final float sidePadding;
    private final float circleRadius;

    private final Stroke stroke;

    private final Object lock = new Object();

    private final Paint gridPaint   = new Paint();
    private final Paint iconPaint   = new Paint();
    private final Paint circlePaint = new Paint();

    private float   transformationValue   = 0f;
    private float   pressedProgressValue  = 0f;
    private boolean transformationRunning = false;

    private IconState      currentIconState = IconState.BURGER;
    private AnimationState animationState   = AnimationState.BURGER_ARROW;

    private IconState animatingIconState;
    private boolean   drawTouchCircle;
    private boolean   neverDrawTouch;
    private boolean   rtlEnabled;

    private ObjectAnimator transformation;
    private ObjectAnimator pressedCircle;

    private MaterialMenuState materialMenuState;

    public MaterialIndicatorDrawable(Context context, int color, Stroke stroke) {
        this(context, color, stroke, DEFAULT_SCALE, DEFAULT_TRANSFORM_DURATION, DEFAULT_PRESSED_DURATION);
    }

    public MaterialIndicatorDrawable(Context context, int color, Stroke stroke, int transformDuration, int pressedDuration) {
        this(context, color, stroke, DEFAULT_SCALE, transformDuration, pressedDuration);
    }

    public MaterialIndicatorDrawable(Context context, int color, Stroke stroke, int scale, int transformDuration, int pressedDuration) {
        Resources resources = context.getResources();
        // convert each separately due to various densities
        this.dip1 = dpToPx(resources, 1) * scale;
        this.dip2 = dpToPx(resources, 2) * scale;
        this.dip3 = dpToPx(resources, 3) * scale;
        this.dip4 = dpToPx(resources, 4) * scale;
        this.diph = dip1 / 2;

        this.stroke = stroke;
        this.width = (int) (dpToPx(resources, BASE_DRAWABLE_WIDTH) * scale);
        this.height = (int) (dpToPx(resources, BASE_DRAWABLE_HEIGHT) * scale);
        this.iconWidth = dpToPx(resources, BASE_ICON_WIDTH) * scale;
        this.circleRadius = dpToPx(resources, BASE_CIRCLE_RADIUS) * scale;
        this.strokeWidth = dpToPx(resources, stroke.strokeWidth) * scale;

        this.sidePadding = (width - iconWidth) / 2;
        this.topPadding = (height - 5 * dip3) / 2;

        initPaint(color);
        initAnimations(transformDuration, pressedDuration);

        materialMenuState = new MaterialMenuState();
    }

    private MaterialIndicatorDrawable(int color, Stroke stroke, long transformDuration, long pressedDuration,
                                 int width, int height, float iconWidth, float circleRadius, float strokeWidth, float dip1
    ) {
        this.dip1 = dip1;
        this.dip2 = dip1 * 2;
        this.dip3 = dip1 * 3;
        this.dip4 = dip1 * 4;
        this.diph = dip1 / 2;
        this.stroke = stroke;
        this.width = width;
        this.height = height;
        this.iconWidth = iconWidth;
        this.circleRadius = circleRadius;
        this.strokeWidth = strokeWidth;
        this.sidePadding = (width - iconWidth) / 2;
        this.topPadding = (height - 5 * dip3) / 2;

        initPaint(color);
        initAnimations((int) transformDuration, (int) pressedDuration);

        materialMenuState = new MaterialMenuState();
    }

    private void initPaint(int color) {
        gridPaint.setAntiAlias(false);
        gridPaint.setColor(Color.GREEN);
        gridPaint.setStrokeWidth(1);

        iconPaint.setAntiAlias(true);
        iconPaint.setStyle(Style.STROKE);
        iconPaint.setStrokeWidth(strokeWidth);
        iconPaint.setColor(color);

        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Style.FILL);
        circlePaint.setColor(color);
        circlePaint.setAlpha(DEFAULT_CIRCLE_ALPHA);

        setBounds(0, 0, width, height);
    }

    /*
     * Drawing
     */

    @Override public void draw(Canvas canvas) {
        final float ratio = transformationValue <= 1 ? transformationValue : 2 - transformationValue;

        if (rtlEnabled) {
            canvas.save();
            canvas.scale(-1, 1, 0, 0);
            canvas.translate(-getIntrinsicWidth(), 0);
        }

        drawTopLine(canvas, ratio);
        drawMiddleLine(canvas, ratio);
        drawBottomLine(canvas, ratio);

        if (rtlEnabled) {
            canvas.restore();
        }

        if (drawTouchCircle) drawTouchCircle(canvas);
    }

    private void drawTouchCircle(Canvas canvas) {
        canvas.restore();
        canvas.drawCircle(width / 2, height / 2, pressedProgressValue, circlePaint);
    }

    private void drawMiddleLine(Canvas canvas, float ratio) {
        canvas.restore();
        canvas.save();

        float rotation = 0;
        float pivotX = width / 2;
        float pivotY = width / 2;
        float startX = sidePadding;
        float startY = topPadding + dip3 / 2 * 5;
        float stopX = width - sidePadding;
        float stopY = topPadding + dip3 / 2 * 5;
        int alpha = 255;

        switch (animationState) {
            case BURGER_ARROW:
                // rotate by 180
                if (isMorphingForward()) {
                    rotation = ratio * ARROW_MID_LINE_ANGLE;
                } else {
                    rotation = ARROW_MID_LINE_ANGLE + (1 - ratio) * ARROW_MID_LINE_ANGLE;
                }
                // shorten one end
                stopX -= ratio * resolveStrokeModifier(ratio) / 2;
                break;
        }

        iconPaint.setAlpha(alpha);
        canvas.rotate(rotation, pivotX, pivotY);
        canvas.drawLine(startX, startY, stopX, stopY, iconPaint);
        iconPaint.setAlpha(255);
    }

    private void drawTopLine(Canvas canvas, float ratio) {
        canvas.save();

        float rotation = 0, pivotX = 0, pivotY = 0;
        float rotation2 = 0;
        // pivot at center of line
        float pivotX2 = width / 2 + dip3 / 2;
        float pivotY2 = topPadding + dip2;

        float startX = sidePadding;
        float startY = topPadding + dip2;
        float stopX = width - sidePadding;
        float stopY = topPadding + dip2;
        int alpha = 255;

        switch (animationState) {
            case BURGER_ARROW:
                if (isMorphingForward()) {
                    // rotate until required angle
                    rotation = ratio * ARROW_BOT_LINE_ANGLE;
                } else {
                    // rotate back to start doing a 360
                    rotation = ARROW_BOT_LINE_ANGLE + (1 - ratio) * ARROW_TOP_LINE_ANGLE;
                }
                // rotate by middle
                pivotX = width / 2;
                pivotY = height / 2;

                // shorten both ends
                stopX -= resolveStrokeModifier(ratio);
                startX += dip3 * ratio;

                break;
        }

        iconPaint.setAlpha(alpha);
        canvas.rotate(rotation, pivotX, pivotY);
        canvas.rotate(rotation2, pivotX2, pivotY2);
        canvas.drawLine(startX, startY, stopX, stopY, iconPaint);
        iconPaint.setAlpha(255);
    }

    private void drawBottomLine(Canvas canvas, float ratio) {
        canvas.restore();
        canvas.save();

        float rotation = 0, pivotX = 0, pivotY = 0;
        float rotation2 = 0;
        // pivot at center of line
        float pivotX2 = width / 2 + dip3 / 2;
        float pivotY2 = height - topPadding - dip2;

        float startX = sidePadding;
        float startY = height - topPadding - dip2;
        float stopX = width - sidePadding;
        float stopY = height - topPadding - dip2;

        switch (animationState) {
            case BURGER_ARROW:
                if (isMorphingForward()) {
                    // rotate to required angle
                    rotation = ARROW_TOP_LINE_ANGLE * ratio;
                } else {
                    // rotate back to start doing a 360
                    rotation = ARROW_TOP_LINE_ANGLE + (1 - ratio) * ARROW_BOT_LINE_ANGLE;
                }
                // pivot center of canvas
                pivotX = width / 2;
                pivotY = height / 2;

                // shorten both ends
                stopX = width - sidePadding - resolveStrokeModifier(ratio);
                startX = sidePadding + dip3 * ratio;
                break;
        }

        canvas.rotate(rotation, pivotX, pivotY);
        canvas.rotate(rotation2, pivotX2, pivotY2);
        canvas.drawLine(startX, startY, stopX, stopY, iconPaint);
    }

    private boolean isMorphingForward() {
        return transformationValue <= TRANSFORMATION_MID;
    }

    private float resolveStrokeModifier(float ratio) {
        switch (stroke) {
            case REGULAR:
                return ratio * dip3;
            case THIN:
                return ratio * (dip3 + diph);
            case EXTRA_THIN:
                return ratio * dip4;
        }
        return 0;
    }

    @Override public void setAlpha(int alpha) {
        iconPaint.setAlpha(alpha);
    }

    @Override public void setColorFilter(ColorFilter cf) {
        iconPaint.setColorFilter(cf);
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    /*
     * Accessor methods
     */

    public void setColor(int color) {
        iconPaint.setColor(color);
        circlePaint.setColor(color);
        invalidateSelf();
    }

    public void setTransformationDuration(int duration) {
        transformation.setDuration(duration);
    }

    public void setPressedDuration(int duration) {
        pressedCircle.setDuration(duration);
    }

    public void setInterpolator(Interpolator interpolator) {
        transformation.setInterpolator(interpolator);
    }

    public void setNeverDrawTouch(boolean neverDrawTouch) {
        this.neverDrawTouch = neverDrawTouch;
    }

    public void setIconState(IconState iconState) {
        synchronized (lock) {
            if (transformationRunning) {
                transformation.cancel();
                transformationRunning = false;
            }

            if (currentIconState == iconState) return;

            switch (iconState) {
                case BURGER:
                    animationState = AnimationState.BURGER_ARROW;
                    transformationValue = TRANSFORMATION_START;
                    break;
                case ARROW:
                    animationState = AnimationState.BURGER_ARROW;
                    transformationValue = TRANSFORMATION_MID;
                    break;
            }
            currentIconState = iconState;
            invalidateSelf();
        }
    }

    public void animateIconState(IconState state, boolean drawTouch) {
        synchronized (lock) {
            if (transformationRunning) {
                transformation.end();
                pressedCircle.end();
            }
            drawTouchCircle = drawTouch;
            animatingIconState = state;
            start();
        }
    }

    public IconState setTransformationOffset(AnimationState animationState, float offset) {
        if (offset < TRANSFORMATION_START || offset > TRANSFORMATION_END) {
            throw new IllegalArgumentException(
                String.format("Value must be between %s and %s", TRANSFORMATION_START, TRANSFORMATION_END)
            );
        }

        this.animationState = animationState;

        final boolean isFirstIcon = offset < TRANSFORMATION_MID || offset == TRANSFORMATION_END;

        currentIconState = isFirstIcon ? animationState.getFirstState() : animationState.getSecondState();
        animatingIconState = isFirstIcon ? animationState.getSecondState() : animationState.getFirstState();

        setTransformationValue(offset);

        return currentIconState;
    }

    public void setRTLEnabled(boolean rtlEnabled) {
        this.rtlEnabled = rtlEnabled;
        invalidateSelf();
    }

    public IconState getIconState() {
        return currentIconState;
    }

    /*
     * Animations
     */
    private final Property<MaterialIndicatorDrawable, Float> transformationProperty
        = new Property<MaterialIndicatorDrawable, Float>(Float.class, "transformation") {
        @Override
        public Float get(MaterialIndicatorDrawable object) {
            return object.getTransformationValue();
        }

        @Override
        public void set(MaterialIndicatorDrawable object, Float value) {
            object.setTransformationValue(value);
        }
    };

    private final Property<MaterialIndicatorDrawable, Float> pressedProgressProperty
        = new Property<MaterialIndicatorDrawable, Float>(Float.class, "pressedProgress") {
        @Override
        public Float get(MaterialIndicatorDrawable object) {
            return object.getPressedProgress();
        }

        @Override
        public void set(MaterialIndicatorDrawable object, Float value) {
            object.setPressedProgress(value);
        }
    };

    public Float getTransformationValue() {
        return transformationValue;
    }

    public void setTransformationValue(Float value) {
        this.transformationValue = value;
        invalidateSelf();
    }

    public Float getPressedProgress() {
        return pressedProgressValue;
    }

    public void setPressedProgress(Float value) {
        this.pressedProgressValue = value;
        circlePaint.setAlpha((int) (DEFAULT_CIRCLE_ALPHA * (1 - value / (circleRadius * 1.22f))));
        invalidateSelf();
    }

    private void initAnimations(int transformDuration, int pressedDuration) {
        transformation = ObjectAnimator.ofFloat(this, transformationProperty, 0);
        transformation.setInterpolator(new DecelerateInterpolator(3));
        transformation.setDuration(transformDuration);
        transformation.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                transformationRunning = false;
                setIconState(animatingIconState);
            }
        });


        pressedCircle = ObjectAnimator.ofFloat(this, pressedProgressProperty, 0, 0);
        pressedCircle.setDuration(pressedDuration);
        pressedCircle.setInterpolator(new DecelerateInterpolator());
        pressedCircle.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                pressedProgressValue = 0;
            }

            @Override public void onAnimationCancel(Animator animation) {
                pressedProgressValue = 0;
            }
        });
    }

    private boolean resolveTransformation() {
        boolean isCurrentBurger = currentIconState == IconState.BURGER;
        boolean isCurrentArrow = currentIconState == IconState.ARROW;
        boolean isAnimatingBurger = animatingIconState == IconState.BURGER;
        boolean isAnimatingArrow = animatingIconState == IconState.ARROW;

        if ((isCurrentBurger && isAnimatingArrow) || (isCurrentArrow && isAnimatingBurger)) {
            animationState = AnimationState.BURGER_ARROW;
            return isCurrentBurger;
        }

        throw new IllegalStateException(
            String.format("Animating from %s to %s is not supported", currentIconState, animatingIconState)
        );
    }

    @Override public void start() {
        if (transformationRunning || animatingIconState == null || animatingIconState == currentIconState) return;
        transformationRunning = true;

        final boolean direction = resolveTransformation();
        transformation.setFloatValues(
            direction ? TRANSFORMATION_START : TRANSFORMATION_MID,
            direction ? TRANSFORMATION_MID : TRANSFORMATION_END
        );
        transformation.start();

        if (pressedCircle.isRunning()) {
            pressedCircle.cancel();
        }
        if (drawTouchCircle && !neverDrawTouch) {
            pressedCircle.setFloatValues(0, circleRadius * 1.22f);
            pressedCircle.start();
        }

        invalidateSelf();
    }

    @Override public void stop() {
        if (isRunning() && transformation.isRunning()) {
            transformation.end();
        } else {
            transformationRunning = false;
            invalidateSelf();
        }
    }

    @Override public boolean isRunning() {
        return transformationRunning;
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }


    @Override
    public ConstantState getConstantState() {
        materialMenuState.changingConfigurations = getChangingConfigurations();
        return materialMenuState;
    }

    @Override
    public Drawable mutate() {
        materialMenuState = new MaterialMenuState();
        return this;
    }

    /**
     * Call from {@link android.app.Activity#onSaveInstanceState(android.os.Bundle)} to store current icon state
     *
     * @param outState outState
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_KEY, currentIconState.name());
    }

    /**
     * Call from {@link android.app.Activity#onPostCreate(android.os.Bundle)} to restore icon state
     *
     * @param state state
     */
    public void syncState(Bundle state) {
        if (state != null) {
            String iconStateName = state.getString(STATE_KEY);
            if (iconStateName == null) {
                iconStateName = MaterialIndicatorDrawable.IconState.BURGER.name();
            }
            setIconState(MaterialIndicatorDrawable.IconState.valueOf(iconStateName));
        }
    }

    private final class MaterialMenuState extends ConstantState {
        private int changingConfigurations;

        private MaterialMenuState() {
        }

        @Override
        public Drawable newDrawable() {
            MaterialIndicatorDrawable drawable = new MaterialIndicatorDrawable(
                circlePaint.getColor(), stroke, transformation.getDuration(),
                pressedCircle.getDuration(), width, height, iconWidth, circleRadius, strokeWidth, dip1
            );
            drawable.setIconState(animatingIconState != null ? animatingIconState : currentIconState);
            drawable.setRTLEnabled(rtlEnabled);
            return drawable;
        }

        @Override
        public int getChangingConfigurations() {
            return changingConfigurations;
        }
    }

    static float dpToPx(Resources resources, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }
}
