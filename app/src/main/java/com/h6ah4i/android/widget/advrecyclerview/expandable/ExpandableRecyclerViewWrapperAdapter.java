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

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.utils.BaseWrapperAdapter;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;

class ExpandableRecyclerViewWrapperAdapter
        extends BaseWrapperAdapter<RecyclerView.ViewHolder>
        implements DraggableItemAdapter<RecyclerView.ViewHolder>,
        SwipeableItemAdapter<RecyclerView.ViewHolder> {
    private static final String TAG = "ARVExpandableWrapper";

    private static final int VIEW_TYPE_FLAG_IS_GROUP = ExpandableAdapterHelper.VIEW_TYPE_FLAG_IS_GROUP;

    private static final int STATE_FLAG_INITIAL_VALUE = -1;

    private ExpandableItemAdapter mExpandableItemAdapter;
    private RecyclerViewExpandableItemManager mExpandableListManager;
    private ExpandablePositionTranslator mPositionTranslator;
    private int mDraggingItemGroupRangeStart = RecyclerView.NO_POSITION;
    private int mDraggingItemGroupRangeEnd = RecyclerView.NO_POSITION;
    private int mDraggingItemChildRangeStart = RecyclerView.NO_POSITION;
    private int mDraggingItemChildRangeEnd = RecyclerView.NO_POSITION;

    private RecyclerViewExpandableItemManager.OnGroupExpandListener mOnGroupExpandListener;
    private RecyclerViewExpandableItemManager.OnGroupCollapseListener mOnGroupCollapseListener;

    public ExpandableRecyclerViewWrapperAdapter(RecyclerViewExpandableItemManager manager, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, int[] expandedItemsSavedState) {
        super(adapter);

        mExpandableItemAdapter = getExpandableItemAdapter(adapter);
        if (mExpandableItemAdapter == null) {
            throw new IllegalArgumentException("adapter does not implement RecyclerViewExpandableListManager");
        }

        if (manager == null) {
            throw new IllegalArgumentException("manager cannot be null");
        }

        mExpandableListManager = manager;

        mPositionTranslator = new ExpandablePositionTranslator();
        mPositionTranslator.build(mExpandableItemAdapter);

        if (expandedItemsSavedState != null) {
            // NOTE: do not call hook routines and listener methods
            mPositionTranslator.restoreExpandedGroupItems(expandedItemsSavedState, null, null, null);
        }
    }

    @Override
    protected void onRelease() {
        super.onRelease();

        mExpandableItemAdapter = null;
        mExpandableListManager = null;
        mOnGroupExpandListener = null;
        mOnGroupCollapseListener = null;
    }

    @Override
    public int getItemCount() {
        return mPositionTranslator.getItemCount();
    }

    @Override
    public long getItemId(int position) {
        if (mExpandableItemAdapter == null) {
            return RecyclerView.NO_ID;
        }

        final long expandablePosition = mPositionTranslator.getExpandablePosition(position);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

        if (childPosition == RecyclerView.NO_POSITION) {
            final long groupId = mExpandableItemAdapter.getGroupId(groupPosition);
            return ExpandableAdapterHelper.getCombinedGroupId(groupId);
        } else {
            final long groupId = mExpandableItemAdapter.getGroupId(groupPosition);
            final long childId = mExpandableItemAdapter.getChildId(groupPosition, childPosition);
            return ExpandableAdapterHelper.getCombinedChildId(groupId, childId);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mExpandableItemAdapter == null) {
            return 0;
        }

        final long expandablePosition = mPositionTranslator.getExpandablePosition(position);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

        final int type;

        if (childPosition == RecyclerView.NO_POSITION) {
            type = mExpandableItemAdapter.getGroupItemViewType(groupPosition);
        } else {
            type = mExpandableItemAdapter.getChildItemViewType(groupPosition, childPosition);
        }

        if ((type & VIEW_TYPE_FLAG_IS_GROUP) != 0) {
            throw new IllegalStateException("Illegal view type (type = " + Integer.toHexString(type) + ")");
        }

        return (childPosition == RecyclerView.NO_POSITION) ? (type | VIEW_TYPE_FLAG_IS_GROUP) : (type);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mExpandableItemAdapter == null) {
            return null;
        }

        final int maskedViewType = (viewType & (~VIEW_TYPE_FLAG_IS_GROUP));

        final RecyclerView.ViewHolder holder;

        if ((viewType & VIEW_TYPE_FLAG_IS_GROUP) != 0) {
            holder = mExpandableItemAdapter.onCreateGroupViewHolder(parent, maskedViewType);
        } else {
            holder = mExpandableItemAdapter.onCreateChildViewHolder(parent, maskedViewType);
        }

        if (holder instanceof ExpandableItemViewHolder) {
            ((ExpandableItemViewHolder) holder).setExpandStateFlags(STATE_FLAG_INITIAL_VALUE);
        }

        return holder;

    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (mExpandableItemAdapter == null) {
            return;
        }

        final long expandablePosition = mPositionTranslator.getExpandablePosition(position);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);
        final int viewType = (holder.getItemViewType() & (~VIEW_TYPE_FLAG_IS_GROUP));

        // update flags
        int flags = 0;

        if (childPosition == RecyclerView.NO_POSITION) {
            flags |= RecyclerViewExpandableItemManager.STATE_FLAG_IS_GROUP;
        } else {
            flags |= RecyclerViewExpandableItemManager.STATE_FLAG_IS_CHILD;
        }

        if (mPositionTranslator.isGroupExpanded(groupPosition)) {
            flags |= RecyclerViewExpandableItemManager.STATE_FLAG_IS_EXPANDED;
        }

        safeUpdateExpandStateFlags(holder, flags);

        correctItemDragStateFlags(holder, groupPosition, childPosition);

        if (childPosition == RecyclerView.NO_POSITION) {
            mExpandableItemAdapter.onBindGroupViewHolder(holder, groupPosition, viewType);
        } else {
            mExpandableItemAdapter.onBindChildViewHolder(holder, groupPosition, childPosition, viewType);
        }
    }

    private void rebuildPositionTranslator() {
        if (mPositionTranslator != null) {
            int [] savedState = mPositionTranslator.getSavedStateArray();
            mPositionTranslator.build(mExpandableItemAdapter);

            // NOTE: do not call hook routines and listener methods
            mPositionTranslator.restoreExpandedGroupItems(savedState, null, null, null);
        }
    }

    @Override
    protected void onHandleWrappedAdapterChanged() {
        rebuildPositionTranslator();
        super.onHandleWrappedAdapterChanged();
    }

    @Override
    protected void onHandleWrappedAdapterItemRangeChanged(int positionStart, int itemCount) {
        super.onHandleWrappedAdapterItemRangeChanged(positionStart, itemCount);
    }

    @Override
    protected void onHandleWrappedAdapterItemRangeInserted(int positionStart, int itemCount) {
        rebuildPositionTranslator();
        super.onHandleWrappedAdapterItemRangeInserted(positionStart, itemCount);
    }

    @Override
    protected void onHandleWrappedAdapterItemRangeRemoved(int positionStart, int itemCount) {
        if (itemCount == 1) {
            final long expandablePosition = mPositionTranslator.getExpandablePosition(positionStart);
            final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
            final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

            if (childPosition == RecyclerView.NO_POSITION) {
                mPositionTranslator.removeGroupItem(groupPosition);
            } else {
                mPositionTranslator.removeChildItem(groupPosition, childPosition);
            }
        } else {
            rebuildPositionTranslator();
        }

        super.onHandleWrappedAdapterItemRangeRemoved(positionStart, itemCount);
    }

    @Override
    protected void onHandleWrappedAdapterRangeMoved(int fromPosition, int toPosition, int itemCount) {
        rebuildPositionTranslator();
        super.onHandleWrappedAdapterRangeMoved(fromPosition, toPosition, itemCount);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onCheckCanStartDrag(RecyclerView.ViewHolder holder, int position, int x, int y) {
        if (!(mExpandableItemAdapter instanceof ExpandableDraggableItemAdapter)) {
            return false;
        }

        final ExpandableDraggableItemAdapter adapter = (ExpandableDraggableItemAdapter) mExpandableItemAdapter;

        final int flatPosition = position;
        final long expandablePosition = mPositionTranslator.getExpandablePosition(flatPosition);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

        boolean canStart;
        if (childPosition == RecyclerView.NO_POSITION) {
            canStart = adapter.onCheckGroupCanStartDrag(holder, groupPosition, x, y);
        } else {
            canStart = adapter.onCheckChildCanStartDrag(holder, groupPosition, childPosition, x, y);
        }

        mDraggingItemGroupRangeStart = RecyclerView.NO_POSITION;
        mDraggingItemGroupRangeEnd = RecyclerView.NO_POSITION;
        mDraggingItemChildRangeStart = RecyclerView.NO_POSITION;
        mDraggingItemChildRangeEnd = RecyclerView.NO_POSITION;

        return canStart;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ItemDraggableRange onGetItemDraggableRange(RecyclerView.ViewHolder holder, int position) {
        if (!(mExpandableItemAdapter instanceof ExpandableDraggableItemAdapter)) {
            return null;
        }

        if (mExpandableItemAdapter.getGroupCount() < 1) {
            return null;
        }

        final ExpandableDraggableItemAdapter adapter = (ExpandableDraggableItemAdapter) mExpandableItemAdapter;

        final int flatPosition = position;
        final long expandablePosition = mPositionTranslator.getExpandablePosition(flatPosition);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

        if (childPosition == RecyclerView.NO_POSITION) {
            // group
            final ItemDraggableRange range = adapter.onGetGroupItemDraggableRange(holder, groupPosition);

            if (range == null) {
                final int lastGroup = Math.max(0, mExpandableItemAdapter.getGroupCount() - 1);
                final int start = 0;
                final int end = Math.max(start, mPositionTranslator.getItemCount() - mPositionTranslator.getVisibleChildCount(lastGroup) - 1);

                return new ItemDraggableRange(start, end);
            } else if (isGroupPositionRange(range)) {
                final long startPackedGroupPosition = ExpandableAdapterHelper.getPackedPositionForGroup(range.getStart());
                final long endPackedGroupPosition = ExpandableAdapterHelper.getPackedPositionForGroup(range.getEnd());
                final int start = mPositionTranslator.getFlatPosition(startPackedGroupPosition);
                int end = mPositionTranslator.getFlatPosition(endPackedGroupPosition);

                if (range.getEnd() > groupPosition) {
                    end += mPositionTranslator.getVisibleChildCount(range.getEnd());
                }

                mDraggingItemGroupRangeStart = range.getStart();
                mDraggingItemGroupRangeEnd = range.getEnd();

                return new ItemDraggableRange(start, end);
            } else {
                throw new IllegalStateException("Invalid range specified: " + range);
            }
        } else {
            // child
            final ItemDraggableRange range = adapter.onGetChildItemDraggableRange(holder, groupPosition, childPosition);

            // NOTE:
            // This method returns actual drag-sortable range, but the visual drag-sortable range would be different.
            // Thus appending the STATE_FLAG_IS_IN_RANGE flag at correctItemDragStateFlags() to avoid visual corruption.
            if (range == null) {
                final int start = 1;  // 1 --- to avoid swapping with the first group item

                return new ItemDraggableRange(start, Math.max(start, mPositionTranslator.getItemCount() - 1));
            } else if (isGroupPositionRange(range)) {
                final long startPackedGroupPosition = ExpandableAdapterHelper.getPackedPositionForGroup(range.getStart());
                final long endPackedGroupPosition = ExpandableAdapterHelper.getPackedPositionForGroup(range.getEnd());
                final int end = mPositionTranslator.getFlatPosition(endPackedGroupPosition)
                        + mPositionTranslator.getVisibleChildCount(range.getEnd());

                int start = mPositionTranslator.getFlatPosition(startPackedGroupPosition) + 1;

                start = Math.min(start, end);

                mDraggingItemGroupRangeStart = range.getStart();
                mDraggingItemGroupRangeEnd = range.getEnd();

                return new ItemDraggableRange(start, end);
            } else if (isChildPositionRange(range)) {
                final int maxChildrenPos = Math.max(mPositionTranslator.getVisibleChildCount(groupPosition) - 1, 0);
                final int childStart = Math.min(range.getStart(), maxChildrenPos);
                final int childEnd = Math.min(range.getEnd(), maxChildrenPos);
                final long startPackedChildPosition = ExpandableAdapterHelper.getPackedPositionForChild(groupPosition, childStart);
                final long endPackedChildPosition = ExpandableAdapterHelper.getPackedPositionForChild(groupPosition, childEnd);
                final int start = mPositionTranslator.getFlatPosition(startPackedChildPosition);
                final int end = mPositionTranslator.getFlatPosition(endPackedChildPosition);

                mDraggingItemChildRangeStart = childStart;
                mDraggingItemChildRangeEnd = childEnd;

                return new ItemDraggableRange(start, end);
            }  else {
                throw new IllegalStateException("Invalid range specified: " + range);
            }
        }
    }

    private static boolean isGroupPositionRange(ItemDraggableRange range) {
        if (range.getClass().equals(GroupPositionItemDraggableRange.class)) {
            return true;
        } else if (range.getClass().equals(ItemDraggableRange.class)) {
            // NOTE: ItemDraggableRange is regarded as group position
            return true;
        }
        return false;
    }

    private static boolean isChildPositionRange(ItemDraggableRange range) {
        return range.getClass().equals(ChildPositionItemDraggableRange.class);
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        if (!(mExpandableItemAdapter instanceof ExpandableDraggableItemAdapter)) {
            return;
        }

        mDraggingItemGroupRangeStart = RecyclerView.NO_POSITION;
        mDraggingItemGroupRangeEnd = RecyclerView.NO_POSITION;
        mDraggingItemChildRangeStart = RecyclerView.NO_POSITION;
        mDraggingItemChildRangeEnd = RecyclerView.NO_POSITION;

        if (fromPosition == toPosition) {
            return;
        }

        final ExpandableDraggableItemAdapter adapter = (ExpandableDraggableItemAdapter) mExpandableItemAdapter;

        final long expandableFromPosition = mPositionTranslator.getExpandablePosition(fromPosition);
        final int fromGroupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandableFromPosition);
        final int fromChildPosition = ExpandableAdapterHelper.getPackedPositionChild(expandableFromPosition);

        final long expandableToPosition = mPositionTranslator.getExpandablePosition(toPosition);
        final int toGroupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandableToPosition);
        final int toChildPosition = ExpandableAdapterHelper.getPackedPositionChild(expandableToPosition);

        final boolean fromIsGroup = (fromChildPosition == RecyclerView.NO_POSITION);
        final boolean toIsGroup = (toChildPosition == RecyclerView.NO_POSITION);

        int actualToFlatPosition = fromPosition;

        if (fromIsGroup && toIsGroup) {
            adapter.onMoveGroupItem(fromGroupPosition, toGroupPosition);
            mPositionTranslator.moveGroupItem(fromGroupPosition, toGroupPosition);
            actualToFlatPosition = toPosition;
        } else if (!fromIsGroup && !toIsGroup) {
            int modToChildPosition;

            // correct child position
            if (fromGroupPosition == toGroupPosition) {
                modToChildPosition = toChildPosition;
            } else {
                if (fromPosition < toPosition) {
                    modToChildPosition = toChildPosition + 1;
                } else {
                    modToChildPosition = toChildPosition;
                }
            }

            actualToFlatPosition = mPositionTranslator.getFlatPosition(
                    ExpandableAdapterHelper.getPackedPositionForChild(fromGroupPosition, modToChildPosition));

            adapter.onMoveChildItem(fromGroupPosition, fromChildPosition, toGroupPosition, modToChildPosition);
            mPositionTranslator.moveChildItem(fromGroupPosition, fromChildPosition, toGroupPosition, modToChildPosition);
        } else if (!fromIsGroup /*&& toIsGroup NOTE: toIsGroup is always true here*/) {
            int modToGroupPosition;
            int modToChildPosition;

            if (toPosition < fromPosition) {
                if (toGroupPosition == 0) {
                    // insert at the top
                    modToGroupPosition = toGroupPosition;
                    modToChildPosition = 0;
                } else {
                    // insert at the end
                    modToGroupPosition = toGroupPosition - 1;
                    modToChildPosition = mPositionTranslator.getChildCount(modToGroupPosition);
                }
            } else {
                if (mPositionTranslator.isGroupExpanded(toGroupPosition)) {
                    // insert at the top
                    modToGroupPosition = toGroupPosition;
                    modToChildPosition = 0;
                } else {
                    // insert at the end
                    modToGroupPosition = toGroupPosition;
                    modToChildPosition = mPositionTranslator.getChildCount(modToGroupPosition);
                }
            }

            if (fromGroupPosition == modToGroupPosition) {
                final int lastIndex = Math.max(0, mPositionTranslator.getChildCount(modToGroupPosition) - 1);
                modToChildPosition = Math.min(modToChildPosition, lastIndex);
            }

            if (!((fromGroupPosition == modToGroupPosition) && (fromChildPosition == modToChildPosition))) {
                if (mPositionTranslator.isGroupExpanded(toGroupPosition)) {
                    actualToFlatPosition = toPosition;
                } else {
                    actualToFlatPosition = RecyclerView.NO_POSITION;
                }

                adapter.onMoveChildItem(fromGroupPosition, fromChildPosition, modToGroupPosition, modToChildPosition);
                mPositionTranslator.moveChildItem(fromGroupPosition, fromChildPosition, modToGroupPosition, modToChildPosition);
            }
        } else { // if (fromIsGroup && !toIsGroup)
            if (fromGroupPosition != toGroupPosition) {
                actualToFlatPosition = mPositionTranslator.getFlatPosition(ExpandableAdapterHelper.getPackedPositionForGroup(toGroupPosition));

                adapter.onMoveGroupItem(fromGroupPosition, toGroupPosition);
                mPositionTranslator.moveGroupItem(fromGroupPosition, toGroupPosition);
            }
        }

        if (actualToFlatPosition != fromPosition) {
            if (actualToFlatPosition != RecyclerView.NO_POSITION) {
                notifyItemMoved(fromPosition, actualToFlatPosition);
            } else {
                notifyItemRemoved(fromPosition);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public int onGetSwipeReactionType(RecyclerView.ViewHolder holder, int position, int x, int y) {
        if (!(mExpandableItemAdapter instanceof ExpandableSwipeableItemAdapter)) {
            return RecyclerViewSwipeManager.REACTION_CAN_NOT_SWIPE_BOTH;
        }

        final ExpandableSwipeableItemAdapter adapter = (ExpandableSwipeableItemAdapter) mExpandableItemAdapter;

        final int flatPosition = position;
        final long expandablePosition = mPositionTranslator.getExpandablePosition(flatPosition);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

        if (childPosition == RecyclerView.NO_POSITION) {
            return adapter.onGetGroupItemSwipeReactionType(holder, groupPosition, x, y);
        } else {
            return adapter.onGetChildItemSwipeReactionType(holder, groupPosition, childPosition, x, y);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSetSwipeBackground(RecyclerView.ViewHolder holder, int position, int type) {
        if (!(mExpandableItemAdapter instanceof ExpandableSwipeableItemAdapter)) {
            return;
        }

        final ExpandableSwipeableItemAdapter adapter = (ExpandableSwipeableItemAdapter) mExpandableItemAdapter;

        final int flatPosition = position;
        final long expandablePosition = mPositionTranslator.getExpandablePosition(flatPosition);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

        if (childPosition == RecyclerView.NO_POSITION) {
            adapter.onSetGroupItemSwipeBackground(holder, groupPosition, type);
        } else {
            adapter.onSetChildItemSwipeBackground(holder, groupPosition, childPosition, type);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public int onSwipeItem(RecyclerView.ViewHolder holder, int position, int result) {
        if (!(mExpandableItemAdapter instanceof ExpandableSwipeableItemAdapter)) {
            return RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_DEFAULT;
        }

        final ExpandableSwipeableItemAdapter adapter = (ExpandableSwipeableItemAdapter) mExpandableItemAdapter;

        final int flatPosition = position;
        final long expandablePosition = mPositionTranslator.getExpandablePosition(flatPosition);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

        if (childPosition == RecyclerView.NO_POSITION) {
            return adapter.onSwipeGroupItem(holder, groupPosition, result);
        } else {
            return adapter.onSwipeChildItem(holder, groupPosition, childPosition, result);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onPerformAfterSwipeReaction(RecyclerView.ViewHolder holder, int position, int result, int reaction) {
        if (!(mExpandableItemAdapter instanceof ExpandableSwipeableItemAdapter)) {
            return;
        }

        final ExpandableSwipeableItemAdapter adapter = (ExpandableSwipeableItemAdapter) mExpandableItemAdapter;

        final int flatPosition = position;
        final long expandablePosition = mPositionTranslator.getExpandablePosition(flatPosition);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

        if (childPosition == RecyclerView.NO_POSITION) {
            adapter.onPerformAfterSwipeGroupReaction(holder, groupPosition, result, reaction);
        } else {
            adapter.onPerformAfterSwipeChildReaction(holder, groupPosition, childPosition, result, reaction);
        }
    }

    // NOTE: This method is called from RecyclerViewExpandableItemManager
    /*package*/
    @SuppressWarnings("unchecked")
    boolean onTapItem(RecyclerView.ViewHolder holder, int position, int x, int y) {
        if (mExpandableItemAdapter == null) {
            return false;
        }

        final int flatPosition = position;
        final long expandablePosition = mPositionTranslator.getExpandablePosition(flatPosition);
        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(expandablePosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(expandablePosition);

        if (childPosition != RecyclerView.NO_POSITION) {
            return false;
        }

        final boolean expand = !(mPositionTranslator.isGroupExpanded(groupPosition));

        boolean result = mExpandableItemAdapter.onCheckCanExpandOrCollapseGroup(holder, groupPosition, x, y, expand);

        if (!result) {
            return false;
        }

        if (expand) {
            expandGroup(groupPosition, true);
        } else {
            collapseGroup(groupPosition, true);
        }

        return true;
    }

    /*package*/ boolean collapseGroup(int groupPosition, boolean fromUser) {
        if (!mPositionTranslator.isGroupExpanded(groupPosition)) {
            return false;
        }

        // call hook method
        if (!mExpandableItemAdapter.onHookGroupCollapse(groupPosition, fromUser)) {
            return false;
        }

        if (mPositionTranslator.collapseGroup(groupPosition)) {
            final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPosition);
            final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);
            final int childCount = mPositionTranslator.getChildCount(groupPosition);

            notifyItemRangeRemoved(flatPosition + 1, childCount);
        }


        {
            final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPosition);
            final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

            notifyItemChanged(flatPosition);
        }

        // raise onGroupCollapse() event
        if (mOnGroupCollapseListener != null) {
            mOnGroupCollapseListener.onGroupCollapse(groupPosition, fromUser);
        }

        return true;
    }

    /*package*/ boolean expandGroup(int groupPosition, boolean fromUser) {
        if (mPositionTranslator.isGroupExpanded(groupPosition)) {
            return false;
        }

        // call hook method
        if (!mExpandableItemAdapter.onHookGroupExpand(groupPosition, fromUser)) {
            return false;
        }

        if (mPositionTranslator.expandGroup(groupPosition)) {
            final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPosition);
            final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);
            final int childCount = mPositionTranslator.getChildCount(groupPosition);

            notifyItemRangeInserted(flatPosition + 1, childCount);
        }

        {
            final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPosition);
            final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

            notifyItemChanged(flatPosition);
        }

        // raise onGroupExpand() event
        if (mOnGroupExpandListener != null) {
            mOnGroupExpandListener.onGroupExpand(groupPosition, fromUser);
        }

        return true;
    }

    /*package*/ boolean isGroupExpanded(int groupPosition) {
        return mPositionTranslator.isGroupExpanded(groupPosition);
    }

    /*package*/ long getExpandablePosition(int flatPosition) {
        return mPositionTranslator.getExpandablePosition(flatPosition);
    }

    /*package*/ int getFlatPosition(long packedPosition) {
        return mPositionTranslator.getFlatPosition(packedPosition);
    }

    /*package*/ int[] getExpandedItemsSavedStateArray() {
        if (mPositionTranslator != null) {
            return mPositionTranslator.getSavedStateArray();
        } else {
            return null;
        }
    }

    /*package*/ void setOnGroupExpandListener(RecyclerViewExpandableItemManager.OnGroupExpandListener listener) {
        mOnGroupExpandListener = listener;
    }

    /*package*/ void setOnGroupCollapseListener(RecyclerViewExpandableItemManager.OnGroupCollapseListener listener) {
        mOnGroupCollapseListener = listener;
    }

    /*package*/ void restoreState(int[] adapterSavedState, boolean callHook, boolean callListeners) {
        mPositionTranslator.restoreExpandedGroupItems(
                adapterSavedState,
                (callHook ? mExpandableItemAdapter : null),
                (callListeners ? mOnGroupExpandListener : null),
                (callListeners ? mOnGroupCollapseListener : null));
    }

    /*package*/ void notifyGroupItemChanged(int groupPosition) {
        final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPosition);
        final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

        if (flatPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(flatPosition);
        }
    }

    /*package*/ void notifyGroupAndChildrenItemsChanged(int groupPosition) {
        final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPosition);
        final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);
        final int visibleChildCount = mPositionTranslator.getVisibleChildCount(groupPosition);

        if (flatPosition != RecyclerView.NO_POSITION) {
            notifyItemRangeChanged(flatPosition, 1 + visibleChildCount);
        }
    }

    /*package*/ void notifyChildrenOfGroupItemChanged(int groupPosition) {
        final int visibleChildCount = mPositionTranslator.getVisibleChildCount(groupPosition);

        // notify if the group is expanded
        if (visibleChildCount > 0) {
            final long packedPosition = ExpandableAdapterHelper.getPackedPositionForChild(groupPosition, 0);
            final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

            if (flatPosition != RecyclerView.NO_POSITION) {
                notifyItemRangeChanged(flatPosition, visibleChildCount);
            }
        }
    }

    /*package*/ void notifyChildItemChanged(int groupPosition, int childPosition) {
        notifyChildItemRangeChanged(groupPosition, childPosition, 1);
    }

    /*package*/ void notifyChildItemRangeChanged(int groupPosition, int childPositionStart, int itemCount) {
        final int visibleChildCount = mPositionTranslator.getVisibleChildCount(groupPosition);

        // notify if the group is expanded
        if ((visibleChildCount > 0) && (childPositionStart < visibleChildCount)) {
            final long packedPosition = ExpandableAdapterHelper.getPackedPositionForChild(groupPosition, 0);
            final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

            if (flatPosition != RecyclerView.NO_POSITION) {
                final int startPosition = flatPosition + childPositionStart;
                final int count = Math.min(itemCount, (visibleChildCount - childPositionStart));

                notifyItemRangeChanged(startPosition, count);
            }
        }
    }

    /*package*/ void notifyChildItemInserted(int groupPosition, int childPosition) {
        mPositionTranslator.insertChildItem(groupPosition, childPosition);

        final long packedPosition = ExpandableAdapterHelper.getPackedPositionForChild(groupPosition, childPosition);
        final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

        if (flatPosition != RecyclerView.NO_POSITION) {
            notifyItemInserted(flatPosition);
        }
    }

    /*package*/ void notifyChildItemRangeInserted(int groupPosition, int childPositionStart, int itemCount) {
        mPositionTranslator.insertChildItems(groupPosition, childPositionStart, itemCount);

        final long packedPosition = ExpandableAdapterHelper.getPackedPositionForChild(groupPosition, childPositionStart);
        final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

        if (flatPosition != RecyclerView.NO_POSITION) {
            notifyItemRangeInserted(flatPosition, itemCount);
        }
    }

    /*package*/ void notifyChildItemRemoved(int groupPosition, int childPosition) {
        final long packedPosition = ExpandableAdapterHelper.getPackedPositionForChild(groupPosition, childPosition);
        final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

        mPositionTranslator.removeChildItem(groupPosition, childPosition);

        if (flatPosition != RecyclerView.NO_POSITION) {
            notifyItemRemoved(flatPosition);
        }
    }

    /*package*/ void notifyChildItemRangeRemoved(int groupPosition, int childPositionStart, int itemCount) {
        final long packedPosition = ExpandableAdapterHelper.getPackedPositionForChild(groupPosition, childPositionStart);
        final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

        mPositionTranslator.removeChildItems(groupPosition, childPositionStart, itemCount);

        if (flatPosition != RecyclerView.NO_POSITION) {
            notifyItemRangeRemoved(flatPosition, itemCount);
        }
    }

    /*package*/ void notifyGroupItemInserted(int groupPosition) {
        int insertedCount = mPositionTranslator.insertGroupItem(groupPosition);
        if (insertedCount > 0) {
            final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPosition);
            final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

            notifyItemInserted(flatPosition);
        }
    }

    /*package*/ void notifyGroupItemRangeInserted(int groupPositionStart, int count) {
        int insertedCount = mPositionTranslator.insertGroupItems(groupPositionStart, count);
        if (insertedCount > 0) {
            final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPositionStart);
            final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

            notifyItemRangeInserted(flatPosition, insertedCount);
        }
    }

    /*package*/ void notifyGroupItemRemoved(int groupPosition) {
        final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPosition);
        final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

        int removedCount = mPositionTranslator.removeGroupItem(groupPosition);
        if (removedCount > 0) {
            notifyItemRangeRemoved(flatPosition, removedCount);
        }
    }

    /*package*/ void notifyGroupItemRangeRemoved(int groupPositionStart, int count) {
        final long packedPosition = ExpandableAdapterHelper.getPackedPositionForGroup(groupPositionStart);
        final int flatPosition = mPositionTranslator.getFlatPosition(packedPosition);

        int removedCount = mPositionTranslator.removeGroupItems(groupPositionStart, count);
        if (removedCount > 0) {
            notifyItemRangeRemoved(flatPosition, removedCount);
        }
    }


    private static ExpandableItemAdapter getExpandableItemAdapter(RecyclerView.Adapter adapter) {
        return WrapperAdapterUtils.findWrappedAdapter(adapter, ExpandableItemAdapter.class);
    }

    private static void safeUpdateExpandStateFlags(RecyclerView.ViewHolder holder, int flags) {
        if (!(holder instanceof ExpandableItemViewHolder)) {
            return;
        }

        final ExpandableItemViewHolder holder2 = (ExpandableItemViewHolder) holder;

        final int curFlags = holder2.getExpandStateFlags();
        final int mask = ~RecyclerViewExpandableItemManager.STATE_FLAG_IS_UPDATED;

        // append UPDATED flag
        if ((curFlags == STATE_FLAG_INITIAL_VALUE) || (((curFlags ^ flags) & mask) != 0)) {
            flags |= RecyclerViewExpandableItemManager.STATE_FLAG_IS_UPDATED;
        }

        holder2.setExpandStateFlags(flags);
    }

    private void correctItemDragStateFlags(RecyclerView.ViewHolder holder, int groupPosition, int childPosition) {
        if (!(holder instanceof DraggableItemViewHolder)) {
            return;
        }

        final DraggableItemViewHolder holder2 = (DraggableItemViewHolder) holder;

        final boolean groupRangeSpecified =
                (mDraggingItemGroupRangeStart != RecyclerView.NO_POSITION) &&
                (mDraggingItemGroupRangeEnd != RecyclerView.NO_POSITION);
        final boolean childRangeSpecified =
                (mDraggingItemChildRangeStart != RecyclerView.NO_POSITION) &&
                (mDraggingItemChildRangeEnd != RecyclerView.NO_POSITION);
        final boolean isInGroupRange =
                        (groupPosition >= mDraggingItemGroupRangeStart) &&
                        (groupPosition <= mDraggingItemGroupRangeEnd);
        final boolean isInChildRange =
                (groupPosition != RecyclerView.NO_POSITION) &&
                (childPosition >= mDraggingItemChildRangeStart) &&
                        (childPosition <= mDraggingItemChildRangeEnd);

        final int flags = holder2.getDragStateFlags();
        boolean needCorrection = false;

        if (((flags & RecyclerViewDragDropManager.STATE_FLAG_DRAGGING) != 0) &&
                ((flags & RecyclerViewDragDropManager.STATE_FLAG_IS_IN_RANGE) == 0)) {
            if (!groupRangeSpecified || isInGroupRange) {
                if (!childRangeSpecified || (childRangeSpecified && isInChildRange)) {
                    needCorrection = true;
                }
            }
        }

        if (needCorrection) {
            holder2.setDragStateFlags(
                    flags | RecyclerViewDragDropManager.STATE_FLAG_IS_IN_RANGE |
                            RecyclerViewDragDropManager.STATE_FLAG_IS_UPDATED);
        }
    }
}
