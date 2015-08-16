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

import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class CustomRecyclerViewUtils {
    public static RecyclerView.ViewHolder findChildViewHolderUnderWithoutTranslation(RecyclerView rv, float x, float y) {
        final View child = findChildViewUnderWithoutTranslation(rv, x, y);
        return (child != null) ? rv.getChildViewHolder(child) : null;
    }

    private static View findChildViewUnderWithoutTranslation(ViewGroup parent, float x, float y) {
        final int count = parent.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (x >= child.getLeft() &&
                    x <= child.getRight() &&
                    y >= child.getTop() &&
                    y <= child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    public static RecyclerView.ViewHolder findChildViewHolderUnderWithTranslation(RecyclerView rv, float x, float y) {
        final View child = rv.findChildViewUnder(x, y);
        return (child != null) ? rv.getChildViewHolder(child) : null;
    }

    public static Rect getLayoutMargins(View v, Rect outMargins) {
        final ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            outMargins.left = marginLayoutParams.leftMargin;
            outMargins.right = marginLayoutParams.rightMargin;
            outMargins.top = marginLayoutParams.topMargin;
            outMargins.bottom = marginLayoutParams.bottomMargin;
        } else {
            outMargins.left = outMargins.right = outMargins.top = outMargins.bottom = 0;
        }
        return outMargins;
    }

    public static Rect getDecorationOffsets(RecyclerView.LayoutManager layoutManager, View view, Rect outDecorations) {
        outDecorations.left = layoutManager.getLeftDecorationWidth(view);
        outDecorations.right = layoutManager.getRightDecorationWidth(view);
        outDecorations.top = layoutManager.getTopDecorationHeight(view);
        outDecorations.bottom = layoutManager.getBottomDecorationHeight(view);

        return outDecorations;
    }

    public static Rect getViewBounds(View v, Rect outBounds) {
        outBounds.left = v.getLeft();
        outBounds.right = v.getRight();
        outBounds.top = v.getTop();
        outBounds.bottom = v.getBottom();
        return outBounds;
    }


    public static int findFirstVisibleItemPosition(RecyclerView rv) {
        RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            return (((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition());
        } else {
            return RecyclerView.NO_POSITION;
        }
    }

    public static int findLastVisibleItemPosition(RecyclerView rv) {
        RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            return (((LinearLayoutManager) layoutManager).findLastVisibleItemPosition());
        } else {
            return RecyclerView.NO_POSITION;
        }
    }

    public static int findFirstCompletelyVisibleItemPosition(RecyclerView rv) {
        RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            return (((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition());
        } else {
            return RecyclerView.NO_POSITION;
        }
    }

    public static int findLastCompletelyVisibleItemPosition(RecyclerView rv) {
        RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            return (((LinearLayoutManager) layoutManager).findLastCompletelyVisibleItemPosition());
        } else {
            return RecyclerView.NO_POSITION;
        }
    }

    public static int getSynchronizedPosition(RecyclerView.ViewHolder holder) {
        int pos1 = holder.getLayoutPosition();
        int pos2 = holder.getAdapterPosition();
        if (pos1 == pos2) {
            return pos1;
        } else {
            return RecyclerView.NO_POSITION;
        }
    }

}
