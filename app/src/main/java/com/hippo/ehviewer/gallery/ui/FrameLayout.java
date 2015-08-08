package com.hippo.ehviewer.gallery.ui;

public class FrameLayout extends GLView {

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (int i = 0, n = getComponentCount(); i < n; i++) {
            GLView component = getComponent(i);
            measureComponent(component, widthSpec, heightSpec);
            maxWidth = Math.max(maxWidth, component.getMeasuredWidth());
            maxHeight = Math.max(maxHeight, component.getMeasuredHeight());
        }

        // Consider min
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());

        // The final
        maxWidth = getDefaultSize(maxWidth, widthSpec);
        maxHeight = getDefaultSize(maxHeight, heightSpec);

        setMeasuredSize(maxWidth, maxHeight);

        if (MeasureSpec.getSize(widthSpec) != MeasureSpec.EXACTLY ||
                MeasureSpec.getSize(heightSpec) != MeasureSpec.EXACTLY) {
            // Measure again
            measureAllComponents(this, MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
        int width = getWidth();
        int height = getHeight();

        for (int i = 0, n = getComponentCount(); i < n; i++) {
            GLView component = getComponent(i);
            int measureWidth = component.getMeasuredWidth();
            int measureHeight = component.getMeasuredHeight();
            int componentLeft;
            int componentTop;

            GravityLayoutParams lp = (GravityLayoutParams) component.getLayoutParams();
            int gravity = lp.gravity;
            if (Gravity.centerHorizontal(gravity)) {
                componentLeft = (width / 2) - (measureWidth / 2);
            } else if (Gravity.right(gravity)) {
                componentLeft = width - measureWidth;
            } else {
                componentLeft = 0;
            }
            if (Gravity.centerVertical(gravity)) {
                componentTop = (height / 2) - (measureHeight / 2);
            } else if (Gravity.bottom(gravity)) {
                componentTop = height - measureHeight;
            } else {
                componentTop = 0;
            }

            component.layout(componentLeft, componentTop,
                    componentLeft + measureWidth, componentTop + measureHeight);
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
