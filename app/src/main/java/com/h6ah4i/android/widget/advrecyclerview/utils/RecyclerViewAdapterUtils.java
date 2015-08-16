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
import android.view.View;
import android.view.ViewParent;

public class RecyclerViewAdapterUtils {
    private RecyclerViewAdapterUtils() {
    }

    /**
     * Gets parent RecyclerView instance.
     * @param view Child view of the RecyclerView's item
     * @return Parent RecyclerView instance
     */
    public static RecyclerView getParentRecyclerView(View view) {
        if (view == null) {
            return null;
        }
        ViewParent parent = view.getParent();
        if (parent instanceof RecyclerView) {
            return (RecyclerView) parent;
        } else if (parent instanceof View) {
            return getParentRecyclerView((View) parent);
        } else {
            return null;
        }
    }

    /**
     * Gets directly child of RecyclerView (== {@link android.support.v7.widget.RecyclerView.ViewHolder#itemView}})
     * @param view Child view of the RecyclerView's item
     * @return Item view
     */
    public static View getParentViewHolderItemView(View view) {
        if (view == null) {
            return null;
        }
        ViewParent parent = view.getParent();
        if (parent instanceof RecyclerView) {
            return view;
        } else if (parent instanceof View) {
            return getParentViewHolderItemView((View) parent);
        } else {
            return null;
        }
    }

    /**
     * Gets {@link android.support.v7.widget.RecyclerView.ViewHolder}.
     * @param view Child view of the RecyclerView's item
     * @return ViewHolder
     */
    public static RecyclerView.ViewHolder getViewHolder(View view) {
        RecyclerView rv = getParentRecyclerView(view);
        View rvChild = getParentViewHolderItemView(view);

        if (rv != null && rvChild != null) {
            return rv.getChildViewHolder(rvChild);
        } else {
            return null;
        }
    }
}
