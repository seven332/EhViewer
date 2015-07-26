/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.hippo.ehviewer.gallery.ui;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import com.hippo.ehviewer.gallery.anim.CanvasAnimation;
import com.hippo.ehviewer.gallery.anim.StateTransitionAnimation;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.GLPaint;
import com.hippo.yorozuya.AssertUtils;

import java.util.ArrayList;

// GLView is a UI component. It can render to a GLCanvas and accept touch
// events. A GLView may have zero or more child GLView and they form a tree
// structure. The rendering and event handling will pass through the tree
// structure.
//
// A GLView tree should be attached to a GLRoot before event dispatching and
// rendering happens. GLView asks GLRoot to re-render or re-layout the
// GLView hierarchy using requestRender() and requestLayoutContentPane().
//
// The render() method is called in a separate thread. Before calling
// dispatchTouchEvent() and layout(), GLRoot acquires a lock to avoid the
// rendering thread running at the same time. If there are other entry points
// from main thread (like a Handler) in your GLView, you need to call
// lockRendering() if the rendering thread should not run at the same time.
//
public class GLView {
    private static final String TAG = "GLView";

    private static final boolean DEBUG_DRAW_BOUNDS = false;

    public static final int VISIBLE = 0;
    public static final int INVISIBLE = 1;

    private static final int FLAG_INVISIBLE = 1;
    private static final int FLAG_SET_MEASURED_SIZE = 2;
    private static final int FLAG_LAYOUT_REQUESTED = 4;

    public interface OnClickListener {
        void onClick(GLView v);
    }

    protected final static GLPaint mDrawBoundsPaint;

    static {
        mDrawBoundsPaint = new GLPaint();
        mDrawBoundsPaint.setColor(Color.RED);
        mDrawBoundsPaint.setLineWidth(2);
    }

    protected final Rect mBounds = new Rect();
    private final Rect mRenderCheckBounds = new Rect();
    protected final Rect mPaddings = new Rect();

    private GLRoot mRoot;
    protected GLView mParent;
    private ArrayList<GLView> mComponents;
    private GLView mMotionTarget;

    private CanvasAnimation mAnimation;

    private int mViewFlags = 0;

    protected int mMeasuredWidth = 0;
    protected int mMeasuredHeight = 0;

    private int mLastWidthSpec = -1;
    private int mLastHeightSpec = -1;

    protected int mScrollY = 0;
    protected int mScrollX = 0;
    protected int mScrollHeight = 0;
    protected int mScrollWidth = 0;

    /**
     * The minimum height of the view. We'll try our best to have the height
     * of this view to at least this amount.
     */
    private int mMinHeight;

    /**
     * The minimum width of the view. We'll try our best to have the width
     * of this view to at least this amount.
     */
    private int mMinWidth;

    private LayoutParams mLayoutParams;

    private float [] mBackgroundColor;
    private StateTransitionAnimation mTransition;

    public void startAnimation(CanvasAnimation animation) {
        GLRoot root = getGLRoot();
        if (root == null) throw new IllegalStateException();
        mAnimation = animation;
        if (mAnimation != null) {
            mAnimation.start();
            root.registerLaunchedAnimation(mAnimation);
        }
        invalidate();
    }

    // Sets the visiblity of this GLView (either GLView.VISIBLE or
    // GLView.INVISIBLE).
    public void setVisibility(int visibility) {
        if (visibility == getVisibility()) return;
        if (visibility == VISIBLE) {
            mViewFlags &= ~FLAG_INVISIBLE;
        } else {
            mViewFlags |= FLAG_INVISIBLE;
        }
        onVisibilityChanged(visibility);
        invalidate();
    }

    // Returns GLView.VISIBLE or GLView.INVISIBLE
    public int getVisibility() {
        return (mViewFlags & FLAG_INVISIBLE) == 0 ? VISIBLE : INVISIBLE;
    }

    // This should only be called on the content pane (the topmost GLView).
    public void attachToRoot(GLRoot root) {
        AssertUtils.assertTrue(mParent == null && mRoot == null);
        onAttachToRoot(root);
    }

    // TODO It is not called when the
    // This should only be called on the content pane (the topmost GLView).
    public void detachFromRoot() {
        AssertUtils.assertTrue(mParent == null && mRoot != null);
        onDetachFromRoot();
    }

    // Returns the number of children of the GLView.
    public int getComponentCount() {
        return mComponents == null ? 0 : mComponents.size();
    }

    // Returns the children for the given index.
    public GLView getComponent(int index) {
        if (mComponents == null) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return mComponents.get(index);
    }

    // Adds a child to this GLView.
    public void addComponent(GLView component) {
        addComponent(component, -1, null);
    }

    public void addComponent(GLView component, int index) {
        addComponent(component, index, null);
    }

    public void addComponent(GLView component, LayoutParams params) {
        addComponent(component, -1, params);
    }

    public void addComponent(GLView component, int index, LayoutParams params) {
        // Make component is not null
        if (component == null) {
            throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
        }
        // Make sure the component doesn't have a parent currently.
        if (component.mParent != null) {
            throw new IllegalStateException(
                    "Component " + this + " being added, but it already has a parent");
        }

        // Ensure index is valid
        if (index < 0) {
            index = getComponentCount();
        }

        // Ensure params is valid
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }

        // Build parent-child links
        if (mComponents == null) {
            mComponents = new ArrayList<>();
        }

        mComponents.add(index, component);
        component.mParent = this;
        component.setLayoutParams(params);

        // If this is added after we have a root, tell the component.
        if (mRoot != null) {
            component.onAttachToRoot(mRoot);
        }
    }

    // Removes a child from this GLView.
    public boolean removeComponent(GLView component) {
        if (mComponents == null) return false;
        if (mComponents.remove(component)) {
            removeOneComponent(component);
            return true;
        }
        return false;
    }

    public boolean removeComponentAt(int index) {
        if (mComponents == null) {
            return false;
        }
        if (index >= 0 && index < getComponentCount()) {
            GLView component = mComponents.remove(index);
            removeOneComponent(component);
            return true;
        } else {
            return false;
        }
    }

    // Removes all children of this GLView.
    public void removeAllComponents() {
        for (int i = 0, n = mComponents.size(); i < n; ++i) {
            removeOneComponent(mComponents.get(i));
        }
        mComponents.clear();
    }

    private void removeOneComponent(GLView component) {
        if (mMotionTarget == component) {
            long now = SystemClock.uptimeMillis();
            MotionEvent cancelEvent = MotionEvent.obtain(
                    now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
            dispatchTouchEvent(cancelEvent);
            cancelEvent.recycle();
        }
        component.onDetachFromRoot();
        component.mParent = null;
    }

    public Rect bounds() {
        return mBounds;
    }

    public int getWidth() {
        return mBounds.right - mBounds.left;
    }

    public int getHeight() {
        return mBounds.bottom - mBounds.top;
    }

    /**
     * Offset this GLView's vertical location by the specified number of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public void offsetTopAndBottom(int offset) {
        if (offset != 0){
            mBounds.offset(0, offset);
            invalidate();
        }
    }

    /**
     * Offset this GLView's horizontal location by the specified amount of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public void offsetLeftAndRight(int offset) {
        if (offset != 0){
            mBounds.offset(offset, 0);
            invalidate();
        }
    }

    public GLRoot getGLRoot() {
        return mRoot;
    }

    // Request re-rendering of the view hierarchy.
    // This is used for animation or when the contents changed.
    public void invalidate() {
        GLRoot root = getGLRoot();
        if (root != null) root.requestRender();
    }

    // Request re-layout of the view hierarchy.
    public void requestLayout() {
        mViewFlags |= FLAG_LAYOUT_REQUESTED;
        mLastWidthSpec = -1;
        mLastHeightSpec = -1;
        if (mParent != null) {
            mParent.requestLayout();
        } else {
            // Is this a content pane ?
            GLRoot root = getGLRoot();
            if (root != null) root.requestLayoutContentPane();
        }
    }

    protected void render(GLCanvas canvas) {
        boolean transitionActive = false;
        if (mTransition != null && mTransition.calculate(AnimationTime.get())) {
            invalidate();
            transitionActive = mTransition.isActive();
        }
        renderBackground(canvas);
        onRender(canvas);
        canvas.save();
        if (transitionActive) {
            mTransition.applyContentTransform(this, canvas);
        }
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            GLView component = getComponent(i);
            if (Rect.intersects(mRenderCheckBounds, component.mBounds)) {
                renderChild(canvas, getComponent(i));
            }
        }
        canvas.restore();

        if (DEBUG_DRAW_BOUNDS) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), mDrawBoundsPaint);
        }

        if (transitionActive) {
            mTransition.applyOverlay(this, canvas);
        }
    }

    protected void onRender(GLCanvas canvas) {
    }

    public void setIntroAnimation(StateTransitionAnimation intro) {
        mTransition = intro;
        if (mTransition != null) mTransition.start();
    }

    public float [] getBackgroundColor() {
        return mBackgroundColor;
    }

    public void setBackgroundColor(float [] color) {
        mBackgroundColor = color;
    }

    protected void renderBackground(GLCanvas view) {
        if (mBackgroundColor != null) {
            view.clearBuffer(mBackgroundColor);
        }
        if (mTransition != null && mTransition.isActive()) {
            mTransition.applyBackground(this, view);
        }
    }

    protected void renderChild(GLCanvas canvas, GLView component) {
        if (component.getVisibility() != GLView.VISIBLE
                && component.mAnimation == null) return;

        int xoffset = component.mBounds.left - mScrollX;
        int yoffset = component.mBounds.top - mScrollY;

        canvas.translate(xoffset, yoffset);

        CanvasAnimation anim = component.mAnimation;
        if (anim != null) {
            canvas.save(anim.getCanvasSaveFlags());
            if (anim.calculate(AnimationTime.get())) {
                invalidate();
            } else {
                component.mAnimation = null;
            }
            anim.apply(canvas);
        }
        component.render(canvas);
        if (anim != null) canvas.restore();
        canvas.translate(-xoffset, -yoffset);
    }

    protected boolean onTouch(MotionEvent event) {
        return false;
    }

    protected boolean dispatchTouchEvent(MotionEvent event,
            int x, int y, GLView component, boolean checkBounds) {
        Rect rect = component.mBounds;
        int left = rect.left;
        int top = rect.top;
        if (!checkBounds || rect.contains(x, y)) {
            event.offsetLocation(-left, -top);
            if (component.dispatchTouchEvent(event)) {
                event.offsetLocation(left, top);
                return true;
            }
            event.offsetLocation(left, top);
        }
        return false;
    }

    protected boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int action = event.getAction();
        if (mMotionTarget != null) {
            if (action == MotionEvent.ACTION_DOWN) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                dispatchTouchEvent(cancel, x, y, mMotionTarget, false);
                mMotionTarget = null;
            } else {
                dispatchTouchEvent(event, x, y, mMotionTarget, false);
                if (action == MotionEvent.ACTION_CANCEL
                        || action == MotionEvent.ACTION_UP) {
                    mMotionTarget = null;
                }
                return true;
            }
        }
        if (action == MotionEvent.ACTION_DOWN) {
            // in the reverse rendering order
            for (int i = getComponentCount() - 1; i >= 0; --i) {
                GLView component = getComponent(i);
                if (component.getVisibility() != GLView.VISIBLE) continue;
                if (dispatchTouchEvent(event, x, y, component, true)) {
                    mMotionTarget = component;
                    return true;
                }
            }
        }
        return onTouch(event);
    }

    public Rect getPaddings() {
        return mPaddings;
    }

    public void layout(int left, int top, int right, int bottom) {
        boolean sizeChanged = setBounds(left, top, right, bottom);
        final boolean forceLayout = (mViewFlags & FLAG_LAYOUT_REQUESTED) == FLAG_LAYOUT_REQUESTED;
        if (sizeChanged || forceLayout) {
            onLayout(sizeChanged, left, top, right, bottom);
        }
        mViewFlags &= ~FLAG_LAYOUT_REQUESTED;
    }

    private boolean setBounds(int left, int top, int right, int bottom) {
        boolean sizeChanged = (right - left) != (mBounds.right - mBounds.left)
                || (bottom - top) != (mBounds.bottom - mBounds.top);
        mBounds.set(left, top, right, bottom);
        mRenderCheckBounds.set(0, 0, right - left, bottom - top);
        return sizeChanged;
    }

    public void measure(int widthSpec, int heightSpec) {
        final boolean forceLayout = (mViewFlags & FLAG_LAYOUT_REQUESTED) == FLAG_LAYOUT_REQUESTED;
        final boolean isExactly = MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY &&
                MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY;
        final boolean matchingSize = isExactly &&
                getMeasuredWidth() == MeasureSpec.getSize(widthSpec) &&
                getMeasuredHeight() == MeasureSpec.getSize(heightSpec);
        if (forceLayout || !matchingSize &&
                (widthSpec != mLastWidthSpec ||
                        heightSpec != mLastHeightSpec)) {
            mLastWidthSpec = widthSpec;
            mLastHeightSpec = heightSpec;

            mViewFlags &= ~FLAG_SET_MEASURED_SIZE;
            onMeasure(widthSpec, heightSpec);
            if ((mViewFlags & FLAG_SET_MEASURED_SIZE) == 0) {
                throw new IllegalStateException(getClass().getName()
                        + " should call setMeasuredSize() in onMeasure()");
            }
        }
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredSize(getDefaultSize(getMinimumWidth(), widthSpec),
                getDefaultSize(getMinimumHeight(), heightSpec));
    }

    /**
     * Returns the minimum height of the view.
     *
     * @return the minimum height the view will try to be.
     *
     * @see #setMinimumHeight(int)
     */
    public int getMinimumHeight() {
        return mMinHeight;
    }

    /**
     * Sets the minimum height of the view. It is not guaranteed the view will
     * be able to achieve this minimum height (for example, if its parent layout
     * constrains it with less available height).
     *
     * @param minHeight The minimum height the view will try to be.
     *
     * @see #getMinimumHeight()
     */
    public void setMinimumHeight(int minHeight) {
        mMinHeight = minHeight;
        requestLayout();
    }

    /**
     * Returns the minimum width of the view.
     *
     * @return the minimum width the view will try to be.
     *
     * @see #setMinimumWidth(int)
     */
    public int getMinimumWidth() {
        return mMinWidth;
    }

    /**
     * Sets the minimum width of the view. It is not guaranteed the view will
     * be able to achieve this minimum width (for example, if its parent layout
     * constrains it with less available width).
     *
     * @param minWidth The minimum width the view will try to be.
     *
     * @see #getMinimumWidth()
     */
    public void setMinimumWidth(int minWidth) {
        mMinWidth = minWidth;
        requestLayout();

    }

    protected void setMeasuredSize(int width, int height) {
        mViewFlags |= FLAG_SET_MEASURED_SIZE;
        mMeasuredWidth = width;
        mMeasuredHeight = height;
    }

    public int getMeasuredWidth() {
        return mMeasuredWidth;
    }

    public int getMeasuredHeight() {
        return mMeasuredHeight;
    }

    protected void onLayout(
            boolean changeSize, int left, int top, int right, int bottom) {
    }

    /**
     * Utility to return a default size. Uses the supplied size if the
     * MeasureSpec imposed no constraints. Will get larger if allowed
     * by the MeasureSpec.
     *
     * @param size Default size for this view
     * @param measureSpec Constraints imposed by the parent
     * @return The size this view should be.
     */
    public static int getDefaultSize(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
            case MeasureSpec.AT_MOST:
                return size == 0 ? specSize : Math.min(size, specSize);
        }
        return result;
    }

    public static int getComponentSpec(int spec, int childSize) {
        int specMode = MeasureSpec.getMode(spec);
        int specSize = MeasureSpec.getSize(spec);

        int size = Math.max(0, specSize);

        int resultSize = 0;
        int resultMode = 0;

        switch (specMode) {
            // Parent has imposed an exact size on us
            case MeasureSpec.EXACTLY:
                if (childSize >= 0) {
                    resultSize = childSize;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childSize == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size. So be it.
                    resultSize = size;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childSize == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;

            // Parent has imposed a maximum size on us
            case MeasureSpec.AT_MOST:
                if (childSize >= 0) {
                    // Child wants a specific size... so be it
                    resultSize = childSize;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childSize == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size, but our size is not fixed.
                    // Constrain child to not be bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                } else if (childSize == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;

            // Parent asked to see how big we want to be
            case MeasureSpec.UNSPECIFIED:
                if (childSize >= 0) {
                    // Child wants a specific size... let him have it
                    resultSize = childSize;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childSize == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size... find out how big it should
                    // be
                    resultSize = 0;
                    resultMode = MeasureSpec.UNSPECIFIED;
                } else if (childSize == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size.... find out how
                    // big it should be
                    resultSize = 0;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
                break;
        }
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    public static void measureComponent(GLView component, int widthSpec, int heightSpec) {
        final LayoutParams lp = component.getLayoutParams();
        final int componentWidthSpec = getComponentSpec(widthSpec, lp.width);
        final int componentHeightSpec = getComponentSpec(heightSpec, lp.height);
        component.measure(componentWidthSpec, componentHeightSpec);
    }

    public static void measureAllComponents(GLView parent, int widthSpec, int heightSpec) {
        for (int i = 0, n = parent.getComponentCount(); i < n; i++) {
            GLView component = parent.getComponent(i);
            measureComponent(component, widthSpec, heightSpec);
        }
    }

    public LayoutParams getLayoutParams() {
        return mLayoutParams;
    }

    public void setLayoutParams(LayoutParams params) {
        if (params == null) {
            if (mParent == null) {
                throw new NullPointerException("Layout parameters cannot be null");
            } else {
                params = mParent.generateDefaultLayoutParams();
            }
        }
        mLayoutParams = params;
        requestLayout();
    }

    protected boolean checkLayoutParams(LayoutParams p) {
        return  p != null;
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new LayoutParams(p);
    }

    /**
     * Gets the bounds of the given descendant that relative to this view.
     */
    public boolean getBoundsOf(GLView descendant, Rect out) {
        int xoffset = 0;
        int yoffset = 0;
        GLView view = descendant;
        while (view != this) {
            if (view == null) return false;
            Rect bounds = view.mBounds;
            xoffset += bounds.left;
            yoffset += bounds.top;
            view = view.mParent;
        }
        out.set(xoffset, yoffset, xoffset + descendant.getWidth(),
                yoffset + descendant.getHeight());
        return true;
    }

    protected void onVisibilityChanged(int visibility) {
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            GLView child = getComponent(i);
            if (child.getVisibility() == GLView.VISIBLE) {
                child.onVisibilityChanged(visibility);
            }
        }
    }

    protected void onAttachToRoot(GLRoot root) {
        mRoot = root;
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            getComponent(i).onAttachToRoot(root);
        }
    }

    protected void onDetachFromRoot() {
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            getComponent(i).onDetachFromRoot();
        }
        mRoot = null;
    }

    public void lockRendering() {
        if (mRoot != null) {
            mRoot.lockRenderThread();
        }
    }

    public void unlockRendering() {
        if (mRoot != null) {
            mRoot.unlockRenderThread();
        }
    }

    // This is for debugging only.
    // Dump the view hierarchy into log.
    void dumpTree(String prefix) {
        Log.d(TAG, prefix + getClass().getSimpleName());
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            getComponent(i).dumpTree(prefix + "....");
        }
    }

    /**
     * A MeasureSpec encapsulates the layout requirements passed from parent to child.
     * Each MeasureSpec represents a requirement for either the width or the height.
     * A MeasureSpec is comprised of a size and a mode. There are three possible
     * modes:
     * <dl>
     * <dt>UNSPECIFIED</dt>
     * <dd>
     * The parent has not imposed any constraint on the child. It can be whatever size
     * it wants.
     * </dd>
     *
     * <dt>EXACTLY</dt>
     * <dd>
     * The parent has determined an exact size for the child. The child is going to be
     * given those bounds regardless of how big it wants to be.
     * </dd>
     *
     * <dt>AT_MOST</dt>
     * <dd>
     * The child can be as large as it wants up to the specified size.
     * </dd>
     * </dl>
     *
     * MeasureSpecs are implemented as ints to reduce object allocation. This class
     * is provided to pack and unpack the &lt;size, mode&gt; tuple into the int.
     */
    public static class MeasureSpec {
        private static final int MODE_SHIFT = 30;
        private static final int MODE_MASK  = 0x3 << MODE_SHIFT;

        /**
         * Measure specification mode: The parent has not imposed any constraint
         * on the child. It can be whatever size it wants.
         */
        public static final int UNSPECIFIED = 0 << MODE_SHIFT;

        /**
         * Measure specification mode: The parent has determined an exact size
         * for the child. The child is going to be given those bounds regardless
         * of how big it wants to be.
         */
        public static final int EXACTLY     = 1 << MODE_SHIFT;

        /**
         * Measure specification mode: The child can be as large as it wants up
         * to the specified size.
         */
        public static final int AT_MOST     = 2 << MODE_SHIFT;

        /**
         * The max size allowed
         */
        public static final int MAX_SIZE    = ~MODE_MASK;

        /**
         * Creates a measure specification based on the supplied size and mode.
         *
         * The mode must always be one of the following:
         * <ul>
         *  <li>{@link #UNSPECIFIED}</li>
         *  <li>{@link #EXACTLY}</li>
         *  <li>{@link #AT_MOST}</li>
         * </ul>
         *
         * <p><strong>Note:</strong> On API level 17 and lower, makeMeasureSpec's
         * implementation was such that the order of arguments did not matter
         * and overflow in either value could impact the resulting MeasureSpec.
         * {@link android.widget.RelativeLayout} was affected by this bug.
         * Apps targeting API levels greater than 17 will get the fixed, more strict
         * behavior.</p>
         *
         * @param size the size of the measure specification
         * @param mode the mode of the measure specification
         * @return the measure specification based on size and mode
         */
        public static int makeMeasureSpec(int size, int mode) {
            return (size & ~MODE_MASK) | (mode & MODE_MASK);
        }

        /**
         * Extracts the mode from the supplied measure specification.
         *
         * @param measureSpec the measure specification to extract the mode from
         * @return {@link #UNSPECIFIED},
         *         {@link #AT_MOST} or
         *         {@link #EXACTLY}
         */
        public static int getMode(int measureSpec) {
            return (measureSpec & MODE_MASK);
        }

        /**
         * Extracts the size from the supplied measure specification.
         *
         * @param measureSpec the measure specification to extract the size from
         * @return the size in pixels defined in the supplied measure specification
         */
        public static int getSize(int measureSpec) {
            return (measureSpec & ~MODE_MASK);
        }

        /**
         * Returns a String representation of the specified measure
         * specification.
         *
         * @param measureSpec the measure specification to convert to a String
         * @return a String with the following format: "MeasureSpec: MODE SIZE"
         */
        public static String toString(int measureSpec) {
            int mode = getMode(measureSpec);
            int size = getSize(measureSpec);

            StringBuilder sb = new StringBuilder("MeasureSpec: ");

            if (mode == UNSPECIFIED)
                sb.append("UNSPECIFIED ");
            else if (mode == EXACTLY)
                sb.append("EXACTLY ");
            else if (mode == AT_MOST)
                sb.append("AT_MOST ");
            else
                sb.append(mode).append(" ");

            sb.append(size);
            return sb.toString();
        }
    }

    /**
     * The LayoutParams look like {@link android.view.ViewGroup.LayoutParams},
     * and work like it.
     */
    public static class LayoutParams {

        /**
         * Special value for the height or width requested by a View.
         * MATCH_PARENT means that the view wants to be as big as its parent,
         * minus the parent's padding, if any. Introduced in API Level 8.
         */
        public static final int MATCH_PARENT = -1;

        /**
         * Special value for the height or width requested by a View.
         * WRAP_CONTENT means that the view wants to be just large enough to fit
         * its own internal content, taking its own padding into account.
         */
        public static final int WRAP_CONTENT = -2;

        /**
         * Information about how wide the view wants to be. Can be one of the
         * constants {@link #MATCH_PARENT} or {@link #WRAP_CONTENT} or an exact size.
         */
        public int width;

        /**
         * Information about how tall the view wants to be. Can be one of the
         * constants {@link #MATCH_PARENT} or {@link #WRAP_CONTENT} or an exact size.
         */
        public int height;

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width the width, either {@link #WRAP_CONTENT},
         *        {@link #MATCH_PARENT}, or a fixed size in pixels
         * @param height the height, either {@link #WRAP_CONTENT},
         *        {@link #MATCH_PARENT}, or a fixed size in pixels
         */
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Copy constructor. Clones the width and height values of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(LayoutParams source) {
            this.width = source.width;
            this.height = source.height;
        }
    }

    /**
     * LayoutParams with gravity inside
     */
    public static class GravityLayoutParams extends LayoutParams {

        public int gravity = Gravity.NO_GRAVITY;

        public GravityLayoutParams(GLView.LayoutParams source) {
            super(source);
        }

        public GravityLayoutParams(int width, int height) {
            super(width, height);
        }
    }
}
