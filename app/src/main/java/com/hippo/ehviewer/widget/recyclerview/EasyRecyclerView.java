/*
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

package com.hippo.ehviewer.widget.recyclerview;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.InputDevice;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Checkable;
import android.widget.ListAdapter;

/**
 *
 * Get some code from twoway-view and AbsListView
 *
 * @author Hippo
 *
 */
public class EasyRecyclerView extends RecyclerView implements View.OnClickListener,
        View.OnLongClickListener {

    private static final String TAG = EasyRecyclerView.class.getSimpleName();

    private static final String STATE_KEY_CHOICE_MODE = "choiceMode";
    private static final String STATE_KEY_CHECKED_STATES = "checkedStates";
    private static final String STATE_KEY_CHECKED_ID_STATES = "checkedIdStates";
    private static final String STATE_KEY_CHECKED_COUNT = "checkedCount";

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

    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;
    private boolean mNeedOnClickListener = false;
    private boolean mNeedOnLongClickListener = false;

    private FooterAdapter mAdapter;

    private float mVerticalScrollFactor = 0;

    public EasyRecyclerView(Context context) {
        super(context);
    }

    public EasyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EasyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public View getFootView() {
        if (mAdapter != null)
            return mAdapter.getFooterView();
        else
            return null;
    }

    private boolean isNeedOnClickListener() {
        if (mOnItemClickListener == null && mChoiceMode == CHOICE_MODE_NONE)
            return false;
        else
            return true;
    }

    private boolean isNeedOnLongClickListener() {
        if (mOnItemLongClickListener != null || mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL)
            return true;
        else
            return false;
    }

    private void setAllOnClickListener() {
        final int count = getChildCount();
        final View footView = getFootView();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != footView) {
                child.setOnClickListener(this);
            }
        }
    }

    private void resetAllOnClickListener() {
        final int count = getChildCount();
        final View footView = getFootView();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != footView) {
                child.setOnClickListener(null);
                child.setClickable(false);
            }
        }
    }

    private void setAllOnLongClickListener() {
        final int count = getChildCount();
        final View footView = getFootView();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != footView) {
                child.setOnLongClickListener(this);
            }
        }
    }

    private void resetAllOnLongClickListener() {
        final int count = getChildCount();
        final View footView = getFootView();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != footView) {
                child.setOnLongClickListener(null);
                child.setLongClickable(false);
            }
        }
    }

    public void updateOnClickState() {
        boolean newNeedOnClickListener = isNeedOnClickListener();
        if (mNeedOnClickListener != newNeedOnClickListener) {
            mNeedOnClickListener = newNeedOnClickListener;
            if (mNeedOnClickListener)
                setAllOnClickListener();
            else
                resetAllOnClickListener();
        }
    }

    public void updateOnLongClickState() {
        boolean newNeedOnLongClickListener = isNeedOnLongClickListener();
        if (mNeedOnLongClickListener != newNeedOnLongClickListener) {
            mNeedOnLongClickListener = newNeedOnLongClickListener;
            if (mNeedOnLongClickListener)
                setAllOnLongClickListener();
            else
                resetAllOnLongClickListener();
        }
    }

    /**
     * Register a callback to be invoked when an item in the
     * RecyclerView has been clicked.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
        updateOnClickState();
    }

    /**
     * Register a callback to be invoked when an item in the
     * RecyclerView has been clicked and held.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mOnItemLongClickListener = listener;
        updateOnLongClickState();
    }

    /**
     *
     * adapter must be {@link com.hippo.ehviewer.widget.recyclerview.EasyRecyclerView.FooterAdapter}
     */
    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        super.setAdapter(adapter);

        mAdapter = (FooterAdapter) adapter;
        mAdapter.mHolder = this;
        if (adapter != null) {
            if (mChoiceMode != CHOICE_MODE_NONE && adapter.hasStableIds() &&
                    mCheckedIdStates == null) {
                mCheckedIdStates = new LongSparseArray<Integer>();
            }
        }

        if (mCheckStates != null) {
            mCheckStates.clear();
        }

        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
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
     * has stable IDs. ({@link ListAdapter#hasStableIds()} == {@code true})
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

        // Start selection mode if needed. We don't need to if we're unchecking something.
        if (value && mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode == null) {
            if (mMultiChoiceModeCallback == null ||
                    !mMultiChoiceModeCallback.hasWrappedCallback()) {
                throw new IllegalStateException("AbsListView: attempted to start selection mode " +
                        "for CHOICE_MODE_MULTIPLE_MODAL but no choice mode callback was " +
                        "supplied. Call setMultiChoiceModeListener to set a callback.");
            }
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }

        if (mChoiceMode == CHOICE_MODE_MULTIPLE || mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
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
                mCheckedIdStates = new LongSparseArray<Integer>(0);
            }
            // Modal multi-choice mode only has choices when the mode is active. Clear them.
            if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
                clearChoices();
            }
        }

        updateOnClickState();
        updateOnLongClickState();
    }

    /**
     * Set a {@link MultiChoiceModeListener} that will manage the lifecycle of the
     * selection {@link ActionMode}. Only used when the choice mode is set to
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
     * Perform a quick, in-place update of the checked or activated state
     * on all visible item views. This should only be called when a valid
     * choice mode is active.
     */
    private void updateOnScreenCheckedViews() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int position = getChildPosition(child);
            setViewChecked(child, mCheckStates.get(position));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void setViewChecked(View view, boolean checked) {
        final boolean useActivated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        if (view instanceof Checkable) {
            ((Checkable) view).setChecked(checked);
        } else if (useActivated) {
            view.setActivated(checked);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_SCROLL: {
                    if (getScrollState() != SCROLL_STATE_DRAGGING) {
                        final float vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                        if (vscroll != 0) {
                            final int delta = (int) (vscroll * getVerticalScrollFactor());
                            scrollBy(0, -delta);
                        }
                    }
                }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    /**
     * Gets a scale factor that determines the distance the view should scroll
     * vertically in response to {@link MotionEvent#ACTION_SCROLL}.
     * @return The vertical scroll scale factor.
     */
    protected float getVerticalScrollFactor() {
        if (mVerticalScrollFactor == 0) {
            TypedValue outValue = new TypedValue();
            if (!getContext().getTheme().resolveAttribute(
                    0x0101004d, outValue, true)) {//com.android.internal.R.attr.listPreferredItemHeight
                throw new IllegalStateException(
                        "Expected theme to define listPreferredItemHeight.");
            }
            mVerticalScrollFactor = outValue.getDimension(
                    getContext().getResources().getDisplayMetrics());
        }
        return mVerticalScrollFactor;
    }

    /*
    @Override
    public Parcelable onSaveInstanceState() {
        final Parcelable state = new Bundle(super.onSaveInstanceState());

        state.putInt(STATE_KEY_CHOICE_MODE, mChoiceMode);
        state.putParcelable(STATE_KEY_CHECKED_STATES, mCheckStates);
        state.putParcelable(STATE_KEY_CHECKED_ID_STATES, mCheckedIdStates);
        state.putInt(STATE_KEY_CHECKED_COUNT, mCheckedItemCount);

        return state;
    }

    public void onRestoreInstanceState(Bundle state) {
        mChoiceMode = ChoiceMode.values()[state.getInt(STATE_KEY_CHOICE_MODE)];
        mCheckedStates = state.getParcelable(STATE_KEY_CHECKED_STATES);
        mCheckedIdStates = state.getParcelable(STATE_KEY_CHECKED_ID_STATES);
        mCheckedCount = state.getInt(STATE_KEY_CHECKED_COUNT);

        // TODO confirm ids here
    }*/

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


    /*
     * EasyRecyclerView.OnItemTouchListener will not work fine
     * when item view's child need click action, because they make
     * touch event deliver different
     */

    @Override
    public void onClick(View view) {
        final int position = getChildPosition(view);
        final long id = mAdapter.getItemId(position);

        boolean dispatchItemClick = true;

        if (mChoiceMode != CHOICE_MODE_NONE) {
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

        if (dispatchItemClick && mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(this, view, position, id);
        }
    }


    @Override
    public boolean onLongClick(View view) {
        final int position = getChildPosition(view);
        final long id = mAdapter.getItemId(position);

        // CHOICE_MODE_MULTIPLE_MODAL takes over long press.
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
            if (mChoiceActionMode == null &&
                    (mChoiceActionMode = startActionMode(mMultiChoiceModeCallback)) != null) {
                setItemChecked(position, true);
            }
            return true;
        }

        boolean handled = false;
        if (mOnItemLongClickListener != null) {
            handled = mOnItemLongClickListener.onItemLongClick(this, view,
                    position, id);
        }
        return handled;
    }

    /**
     * A MultiChoiceModeListener receives events for {@link AbsListView#CHOICE_MODE_MULTIPLE_MODAL}.
     * It acts as the {@link ActionMode.Callback} for the selection mode and also receives
     * {@link #onItemCheckedStateChanged(ActionMode, int, long, boolean)} events when the user
     * selects and deselects list items.
     */
    public interface MultiChoiceModeListener extends ActionMode.Callback {
        /**
         * Called when an item is checked or unchecked during selection mode.
         *
         * @param mode The {@link ActionMode} providing the selection mode
         * @param position Adapter position of the item that was checked or unchecked
         * @param id Adapter ID of the item that was checked or unchecked
         * @param checked <code>true</code> if the item is now checked, <code>false</code>
         *                if the item is now unchecked.
         */
        public void onItemCheckedStateChanged(ActionMode mode,
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

    public static abstract class FooterAdapter<VH extends ViewHolder> extends RecyclerView.Adapter<VH> {

        public static final int TYPE_FOOTER = Integer.MAX_VALUE;

        private EasyRecyclerView mHolder;
        private View mFooterView;

        public boolean hasFootView() {
            return mFooterView != null;
        }

        /**
         * footerView == null to remove foot view
         * @param footerView
         */
        public void setFooterView(View footerView) {
            if (mFooterView == footerView)
                return;

            int itemCountActual = getItemCountActual();
            View originView = mFooterView;
            mFooterView = footerView;
            if (originView != null) {
                if (mFooterView != null)
                    notifyItemChanged(itemCountActual);
                else
                    notifyItemRemoved(itemCountActual);
            } else {
                notifyItemInserted(itemCountActual);
            }
        }

        public View getFooterView() {
            return mFooterView;
        }

        @Override
        public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
            if (mFooterView != null && viewType == TYPE_FOOTER) {
                StaggeredGridLayoutManager.LayoutParams lp = new StaggeredGridLayoutManager.LayoutParams(
                        StaggeredGridLayoutManager.LayoutParams.WRAP_CONTENT,
                        StaggeredGridLayoutManager.LayoutParams.WRAP_CONTENT);
                lp.setFullSpan(true);
                mFooterView.setLayoutParams(lp);
                return onCreateAndBindFooterViewHolder(parent, mFooterView);
            } else {
                return onCreateViewHolderActual(parent, viewType);
            }
        }

        @Override
        public final void onBindViewHolder(VH holder, int position) {
            if (mFooterView == null || position != getItemCountActual()) {
                onBindViewHolderActual(holder, position);
                if (mHolder.mNeedOnClickListener)
                    holder.itemView.setOnClickListener(mHolder);
                if (mHolder.mNeedOnLongClickListener)
                    holder.itemView.setOnLongClickListener(mHolder);
                EasyRecyclerView.setViewChecked(holder.itemView,
                        mHolder.isItemChecked(position));
            }
        }

        @Override
        public final int getItemViewType(int position) {
            if (mFooterView != null && position == getItemCountActual())
                return TYPE_FOOTER;
            else
                return getItemViewTypeActual(position);
        }

        @Override
        public final int getItemCount() {
            int itemCount = getItemCountActual();
            if (mFooterView != null)
                itemCount++;
            return itemCount;
        }

        public abstract VH onCreateAndBindFooterViewHolder(ViewGroup parent, View footerView);

        public abstract VH onCreateViewHolderActual(ViewGroup parent, int viewType);

        public abstract void onBindViewHolderActual(VH holder, int position);

        /**
         * Foot view type is Integer.MAX_VALUE, do not be the same
         *
         * @param position
         * @return
         */
        public int getItemViewTypeActual(int position) {
            return 0;
        }

        public abstract int getItemCountActual();

    }
}
