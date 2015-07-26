package com.hippo.ehviewer.gallery.ui;

public class LinearView extends GLView {

    public int mInterval;

    public void setInterval(int interval) {
        mInterval = interval;
        requestLayout();
    }

    // TODO
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        measureAllComponents(this, widthSpec, heightSpec);

        int maxWidth = 0;
        int maxHeight = 0;
        int n = getComponentCount();
        for (int i = 0; i < n; i++) {
            GLView component = getComponent(i);
            maxWidth = Math.max(maxWidth, component.getMeasuredWidth());
            if (i != 0) {
                maxHeight += mInterval;
            }
            maxHeight += component.getMeasuredHeight();
        }

        setMeasuredSize(getDefaultSize(maxWidth, widthSpec), getDefaultSize(maxHeight, heightSpec));
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
        int width = getWidth();
        int componentLeft;
        int componentTop = 0;

        for (int i = 0, n = getComponentCount(); i < n; i++) {
            GLView component = getComponent(i);
            int measureWidth = component.getMeasuredWidth();
            int measureHeight = component.getMeasuredHeight();

            GravityLayoutParams lp = (GravityLayoutParams) component.getLayoutParams();
            int gravity = lp.gravity;
            if (Gravity.centerHorizontal(gravity)) {
                componentLeft = (width / 2) - (measureWidth / 2);
            } else if (Gravity.right(gravity)) {
                componentLeft = width - measureWidth;
            } else {
                componentLeft = 0;
            }

            component.layout(componentLeft, componentTop,
                    componentLeft + measureWidth, componentTop + measureHeight);

            componentTop += measureHeight + mInterval;
        }
    }

    @Override
    protected boolean checkLayoutParams(GLView.LayoutParams p) {
        return p instanceof GravityLayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new GravityLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(GLView.LayoutParams p) {
        return p == null ? generateDefaultLayoutParams() : new GravityLayoutParams(p);
    }
}
