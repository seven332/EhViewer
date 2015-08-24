/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.widget.recyclerview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.util.StateSet;
import android.view.ActionMode;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Checkable;

import com.hippo.util.DrawableUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.ViewUtils;

/**
 * Add setOnItemClickListener, setOnItemLongClickListener and setChoiceMode for
 * RecyclerView
 */
// Get some code from twoway-view and AbsListView.
public class EasyRecyclerView extends RecyclerView {

    /**
     * Represents an invalid position. All valid positions are in the range 0 to 1 less than the
     * number of items in the current adapter.
     */
    public static final int INVALID_POSITION = -1;

    /**
     * Normal list that does not indicate choices
     */
    public static final int CHOICE_MODE_NONE = 0;

    /**
     * The list allows up to one choice
     */
    public static final int CHOICE_MODE_SINGLE = 1;

    /**
     * The list allows multiple choices
     */
    public static final int CHOICE_MODE_MULTIPLE = 2;

    /**
     * The list allows multiple choices in a modal selection mode
     */
    public static final int CHOICE_MODE_MULTIPLE_MODAL = 3;

    /**
     * The list allows multiple choices in custom action
     */
    public static final int CHOICE_MODE_MULTIPLE_CUSTOM = 4;

    /**
     * Controls if/how the user may choose/check items in the list
     */
    private int mChoiceMode = CHOICE_MODE_NONE;

    /**
     * Controls CHOICE_MODE_MULTIPLE_MODAL. null when inactive.
     */
    private ActionMode mChoiceActionMode;

    /**
     * Wrapper for the multiple choice mode callback; AbsListView needs to perform
     * a few extra actions around what application code does.
     */
    MultiChoiceModeWrapper mMultiChoiceModeCallback;

    /**
     * Listener for custom multiple choices
     */
    private CustomChoiceListener mCustomChoiceListener;

    private boolean mCustomChoice = false;

    private boolean mOutOfCustomChoiceModing = false;

    private SparseBooleanArray mTempCheckStates;

    /**
     * Running count of how many items are currently checked
     */
    private int mCheckedItemCount;

    /**
     * Running state of which positions are currently checked
     */
    private SparseBooleanArray mCheckStates;

    /**
     * Running state of which IDs are currently checked.
     * If there is a value for a given key, the checked state for that ID is true
     * and the value holds the last known position in the adapter for that id.
     */
    private LongSparseArray<Integer> mCheckedIdStates;

    private Adapter mAdapter;

    private float mVerticalScrollFactor = 0;

    // For click and long click
    private boolean mPrePressed = false;
    private boolean mHasPerformedLongPress = false;
    private View mMotionView;
    private int mMotionPosition;
    private CheckForTap mPendingCheckForTap;
    private CheckForLongPress mPendingCheckForLongPress;
    private UnsetPressedState mUnsetPressedState;
    private PerformClick mPerformClick;
    private final float[] mTmpPoint = new float[2];
    private int mTouchSlop = -1;
    private float mStartX;
    private float mStartY;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    /**
     * Indicates whether the list selector should be drawn on top of the children or behind
     */
    boolean mDrawSelectorOnTop = false;
    /**
     * The drawable used to draw the selector
     */
    private Drawable mSelector;
    /**
     * The current position of the selector in the list.
     */
    private int mSelectorPosition = INVALID_POSITION;
    /**
     * Defines the selector's location and dimension at drawing time
     */
    private Rect mSelectorRect = new Rect();
    /**
     * The selection's left padding
     */
    int mSelectionLeftPadding = 0;
    /**
     * The selection's top padding
     */
    int mSelectionTopPadding = 0;
    /**
     * The selection's right padding
     */
    int mSelectionRightPadding = 0;
    /**
     * The selection's bottom padding
     */
    int mSelectionBottomPadding = 0;

    private OnDrawSelectorListener mOnDrawSelectorListener;

    private boolean mClipToPadding = false;

    public EasyRecyclerView(Context context) {
        super(context);
    }

    public EasyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EasyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        super.setClipToPadding(clipToPadding);

        mClipToPadding = clipToPadding;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {

        super.setAdapter(adapter);

        mAdapter = adapter;
        if (adapter != null) {
            if (mChoiceMode != CHOICE_MODE_NONE && adapter.hasStableIds() &&
                    mCheckedIdStates == null) {
                mCheckedIdStates = new LongSparseArray<>();
            }
        }

        if (mCheckStates != null) {
            mCheckStates.clear();
        }

        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
        }
    }

    void positionSelector(int position, View sel) {
        positionSelector(position, sel, false, -1, -1);
    }

    private void positionSelector(int position, View sel, boolean manageHotspot, float x, float y) {
        final boolean positionChanged = position != mSelectorPosition;
        if (position != INVALID_POSITION) {
            mSelectorPosition = position;
        }

        final Rect selectorRect = mSelectorRect;
        selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());

        // Adjust for selection padding.
        selectorRect.left -= mSelectionLeftPadding;
        selectorRect.top -= mSelectionTopPadding;
        selectorRect.right += mSelectionRightPadding;
        selectorRect.bottom += mSelectionBottomPadding;

        // Update the selector drawable.
        final Drawable selector = mSelector;
        if (selector != null) {
            if (positionChanged) {
                // Wipe out the current selector state so that we can start
                // over in the new position with a fresh state.
                selector.setVisible(false, false);
                selector.setState(StateSet.NOTHING);
            }
            selector.setBounds(selectorRect);
            if (positionChanged) {
                if (getVisibility() == VISIBLE) {
                    selector.setVisible(true, false);
                }
                updateSelectorState();
            }
            if (manageHotspot) {
                DrawableUtils.setHotspot(selector, x, y);
            }
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        boolean clipToPadding = mClipToPadding;
        int saveCount = 0;
        if (clipToPadding) {
            saveCount = canvas.save();
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.clipRect(scrollX + getPaddingLeft(), scrollY + getPaddingTop(),
                    scrollX + getRight() - getLeft() - getPaddingRight(),
                    scrollY + getBottom() - getTop() - getPaddingBottom());
        }

        // TODO disable selector drawable state change when need not to draw selector
        boolean drawSelector = mOnDrawSelectorListener == null ||
                mSelectorPosition < 0 ||
                mSelectorPosition >= mAdapter.getItemCount() ||
                mOnDrawSelectorListener.beforeDrawSelector(mSelectorPosition);
        final boolean drawSelectorOnTop = mDrawSelectorOnTop;

        if (drawSelector && !drawSelectorOnTop) {
            drawSelector(canvas);
        }

        super.dispatchDraw(canvas);

        if (drawSelector && drawSelectorOnTop) {
            drawSelector(canvas);
        }

        if (clipToPadding) {
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * Indicates whether this view is in a state where the selector should be drawn. This will
     * happen if we have focus but are not in touch mode, or we are in the middle of displaying
     * the pressed state for an item.
     *
     * @return True if the selector should be shown
     */
    boolean shouldShowSelector() {
        return (isFocused() && !isInTouchMode()) || isPressed();
    }

    private void drawSelector(Canvas canvas) {
        final Drawable selector = mSelector;
        if (selector != null && !mSelectorRect.isEmpty()) {
            selector.setBounds(mSelectorRect);
            selector.draw(canvas);
        }
    }

    public void setOnDrawSelectorListener(OnDrawSelectorListener listener) {
        mOnDrawSelectorListener = listener;
    }

    /**
     * Controls whether the selection highlight drawable should be drawn on top of the item or
     * behind it.
     *
     * @param onTop If true, the selector will be drawn on the item it is highlighting. The default
     *        is false.
     */
    public void setDrawSelectorOnTop(boolean onTop) {
        mDrawSelectorOnTop = onTop;
    }

    public void setSelector(Drawable sel) {
        if (mSelector != null) {
            mSelector.setCallback(null);
            unscheduleDrawable(mSelector);
        }
        mSelector = sel;
        Rect padding = new Rect();
        sel.getPadding(padding);
        mSelectionLeftPadding = padding.left;
        mSelectionTopPadding = padding.top;
        mSelectionRightPadding = padding.right;
        mSelectionBottomPadding = padding.bottom;
        sel.setCallback(this);
        updateSelectorState();
    }

    /**
     * Returns the selector {@link android.graphics.drawable.Drawable} that is used to draw the
     * selection in the list.
     *
     * @return the drawable used to display the selector
     */
    public Drawable getSelector() {
        return mSelector;
    }

    private void cancelDrawingSelector() {
        Rect rect = mSelectorRect;
        rect.left = 0;
        rect.top = 0;
        rect.right = 0;
        rect.bottom = 0;
    }

    void updateSelectorState() {
        if (mSelector != null) {
            if (shouldShowSelector()) {
                mSelector.setState(getDrawableState());
            } else {
                mSelector.setState(StateSet.NOTHING);
            }
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateSelectorState();
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // Don't dispatch setPressed to our children. We call setPressed on ourselves to
        // get the selector in the right state, but we don't want to press each child.
    }

    @Override
    public void dispatchDrawableHotspotChanged(float x, float y) {
        // Don't dispatch hotspot changes to children. We'll manually handle
        // calling drawableHotspotChanged on the correct child.
    }

    @Override
    public boolean verifyDrawable(Drawable dr) {
        return mSelector == dr || super.verifyDrawable(dr);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mSelector != null) {
            mSelector.jumpToCurrentState();
        }
    }

    @Override
    public void onChildAttachedToWindow(View child) {
        super.onChildAttachedToWindow(child);

        if (mCheckStates != null) {
            int position = getChildAdapterPosition(child);
            if (position >= 0) {
                setViewChecked(child, mCheckStates.get(position));
            }
        }
    }

    /**
     * Returns the number of items currently selected. This will only be valid
     * if the choice mode is not {@link #CHOICE_MODE_NONE} (default).
     *
     * <p>To determine the specific items that are currently selected, use one of
     * the <code>getChecked*</code> methods.
     *
     * @return The number of items currently selected
     *
     * @see #getCheckedItemPosition()
     * @see #getCheckedItemPositions()
     * @see #getCheckedItemIds()
     */
    public int getCheckedItemCount() {
        return mCheckedItemCount;
    }

    /**
     * Returns the checked state of the specified position. The result is only
     * valid if the choice mode has been set to {@link #CHOICE_MODE_SINGLE}
     * or {@link #CHOICE_MODE_MULTIPLE}.
     *
     * @param position The item whose checked state to return
     * @return The item's checked state or <code>false</code> if choice mode
     *         is invalid
     *
     * @see #setChoiceMode(int)
     */
    public boolean isItemChecked(int position) {
        //noinspection SimplifiableIfStatement
        if (mChoiceMode != CHOICE_MODE_NONE && mCheckStates != null) {
            return mCheckStates.get(position);
        }

        return false;
    }

    /**
     * Returns the currently checked item. The result is only valid if the choice
     * mode has been set to {@link #CHOICE_MODE_SINGLE}.
     *
     * @return The position of the currently checked item or
     *         {@link #INVALID_POSITION} if nothing is selected
     *
     * @see #setChoiceMode(int)
     */
    public int getCheckedItemPosition() {
        if (mChoiceMode == CHOICE_MODE_SINGLE && mCheckStates != null && mCheckStates.size() == 1) {
            return mCheckStates.keyAt(0);
        }

        return INVALID_POSITION;
    }

    /**
     * Returns the set of checked items in the list. The result is only valid if
     * the choice mode has not been set to {@link #CHOICE_MODE_NONE}.
     *
     * @return  A SparseBooleanArray which will return true for each call to
     *          get(int position) where position is a checked position in the
     *          list and false otherwise, or <code>null</code> if the choice
     *          mode is set to {@link #CHOICE_MODE_NONE}.
     */
    public SparseBooleanArray getCheckedItemPositions() {
        if (mChoiceMode != CHOICE_MODE_NONE) {
            return mCheckStates;
        }
        return null;
    }

    /**
     * Returns the set of checked items ids. The result is only valid if the
     * choice mode has not been set to {@link #CHOICE_MODE_NONE} and the adapter
     * has stable IDs. ({@link android.widget.ListAdapter#hasStableIds()} == {@code true})
     *
     * @return A new array which contains the id of each checked item in the
     *         list.
     */
    public long[] getCheckedItemIds() {
        if (mChoiceMode == CHOICE_MODE_NONE || mCheckedIdStates == null || mAdapter == null) {
            return new long[0];
        }

        final LongSparseArray<Integer> idStates = mCheckedIdStates;
        final int count = idStates.size();
        final long[] ids = new long[count];

        for (int i = 0; i < count; i++) {
            ids[i] = idStates.keyAt(i);
        }

        return ids;
    }

    public boolean inCustomChoice() {
        return mCustomChoice;
    }

    public void intoCustomChoiceMode() {
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM && !mCustomChoice) {
            mCustomChoice = true;

            mCustomChoiceListener.onIntoCustomChoice(this);
        }
    }

    public void outOfCustomChoiceMode() {
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM && mCustomChoice && !mOutOfCustomChoiceModing) {
            mOutOfCustomChoiceModing = true;

            // Copy mCheckStates
            mTempCheckStates.clear();
            for (int i = 0, n = mCheckStates.size(); i < n; i++) {
                mTempCheckStates.put(mCheckStates.keyAt(i), mCheckStates.valueAt(i));
            }
            // Uncheck remain checked items
            for (int i = 0, n = mTempCheckStates.size(); i < n; i++) {
                if (mTempCheckStates.valueAt(i)) {
                    setItemChecked(mTempCheckStates.keyAt(i), false);
                }
            }

            mCustomChoice = false;
            mCustomChoiceListener.onOutOfCustomChoice(this);

            mOutOfCustomChoiceModing = false;
        }
    }

    /**
     * Clear any choices previously set
     */
    public void clearChoices() {
        if (mCheckStates != null) {
            mCheckStates.clear();
        }
        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
        }
        mCheckedItemCount = 0;
        updateOnScreenCheckedViews();
    }

    public void toggleItemChecked(int position) {
        if (mCheckStates != null) {
            setItemChecked(position, !mCheckStates.get(position));
        }
    }

    /**
     * Sets the checked state of the specified position. The is only valid if
     * the choice mode has been set to {@link #CHOICE_MODE_SINGLE} or
     * {@link #CHOICE_MODE_MULTIPLE}.
     *
     * @param position The item whose checked state is to be checked
     * @param value The new checked state for the item
     */
    public void setItemChecked(int position, boolean value) {
        if (mChoiceMode == CHOICE_MODE_NONE) {
            return;
        }

        // Check is intoCheckMode
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM && !mCustomChoice) {
            throw new IllegalStateException("Call intoCheckMode first");
        }

        // Start selection mode if needed. We don't need to if we're unchecking something.
        if (value && mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode == null) {
            if (mMultiChoiceModeCallback == null ||
                    !mMultiChoiceModeCallback.hasWrappedCallback()) {
                throw new IllegalStateException("EasyRecyclerView: attempted to start selection mode " +
                        "for CHOICE_MODE_MULTIPLE_MODAL but no choice mode callback was " +
                        "supplied. Call setMultiChoiceModeListener to set a callback.");
            }
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }

        if (mChoiceMode == CHOICE_MODE_MULTIPLE || mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL || mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM) {
            boolean oldValue = mCheckStates.get(position);
            mCheckStates.put(position, value);
            if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                if (value) {
                    mCheckedIdStates.put(mAdapter.getItemId(position), position);
                } else {
                    mCheckedIdStates.delete(mAdapter.getItemId(position));
                }
            }
            if (oldValue != value) {
                if (value) {
                    mCheckedItemCount++;
                } else {
                    mCheckedItemCount--;
                }
            }
            if (mChoiceActionMode != null) {
                final long id = mAdapter.getItemId(position);
                mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode,
                        position, id, value);
            }
            if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM) {
                final long id = mAdapter.getItemId(position);
                mCustomChoiceListener.onItemCheckedStateChanged(this, position, id, value);
            }
        } else {
            boolean updateIds = mCheckedIdStates != null && mAdapter.hasStableIds();
            // Clear all values if we're checking something, or unchecking the currently
            // selected item
            if (value || isItemChecked(position)) {
                mCheckStates.clear();
                if (updateIds) {
                    mCheckedIdStates.clear();
                }
            }
            // this may end up selecting the value we just cleared but this way
            // we ensure length of mCheckStates is 1, a fact getCheckedItemPosition relies on
            if (value) {
                mCheckStates.put(position, true);
                if (updateIds) {
                    mCheckedIdStates.put(mAdapter.getItemId(position), position);
                }
                mCheckedItemCount = 1;
            } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
                mCheckedItemCount = 0;
            }
        }

        updateOnScreenCheckedViews();
    }

    /**
     * @see #setChoiceMode(int)
     *
     * @return The current choice mode
     */
    public int getChoiceMode() {
        return mChoiceMode;
    }

    /**
     * Defines the choice behavior for the List. By default, Lists do not have any choice behavior
     * ({@link #CHOICE_MODE_NONE}). By setting the choiceMode to {@link #CHOICE_MODE_SINGLE}, the
     * List allows up to one item to  be in a chosen state. By setting the choiceMode to
     * {@link #CHOICE_MODE_MULTIPLE}, the list allows any number of items to be chosen.
     *
     * @param choiceMode One of {@link #CHOICE_MODE_NONE}, {@link #CHOICE_MODE_SINGLE}, or
     * {@link #CHOICE_MODE_MULTIPLE}
     */
    public void setChoiceMode(int choiceMode) {
        mChoiceMode = choiceMode;
        if (mChoiceActionMode != null) {
            mChoiceActionMode.finish();
            mChoiceActionMode = null;
        }
        if (mChoiceMode != CHOICE_MODE_NONE) {
            if (mCheckStates == null) {
                mCheckStates = new SparseBooleanArray(0);
            }
            if (mCheckedIdStates == null && mAdapter != null && mAdapter.hasStableIds()) {
                mCheckedIdStates = new LongSparseArray<>(0);
            }
            // Modal multi-choice mode only has choices when the mode is active. Clear them.
            if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
                clearChoices();
                setLongClickable(true);
            } else if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM) {
                if (mTempCheckStates == null) {
                    mTempCheckStates = new SparseBooleanArray(0);
                }
                clearChoices();
            }
        }
    }

    /**
     * Set a {@link MultiChoiceModeListener} that will manage the lifecycle of the
     * selection {@link android.support.v7.view.ActionMode}. Only used when the choice mode is set to
     * {@link #CHOICE_MODE_MULTIPLE_MODAL}.
     *
     * @param listener Listener that will manage the selection mode
     *
     * @see #setChoiceMode(int)
     */
    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
        if (mMultiChoiceModeCallback == null) {
            mMultiChoiceModeCallback = new MultiChoiceModeWrapper();
        }
        mMultiChoiceModeCallback.setWrapped(listener);
    }

    /**
     *
     * @param listener
     */
    public void setCustomCheckedListener(CustomChoiceListener listener) {
        mCustomChoiceListener = listener;
    }

    /**
     * Perform a quick, in-place update of the checked or activated state
     * on all visible item views. This should only be called when a valid
     * choice mode is active.
     */
    private void updateOnScreenCheckedViews() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int position = getChildAdapterPosition(child);
            setViewChecked(child, mCheckStates.get(position));
        }
    }

    public static void setViewChecked(View view, boolean checked) {
        final boolean useActivated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        if (view instanceof Checkable) {
            ((Checkable) view).setChecked(checked);
        } else if (useActivated) {
            view.setActivated(checked);
        }
    }

    public float getTouchStartX() {
        return mStartX;
    }

    public float getTouchStartY() {
        return mStartY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return isClickable() || isLongClickable();
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                onTouchDown(ev);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                onTouchMove(ev);
                break;
            }

            case MotionEvent.ACTION_UP: {
                onTouchUp(ev);
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                onTouchCancel();
                break;
            }
        }

        super.onTouchEvent(ev);

        return true;
    }

    private void onTouchDown(MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();
        mStartX = x;
        mStartY = y;

        mMotionView = findChildViewUnder(x, y);
        mMotionPosition = getChildAdapterPosition(mMotionView);

        if (mMotionView != null && mMotionPosition >= 0 && mMotionView.isEnabled()) {
            mHasPerformedLongPress = false;

            mPrePressed = true;
            if (mPendingCheckForTap == null) {
                mPendingCheckForTap = new CheckForTap();
            }
            mPendingCheckForTap.x = x;
            mPendingCheckForTap.y = y;
            mPendingCheckForTap.v = mMotionView;
            mPendingCheckForTap.p = mMotionPosition;
            postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());

        } else {
            mMotionView = null;
        }
    }

    private void onTouchMove(MotionEvent ev) {
        if (mTouchSlop == -1) {
            mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }

        if (mMotionView != null && mMotionPosition >= 0 &&
                !MathUtils.near(mStartX, mStartY, ev.getX(), ev.getY(), mTouchSlop)) {
            removeTapCallback();
            if (mMotionView.isPressed()) {
                // Remove any future long press/tap checks
                removeLongPressCallback();
                mMotionView.setPressed(false);
                setPressed(false);
            }
            updateSelectorState();
            cancelDrawingSelector();
        }
    }

    private void onTouchUp(MotionEvent ev) {
        View motionView = mMotionView;
        int motionPosition = mMotionPosition;

        if (motionView != null && motionPosition >= 0) {
            final float x = ev.getX();
            final float y = ev.getY();

            if (motionView.isPressed() || mPrePressed) {
                // The button is being released before we actually
                // showed it as pressed.  Make it show the pressed
                // state now (before scheduling the click) to ensure
                // the user sees it.
                final float[] point = mTmpPoint;
                point[0] = x;
                point[1] = y;
                ViewUtils.transformPointToViewLocal(point, EasyRecyclerView.this, motionView);
                setPressed(motionView, true, point[0], point[1]);
                setPressed(true);
                positionSelector(mMotionPosition, motionView);
                if (mSelector != null) {
                    Drawable d = mSelector.getCurrent();
                    if (d != null && d instanceof TransitionDrawable) {
                        ((TransitionDrawable) d).resetTransition();
                    }
                    DrawableUtils.setHotspot(mSelector, x, y);
                }

                if (!mHasPerformedLongPress) {
                    // This is a tap, so remove the longpress check
                    removeLongPressCallback();

                    // Use a Runnable and post this rather than calling
                    // performClick directly. This lets other visual state
                    // of the view update before click actions start.
                    if (mPerformClick == null) {
                        mPerformClick = new PerformClick();
                    }
                    mPerformClick.v = motionView;
                    mPerformClick.p = motionPosition;
                    mPerformClick.rememberWindowAttachCount();
                    if (!post(mPerformClick)) {
                        mPerformClick.run();
                    }
                }

                if (mUnsetPressedState == null) {
                    mUnsetPressedState = new UnsetPressedState();
                }

                mUnsetPressedState.v = motionView;
                if (mPrePressed) {
                    postDelayed(mUnsetPressedState, ViewConfiguration.getPressedStateDuration());
                } else if (!post(mUnsetPressedState)) {
                    // If the post failed, unpress right now
                    mUnsetPressedState.run();
                }

                removeTapCallback();
            }

            updateSelectorState();

            // Release
            mMotionView = null;
        }
    }

    private void onTouchCancel() {
        if (mMotionView != null && mMotionPosition >= 0) {
            mMotionView.setPressed(false);
            setPressed(false);
            removeTapCallback();
            removeLongPressCallback();

            // Release motion view
            mMotionView = null;
        }
    }

    private void removeTapCallback() {
        if (mPendingCheckForTap != null) {
            mPrePressed = false;
            removeCallbacks(mPendingCheckForTap);
            mPendingCheckForTap.v = null;
        }
    }

    private void removeLongPressCallback() {
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
            mPendingCheckForLongPress.v = null;
        }
    }

    private void setPressed(View v, boolean pressed, float x, float y){
        v.setPressed(pressed);
        if (pressed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            v.drawableHotspotChanged(x, y);
        }
    }

    private boolean performItemClick(View view, int position, long id) {
        boolean handled = false;
        boolean dispatchItemClick = true;

        if (mChoiceMode != CHOICE_MODE_NONE && mChoiceMode != CHOICE_MODE_MULTIPLE_CUSTOM) {
            handled = true;
            boolean checkedStateChanged = false;

            if (mChoiceMode == CHOICE_MODE_MULTIPLE ||
                    (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode != null)) {
                boolean checked = !mCheckStates.get(position, false);
                mCheckStates.put(position, checked);
                if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                    if (checked) {
                        mCheckedIdStates.put(mAdapter.getItemId(position), position);
                    } else {
                        mCheckedIdStates.delete(mAdapter.getItemId(position));
                    }
                }
                if (checked) {
                    mCheckedItemCount++;
                } else {
                    mCheckedItemCount--;
                }
                if (mChoiceActionMode != null) {
                    mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode,
                            position, id, checked);
                    dispatchItemClick = false;
                }
                checkedStateChanged = true;
            } else if (mChoiceMode == CHOICE_MODE_SINGLE) {
                boolean checked = !mCheckStates.get(position, false);
                if (checked) {
                    mCheckStates.clear();
                    mCheckStates.put(position, true);
                    if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                        mCheckedIdStates.clear();
                        mCheckedIdStates.put(mAdapter.getItemId(position), position);
                    }
                    mCheckedItemCount = 1;
                } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
                    mCheckedItemCount = 0;
                }
                checkedStateChanged = true;
            }

            if (checkedStateChanged) {
                updateOnScreenCheckedViews();
            }
        }

        if (dispatchItemClick) {
            if (mOnItemClickListener != null) {
                playSoundEffect(SoundEffectConstants.CLICK);
                mOnItemClickListener.onItemClick(this, view, position, id);
                if (view != null) {
                    view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                }
                handled = true;
            }
        }

        return handled;
    }

    private boolean performItemLongPress(final View child,
            final int longPressPosition, final long longPressId) {
        // CHOICE_MODE_MULTIPLE_MODAL takes over long press.
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
            if (mChoiceActionMode == null &&
                    (mChoiceActionMode = startActionMode(mMultiChoiceModeCallback)) != null) {
                setItemChecked(longPressPosition, true);
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
            return true;
        }

        boolean handled = false;
        if (mOnItemLongClickListener != null) {
            handled = mOnItemLongClickListener.onItemLongClick(this, child,
                    longPressPosition, longPressId);
        }
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return handled;
    }

    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been clicked.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }


    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        mOnItemLongClickListener = listener;
    }

    /**
     * This saved state class is a Parcelable and should not extend
     * {@link android.view.View.BaseSavedState} nor {@link android.view.AbsSavedState}
     * because its super class AbsSavedState's constructor
     * {@link android.view.AbsSavedState#AbsSavedState(Parcel)} currently passes null
     * as a class loader to read its superstate from Parcelable.
     * This causes {@link android.os.BadParcelableException} when restoring saved states.
     * <p/>
     * The super class "RecyclerView" is a part of the support library,
     * and restoring its saved state requires the class loader that loaded the RecyclerView.
     * It seems that the class loader is not required when restoring from RecyclerView itself,
     * but it is required when restoring from RecyclerView's subclasses.
     */
    static class SavedState implements Parcelable {

        public static final SavedState EMPTY_STATE = new SavedState() {
        };

        int choiceMode;
        int checkedItemCount;
        SparseBooleanArray checkState;
        LongSparseArray<Integer> checkIdState;

        // This keeps the parent(RecyclerView)'s state
        Parcelable mSuperState;

        SavedState() {
            mSuperState = null;
        }

        /**
         * Constructor called from {@link #onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            mSuperState = superState != EMPTY_STATE ? superState : null;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            // Parcel 'in' has its parent(RecyclerView)'s saved state.
            // To restore it, class loader that loaded RecyclerView is required.
            Parcelable superState = in.readParcelable(RecyclerView.class.getClassLoader());
            mSuperState = superState != null ? superState : EMPTY_STATE;

            choiceMode = in.readInt();
            checkedItemCount = in.readInt();
            checkState = in.readSparseBooleanArray();
            final int N = in.readInt();
            if (N > 0) {
                checkIdState = new LongSparseArray<>();
                for (int i=0; i<N; i++) {
                    final long key = in.readLong();
                    final int value = in.readInt();
                    checkIdState.put(key, value);
                }
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeParcelable(mSuperState, flags);

            out.writeInt(choiceMode);
            out.writeInt(checkedItemCount);
            out.writeSparseBooleanArray(checkState);
            final int N = checkIdState != null ? checkIdState.size() : 0;
            out.writeInt(N);
            for (int i=0; i<N; i++) {
                out.writeLong(checkIdState.keyAt(i));
                out.writeInt(checkIdState.valueAt(i));
            }
        }

        public Parcelable getSuperState() {
            return mSuperState;
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final SavedState ss = new SavedState(super.onSaveInstanceState());

        ss.choiceMode = mChoiceMode;
        ss.checkedItemCount = mCheckedItemCount;
        ss.checkState = mCheckStates;
        ss.checkIdState = mCheckedIdStates;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setChoiceMode(ss.choiceMode);
        mCheckedItemCount = ss.checkedItemCount;
        if (ss.checkState != null) {
            mCheckStates = ss.checkState;
        }
        if (ss.checkIdState != null) {
            mCheckedIdStates = ss.checkIdState;
        }

        if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mCheckedItemCount > 0) {
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }
        updateOnScreenCheckedViews();
    }

    private final class CheckForTap implements Runnable {
        float x;
        float y;
        View v;
        int p;

        @Override
        public void run() {
            View view = v;
            int position = p;
            if (view != null && position >= 0) {

                mPrePressed = false;
                final float[] point = mTmpPoint;
                point[0] = x;
                point[1] = y;
                ViewUtils.transformPointToViewLocal(point, EasyRecyclerView.this, view);
                setPressed(v, true, point[0], point[1]);
                setPressed(true);
                positionSelector(mMotionPosition, v);
                refreshDrawableState();

                final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
                final boolean longClickable = isLongClickable();

                if (mSelector != null) {
                    final Drawable d = mSelector.getCurrent();
                    if (d != null && d instanceof TransitionDrawable) {
                        if (longClickable) {
                            ((TransitionDrawable) d).startTransition(longPressTimeout);
                        } else {
                            ((TransitionDrawable) d).resetTransition();
                        }
                    }
                    DrawableUtils.setHotspot(mSelector, x, y);
                }

                if (longClickable) {
                    mHasPerformedLongPress = false;

                    if (mPendingCheckForLongPress == null) {
                        mPendingCheckForLongPress = new CheckForLongPress();
                    }
                    mPendingCheckForLongPress.rememberWindowAttachCount();
                    mPendingCheckForLongPress.v = view;
                    mPendingCheckForLongPress.p = position;
                    postDelayed(mPendingCheckForLongPress, longPressTimeout -
                            ViewConfiguration.getTapTimeout());
                }

            }

            // Release
            v = null;
        }
    }



    private class CheckForLongPress extends WindowRunnnable implements Runnable {
        View v;
        int p;

        @Override
        public void run() {
            View view = v;
            int position = p;
            if (view != null && position >= 0 && view.isPressed() && view.getParent() != null) {
                boolean handled = false;

                if (sameWindow()) {
                    handled = performItemLongPress(view, position, getAdapter().getItemId(position));
                }
                if (handled) {
                    mHasPerformedLongPress = true;
                    view.setPressed(false);
                    setPressed(false);
                }
            }

            // Release
            v = null;
        }
    }

    private final class UnsetPressedState implements Runnable {
        View v;

        @Override
        public void run() {
            if (v != null) {
                v.setPressed(false);
                setPressed(false);

                // Release
                v = null;
            }
        }
    }

    private class PerformClick extends WindowRunnnable implements Runnable {
        View v;
        int p;

        @Override
        public void run() {
            View view = v;
            int position = p;
            if (view != null && position >= 0 && sameWindow()) {
                performItemClick(view, position, mAdapter.getItemId(position));
            }

            // Release
            v = null;
        }
    }

    /**
     * A base class for Runnables that will check that their view is still attached to
     * the original window as when the Runnable was created.
     *
     */
    private class WindowRunnnable {
        private int mOriginalAttachCount;

        public void rememberWindowAttachCount() {
            mOriginalAttachCount = getWindowAttachCount();
        }

        public boolean sameWindow() {
            return getWindowAttachCount() == mOriginalAttachCount;
        }
    }

    /**
     * Interface definition for a callback to be invoked when an item in the
     * EasyRecyclerView has been clicked.
     */
    public interface OnItemClickListener {
        /**
         * Callback method to be invoked when an item in the EasyRecyclerView
         * has been clicked.
         *
         * @param parent The EasyRecyclerView where the click happened.
         * @param view The view within the EasyRecyclerView that was clicked
         * @param position The position of the view in the adapter.
         * @param id The row id of the item that was clicked.
         *
         * @return true if the callback consumed the click, false otherwise
         */
        boolean onItemClick(EasyRecyclerView parent, View view, int position, long id);
    }

    /**
     * Interface definition for a callback to be invoked when an item in the
     * EasyRecyclerView has been clicked and held.
     */
    public interface OnItemLongClickListener {
        /**
         * Callback method to be invoked when an item in the EasyRecyclerView
         * has been clicked and held.
         *
         * @param parent The EasyRecyclerView where the click happened
         * @param view The view within the EasyRecyclerView that was clicked
         * @param position The position of the view in the list
         * @param id The row id of the item that was clicked
         *
         * @return true if the callback consumed the long click, false otherwise
         */
        boolean onItemLongClick(EasyRecyclerView parent, View view, int position, long id);
    }

    /**
     * Interface definition for a callback to be invoked when draw selector.
     */
    public interface OnDrawSelectorListener {
        /**
         * Callback method to be invoked before selector draw.
         *
         * @param position The EasyRecyclerView where the click happened
         * @return true if need draw selector, false otherwise
         */
        boolean beforeDrawSelector(int position);
    }

    /**
     * A MultiChoiceModeListener receives events for {@link android.widget.AbsListView#CHOICE_MODE_MULTIPLE_MODAL}.
     * It acts as the {@link android.support.v7.view.ActionMode.Callback} for the selection mode and also receives
     * {@link #onItemCheckedStateChanged(android.view.ActionMode, int, long, boolean)} events when the user
     * selects and deselects list items.
     */
    public interface MultiChoiceModeListener extends ActionMode.Callback {
        /**
         * Called when an item is checked or unchecked during selection mode.
         *
         * @param mode The {@link android.support.v7.view.ActionMode} providing the selection mode
         * @param position Adapter position of the item that was checked or unchecked
         * @param id Adapter ID of the item that was checked or unchecked
         * @param checked <code>true</code> if the item is now checked, <code>false</code>
         *                if the item is now unchecked.
         */
        void onItemCheckedStateChanged(ActionMode mode,
                int position, long id, boolean checked);
    }

    class MultiChoiceModeWrapper implements MultiChoiceModeListener {
        private MultiChoiceModeListener mWrapped;

        public void setWrapped(MultiChoiceModeListener wrapped) {
            mWrapped = wrapped;
        }

        public boolean hasWrappedCallback() {
            return mWrapped != null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mWrapped.onCreateActionMode(mode, menu)) {
                // Initialize checked graphic state?
                setLongClickable(false);
                return true;
            }
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            mChoiceActionMode = null;

            // Ending selection mode means deselecting everything.
            clearChoices();

            requestLayout();

            setLongClickable(true);
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode,
                int position, long id, boolean checked) {
            mWrapped.onItemCheckedStateChanged(mode, position, id, checked);

            // If there are no items selected we no longer need the selection mode.
            if (getCheckedItemCount() == 0) {
                mode.finish();
            }
        }
    }

    /**
     * Custom checked
     */
    public interface CustomChoiceListener {

        void onIntoCustomChoice(EasyRecyclerView view);

        void onOutOfCustomChoice(EasyRecyclerView view);

        void onItemCheckedStateChanged(EasyRecyclerView view, int position, long id, boolean checked);
    }
}
