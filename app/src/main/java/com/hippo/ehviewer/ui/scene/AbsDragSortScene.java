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

package com.hippo.ehviewer.ui.scene;

import android.content.res.Resources;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.AppbarScene;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.widget.recyclerview.LinearDividerItemDecoration;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ViewUtils;

import java.util.List;

public abstract class AbsDragSortScene extends AppbarScene {

    private ViewGroup mTip;
    private TextView mTipTextView;
    private EasyRecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewDragDropManager mRecyclerViewDragDropManager;
    private RecyclerViewSwipeManager mRecyclerViewSwipeManager;
    private RecyclerViewTouchActionGuardManager mRecyclerViewTouchActionGuardManager;

    private ViewTransition mViewTransition;

    private int mOriginalPaddingBottom;

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);
        setContentView(R.layout.scene_abs_drag_sort_scene);
        setTitle(getTitle());
        setIcon(R.drawable.ic_arrow_left_dark_x24);

        Resources resources = getStageActivity().getResources();

        mTip = (ViewGroup) findViewById(R.id.tip);
        mTipTextView = (TextView) mTip.findViewById(R.id.text_view);
        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);
        mOriginalPaddingBottom = mRecyclerView.getPaddingBottom();

        // Layout Manager
        mLayoutManager = new LinearLayoutManager(getStageActivity());

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mRecyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mRecyclerViewTouchActionGuardManager.setEnabled(true);

        // drag & drop manager
        mRecyclerViewDragDropManager = new RecyclerViewDragDropManager();
        mRecyclerViewDragDropManager.setDraggingItemShadowDrawable(
                (NinePatchDrawable) resources.getDrawable(R.drawable.shadow_8dp));

        // swipe manager
        mRecyclerViewSwipeManager = new RecyclerViewSwipeManager();

        mAdapter = getAdapter();
        mWrappedAdapter = mRecyclerViewDragDropManager.createWrappedAdapter(mAdapter);      // wrap for dragging
        mWrappedAdapter = mRecyclerViewSwipeManager.createWrappedAdapter(mWrappedAdapter);      // wrap for swiping\

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mWrappedAdapter);  // requires *wrapped* adapter
        mRecyclerView.setItemAnimator(animator);
        mRecyclerView.setOnItemClickListener((EasyRecyclerView.OnItemClickListener) mAdapter);

        // additional decorations
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL,
                resources.getColor(R.color.divider_light),
                LayoutUtils.dp2pix(getStageActivity(), 1));
        decoration.setOverlap(true);
        mRecyclerView.addItemDecoration(decoration);

        // additional selector
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        mRecyclerView.setDrawSelectorOnTop(true);

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mRecyclerViewTouchActionGuardManager.attachRecyclerView(mRecyclerView);
        mRecyclerViewSwipeManager.attachRecyclerView(mRecyclerView);
        mRecyclerViewDragDropManager.attachRecyclerView(mRecyclerView);

        mViewTransition = new ViewTransition(mTip, mRecyclerView);
        if (mWrappedAdapter.getItemCount() == 0) {
            mViewTransition.showView(0, false);
        } else {
            mViewTransition.showView(1, false);
        }
    }

    public void setTipText(String text) {
        mTipTextView.setText(text);
    }

    public void notifyDataSetChanged() {
        if (mWrappedAdapter != null) {
            mWrappedAdapter.notifyDataSetChanged();
            if (mWrappedAdapter.getItemCount() == 0) {
                mViewTransition.showView(0, true);
            } else {
                mViewTransition.showView(1, true);
            }
        }
    }

    @Override
    public void onIconClick() {
        finish();
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        super.onGetFitPaddingBottom(b);
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), mRecyclerView.getPaddingTop(),
                mRecyclerView.getPaddingRight(), mOriginalPaddingBottom + b);
    }

    @Override
    protected void onDestroy(boolean die) {
        // TODO onClose
        if (mRecyclerViewDragDropManager != null) {
            mRecyclerViewDragDropManager.release();
            mRecyclerViewDragDropManager = null;
        }

        if (mRecyclerViewSwipeManager != null) {
            mRecyclerViewSwipeManager.release();
            mRecyclerViewSwipeManager = null;
        }

        if (mRecyclerViewTouchActionGuardManager != null) {
            mRecyclerViewTouchActionGuardManager.release();
            mRecyclerViewTouchActionGuardManager = null;
        }

        if (mRecyclerView != null) {
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.setAdapter(null);
            mRecyclerView = null;
        }

        if (mWrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(mWrappedAdapter);
            mWrappedAdapter = null;
        }
        mAdapter = null;
        mLayoutManager = null;

        super.onDestroy(die);
    }

    protected abstract String getTitle();

    protected abstract SortAdapter getAdapter();

    private class DragSortHolder extends AbstractDraggableSwipeableItemViewHolder {

        public ViewGroup contentContainer;
        public TextView textView;
        public View dragHandler;
        public ViewGroup background;
        public View leftDelete;
        public View rightDelete;

        public DragSortHolder(View itemView) {
            super(itemView);

            contentContainer = (ViewGroup) itemView.findViewById(R.id.content_container);
            textView = (TextView) contentContainer.findViewById(R.id.text_view);
            dragHandler = contentContainer.findViewById(R.id.drag_handler);
            background = (ViewGroup) itemView.findViewById(R.id.background);
            leftDelete = background.findViewById(R.id.delete_left);
            rightDelete = background.findViewById(R.id.delete_right);
        }

        @Override
        public View getSwipeableContainerView() {
            return contentContainer;
        }
    }

    public abstract class SortAdapter<E> extends RecyclerView.Adapter<DragSortHolder>
            implements DraggableItemAdapter<DragSortHolder>,
            SwipeableItemAdapter<DragSortHolder>,
            EasyRecyclerView.OnItemClickListener {

        private List<E> mList;

        public SortAdapter(List<E> list) {
            mList = list;
            setHasStableIds(true);
        }

        public abstract String getString(E e, int position);

        public abstract long getId(E e);

        public E getData(int position) {
            return mList.get(position);
        }

        @Override
        public DragSortHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DragSortHolder(getStageActivity().getLayoutInflater().inflate(R.layout.item_drag_sort, parent, false));
        }

        @Override
        public void onBindViewHolder(DragSortHolder holder, int position) {
            holder.textView.setText(getString(mList.get(position), position));
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        @Override
        public long getItemId(int position) {
            return getId(mList.get(position));
        }

        @Override
        public boolean onCheckCanStartDrag(DragSortHolder holder, int position, int x, int y) {
            return ViewUtils.isViewUnder(holder.dragHandler, x, y, 0);
        }

        @Override
        public ItemDraggableRange onGetItemDraggableRange(DragSortHolder holder, int position) {
            // no drag-sortable range specified
            return null;
        }

        @Override
        public void onMoveItem(int fromPosition, int toPosition) {
            if (fromPosition == toPosition) {
                return;
            }

            // onMove must be first, ensure call getData inside
            onMove(fromPosition, toPosition);
            mList.add(toPosition, mList.remove(fromPosition));
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public int onGetSwipeReactionType(DragSortHolder holder, int position, int x, int y) {
            return RecyclerViewSwipeManager.REACTION_CAN_SWIPE_BOTH;
        }

        @Override
        public void onSetSwipeBackground(DragSortHolder holder, int position, int type) {
            switch (type) {
                case RecyclerViewSwipeManager.DRAWABLE_SWIPE_LEFT_BACKGROUND:
                    holder.background.setVisibility(View.VISIBLE);
                    holder.leftDelete.setVisibility(View.INVISIBLE);
                    holder.rightDelete.setVisibility(View.VISIBLE);
                    break;
                case RecyclerViewSwipeManager.DRAWABLE_SWIPE_RIGHT_BACKGROUND:
                    holder.background.setVisibility(View.VISIBLE);
                    holder.leftDelete.setVisibility(View.VISIBLE);
                    holder.rightDelete.setVisibility(View.INVISIBLE);
                    break;
                case RecyclerViewSwipeManager.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND:
                    holder.background.setVisibility(View.INVISIBLE);
                default:
                    break;
            }
        }

        @Override
        public int onSwipeItem(DragSortHolder holder, int position, int result) {
            switch (result) {
                // remove
                case RecyclerViewSwipeManager.RESULT_SWIPED_RIGHT:
                case RecyclerViewSwipeManager.RESULT_SWIPED_LEFT:
                    return RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM;
                // other --- do nothing
                case RecyclerViewSwipeManager.RESULT_CANCELED:
                default:
                    return RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_DEFAULT;
            }
        }

        @Override
        public void onPerformAfterSwipeReaction(DragSortHolder holder, int position, int result, int reaction) {
            if (reaction == RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM) {
                // onRemove must be first, ensure call getData inside
                onRemove(position);
                mList.remove(position);
                notifyItemRemoved(position);
                // Try to show tip
                if (mWrappedAdapter.getItemCount() == 0) {
                    mViewTransition.showView(0, true);
                } else {
                    mViewTransition.showView(1, true);
                }
            }
        }

        protected abstract void onMove(int fromPosition, int toPosition);

        protected abstract void onRemove(int position);
    }
}
