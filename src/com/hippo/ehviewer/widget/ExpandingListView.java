/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * A custom listview which supports the preview of extra content corresponding to each cell
 * by clicking on the cell to hide and show the extra content.
 */
public class ExpandingListView extends ListView {

    @SuppressWarnings("unused")
    private static final String TAG = ExpandingListView.class.getSimpleName();

    private int mExpandingId;
    private boolean mShouldRemoveObserver = false;
    private boolean mAnimating = false;

    private final List<View> mViewsToDraw = new ArrayList<View>();

    private int[] mTranslate;

    public ExpandingListView(Context context) {
        super(context);
        init();
    }

    public ExpandingListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExpandingListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOnItemClickListener(mItemClickListener);
    }

    public void setExpandingId(int id) {
        mExpandingId = id;
    }

    public boolean isAnimating() {
        return mAnimating;
    }

    /**
     * Listens for item clicks and expands or collapses the selected view depending on
     * its current state.
     */
    private final AdapterView.OnItemClickListener mItemClickListener = new AdapterView
            .OnItemClickListener() {
        @Override
        public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
            if (view.findViewById(mExpandingId).getVisibility() == View.GONE) {
                expandView(view);
            } else {
                collapseView(view);
            }
        }
    };



    /**
     * Calculates the top and bottom bound changes of the selected item. These values are
     * also used to move the bounds of the items around the one that is actually being
     * expanded or collapsed.
     *
     * This method can be modified to achieve different user experiences depending
     * on how you want the cells to expand or collapse. In this specific demo, the cells
     * always try to expand downwards (leaving top bound untouched), and similarly,
     * collapse upwards (leaving top bound untouched). If the change in bounds
     * results in the complete disappearance of a cell, its lower bound is moved is
     * moved to the top of the screen so as not to hide any additional content that
     * the user has not interacted with yet. Furthermore, if the collapsed cell is
     * partially off screen when it is first clicked, it is translated such that its
     * full contents are visible. Lastly, this behaviour varies slightly near the bottom
     * of the listview in order to account for the fact that the bottom bounds of the actual
     * listview cannot be modified.
     */
    private int[] getTopAndBottomTranslations(int top, int bottom, int yDelta, View view,
                                              boolean isExpanding) {
        int yTranslateTop = 0;
        int yTranslateBottom = yDelta;

        int height = bottom - top;
        int bottomLine = getHeight() - getPaddingBottom();

        if (isExpanding) {
            boolean isOverTop = top < getPaddingTop();
            boolean isBelowBottom = (top + height + yDelta) > bottomLine;
            if (isOverTop) {
                yTranslateTop = top - getPaddingTop();
                yTranslateBottom = yDelta - yTranslateTop;
            } else if (isBelowBottom){
                int deltaBelow = top + height + yDelta - bottomLine;
                yTranslateTop = top - deltaBelow < getPaddingTop() ? (top - getPaddingTop()) : deltaBelow;
                yTranslateBottom = yDelta - yTranslateTop;
            }
        } else if (view.getParent() != null) {
            int index = 0;
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (getChildAt(i).equals(view)) {
                    index = i;
                    break;
                }
            }

            int maxBottom = bottom;
            for (int i = index + 1; i < getChildCount(); i++) {
                View v = getChildAt(i);
                maxBottom += v.getVisibility() != View.GONE ? v.getHeight() + getDividerHeight() : 0;
            }

            int newMaxBottom = maxBottom - yDelta;

            boolean isCollapsingAboveBottom = maxBottom >= bottomLine && newMaxBottom < bottomLine;

            if (isCollapsingAboveBottom) {
                yTranslateTop = bottomLine - newMaxBottom;
                yTranslateBottom = yDelta - yTranslateTop;
            }
        }

        return new int[] {yTranslateTop, yTranslateBottom};
    }

    /**
     * This method expands the view that was clicked and animates all the views
     * around it to make room for the expanding view. There are several steps required
     * to do this which are outlined below.
     *
     * 1. Store the current top and bottom bounds of each visible item in the listview.
     * 2. Update the layout parameters of the selected view. In the context of this
     *    method, the view should be originally collapsed and set to some custom height.
     *    The layout parameters are updated so as to wrap the content of the additional
     *    text that is to be displayed.
     *
     * After invoking a layout to take place, the listview will order all the items
     * such that there is space for each view. This layout will be independent of what
     * the bounds of the items were prior to the layout so two pre-draw passes will
     * be made. This is necessary because after the layout takes place, some views that
     * were visible before the layout may now be off bounds but a reference to these
     * views is required so the animation completes as intended.
     *
     * 3. The first predraw pass will set the bounds of all the visible items to
     *    their original location before the layout took place and then force another
     *    layout. Since the bounds of the cells cannot be set directly, the method
     *    setSelectionFromTop can be used to achieve a very similar effect.
     * 4. The expanding view's bounds are animated to what the final values should be
     *    from the original bounds.
     * 5. The bounds above the expanding view are animated upwards while the bounds
     *    below the expanding view are animated downwards.
     * 6. The extra text is faded in as its contents become visible throughout the
     *    animation process.
     *
     * It is important to note that the listview is disabled during the animation
     * because the scrolling behaviour is unpredictable if the bounds of the items
     * within the listview are not constant during the scroll.
     */

    private void expandView(final View view) {
        mAnimating = true;

        final ExpandableListItem viewObject = (ExpandableListItem)getItemAtPosition
                (getPositionForView(view));

        /* Store the original top and bottom bounds of all the cells.*/
        final int oldTop = view.getTop();
        final int oldBottom = view.getBottom();

        final HashMap<View, int[]> oldCoordinates = new HashMap<View, int[]>();

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            ViewCompat.setHasTransientState(v, true);
            oldCoordinates.put(v, new int[] {v.getTop(), v.getBottom()});
        }

        /* Update the layout so the extra content becomes visible.*/
        final View expandingLayout = view.findViewById(mExpandingId);
        expandingLayout.setVisibility(View.VISIBLE);

        /* Add an onPreDraw Listener to the listview. onPreDraw will get invoked after onLayout
        * and onMeasure have run but before anything has been drawn. This
        * means that the final post layout properties for all the items have already been
        * determined, but still have not been rendered onto the screen.*/
        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                /* Determine if this is the first or second pass.*/
                if (!mShouldRemoveObserver) {
                    mShouldRemoveObserver = true;

                    /* Calculate what the parameters should be for setSelectionFromTop.
                    * The ListView must be offset in a way, such that after the animation
                    * takes place, all the cells that remain visible are rendered completely
                    * by the ListView.*/
                    int newTop = view.getTop();
                    int newBottom = view.getBottom();

                    int newHeight = newBottom - newTop;
                    int oldHeight = oldBottom - oldTop;
                    int delta = newHeight - oldHeight;

                    mTranslate = getTopAndBottomTranslations(oldTop, oldBottom, delta, view, true);

                    int currentTop = view.getTop();
                    int futureTop = oldTop - mTranslate[0];

                    int firstChildStartTop = getChildAt(0).getTop();
                    int firstVisiblePosition = getFirstVisiblePosition();
                    int deltaTop = currentTop - futureTop;

                    int i;
                    int childCount = getChildCount();
                    for (i = 0; i < childCount; i++) {
                        View v = getChildAt(i);
                        int height = v.getBottom() - Math.max(0, v.getTop());
                        if (deltaTop - height > 0) {
                            firstVisiblePosition++;
                            deltaTop -= height;
                        } else {
                            break;
                        }
                    }

                    if (i > 0) {
                        firstChildStartTop = 0;
                    }

                    setSelectionFromTop(firstVisiblePosition, firstChildStartTop - deltaTop - getPaddingTop());

                    /* Return false such that the ListView does not redraw its contents on
                     * this layout but only updates all the parameters associated with its
                     * children.*/
                    return false;
                }

                /* Remove the predraw listener so this method does not keep getting called. */
                mShouldRemoveObserver = false;
                observer.removeOnPreDrawListener(this);

                int yTranslateTop = mTranslate[0];
                int yTranslateBottom = mTranslate[1];

                ArrayList <Animator> animations = new ArrayList<Animator>();

                int index = indexOfChild(view);

                /* Loop through all the views that were on the screen before the cell was
                *  expanded. Some cells will still be children of the ListView while
                *  others will not. The cells that remain children of the ListView
                *  simply have their bounds animated appropriately. The cells that are no
                *  longer children of the ListView also have their bounds animated, but
                *  must also be added to a list of views which will be drawn in dispatchDraw.*/
                for (View v: oldCoordinates.keySet()) {
                    int[] old = oldCoordinates.get(v);
                    v.setTop(old[0]);
                    v.setBottom(old[1]);
                    if (v.getParent() == null) {
                        mViewsToDraw.add(v);
                        int delta = old[0] < oldTop ? -yTranslateTop : yTranslateBottom;
                        animations.add(getAnimation(v, delta, delta));
                    } else {
                        int i = indexOfChild(v);
                        if (v != view) {
                            int delta = i > index ? yTranslateBottom : -yTranslateTop;
                            animations.add(getAnimation(v, delta, delta));
                        }
                        ViewCompat.setHasTransientState(v, false);
                    }
                }

                /* Adds animation for expanding the cell that was clicked. */
                animations.add(getAnimation(view, -yTranslateTop, yTranslateBottom));

                /* Adds an animation for fading in the extra content. */
                animations.add(ObjectAnimator.ofFloat(view.findViewById(mExpandingId),
                        View.ALPHA, 0, 1));

                /* Disabled the ListView for the duration of the animation.*/
                setEnabled(false);
                setClickable(false);

                /* Play all the animations created above together at the same time. */
                AnimatorSet s = new AnimatorSet();
                s.playTogether(animations);
                s.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAnimating = false;
                        viewObject.setExpanded(true);
                        setEnabled(true);
                        setClickable(true);
                        if (mViewsToDraw.size() > 0) {
                            for (View v : mViewsToDraw) {
                                ViewCompat.setHasTransientState(v, false);
                            }
                        }
                        mViewsToDraw.clear();
                    }
                });
                s.start();
                return true;
            }
        });
    }

    /**
     * By overriding dispatchDraw, we can draw the cells that disappear during the
     * expansion process. When the cell expands, some items below or above the expanding
     * cell may be moved off screen and are thus no longer children of the ListView's
     * layout. By storing a reference to these views prior to the layout, and
     * guaranteeing that these cells do not get recycled, the cells can be drawn
     * directly onto the canvas during the animation process. After the animation
     * completes, the references to the extra views can then be discarded.
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mViewsToDraw.size() == 0) {
            return;
        }

        for (View v: mViewsToDraw) {
            canvas.translate(0, v.getTop());
            v.draw(canvas);
            canvas.translate(0, -v.getTop());
        }
    }

    /**
     * This method collapses the view that was clicked and animates all the views
     * around it to close around the collapsing view. There are several steps required
     * to do this which are outlined below.
     *
     * 1. Update the layout parameters of the view clicked so as to minimize its height
     *    to the original collapsed (default) state.
     * 2. After invoking a layout, the listview will shift all the cells so as to display
     *    them most efficiently. Therefore, during the first predraw pass, the listview
     *    must be offset by some amount such that given the custom bound change upon
     *    collapse, all the cells that need to be on the screen after the layout
     *    are rendered by the listview.
     * 3. On the second predraw pass, all the items are first returned to their original
     *    location (before the first layout).
     * 4. The collapsing view's bounds are animated to what the final values should be.
     * 5. The bounds above the collapsing view are animated downwards while the bounds
     *    below the collapsing view are animated upwards.
     * 6. The extra text is faded out as its contents become visible throughout the
     *    animation process.
     */

     private void collapseView(final View view) {
         mAnimating = true;

         final ExpandableListItem viewObject = (ExpandableListItem)getItemAtPosition
                 (getPositionForView(view));

         /* Store the original top and bottom bounds of all the cells.*/
         final int oldTop = view.getTop();
         final int oldBottom = view.getBottom();

         final HashMap<View, int[]> oldCoordinates = new HashMap<View, int[]>();

         int childCount = getChildCount();
         for (int i = 0; i < childCount; i++) {
             View v = getChildAt(i);
             ViewCompat.setHasTransientState(v, true);
             oldCoordinates.put(v, new int [] {v.getTop(), v.getBottom()});
         }

         /* Update the layout so the extra content becomes gone.*/
         final View expandingLayout = view.findViewById(mExpandingId);
         expandingLayout.setVisibility(View.GONE);

         /* Add an onPreDraw listener. */
         final ViewTreeObserver observer = getViewTreeObserver();
         observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

             @Override
             public boolean onPreDraw() {

                 if (!mShouldRemoveObserver) {
                     /*Same as for expandingView, the parameters for setSelectionFromTop must
                     * be determined such that the necessary cells of the ListView are rendered
                     * and added to it.*/
                     mShouldRemoveObserver = true;

                     expandingLayout.setVisibility(View.VISIBLE);

                     int newTop = view.getTop();
                     int newBottom = view.getBottom();

                     int newHeight = newBottom - newTop;
                     int oldHeight = oldBottom - oldTop;
                     int deltaHeight = oldHeight - newHeight;

                     mTranslate = getTopAndBottomTranslations(oldTop, oldBottom, deltaHeight, view, false);

                     int currentTop = view.getTop();
                     int futureTop = oldTop + mTranslate[0];

                     int firstChildStartTop = getChildAt(0).getTop();
                     int firstVisiblePosition = getFirstVisiblePosition();
                     int deltaTop = currentTop - futureTop;

                     int i;
                     int childCount = getChildCount();
                     for (i = 0; i < childCount; i++) {
                         View v = getChildAt(i);
                         int height = v.getBottom() - Math.max(0, v.getTop());
                         if (deltaTop - height > 0) {
                             firstVisiblePosition++;
                             deltaTop -= height;
                         } else {
                             break;
                         }
                     }

                     if (i > 0) {
                         firstChildStartTop = 0;
                     }

                     setSelectionFromTop(firstVisiblePosition, firstChildStartTop - deltaTop - getPaddingTop());

                     return false;
                 }

                 mShouldRemoveObserver = false;
                 observer.removeOnPreDrawListener(this);

                 int yTranslateTop = mTranslate[0];
                 int yTranslateBottom = mTranslate[1];

                 int index = indexOfChild(view);
                 int childCount = getChildCount();
                 for (int i = 0; i < childCount; i++) {
                     View v = getChildAt(i);
                     int [] old = oldCoordinates.get(v);
                     if (old != null) {
                         /* If the cell was present in the ListView before the collapse and
                         * after the collapse then the bounds are reset to their old values.*/
                         v.setTop(old[0]);
                         v.setBottom(old[1]);
                         ViewCompat.setHasTransientState(v, false);
                     } else {
                         /* If the cell is present in the ListView after the collapse but
                          * not before the collapse then the bounds are calculated using
                          * the bottom and top translation of the collapsing cell.*/
                         int delta = i > index ? yTranslateBottom : -yTranslateTop;
                         v.setTop(v.getTop() + delta);
                         v.setBottom(v.getBottom() + delta);
                     }
                 }

                 final View expandingLayout = view.findViewById(mExpandingId);

                 /* Animates all the cells present on the screen after the collapse. */
                 ArrayList <Animator> animations = new ArrayList<Animator>();
                 for (int i = 0; i < childCount; i++) {
                     View v = getChildAt(i);
                     if (v != view) {
                         float diff = i > index ? -yTranslateBottom : yTranslateTop;
                         animations.add(getAnimation(v, diff, diff));
                     }
                 }

                 /* Adds animation for collapsing the cell that was clicked. */
                 animations.add(getAnimation(view, yTranslateTop, -yTranslateBottom));

                 /* Adds an animation for fading out the extra content. */
                 animations.add(ObjectAnimator.ofFloat(expandingLayout, View.ALPHA, 1, 0));

                 /* Disabled the ListView for the duration of the animation.*/
                 setEnabled(false);
                 setClickable(false);

                 /* Play all the animations created above together at the same time. */
                 AnimatorSet s = new AnimatorSet();
                 s.playTogether(animations);
                 s.addListener(new AnimatorListenerAdapter() {
                     @Override
                     public void onAnimationEnd(Animator animation) {
                         mAnimating = false;
                         expandingLayout.setVisibility(View.GONE);
                         view.setLayoutParams(new AbsListView.LayoutParams(AbsListView
                                 .LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
                         viewObject.setExpanded(false);
                         setEnabled(true);
                         setClickable(true);
                         /* Note that alpha must be set back to 1 in case this view is reused
                         * by a cell that was expanded, but not yet collapsed, so its state
                         * should persist in an expanded state with the extra content visible.*/
                         expandingLayout.setAlpha(1);
                     }
                 });
                 s.start();

                 return true;
             }
         });
     }

    /**
     * This method takes some view and the values by which its top and bottom bounds
     * should be changed by. Given these params, an animation which will animate
     * these bound changes is created and returned.
     */
    private Animator getAnimation(final View view, float translateTop, float translateBottom) {

        int top = view.getTop();
        int bottom = view.getBottom();

        int endTop = (int)(top + translateTop);
        int endBottom = (int)(bottom + translateBottom);

        PropertyValuesHolder translationTop = PropertyValuesHolder.ofInt("top", top, endTop);
        PropertyValuesHolder translationBottom = PropertyValuesHolder.ofInt("bottom", bottom,
                endBottom);

        return ObjectAnimator.ofPropertyValuesHolder(view, translationTop, translationBottom);
    }
}
