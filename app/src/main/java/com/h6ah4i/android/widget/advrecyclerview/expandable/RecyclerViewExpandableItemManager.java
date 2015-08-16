/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.h6ah4i.android.widget.advrecyclerview.expandable;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.h6ah4i.android.widget.advrecyclerview.utils.CustomRecyclerViewUtils;

/**
 * Provides item swipe operation for {@link android.support.v7.widget.RecyclerView}
 */
public class RecyclerViewExpandableItemManager {
    private static final String TAG = "ARVExpandableItemMgr";

    /**
     * Packed position version of {@link android.support.v7.widget.RecyclerView#NO_POSITION}
     */
    public static final long NO_EXPANDABLE_POSITION = ExpandableAdapterHelper.NO_EXPANDABLE_POSITION;


    /**
     * State flag for the {@link ExpandableItemViewHolder#setExpandStateFlags(int)} and {@link ExpandableItemViewHolder#getExpandStateFlags()} methods.
     * Indicates that this ViewHolder is associated to group item.
     */
    @SuppressWarnings("PointlessBitwiseExpression")
    public static final int STATE_FLAG_IS_GROUP = (1 << 0);

    /**
     * State flag for the {@link ExpandableItemViewHolder#setExpandStateFlags(int)} and {@link ExpandableItemViewHolder#getExpandStateFlags()} methods.
     * Indicates that this ViewHolder is associated to child item.
     */
    public static final int STATE_FLAG_IS_CHILD = (1 << 1);

    /**
     * State flag for the {@link ExpandableItemViewHolder#setExpandStateFlags(int)} and {@link ExpandableItemViewHolder#getExpandStateFlags()} methods.
     * Indicates that this is a group item.
     */
    public static final int STATE_FLAG_IS_EXPANDED = (1 << 2);

    /**
     * State flag for the {@link ExpandableItemViewHolder#setExpandStateFlags(int)} and {@link ExpandableItemViewHolder#getExpandStateFlags()} methods.
     * If this flag is set, some other flags are changed and require to apply.
     */
    public static final int STATE_FLAG_IS_UPDATED = (1 << 31);


    // ---

    /**
     * Used for being notified when a group is expanded
     */
    public interface OnGroupExpandListener {
        /**
         * Callback method to be invoked when a group in this expandable list has been expanded.
         *
         * @param groupPosition The group position that was expanded
         * @param fromUser Whether the expand request is issued by a user operation
         */
        void onGroupExpand(int groupPosition, boolean fromUser);
    }

    /**
     * Used for being notified when a group is collapsed
     */
    public interface OnGroupCollapseListener {
        /**
         * Callback method to be invoked when a group in this expandable list has been collapsed.
         *
         * @param groupPosition The group position that was collapsed
         * @param fromUser Whether the collapse request is issued by a user operation
         */
        void onGroupCollapse(int groupPosition, boolean fromUser);
    }

    // ---

    private SavedState mSavedState;

    private RecyclerView mRecyclerView;
    private ExpandableRecyclerViewWrapperAdapter mAdapter;
    private RecyclerView.OnItemTouchListener mInternalUseOnItemTouchListener;
    private OnGroupExpandListener mOnGroupExpandListener;
    private OnGroupCollapseListener mOnGroupCollapseListener;

    private long mTouchedItemId = RecyclerView.NO_ID;
    private int mTouchSlop;
    private int mInitialTouchX;
    private int mInitialTouchY;

    /**
     * Constructor.
     *
     * @param savedState The saved state object which is obtained from the {@link #getSavedState()} method.
     */
    public RecyclerViewExpandableItemManager(Parcelable savedState) {
        mInternalUseOnItemTouchListener = new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return RecyclerViewExpandableItemManager.this.onInterceptTouchEvent(rv, e);
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        };

        if (savedState instanceof SavedState) {
            mSavedState = (SavedState) savedState;
        }
    }

    /**
     * Indicates this manager instance has released or not.
     *
     * @return True if this manager instance has released
     */
    public boolean isReleased() {
        return (mInternalUseOnItemTouchListener == null);
    }

    /**
     * Attaches {@link android.support.v7.widget.RecyclerView} instance.
     *
     * Before calling this method, the target {@link android.support.v7.widget.RecyclerView} must set
     * the wrapped adapter instance which is returned by the
     * {@link #createWrappedAdapter(android.support.v7.widget.RecyclerView.Adapter)} method.
     *
     * @param rv The {@link android.support.v7.widget.RecyclerView} instance
     */
    public void attachRecyclerView(RecyclerView rv) {
        if (rv == null) {
            throw new IllegalArgumentException("RecyclerView cannot be null");
        }

        if (isReleased()) {
            throw new IllegalStateException("Accessing released object");
        }

        if (mRecyclerView != null) {
            throw new IllegalStateException("RecyclerView instance has already been set");
        }

        mRecyclerView = rv;
        mRecyclerView.addOnItemTouchListener(mInternalUseOnItemTouchListener);
        mTouchSlop = ViewConfiguration.get(mRecyclerView.getContext()).getScaledTouchSlop();
    }

    /**
     * Detach the {@link android.support.v7.widget.RecyclerView} instance and release internal field references.
     *
     * This method should be called in order to avoid memory leaks.
     */
    public void release() {
        if (mRecyclerView != null && mInternalUseOnItemTouchListener != null) {
            mRecyclerView.removeOnItemTouchListener(mInternalUseOnItemTouchListener);
        }
        mInternalUseOnItemTouchListener = null;
        mOnGroupExpandListener = null;
        mOnGroupCollapseListener = null;
        mRecyclerView = null;
        mSavedState = null;
    }

    /**
     * Create wrapped adapter.
     *
     * @param adapter The target adapter.
     *
     * @return Wrapped adapter which is associated to this {@link RecyclerViewExpandableItemManager} instance.
     */
    @SuppressWarnings("unchecked")
    public RecyclerView.Adapter createWrappedAdapter(RecyclerView.Adapter adapter) {
        if (mAdapter != null) {
            throw new IllegalStateException("already have a wrapped adapter");
        }

        int [] adapterSavedState = (mSavedState != null) ? mSavedState.adapterSavedState : null;
        mSavedState = null;

        mAdapter = new ExpandableRecyclerViewWrapperAdapter(this, adapter, adapterSavedState);

        // move listeners to wrapper adapter
        mAdapter.setOnGroupExpandListener(mOnGroupExpandListener);
        mOnGroupExpandListener = null;

        mAdapter.setOnGroupCollapseListener(mOnGroupCollapseListener);
        mOnGroupCollapseListener = null;

        return mAdapter;
    }

    /**
     * Gets saved state object in order to restore the internal state.
     *
     * Call this method in Activity/Fragment's onSavedInstance() and save to the bundle.
     *
     * @return The Parcelable object which stores information need to restore the internal states.
     */
    public Parcelable getSavedState() {
        int [] adapterSavedState = null;

        if (mAdapter != null) {
            adapterSavedState = mAdapter.getExpandedItemsSavedStateArray();
        }

        return new SavedState(adapterSavedState);
    }

    /*package*/ boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (mAdapter == null) {
            return false;
        }

        final int action = MotionEventCompat.getActionMasked(e);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(rv, e);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (handleActionUpOrCancel(rv, e)) {
                    // NOTE: it requires to return false to work click effect properly (issue #44)
                    return false;
                }
                break;
        }

        return false;
    }

    private void handleActionDown(RecyclerView rv, MotionEvent e) {
        final RecyclerView.ViewHolder holder = CustomRecyclerViewUtils.findChildViewHolderUnderWithTranslation(rv, e.getX(), e.getY());

        mInitialTouchX = (int) (e.getX() + 0.5f);
        mInitialTouchY = (int) (e.getY() + 0.5f);

        if (holder instanceof ExpandableItemViewHolder) {
            mTouchedItemId = holder.getItemId();
        } else {
            mTouchedItemId = RecyclerView.NO_ID;
        }
    }

    private boolean handleActionUpOrCancel(RecyclerView rv, MotionEvent e) {
        final long touchedItemId = mTouchedItemId;
        final int initialTouchX = mInitialTouchX;
        final int initialTouchY = mInitialTouchY;

        mTouchedItemId = RecyclerView.NO_ID;
        mInitialTouchX = 0;
        mInitialTouchY = 0;

        if (!((touchedItemId != RecyclerView.NO_ID) && (MotionEventCompat.getActionMasked(e) == MotionEvent.ACTION_UP))) {
            return false;
        }

        final int touchX = (int) (e.getX() + 0.5f);
        final int touchY = (int) (e.getY() + 0.5f);

        final int diffX = touchX - initialTouchX;
        final int diffY = touchY - initialTouchY;

        if (!((Math.abs(diffX) < mTouchSlop) && (Math.abs(diffY) < mTouchSlop))) {
            return false;
        }

        final RecyclerView.ViewHolder holder = CustomRecyclerViewUtils.findChildViewHolderUnderWithTranslation(rv, e.getX(), e.getY());

        if (!((holder != null) && (holder.getItemId() == touchedItemId))) {
            return false;
        }

        final int position = CustomRecyclerViewUtils.getSynchronizedPosition(holder);

        if (position == RecyclerView.NO_POSITION) {
            return false;
        }

        final View view = holder.itemView;
        final int translateX = (int) (ViewCompat.getTranslationX(view) + 0.5f);
        final int translateY = (int) (ViewCompat.getTranslationY(view) + 0.5f);
        final int viewX = touchX - (view.getLeft() + translateX);
        final int viewY = touchY - (view.getTop() + translateY);

        return mAdapter.onTapItem(holder, position, viewX, viewY);
    }

    /**
     * Expand a group.
     *
     * @param groupPosition The group position to be expanded
     *
     * @return True if the group was expanded, false otherwise  (If the group was already expanded, this will return false)
     */
    public boolean expandGroup(int groupPosition) {
        return (mAdapter != null) && mAdapter.expandGroup(groupPosition, false);
    }

    /**
     * Collapse a group.
     *
     * @param groupPosition The group position to be collapsed
     *
     * @return True if the group was collapsed, false otherwise  (If the group was already collapsed, this will return false)
     */
    public boolean collapseGroup(int groupPosition) {
        return (mAdapter != null) && mAdapter.collapseGroup(groupPosition, false);
    }

    /**
     * Converts a flat position (the raw position of an item in the list) to a group and/or child position
     * (represented in a packed position). Use {@link #getPackedPositionChild(long)}, {@link #getPackedPositionGroup(long)} to unpack.
     *
     * @param flatPosition The flat position to be converted
     *
     * @return The group and/or child position for the given flat position in packed position representation.
     */
    public long getExpandablePosition(int flatPosition) {
        if (mAdapter == null) {
            return ExpandableAdapterHelper.NO_EXPANDABLE_POSITION;
        }
        return mAdapter.getExpandablePosition(flatPosition);
    }

    /**
     * Converts a group and/or child position to a flat position.
     *
     * @param packedPosition The group and/or child position to be converted in packed position representation.
     *
     * @return The group and/or child position for the given flat position in packed position representation.
     */
    public int getFlatPosition(long packedPosition) {
        if (mAdapter == null) {
            return RecyclerView.NO_POSITION;
        }
        return mAdapter.getFlatPosition(packedPosition);
    }

    /**
     * Gets the child position from a packed position.
     * To get the group that this child belongs to, use {@link #getPackedPositionGroup(long)}.
     * See {@link #getPackedPositionForChild(int, int)}.
     *
     * @param packedPosition The packed position from which the child position will be returned.
     *
     * @return The child position portion of the packed position. If this does not contain a child, returns {@link android.support.v7.widget.RecyclerView#NO_POSITION}.
     */
    public static int getPackedPositionChild(long packedPosition) {
        return ExpandableAdapterHelper.getPackedPositionChild(packedPosition);
    }

    /**
     * Returns the packed position representation of a child position.
     *
     * In general, a packed position should be used in situations where the position given to/returned from
     * {@link RecyclerViewExpandableItemManager} method can either be a child or group.
     * The two positions are packed into a single long which can be unpacked using {@link #getPackedPositionChild(long)} and
     * {@link #getPackedPositionGroup(long)}.
     *
     * @param groupPosition The child's parent group's position
     * @param childPosition The child position within the group
     *
     * @return The packed position representation of the child (and parent group).
     */
    public static long getPackedPositionForChild(int groupPosition, int childPosition) {
        return ExpandableAdapterHelper.getPackedPositionForChild(groupPosition, childPosition);
    }

    /**
     * Returns the packed position representation of a group's position. See {@link #getPackedPositionForChild(int, int)}.
     *
     * @param groupPosition The child's parent group's position.
     *
     * @return The packed position representation of the group.
     */
    public static long getPackedPositionForGroup(int groupPosition) {
        return ExpandableAdapterHelper.getPackedPositionForGroup(groupPosition);
    }

    /**
     * Gets the group position from a packed position. See {@link #getPackedPositionForChild(int, int)}.
     *
     * @param packedPosition The packed position from which the group position will be returned.
     *
     * @return THe group position of the packed position. If this does not contain a group, returns {@link android.support.v7.widget.RecyclerView#NO_POSITION}.
     */
    public static int getPackedPositionGroup(long packedPosition) {
        return ExpandableAdapterHelper.getPackedPositionGroup(packedPosition);
    }

    /**
     * Whether the given group is currently expanded.
     *
     * @param groupPosition The group to check
     *
     * @return Whether the group is currently expanded
     */
    public boolean isGroupExpanded(int groupPosition) {
        return (mAdapter != null) && mAdapter.isGroupExpanded(groupPosition);
    }

    /**
     * Gets combined ID for child item.
     *
     * bit 0-31: Lower 32 bits of the childId
     * bit 32-62: Lower 31 bits of the groupId
     * bit 63: reserved
     *
     * @param groupId The ID of the group that contains the child.
     * @param childId The ID of the child.
     *
     * @return The unique ID of the child across all groups and children in the list
     */
    public static long getCombinedChildId(long groupId, long childId) {
        return ExpandableAdapterHelper.getCombinedChildId(groupId, childId);
    }

    /**
     * Gets combined ID for child item.
     *
     * bit 0-31: all bits are set to 1
     * bit 32-62: Lower 31 bits of the groupId
     * bit 63: reserved
     *
     * @param groupId The ID of the group that contains the child.
     *
     * @return The unique ID of the child across all groups and children in the list
     */
    public static long getCombinedGroupId(long groupId) {
        return ExpandableAdapterHelper.getCombinedGroupId(groupId);
    }

    /**
     * Checks whether the passed view type is a group's one.
     *
     * @param rawViewType raw view type value (return value of {@link android.support.v7.widget.RecyclerView.ViewHolder#getItemViewType()})
     *
     * @return True for the a group view type, otherwise false
     */
    public static boolean isGroupViewType(int rawViewType) {
        return ExpandableAdapterHelper.isGroupViewType(rawViewType);
    }

    /**
     * Gets group view type from a raw view type.
     *
     * @param rawViewType raw view type value (return value of {@link android.support.v7.widget.RecyclerView.ViewHolder#getItemViewType()})
     *
     * @return Group view type for the given raw view type.
     */
    public static int getGroupViewType(int rawViewType) {
        return ExpandableAdapterHelper.getGroupViewType(rawViewType);
    }

    /**
     * Gets child view type from a raw view type.
     *
     * @param rawViewType raw view type value (return value of {@link android.support.v7.widget.RecyclerView.ViewHolder#getItemViewType()})
     *
     * @return Child view type for the given raw view type.
     */
    public static int getChildViewType(int rawViewType) {
        return ExpandableAdapterHelper.getChildViewType(rawViewType);
    }

    /**
     * Register a callback to be invoked when an group item has been expanded.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnGroupExpandListener(OnGroupExpandListener listener) {
        if (mAdapter != null) {
            mAdapter.setOnGroupExpandListener(listener);
        } else {
            // pending
            mOnGroupExpandListener = listener;
        }
    }

    /**
     * Register a callback to be invoked when an group item has been collapsed.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnGroupCollapseListener(OnGroupCollapseListener listener) {
        if (mAdapter != null) {
            mAdapter.setOnGroupCollapseListener(listener);
        } else {
            // pending
            mOnGroupCollapseListener = listener;
        }
    }

    /**
     * Restore saves state. See {@link #restoreState(android.os.Parcelable, boolean, boolean)}.
     * (This method does not invoke any hook methods and listener events)
     *
     * @param savedState The saved state object
     */
    public void restoreState(Parcelable savedState) {
        restoreState(savedState, false, false);
    }

    /**
     * Restore saves state.
     *
     * This method is useful when the adapter can not be prepared (because data loading may takes time and processed asynchronously)
     * before creating this manager instance.
     *
     * @param savedState The saved state object
     * @param callHooks Whether to call hook routines
     *                  ({@link ExpandableItemAdapter#onHookGroupExpand(int, boolean)},
     *                  {@link ExpandableItemAdapter#onHookGroupCollapse(int, boolean)})
     * @param callListeners Whether to invoke {@link OnGroupExpandListener} and/or {@link OnGroupCollapseListener} listener events
     */
    public void restoreState(Parcelable savedState, boolean callHooks, boolean callListeners) {
        if (savedState == null) {
            return; // do nothing
        }

        if (!(savedState instanceof SavedState)) {
            throw new IllegalArgumentException("Illegal saved state object passed");
        }

        if (!((mAdapter != null) && (mRecyclerView != null))) {
            throw new IllegalStateException("RecyclerView has not been attached");
        }

        mAdapter.restoreState(((SavedState) savedState).adapterSavedState, callHooks, callListeners);
    }

    /**
     * Notify any registered observers that the group item at <code>groupPosition</code> has changed.
     *
     * <p>This is an group item change event, not a structural change event. It indicates that any
     * reflection of the data at <code>groupPosition</code> is out of date and should be updated.
     * The item at <code>groupPosition</code> retains the same identity.</p>
     *
     * <p>This method does not notify for children that are contained in the specified group.
     * If children have also changed, use {@link #notifyGroupAndChildrenItemsChanged(int)} instead.</p>
     *
     * @param groupPosition Position of the group item that has changed
     *
     * @see #notifyGroupAndChildrenItemsChanged(int)
     */
    public void notifyGroupItemChanged(int groupPosition) {
        mAdapter.notifyGroupItemChanged(groupPosition);
    }

    /**
     * Notify any registered observers that the group and children items at <code>groupPosition</code> have changed.
     *
     * <p>This is an group item change event, not a structural change event. It indicates that any
     * reflection of the data at <code>groupPosition</code> is out of date and should be updated.
     * The item at <code>groupPosition</code> retains the same identity.</p>
     *
     * @param groupPosition Position of the group item which contains changed children
     *
     * @see #notifyGroupItemChanged(int)
     * @see #notifyChildrenOfGroupItemChanged(int)
     */
    public void notifyGroupAndChildrenItemsChanged(int groupPosition) {
        mAdapter.notifyGroupAndChildrenItemsChanged(groupPosition);
    }

    /**
     * Notify any registered observers that the children items contained in the group item at <code>groupPosition</code> have changed.
     *
     * <p>This is an group item change event, not a structural change event. It indicates that any
     * reflection of the data at <code>groupPosition</code> is out of date and should be updated.
     * The item at <code>groupPosition</code> retains the same identity.</p>
     *
     * <p>This method does not notify for the group item.
     * If the group has also changed, use {@link #notifyGroupAndChildrenItemsChanged(int)} instead.</p>
     *
     * @param groupPosition Position of the group item which contains changed children
     *
     * @see #notifyGroupAndChildrenItemsChanged(int)
     */
    public void notifyChildrenOfGroupItemChanged(int groupPosition) {
        mAdapter.notifyChildrenOfGroupItemChanged(groupPosition);
    }

    /**
     * Notify any registered observers that the child item at <code>{groupPosition, childPosition}</code> has changed.
     *
     * <p>This is an item change event, not a structural change event. It indicates that any
     * reflection of the data at <code>{groupPosition, childPosition}</code> is out of date and should be updated.
     * The item at <code>{groupPosition, childPosition}</code> retains the same identity.</p>
     *
     * @param groupPosition Position of the group item which contains the changed child
     * @param childPosition Position of the child item in the group that has changed
     *
     * @see #notifyChildItemRangeChanged(int, int, int)
     */
    public void notifyChildItemChanged(int groupPosition, int childPosition) {
        mAdapter.notifyChildItemChanged(groupPosition, childPosition);
    }

    /**
     * Notify any registered observers that the <code>itemCount</code> child items starting at
     * position <code>{groupPosition, childPosition}</code> have changed.
     *
     * <p>This is an item change event, not a structural change event. It indicates that
     * any reflection of the data in the given position range is out of date and should
     * be updated. The items in the given range retain the same identity.</p>
     *
     * @param groupPosition Position of the group item which contains the changed child
     * @param childPositionStart Position of the first child item in the group that has changed
     * @param itemCount Number of items that have changed
     *
     * @see #notifyChildItemChanged(int, int)
     */
    public void notifyChildItemRangeChanged(int groupPosition, int childPositionStart, int itemCount) {
        mAdapter.notifyChildItemRangeChanged(groupPosition, childPositionStart, itemCount);
    }

    /**
     * Notify any registered observers that the group item reflected at <code>groupPosition</code>
     * has been newly inserted. The group item previously at <code>groupPosition</code> is now at
     * position <code>groupPosition + 1</code>.
     *
     * <p>This is a structural change event. Representations of other existing items in the
     * data set are still considered up to date and will not be rebound, though their
     * positions may be altered.</p>
     *
     * @param groupPosition Position of the newly inserted group item in the data set
     *
     * @see #notifyGroupItemRangeInserted(int, int)
     */
    public void notifyGroupItemInserted(int groupPosition) {
        mAdapter.notifyGroupItemInserted(groupPosition);
    }

    /**
     * Notify any registered observers that the currently reflected <code>itemCount</code>
     * group items starting at <code>groupPositionStart</code> have been newly inserted. The group items
     * previously located at <code>groupPositionStart</code> and beyond can now be found starting
     * at position <code>groupPositionStart + itemCount</code>.
     *
     * <p>This is a structural change event. Representations of other existing items in the
     * data set are still considered up to date and will not be rebound, though their positions
     * may be altered.</p>
     *
     * @param groupPositionStart Position of the first group item that was inserted
     * @param itemCount Number of group items inserted
     *
     * @see #notifyGroupItemInserted(int)
     */
    public void notifyGroupItemRangeInserted(int groupPositionStart, int itemCount) {
        mAdapter.notifyGroupItemRangeInserted(groupPositionStart, itemCount);
    }


    /**
     * Notify any registered observers that the group item reflected at <code>groupPosition</code>
     * has been newly inserted. The group item previously at <code>groupPosition</code> is now at
     * position <code>groupPosition + 1</code>.
     *
     * <p>This is a structural change event. Representations of other existing items in the
     * data set are still considered up to date and will not be rebound, though their
     * positions may be altered.</p>
     *
     * @param groupPosition Position of the group item which contains the inserted child
     * @param childPosition Position of the newly inserted child item in the data set
     *
     * @see #notifyChildItemRangeInserted(int, int, int)
     */
    public void notifyChildItemInserted(int groupPosition, int childPosition) {
        mAdapter.notifyChildItemInserted(groupPosition, childPosition);
    }

    /**
     * Notify any registered observers that the currently reflected <code>itemCount</code>
     * child items starting at <code>childPositionStart</code> have been newly inserted. The child items
     * previously located at <code>childPositionStart</code> and beyond can now be found starting
     * at position <code>childPositionStart + itemCount</code>.
     *
     * <p>This is a structural change event. Representations of other existing items in the
     * data set are still considered up to date and will not be rebound, though their positions
     * may be altered.</p>
     *
     * @param groupPosition Position of the group item which contains the inserted child
     * @param childPositionStart Position of the first child item that was inserted
     * @param itemCount Number of child items inserted
     *
     * @see #notifyChildItemInserted(int, int)
     */
    public void notifyChildItemRangeInserted(int groupPosition, int childPositionStart, int itemCount) {
        mAdapter.notifyChildItemRangeInserted(groupPosition, childPositionStart, itemCount);
    }

    /**
     * Notify any registered observers that the group item previously located at <code>groupPosition</code>
     * has been removed from the data set. The group items previously located at and after
     * <code>groupPosition</code> may now be found at <code>oldGroupPosition - 1</code>.
     *
     * <p>This is a structural change event. Representations of other existing items in the
     * data set are still considered up to date and will not be rebound, though their positions
     * may be altered.</p>
     *
     * @param groupPosition Position of the group item that has now been removed
     *
     * @see #notifyGroupItemRangeRemoved(int, int)
     */
    public void notifyGroupItemRemoved(int groupPosition) {
        mAdapter.notifyGroupItemRemoved(groupPosition);
    }

    /**
     * Notify any registered observers that the <code>itemCount</code> group items previously
     * located at <code>groupPositionStart</code> have been removed from the data set. The group items
     * previously located at and after <code>groupPositionStart + itemCount</code> may now be found
     * at <code>oldPosition - itemCount</code>.
     *
     * <p>This is a structural change event. Representations of other existing items in the data
     * set are still considered up to date and will not be rebound, though their positions
     * may be altered.</p>
     *
     * @param groupPositionStart Previous position of the first group item that was removed
     * @param itemCount Number of group items removed from the data set
     */
    public void notifyGroupItemRangeRemoved(int groupPositionStart, int itemCount) {
        mAdapter.notifyGroupItemRangeRemoved(groupPositionStart, itemCount);
    }

    /**
     * Notify any registered observers that the child item previously located at <code>childPosition</code>
     * has been removed from the data set. The child items previously located at and after
     * <code>childPosition</code> may now be found at <code>oldGroupPosition - 1</code>.
     *
     * <p>This is a structural change event. Representations of other existing items in the
     * data set are still considered up to date and will not be rebound, though their positions
     * may be altered.</p>
     *
     * @param groupPosition Position of the group item which was the parent of the child item that was removed
     * @param childPosition Position of the child item that has now been removed
     *
     * @see #notifyGroupItemRangeRemoved(int, int)
     */
    public void notifyChildItemRemoved(int groupPosition, int childPosition) {
        mAdapter.notifyChildItemRemoved(groupPosition, childPosition);
    }

    /**
     * Notify any registered observers that the <code>itemCount</code> child items previously
     * located at <code>childPositionStart</code> have been removed from the data set. The child items
     * previously located at and after <code>childPositionStart + itemCount</code> may now be found
     * at <code>oldPosition - itemCount</code>.
     *
     * <p>This is a structural change event. Representations of other existing items in the data
     * set are still considered up to date and will not be rebound, though their positions
     * may be altered.</p>
     *
     * @param groupPosition Position of the group item which was the parent of the child item that was removed
     * @param childPositionStart Previous position of the first child item that was removed
     * @param itemCount Number of child items removed from the data set
     */
    public void notifyChildItemRangeRemoved(int groupPosition, int childPositionStart, int itemCount) {
        mAdapter.notifyChildItemRangeRemoved(groupPosition, childPositionStart, itemCount);
    }

    public static class SavedState implements Parcelable {
        final int [] adapterSavedState;

        public SavedState(int[] adapterSavedState) {
            this.adapterSavedState = adapterSavedState;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeIntArray(this.adapterSavedState);
        }

        private SavedState(Parcel in) {
            this.adapterSavedState = in.createIntArray();
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
