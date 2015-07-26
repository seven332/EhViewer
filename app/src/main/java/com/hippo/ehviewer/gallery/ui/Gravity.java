package com.hippo.ehviewer.gallery.ui;

public class Gravity {

    public static final int NO_GRAVITY = 0x0000;

    public static final int LEFT = 0x0001;
    public static final int TOP = 0x0002;
    public static final int RIGHT = 0x0004;
    public static final int BOTTOM = 0x0008;

    public static final int CENTER_HORIZONTAL = LEFT | RIGHT;
    public static final int CENTER_VERTICAL = TOP | BOTTOM;
    public static final int CENTER = CENTER_VERTICAL|CENTER_HORIZONTAL;

    public static final int HORIZONTAL_MASK = CENTER_HORIZONTAL;
    public static final int VERTICAL_MASK = CENTER_VERTICAL;

    public static boolean centerHorizontal(int gravity) {
        return (gravity & HORIZONTAL_MASK) == CENTER_HORIZONTAL;
    }

    public static boolean right(int gravity) {
        return (gravity & HORIZONTAL_MASK) == RIGHT;
    }

    public static boolean left(int gravity) {
        return (gravity & HORIZONTAL_MASK) == LEFT;
    }

    public static boolean centerVertical(int gravity) {
        return (gravity & VERTICAL_MASK) == CENTER_VERTICAL;
    }

    public static boolean top(int gravity) {
        return (gravity & VERTICAL_MASK) == TOP;
    }

    public static boolean bottom(int gravity) {
        return (gravity & VERTICAL_MASK) == BOTTOM;
    }
}
