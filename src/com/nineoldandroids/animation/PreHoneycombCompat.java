package com.nineoldandroids.animation;

import android.view.View;
import com.nineoldandroids.util.FloatProperty;
import com.nineoldandroids.util.IntProperty;
import com.nineoldandroids.util.Property;
import com.nineoldandroids.view.animation.AnimatorProxy;

final class PreHoneycombCompat {
    static Property<View, Float> ALPHA = new FloatProperty<View>("alpha") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setAlpha(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getAlpha();
        }
    };
    static Property<View, Float> PIVOT_X = new FloatProperty<View>("pivotX") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setPivotX(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getPivotX();
        }
    };
    static Property<View, Float> PIVOT_Y = new FloatProperty<View>("pivotY") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setPivotY(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getPivotY();
        }
    };
    static Property<View, Float> TRANSLATION_X = new FloatProperty<View>("translationX") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setTranslationX(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getTranslationX();
        }
    };
    static Property<View, Float> TRANSLATION_Y = new FloatProperty<View>("translationY") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setTranslationY(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getTranslationY();
        }
    };
    static Property<View, Float> ROTATION = new FloatProperty<View>("rotation") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setRotation(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getRotation();
        }
    };
    static Property<View, Float> ROTATION_X = new FloatProperty<View>("rotationX") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setRotationX(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getRotationX();
        }
    };
    static Property<View, Float> ROTATION_Y = new FloatProperty<View>("rotationY") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setRotationY(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getRotationY();
        }
    };
    static Property<View, Float> SCALE_X = new FloatProperty<View>("scaleX") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setScaleX(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getScaleX();
        }
    };
    static Property<View, Float> SCALE_Y = new FloatProperty<View>("scaleY") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setScaleY(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getScaleY();
        }
    };
    static Property<View, Integer> SCROLL_X = new IntProperty<View>("scrollX") {
        @Override
        public void setValue(View object, int value) {
            AnimatorProxy.wrap(object).setScrollX(value);
        }

        @Override
        public Integer get(View object) {
            return AnimatorProxy.wrap(object).getScrollX();
        }
    };
    static Property<View, Integer> SCROLL_Y = new IntProperty<View>("scrollY") {
        @Override
        public void setValue(View object, int value) {
            AnimatorProxy.wrap(object).setScrollY(value);
        }

        @Override
        public Integer get(View object) {
            return AnimatorProxy.wrap(object).getScrollY();
        }
    };
    static Property<View, Float> X = new FloatProperty<View>("x") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setX(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getX();
        }
    };
    static Property<View, Float> Y = new FloatProperty<View>("y") {
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setY(value);
        }

        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getY();
        }
    };


    //No instances
    private PreHoneycombCompat() {}
}
