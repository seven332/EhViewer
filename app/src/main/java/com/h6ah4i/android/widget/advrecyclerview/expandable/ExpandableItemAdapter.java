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

public interface ExpandableItemAdapter<GVH extends RecyclerView.ViewHolder, CVH extends RecyclerView.ViewHolder> {

    /**
     * Gets the number of groups.
     *
     * @return the number of groups
     */
    int getGroupCount();

    /**
     * Gets the number of children in a specified group.
     *
     * @param groupPosition the position of the group for which the children count should be returned
     *
     * @return the number of children
     */
    int getChildCount(int groupPosition);

    /**
     * Gets the ID for the group at the given position. This group ID must be unique across groups.
     *
     * The combined ID (see {@link RecyclerViewExpandableItemManager#getCombinedGroupId(long)})
     * must be unique across ALL items (groups and all children).
     *
     * @param groupPosition the position of the group for which the ID is wanted
     * @return the ID associated with the group
     */
    long getGroupId(int groupPosition);

    /**
     * Gets the ID for the given child within the given group.
     *
     * This ID must be unique across all children within the group.
     * The combined ID (see {@link RecyclerViewExpandableItemManager#getCombinedChildId(long, long)})
     * must be unique across ALL items (groups and all children).
     *
     * @param groupPosition the position of the group that contains the child
     * @param childPosition the position of the child within the group for which the ID is wanted

     * @return the ID associated with the child
     */
    long getChildId(int groupPosition, int childPosition);

    /**
     * Gets the view type of the specified group.
     *
     * @param groupPosition the position of the group for which the view type is wanted
     *
     * @return integer value identifying the type of the view needed to represent the group item at position. Type codes need positive number but not be contiguous.
     */
    int getGroupItemViewType(int groupPosition);

    /**
     * Gets the view type of the specified child.
     *
     * @param groupPosition the position of the group that contains the child
     * @param childPosition the position of the child within the group for which the view type is wanted
     *
     * @return integer value identifying the type of the view needed to represent the group item at position. Type codes need positive number but not be contiguous.
     */
    int getChildItemViewType(int groupPosition, int childPosition);

    /**
     * Called when RecyclerView needs a new {@link GVH} of the given type to represent a group item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position
     * @param viewType The view type of the new View
     *
     * @return A new group ViewHolder that holds a View of the given view type
     */
    GVH onCreateGroupViewHolder(ViewGroup parent, int viewType);

    /**
     * Called when RecyclerView needs a new {@link CVH} of the given type to represent a child item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position
     * @param viewType The view type of the new View
     *
     * @return A new child ViewHolder that holds a View of the given view type
     */
    CVH onCreateChildViewHolder(ViewGroup parent, int viewType);

    /**
     * Called by RecyclerView to display the group data at the specified position.
     * This method should update the contents of the {@link android.support.v7.widget.RecyclerView.ViewHolder#itemView}
     * to reflect the item at the given position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set
     * @param groupPosition The position of the group item within the adapter's data set
     * @param viewType The view type code
     */
    void onBindGroupViewHolder(GVH holder, int groupPosition, int viewType);

    /**
     * Called by RecyclerView to display the child data at the specified position.
     * This method should update the contents of the {@link android.support.v7.widget.RecyclerView.ViewHolder#itemView}
     * to reflect the item at the given position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set
     * @param groupPosition The position of the group item within the adapter's data set
     * @param childPosition The position of the child item within the group
     * @param viewType The view type code
     */
    void onBindChildViewHolder(CVH holder, int groupPosition, int childPosition, int viewType);

    /**
     * Called when a user attempt to expand/collapse a group item by tapping.
     *
     * @param holder The ViewHolder which is associated to group item user is attempt to expand/collapse
     * @param groupPosition Group position
     * @param x Touched X position. Relative from the itemView's top-left
     * @param y Touched Y position. Relative from the itemView's top-left
     * @param expand true: expand, false: collapse
     *
     * @return Whether to perform expand/collapse operation.
     */
    boolean onCheckCanExpandOrCollapseGroup(GVH holder, int groupPosition, int x, int y, boolean expand);

    /**
     * Called when a group attempt to expand by user operation or by
     * {@link com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager#expandGroup(int)} method.
     *
     * @param groupPosition The position of the group item within the adapter's data set
     * @param fromUser Whether the expand request is issued by a user operation
     *
     * @return Whether the group can be expanded. If returns false, the group keeps collapsed.
     */
    boolean onHookGroupExpand(int groupPosition, boolean fromUser);

    /**
     * Called when a group attempt to expand by user operation or by
     * {@link com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager#collapseGroup(int)} method.
     *
     * @param groupPosition The position of the group item within the adapter's data set
     * @param fromUser Whether the collapse request is issued by a user operation
     *
     * @return Whether the group can be collapsed. If returns false, the group keeps expanded.
     */
    boolean onHookGroupCollapse(int groupPosition, boolean fromUser);
}
