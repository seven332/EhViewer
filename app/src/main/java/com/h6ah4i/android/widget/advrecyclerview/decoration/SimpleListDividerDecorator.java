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

package com.h6ah4i.android.widget.advrecyclerview.decoration;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Item decoration which draws item divider between each items.
 */
public class SimpleListDividerDecorator extends RecyclerView.ItemDecoration {
    private final Drawable mDividerDrawable;
    private final int mDividerHeight;
    private boolean mOverlap;

    /**
     * Constructor.
     *
     * @param divider divider drawable
     * @param overlap whether the divider is drawn overlapped on bottom of the item.
     */
    public SimpleListDividerDecorator(Drawable divider, boolean overlap) {
        mDividerDrawable = divider;
        mDividerHeight = mDividerDrawable.getIntrinsicHeight();
        mOverlap = overlap;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int childCount = parent.getChildCount();
        final float yPositionThreshold = (mOverlap) ? 1.0f : (mDividerHeight + 1.0f); // [px]
        final float zPositionThreshold = 1.0f; // [px]

        if (childCount == 0) {
            return;
        }

        int savedCount = c.save(Canvas.CLIP_SAVE_FLAG);

        c.clipRect(
                parent.getLeft() + parent.getPaddingLeft(),
                parent.getTop() + parent.getPaddingTop(),
                parent.getRight() - parent.getPaddingRight(),
                parent.getBottom() + parent.getPaddingBottom());

        for (int i = 0; i < childCount - 1; i++) {
            final View child = parent.getChildAt(i);
            final View nextChild = parent.getChildAt(i + 1);

            if ((child.getVisibility() != View.VISIBLE) ||
                    (nextChild.getVisibility() != View.VISIBLE)) {
                continue;
            }

            // check if the next item is placed at the bottom
            final float childBottom = child.getBottom() + ViewCompat.getTranslationY(child);
            final float nextChildTop = nextChild.getTop() + ViewCompat.getTranslationY(nextChild);

            if (!(Math.abs(nextChildTop - childBottom) < yPositionThreshold)) {
                continue;
            }

            // check if the next item is placed on the same plane
            final float childZ = ViewCompat.getTranslationZ(child) + ViewCompat.getElevation(child);
            final float nextChildZ = ViewCompat.getTranslationZ(nextChild) + ViewCompat.getElevation(nextChild);

            if (!(Math.abs(nextChildZ - childZ) < zPositionThreshold)) {
                continue;
            }

            final float childAlpha = ViewCompat.getAlpha(child);
            final float nextChildAlpha = ViewCompat.getAlpha(nextChild);

            final int tx = (int) (ViewCompat.getTranslationX(child) + 0.5f);
            final int ty = (int) (ViewCompat.getTranslationY(child) + 0.5f);
            final int left = child.getLeft();
            final int right = child.getRight();
            final int top = child.getBottom();
            final int bottom = top + mDividerHeight;

            mDividerDrawable.setAlpha((int) ((0.5f * 255) * (childAlpha + nextChildAlpha) + 0.5f));
            mDividerDrawable.setBounds(left + tx, top + ty, right + tx, bottom + ty);
            mDividerDrawable.draw(c);
        }

        c.restoreToCount(savedCount);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (mOverlap) {
            outRect.set(0, 0, 0, 0);
        } else {
            outRect.set(0, 0, 0, mDividerHeight);
        }
    }
}
