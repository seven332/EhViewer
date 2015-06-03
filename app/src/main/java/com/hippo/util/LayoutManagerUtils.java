/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.util;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.hippo.widget.recyclerview.SimpleSmoothScroller;

public final class LayoutManagerUtils {

    public static void scrollToPositionWithOffset(
            RecyclerView.LayoutManager layoutManager,int position, int offset) {
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
        } else {
            throw new IllegalStateException("Can't do scrollToPositionWithOffset for " +
                    layoutManager.getClass().getName());
        }
    }

    public static void smoothScrollToPosition(
            RecyclerView.LayoutManager layoutManager, Context context, int position) {
        smoothScrollToPosition(layoutManager, context, position, -1);
    }

    public static void smoothScrollToPosition(
            RecyclerView.LayoutManager layoutManager, Context context, int position,
            int millisecondsPerInch) {
        SimpleSmoothScroller smoothScroller;
        if (layoutManager instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            smoothScroller = new SimpleSmoothScroller(context, millisecondsPerInch) {
                @Override
                public PointF computeScrollVectorForPosition(int targetPosition) {
                    return linearLayoutManager.computeScrollVectorForPosition(targetPosition);
                }
            };
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            final StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
            smoothScroller = new SimpleSmoothScroller(context, millisecondsPerInch) {
                @Override
                public PointF computeScrollVectorForPosition(int targetPosition) {
                    final int direction = staggeredGridLayoutManager.calculateScrollDirectionForPosition(targetPosition);
                    if (direction == 0) {
                        return null;
                    }
                    if (staggeredGridLayoutManager.getOrientation() == StaggeredGridLayoutManager.HORIZONTAL) {
                        return new PointF(direction, 0);
                    } else {
                        return new PointF(0, direction);
                    }
                }
            };
        } else {
            throw new IllegalStateException("Can't do smoothScrollToPosition for " +
                    layoutManager.getClass().getName());
        }
        smoothScroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(smoothScroller);
    }

    public static void scrollToPositionProperly(final RecyclerView.LayoutManager layoutManager,
            final Context context, final int position, final OnScrollToPositionListener listener) {
        AppHandler.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {
                int first = getFirstVisibleItemPostion(layoutManager);
                int last = getLastVisibleItemPostion(layoutManager);
                int offset = Math.abs(position - first);
                int max = last - first;
                if (offset < max && max > 0) {
                    smoothScrollToPosition(layoutManager, context, position,
                            MathUtils.lerp(100, 25, (offset / max)));
                } else {
                    scrollToPositionWithOffset(layoutManager, position, 0);
                    if (listener != null) {
                        listener.onScrollToPosition();
                    }
                }
            }
        }, 200);
    }

    public static int getFirstVisibleItemPostion(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] positions = ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(null);
            return MathUtils.min(positions);
        } else {
            throw new IllegalStateException("Can't do getFirstVisibleItemPostion for " +
                    layoutManager.getClass().getName());
        }
    }

    public static int getLastVisibleItemPostion(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] positions = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
            return MathUtils.max(positions);
        } else {
            throw new IllegalStateException("Can't do getFirstVisibleItemPostion for " +
                    layoutManager.getClass().getName());
        }
    }

    public interface OnScrollToPositionListener {
        void onScrollToPosition();
    }
}
