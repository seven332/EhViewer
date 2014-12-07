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

package com.hippo.ehviewer.widget;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;

public abstract class FooterAdapter<VH extends ViewHolder> extends RecyclerView.Adapter<VH> {

    public static final int TYPE_FOOTER = Integer.MAX_VALUE;

    private View mFooterView;

    public void setFooterView(View footerView) {
        mFooterView = footerView;
        notifyItemInserted(getItemCountActual());
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
        if (mFooterView == null || position != getItemCountActual())
            onBindViewHolderActual(holder, position);
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

    public int getItemViewTypeActual(int position) {
        return 0;
    }

    public abstract int getItemCountActual();

}
