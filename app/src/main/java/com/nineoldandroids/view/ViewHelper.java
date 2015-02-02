package com.nineoldandroids.view;

import android.view.View;

import static com.nineoldandroids.view.animation.AnimatorProxy.NEEDS_PROXY;
import static com.nineoldandroids.view.animation.AnimatorProxy.wrap;

public final class ViewHelper {
    private ViewHelper() {}

    public static float getAlpha(View view) {
        return NEEDS_PROXY ? wrap(view).getAlpha() : Honeycomb.getAlpha(view);
    }

    public static void setAlpha(View view, float alpha) {
        if (NEEDS_PROXY) {
            wrap(view).setAlpha(alpha);
        } else {
            Honeycomb.setAlpha(view, alpha);
        }
    }

    public static float getPivotX(View view) {
        return NEEDS_PROXY ? wrap(view).getPivotX() : Honeycomb.getPivotX(view);
    }

    public static void setPivotX(View view, float pivotX) {
        if (NEEDS_PROXY) {
            wrap(view).setPivotX(pivotX);
        } else {
            Honeycomb.setPivotX(view, pivotX);
        }
    }

    public static float getPivotY(View view) {
        return NEEDS_PROXY ? wrap(view).getPivotY() : Honeycomb.getPivotY(view);
    }

    public static void setPivotY(View view, float pivotY) {
        if (NEEDS_PROXY) {
            wrap(view).setPivotY(pivotY);
        } else {
            Honeycomb.setPivotY(view, pivotY);
        }
    }

    public static float getRotation(View view) {
        return NEEDS_PROXY ? wrap(view).getRotation() : Honeycomb.getRotation(view);
    }

    public static void setRotation(View view, float rotation) {
        if (NEEDS_PROXY) {
            wrap(view).setRotation(rotation);
        } else {
            Honeycomb.setRotation(view, rotation);
        }
    }

    public static float getRotationX(View view) {
        return NEEDS_PROXY ? wrap(view).getRotationX() : Honeycomb.getRotationX(view);
    }

    public static void setRotationX(View view, float rotationX) {
        if (NEEDS_PROXY) {
            wrap(view).setRotationX(rotationX);
        } else {
            Honeycomb.setRotationX(view, rotationX);
        }
    }

    public static float getRotationY(View view) {
        return NEEDS_PROXY ? wrap(view).getRotationY() : Honeycomb.getRotationY(view);
    }

    public static void setRotationY(View view, float rotationY) {
        if (NEEDS_PROXY) {
            wrap(view).setRotationY(rotationY);
        } else {
            Honeycomb.setRotationY(view, rotationY);
        }
    }

    public static float getScaleX(View view) {
        return NEEDS_PROXY ? wrap(view).getScaleX() : Honeycomb.getScaleX(view);
    }

    public static void setScaleX(View view, float scaleX) {
        if (NEEDS_PROXY) {
            wrap(view).setScaleX(scaleX);
        } else {
            Honeycomb.setScaleX(view, scaleX);
        }
    }

    public static float getScaleY(View view) {
        return NEEDS_PROXY ? wrap(view).getScaleY() : Honeycomb.getScaleY(view);
    }

    public static void setScaleY(View view, float scaleY) {
        if (NEEDS_PROXY) {
            wrap(view).setScaleY(scaleY);
        } else {
            Honeycomb.setScaleY(view, scaleY);
        }
    }

    public static float getScrollX(View view) {
        return NEEDS_PROXY ? wrap(view).getScrollX() : Honeycomb.getScrollX(view);
    }

    public static void setScrollX(View view, int scrollX) {
        if (NEEDS_PROXY) {
            wrap(view).setScrollX(scrollX);
        } else {
            Honeycomb.setScrollX(view, scrollX);
        }
    }

    public static float getScrollY(View view) {
        return NEEDS_PROXY ? wrap(view).getScrollY() : Honeycomb.getScrollY(view);
    }

    public static void setScrollY(View view, int scrollY) {
        if (NEEDS_PROXY) {
            wrap(view).setScrollY(scrollY);
        } else {
            Honeycomb.setScrollY(view, scrollY);
        }
    }

    public static float getTranslationX(View view) {
        return NEEDS_PROXY ? wrap(view).getTranslationX() : Honeycomb.getTranslationX(view);
    }

    public static void setTranslationX(View view, float translationX) {
        if (NEEDS_PROXY) {
            wrap(view).setTranslationX(translationX);
        } else {
            Honeycomb.setTranslationX(view, translationX);
        }
    }

    public static float getTranslationY(View view) {
        return NEEDS_PROXY ? wrap(view).getTranslationY() : Honeycomb.getTranslationY(view);
    }

    public static void setTranslationY(View view, float translationY) {
        if (NEEDS_PROXY) {
            wrap(view).setTranslationY(translationY);
        } else {
            Honeycomb.setTranslationY(view, translationY);
        }
    }

    public static float getX(View view) {
        return NEEDS_PROXY ? wrap(view).getX() : Honeycomb.getX(view);
    }

    public static void setX(View view, float x) {
        if (NEEDS_PROXY) {
            wrap(view).setX(x);
        } else {
            Honeycomb.setX(view, x);
        }
    }

    public static float getY(View view) {
        return NEEDS_PROXY ? wrap(view).getY() : Honeycomb.getY(view);
    }

    public static void setY(View view, float y) {
        if (NEEDS_PROXY) {
            wrap(view).setY(y);
        } else {
            Honeycomb.setY(view, y);
        }
    }

    private static final class Honeycomb {
        static float getAlpha(View view) {
            return view.getAlpha();
        }

        static void setAlpha(View view, float alpha) {
            view.setAlpha(alpha);
        }

        static float getPivotX(View view) {
            return view.getPivotX();
        }

        static void setPivotX(View view, float pivotX) {
            view.setPivotX(pivotX);
        }

        static float getPivotY(View view) {
            return view.getPivotY();
        }

        static void setPivotY(View view, float pivotY) {
            view.setPivotY(pivotY);
        }

        static float getRotation(View view) {
            return view.getRotation();
        }

        static void setRotation(View view, float rotation) {
            view.setRotation(rotation);
        }

        static float getRotationX(View view) {
            return view.getRotationX();
        }

        static void setRotationX(View view, float rotationX) {
            view.setRotationX(rotationX);
        }

        static float getRotationY(View view) {
            return view.getRotationY();
        }

        static void setRotationY(View view, float rotationY) {
            view.setRotationY(rotationY);
        }

        static float getScaleX(View view) {
            return view.getScaleX();
        }

        static void setScaleX(View view, float scaleX) {
            view.setScaleX(scaleX);
        }

        static float getScaleY(View view) {
            return view.getScaleY();
        }

        static void setScaleY(View view, float scaleY) {
            view.setScaleY(scaleY);
        }

        static float getScrollX(View view) {
            return view.getScrollX();
        }

        static void setScrollX(View view, int scrollX) {
            view.setScrollX(scrollX);
        }

        static float getScrollY(View view) {
            return view.getScrollY();
        }

        static void setScrollY(View view, int scrollY) {
            view.setScrollY(scrollY);
        }

        static float getTranslationX(View view) {
            return view.getTranslationX();
        }

        static void setTranslationX(View view, float translationX) {
            view.setTranslationX(translationX);
        }

        static float getTranslationY(View view) {
            return view.getTranslationY();
        }

        static void setTranslationY(View view, float translationY) {
            view.setTranslationY(translationY);
        }

        static float getX(View view) {
            return view.getX();
        }

        static void setX(View view, float x) {
            view.setX(x);
        }

        static float getY(View view) {
            return view.getY();
        }

        static void setY(View view, float y) {
            view.setY(y);
        }
    }
}
