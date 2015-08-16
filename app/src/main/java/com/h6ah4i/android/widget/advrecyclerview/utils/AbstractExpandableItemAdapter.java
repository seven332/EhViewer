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

package com.h6ah4i.android.widget.advrecyclerview.utils;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.expandable.ExpandableItemAdapter;

public abstract class AbstractExpandableItemAdapter<GVH extends RecyclerView.ViewHolder, CVH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ExpandableItemAdapter<GVH, CVH> {

    /**
     * This method will not be called.
     * Override {@link #onCreateGroupViewHolder(android.view.ViewGroup, int)} and
     * {@link #onCreateChildViewHolder(android.view.ViewGroup, int)} instead.
     *
     * @param parent   not used
     * @param viewType not used
     * @return null
     */
    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    /**
     * This method will not be called.
     * Override {@link #getGroupId(int)} and {@link #getChildId(int, int)} instead.
     *
     * @param position not used
     * @return {@link RecyclerView#NO_ID}
     */
    @Override
    public final long getItemId(int position) {
        return RecyclerView.NO_ID;
    }

    /**
     * This method will not be called.
     * Override {@link #getGroupItemViewType(int)} and {@link #getChildItemViewType(int, int)} instead.
     *
     * @param position not used
     * @return 0
     */
    @Override
    public final int getItemViewType(int position) {
        return 0;
    }

    /**
     * This method will not be called.
     * Override {@link #getGroupCount()} and {@link #getChildCount(int)} instead.
     *
     * @return 0
     */
    @Override
    public final int getItemCount() {
        return 0;
    }

    /**
     * This method will not be called.
     * Override {@link #onBindGroupViewHolder(RecyclerView.ViewHolder, int, int)} ()} and
     * {@link #onBindChildViewHolder(RecyclerView.ViewHolder, int, int, int)} instead.
     *
     * @param holder not used
     * @param position not used
     */
    @Override
    public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    }

    /**
     * Override this method if need to customize the behavior.
     * {@inheritDoc}
     */
    @Override
    public boolean onHookGroupExpand(int groupPosition, boolean fromUser) {
        return true;
    }

    /**
     * Override this method if need to customize the behavior.
     * {@inheritDoc}
     */
    @Override
    public boolean onHookGroupCollapse(int groupPosition, boolean fromUser) {
        return true;
    }
}