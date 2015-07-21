/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.gallery.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.Say;

public class GalleryView extends RecyclerView {

    private static final String TAG = GalleryView.class.getSimpleName();

    private static int INTERVAL_DP = 32;

    private Mode mMode;
    private int mHalfInterval;
    private boolean mNeedAdjustPosition;

    private GalleryLayoutManager mLayoutManager;

    public enum Mode {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        TOP_TO_BOTTOM
    }

    public GalleryView(Context context) {
        super(context);
        init(context);
    }

    public GalleryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GalleryView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mHalfInterval = LayoutUtils.dp2pix(context, INTERVAL_DP) / 2;

        mLayoutManager = new GalleryLayoutManager(context);
        setLayoutManager(mLayoutManager);
        addItemDecoration(new IntervalDecoration());
        addOnScrollListener(new ScrollListener());
        setHasFixedSize(true);
        setMode(Mode.RIGHT_TO_LEFT);
    }

    public void setMode(Mode mode) {
        mMode = mode;
        if (mMode == Mode.LEFT_TO_RIGHT) {
            mLayoutManager.setOrientation(HORIZONTAL);
            mLayoutManager.setReverseLayout(false);
        } else if (mMode == Mode.RIGHT_TO_LEFT) {
            mLayoutManager.setOrientation(HORIZONTAL);
            mLayoutManager.setReverseLayout(true);
        } else if (mMode == Mode.TOP_TO_BOTTOM) {
            mLayoutManager.setOrientation(VERTICAL);
            mLayoutManager.setReverseLayout(false);
        }
        // Not all DecorInset will be updated at once, so do it by myself
        int length = getChildCount();
        for (int i = 0; i < length; i++) {
            ((RecyclerView.LayoutParams) getChildAt(i).getLayoutParams()).mInsetsDirty = true;
        }
        if (mode == Mode.LEFT_TO_RIGHT || mode == Mode.RIGHT_TO_LEFT) {
            mNeedAdjustPosition = true;
        }
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        if (mMode == Mode.TOP_TO_BOTTOM) {
            return super.fling(velocityX, velocityY);
        } else {
            return false;
        }
    }

    public void scrollByEfficiently(int x, int y) {
        if (x != 0 || y != 0) {
            scrollBy(x, y);
        }
    }

    private int size() {
        return getAdapter().getItemCount();
    }

    // Only work for Mode.LEFT_TO_RIGHT and Mode.RIGHT_TO_LEFT
    private View findCenterXView() {
        int centerX = getWidth() / 2;
        int length = getChildCount();
        for (int i = 0; i < length; i++) {
            View v = getChildAt(i);
            if (v.getLeft() < centerX && v.getRight() >= centerX) {
                return v;
            }
        }
        return null;
    }

    // Only work for Mode.LEFT_TO_RIGHT and Mode.RIGHT_TO_LEFT
    private int getOffsetToPage(int page) throws Exception {
        View v = findCenterXView();
        if (v == null) {
            throw new Exception("Can't find center view");
        }
        int currentPage = getChildAdapterPosition(v);
        if (currentPage == RecyclerView.NO_POSITION) {
            throw new Exception("Can't find current page");
        }

        int d = page - currentPage;
        if (mMode == Mode.RIGHT_TO_LEFT) {
            d = -d;
        }
        return d * (mHalfInterval * 2 + getWidth()) + v.getLeft();
    }

    public void smoothScrollToPage(int page) {
        if (mMode == Mode.TOP_TO_BOTTOM) {
            smoothScrollToPosition(page);
        } else {
            page = MathUtils.clamp(page, 0, size() - 1);
            try {
                int offsetX = getOffsetToPage(page);
                smoothScrollBy(offsetX, 0);
            } catch (Exception e) {
                Say.d(TAG, e.getMessage());
                smoothScrollToPosition(page);
            }
        }
    }

    public void scrollToPage(int page) {
        if (mMode == Mode.TOP_TO_BOTTOM) {
            scrollToPosition(page);
        } else {
            page = MathUtils.clamp(page, 0, size() - 1);
            try {
                int offsetX = getOffsetToPage(page);
                scrollByEfficiently(offsetX, 0);
            } catch (Exception e) {
                Say.d(TAG, e.getMessage());
                scrollToPosition(page);
            }
        }
    }

    public int getCurrentPage() {
        if (mMode == Mode.TOP_TO_BOTTOM) {
            return mLayoutManager.findFirstVisibleItemPosition();
        } else {
            View v = findCenterXView();
            if (v == null) {
                return RecyclerView.NO_POSITION;
            } else {
                return getChildLayoutPosition(v);
            }
        }
    }

    class GalleryLayoutManager extends LinearLayoutManager {

        public GalleryLayoutManager(Context context) {
            super(context);
        }

        @Override
        public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
            // Call it to update decor
            getItemDecorInsetsForChild(child);

            int widthSpec;
            int heightSpec;

            // LayoutParams is useless
            if (mMode == Mode.TOP_TO_BOTTOM) {
                widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY);
                heightSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
            } else {
                widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY);
                heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY);
            }
            child.measure(widthSpec, heightSpec);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            super.onLayoutChildren(recycler, state);

            // Ensure page is in center
            if (mNeedAdjustPosition) {
                mNeedAdjustPosition = false;
                int currentPage = getCurrentPage();
                if (currentPage != RecyclerView.NO_POSITION) {
                    scrollToPage(currentPage);
                }
            }
        }
    }

    class ScrollListener extends OnScrollListener {

        private int mCurrentPage = RecyclerView.NO_POSITION;

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (mMode == Mode.TOP_TO_BOTTOM) {
                // Nothing to do
                return;
            }

            if (newState == SCROLL_STATE_DRAGGING) {
                mCurrentPage = getCurrentPage();
            } else if (newState == SCROLL_STATE_IDLE) {
                if (mCurrentPage != RecyclerView.NO_POSITION) {
                    ViewHolder viewHolder = findViewHolderForLayoutPosition(mCurrentPage);
                    if (viewHolder == null) {
                        // WTF?
                        Say.w(TAG, "Can't find ViewHolder when adjustPosition");
                        smoothScrollToPage(mCurrentPage);
                    } else {
                        View v = viewHolder.itemView;
                        int scrollX;
                        int interval = mHalfInterval * 2;
                        int left = v.getLeft();
                        int width = getWidth();
                        if (left > interval &&
                                ((mMode == Mode.LEFT_TO_RIGHT && mCurrentPage > 0) ||
                                        (mMode == Mode.RIGHT_TO_LEFT && mCurrentPage < size() - 1))) { // Scroll left
                            scrollX = -width - interval + left;
                        } else if (left < -interval &&
                                ((mMode == Mode.LEFT_TO_RIGHT && mCurrentPage < size() - 1) ||
                                        (mMode == Mode.RIGHT_TO_LEFT && mCurrentPage > 0))) { // Scroll right
                            scrollX = width + interval + left;
                        } else {
                            scrollX = left;
                        }
                        smoothScrollBy(scrollX, 0);
                    }
                    mCurrentPage = RecyclerView.NO_POSITION;
                }
            }
        }
    }

    class IntervalDecoration extends RecyclerView.ItemDecoration {

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            int index = getChildLayoutPosition(view);
            if (mMode  == Mode.LEFT_TO_RIGHT) {
                if (index == 0) {
                    outRect.left = 0;
                } else {
                    outRect.left = mHalfInterval;
                }
                if (index == size() - 1) {
                    outRect.right = 0;
                } else {
                    outRect.right = mHalfInterval;
                }
                outRect.top = 0;
                outRect.bottom = 0;
            } else if (mMode == Mode.RIGHT_TO_LEFT) {
                if (index == 0) {
                    outRect.right = 0;
                } else {
                    outRect.right = mHalfInterval;
                }
                if (index == size() - 1) {
                    outRect.left = 0;
                } else {
                    outRect.left = mHalfInterval;
                }
                outRect.top = 0;
                outRect.bottom = 0;
            } else if (mMode == Mode.TOP_TO_BOTTOM) {
                if (index == 0) {
                    outRect.top = 0;
                } else {
                    outRect.top = mHalfInterval;
                }
                if (index == size() - 1) {
                    outRect.bottom = 0;
                } else {
                    outRect.bottom = mHalfInterval;
                }
                outRect.left = 0;
                outRect.right = 0;
            }
        }
    }
}
