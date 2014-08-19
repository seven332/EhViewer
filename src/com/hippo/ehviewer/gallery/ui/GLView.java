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

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.hippo.ehviewer.gallery.anim.CanvasAnimation;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.Utils;

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

    public static final int VISIBLE = 0;
    public static final int INVISIBLE = 1;

    private static final int FLAG_INVISIBLE = 1;
    private static final int FLAG_SET_MEASURED_SIZE = 2;
    private static final int FLAG_LAYOUT_REQUESTED = 4;

    public interface OnClickListener {
        void onClick(GLView v);
    }

    protected final Rect mBounds = new Rect();
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

    private float [] mBackgroundColor;

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
        Utils.assertTrue(mParent == null && mRoot == null);
        onAttachToRoot(root);
    }

    // This should only be called on the content pane (the topmost GLView).
    public void detachFromRoot() {
        Utils.assertTrue(mParent == null && mRoot != null);
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
        // Make sure the component doesn't have a parent currently.
        if (component.mParent != null) throw new IllegalStateException();

        // Build parent-child links
        if (mComponents == null) {
            mComponents = new ArrayList<GLView>();
        }
        mComponents.add(component);
        component.mParent = this;

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
        mLastHeightSpec = -1;
        mLastWidthSpec = -1;
        if (mParent != null) {
            mParent.requestLayout();
        } else {
            // Is this a content pane ?
            GLRoot root = getGLRoot();
            if (root != null) root.requestLayoutContentPane();
        }
    }

    protected void render(GLCanvas canvas) {
        renderBackground(canvas);
        canvas.save();
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            renderChild(canvas, getComponent(i));
        }
        canvas.restore();
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

    @SuppressLint("WrongCall")
    public void layout(int left, int top, int right, int bottom) {
        boolean sizeChanged = setBounds(left, top, right, bottom);
        mViewFlags &= ~FLAG_LAYOUT_REQUESTED;
        // We call onLayout no matter sizeChanged is true or not because the
        // orientation may change without changing the size of the View (for
        // example, rotate the device by 180 degrees), and we want to handle
        // orientation change in onLayout.
        onLayout(sizeChanged, left, top, right, bottom);
    }

    private boolean setBounds(int left, int top, int right, int bottom) {
        boolean sizeChanged = (right - left) != (mBounds.right - mBounds.left)
                || (bottom - top) != (mBounds.bottom - mBounds.top);
        mBounds.set(left, top, right, bottom);
        return sizeChanged;
    }

    @SuppressLint("WrongCall")
    public void measure(int widthSpec, int heightSpec) {
        if (widthSpec == mLastWidthSpec && heightSpec == mLastHeightSpec
                && (mViewFlags & FLAG_LAYOUT_REQUESTED) == 0) {
            return;
        }

        mLastWidthSpec = widthSpec;
        mLastHeightSpec = heightSpec;

        mViewFlags &= ~FLAG_SET_MEASURED_SIZE;
        onMeasure(widthSpec, heightSpec);
        if ((mViewFlags & FLAG_SET_MEASURED_SIZE) == 0) {
            throw new IllegalStateException(getClass().getName()
                    + " should call setMeasuredSize() in onMeasure()");
        }
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
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
}
