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

package com.h6ah4i.android.widget.advrecyclerview.draggable;

import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.support.v7.widget.RecyclerView;

public class EdgeEffectDecorator extends RecyclerView.ItemDecoration {
    private RecyclerView mRecyclerView;
    private EdgeEffectCompat mTopGlow;
    private EdgeEffectCompat mBottomGlow;
    private boolean mStarted;

    public EdgeEffectDecorator(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        boolean needsInvalidate = false;

        if (mTopGlow != null && !mTopGlow.isFinished()) {
            final int restore = c.save();
            if (getClipToPadding(parent)) {
                c.translate(parent.getPaddingLeft(), parent.getPaddingTop());
            }
            //noinspection ConstantConditions
            needsInvalidate |= mTopGlow.draw(c);
            c.restoreToCount(restore);
        }

        if (mBottomGlow != null && !mBottomGlow.isFinished()) {
            final int restore = c.save();
            c.rotate(180);
            if (getClipToPadding(parent)) {
                c.translate(-parent.getWidth() + parent.getPaddingRight(), -parent.getHeight() + parent.getPaddingBottom());
            } else {
                c.translate(-parent.getWidth(), -parent.getHeight());
            }
            needsInvalidate |= mBottomGlow.draw(c);
            c.restoreToCount(restore);
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(parent);
        }
    }

    public void start() {
        if (mStarted) {
            return;
        }
        mRecyclerView.addItemDecoration(this);
        mStarted = true;
    }

    public void finish() {
        if (mStarted) {
            mRecyclerView.removeItemDecoration(this);
        }
        releaseBothGlows();
        mRecyclerView = null;
        mStarted = false;
    }

    public void pullTopGlow(float deltaDistance) {
        ensureTopGlow(mRecyclerView);

        if (mTopGlow.onPull(deltaDistance, 0.5f)) {
            ViewCompat.postInvalidateOnAnimation(mRecyclerView);
        }
    }

    public void pullBottom(float deltaDistance) {
        ensureBottomGlow(mRecyclerView);

        if (mBottomGlow.onPull(deltaDistance, 0.5f)) {
            ViewCompat.postInvalidateOnAnimation(mRecyclerView);
        }
    }

    public void releaseBothGlows() {
        boolean needsInvalidate = false;

        if (mTopGlow != null) {
            //noinspection ConstantConditions
            needsInvalidate |= mTopGlow.onRelease();
        }

        if (mBottomGlow != null) {
            needsInvalidate |= mBottomGlow.onRelease();
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(mRecyclerView);
        }
    }

    private void ensureTopGlow(RecyclerView rv) {
        if (mTopGlow == null) {
            mTopGlow = new EdgeEffectCompat(rv.getContext());
        }

        updateGlowSize(rv, mTopGlow);
    }

    private void ensureBottomGlow(RecyclerView rv) {
        if (mBottomGlow == null) {
            mBottomGlow = new EdgeEffectCompat(rv.getContext());
        }
        updateGlowSize(rv, mBottomGlow);
    }

    private static void updateGlowSize(RecyclerView rv, EdgeEffectCompat topGlow) {
        int width = rv.getMeasuredWidth();
        int height = rv.getMeasuredHeight();

        if (getClipToPadding(rv)) {
            width -= rv.getPaddingLeft() - rv.getPaddingRight();
            height -= rv.getPaddingTop() - rv.getPaddingBottom();
        }

        width = Math.max(0, width);
        height = Math.max(0, height);

        topGlow.setSize(width, height);
    }

    private static boolean getClipToPadding(RecyclerView rv) {
        return rv.getLayoutManager().getClipToPadding();
    }

    public void reorderToTop() {
        if (mStarted) {
            mRecyclerView.removeItemDecoration(this);
            mRecyclerView.addItemDecoration(this);
        }
    }
}
