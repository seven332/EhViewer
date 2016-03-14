/*
 * Copyright 2016 Hippo Seven
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

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
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
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionMoveToSwipedDirection;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.dao.QuickSearch;
import com.hippo.util.DrawableManager;
import com.hippo.view.ViewTransition;
import com.hippo.yorozuya.ViewUtils;

import java.util.List;

public final class QuickSearchScene extends ToolbarScene {

    /*---------------
     Whole life cycle
     ---------------*/
    @Nullable
    private List<QuickSearch> mQuickSearchList;

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private ViewTransition mViewTransition;
    @Nullable
    private RecyclerView.Adapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mQuickSearchList = EhDB.getAllQuickSearch();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mQuickSearchList = null;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    @Override
    public View onCreateView2(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_label_list, container, false);

        EasyRecyclerView recyclerView = (EasyRecyclerView) ViewUtils.$$(view, R.id.recycler_view);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        mViewTransition = new ViewTransition(recyclerView, tip);

        Drawable drawable = DrawableManager.getDrawable(getContext(), R.drawable.big_search);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);
        tip.setText(R.string.no_quick_search);

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        RecyclerViewTouchActionGuardManager guardManager = new RecyclerViewTouchActionGuardManager();
        guardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        guardManager.setEnabled(true);
        // drag & drop manager
        RecyclerViewDragDropManager dragDropManager = new RecyclerViewDragDropManager();
        dragDropManager.setDraggingItemShadowDrawable(
                (NinePatchDrawable) getResources().getDrawable(R.drawable.shadow_8dp));
        // swipe manager
        RecyclerViewSwipeManager swipeManager = new RecyclerViewSwipeManager();
        RecyclerView.Adapter adapter = new QuickSearchAdapter();
        adapter.setHasStableIds(true);
        adapter = dragDropManager.createWrappedAdapter(adapter); // wrap for dragging
        adapter = swipeManager.createWrappedAdapter(adapter); // wrap for swiping
        mAdapter = adapter;
        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();
        animator.setSupportsChangeAnimations(false);
        recyclerView.hasFixedSize();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(animator);
        guardManager.attachRecyclerView(recyclerView);
        swipeManager.attachRecyclerView(recyclerView);
        dragDropManager.attachRecyclerView(recyclerView);

        updateView();

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.quick_search);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mViewTransition = null;
        mAdapter = null;
    }

    @Override
    public void onNavigationClick() {
        onBackPressed();
    }

    private void updateView() {
        if (mViewTransition != null) {
            if (mQuickSearchList != null && mQuickSearchList.size() > 0) {
                mViewTransition.showView(0);
            } else {
                mViewTransition.showView(1);
            }
        }
    }

    private class QuickSearchHolder extends AbstractDraggableSwipeableItemViewHolder {

        public final View swipeHandler;
        public final TextView label;
        public final View dragHandler;

        public QuickSearchHolder(View itemView) {
            super(itemView);

            swipeHandler = ViewUtils.$$(itemView, R.id.swipe_handler);
            label = (TextView) ViewUtils.$$(itemView, R.id.label);
            dragHandler = ViewUtils.$$(itemView, R.id.drag_handler);
        }

        @Override
        public View getSwipeableContainerView() {
            return swipeHandler;
        }
    }


    private class QuickSearchAdapter extends RecyclerView.Adapter<QuickSearchHolder>
            implements DraggableItemAdapter<QuickSearchHolder>,
            SwipeableItemAdapter<QuickSearchHolder> {

        @Override
        public QuickSearchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new QuickSearchHolder(getActivity().getLayoutInflater()
                    .inflate(R.layout.item_label_list, parent, false));
        }

        @Override
        public void onBindViewHolder(QuickSearchHolder holder, int position) {
            if (mQuickSearchList != null) {
                holder.label.setText(mQuickSearchList.get(position).name);
            }
            holder.setSwipeItemHorizontalSlideAmount(0);
        }

        @Override
        public long getItemId(int position) {
            return mQuickSearchList != null ? mQuickSearchList.get(position).getId() : 0;
        }

        @Override
        public int getItemCount() {
            return mQuickSearchList != null ? mQuickSearchList.size() : 0;
        }

        @Override
        public boolean onCheckCanStartDrag(QuickSearchHolder holder, int position, int x, int y) {
            return ViewUtils.isViewUnder(holder.dragHandler, x, y, 0);
        }

        @Override
        public ItemDraggableRange onGetItemDraggableRange(QuickSearchHolder holder, int position) {
            return null;
        }

        @Override
        public void onMoveItem(int fromPosition, int toPosition) {
            if (fromPosition == toPosition) {
                return;
            }
            if (null == mQuickSearchList) {
                return;
            }

            EhDB.moveQuickSearch(fromPosition, toPosition);
            final QuickSearch item = mQuickSearchList.remove(fromPosition);
            mQuickSearchList.add(toPosition, item);
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public int onGetSwipeReactionType(QuickSearchHolder holder, int position, int x, int y) {
            if (ViewUtils.isViewUnder(holder.getSwipeableContainerView(), x, y, 0)) {
                return SwipeableItemConstants.REACTION_CAN_SWIPE_LEFT;
            } else {
                return SwipeableItemConstants.REACTION_CAN_NOT_SWIPE_BOTH_H;
            }
        }

        @Override
        public void onSetSwipeBackground(QuickSearchHolder holder, int position, int type) {}

        @Override
        public SwipeResultAction onSwipeItem(QuickSearchHolder holder, int position, int result) {
            switch (result) {
                // swipe left --- pin
                case SwipeableItemConstants.RESULT_SWIPED_LEFT:
                    return new SwipeLeftResultAction(position);
                // other --- do nothing
                case SwipeableItemConstants.RESULT_SWIPED_RIGHT:
                case SwipeableItemConstants.RESULT_CANCELED:
                default:
                    return new SwipeResultActionDefault();
            }
        }
    }

    private class SwipeLeftResultAction extends SwipeResultActionMoveToSwipedDirection {

        private final int mPosition;

        public SwipeLeftResultAction(int position) {
            mPosition = position;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();

            final List<QuickSearch> quickSearchList = mQuickSearchList;
            if (quickSearchList == null || mPosition < 0 || mPosition >= quickSearchList.size()) {
                return;
            }

            final QuickSearch quickSearch = quickSearchList.get(mPosition);

            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.delete_quick_search_title)
                    .setMessage(getString(R.string.delete_quick_search_message, quickSearch.name))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EhDB.deleteQuickSearch(quickSearch);
                            quickSearchList.remove(mPosition);
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (null != mAdapter) {
                                mAdapter.notifyDataSetChanged();
                            }
                            updateView();
                        }
                    }).show();
        }
    }
}
