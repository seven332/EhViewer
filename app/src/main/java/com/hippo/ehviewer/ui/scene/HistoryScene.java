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

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.hippo.conaco.Conaco;
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.scene.Announcer;
import com.hippo.scene.AppbarScene;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.yorozuya.Messenger;

import java.util.List;

// TODO Avoid get history in UI thread
public class HistoryScene extends AppbarScene implements EasyRecyclerView.OnItemClickListener,
        Messenger.Receiver {

    private EasyRecyclerView mRecyclerView;
    private ViewTransition mViewTransition;
    private int mOriginalPaddingBottom;

    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewSwipeManager mRecyclerViewSwipeManager;
    private RecyclerViewTouchActionGuardManager mRecyclerViewTouchActionGuardManager;

    private List<GalleryBase> mGalleryBases;

    @Override
    protected void onInit() {
        super.onInit();

        Messenger.getInstance().register(Constants.MESSENGER_ID_UPDATE_HISTORY, this);
    }

    @Override
    protected void onDie() {
        super.onDie();

        Messenger.getInstance().unregister(Constants.MESSENGER_ID_UPDATE_HISTORY, this);
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setContentView(R.layout.scene_history);
        setTitle(R.string.history);
        setIcon(R.drawable.ic_arrow_left_dark_x24);

        View tip = findViewById(R.id.tip);
        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);
        mViewTransition = new ViewTransition(tip, mRecyclerView);
        mOriginalPaddingBottom = mRecyclerView.getPaddingBottom();

        mGalleryBases = DBUtils.getAllHistory();

        // Layout Manager
        mLayoutManager = new LinearLayoutManager(getStageActivity());

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mRecyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mRecyclerViewTouchActionGuardManager.setEnabled(true);

        // swipe manager
        mRecyclerViewSwipeManager = new RecyclerViewSwipeManager();

        mAdapter = new HistoryAdapter();
        mAdapter.setHasStableIds(true);
        mWrappedAdapter = mRecyclerViewSwipeManager.createWrappedAdapter(mAdapter);      // wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

        mRecyclerView.hasFixedSize();
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mWrappedAdapter);  // requires *wrapped* adapter
        mRecyclerView.setItemAnimator(animator);
        mRecyclerView.setOnItemClickListener(this);

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mRecyclerViewTouchActionGuardManager.attachRecyclerView(mRecyclerView);
        mRecyclerViewSwipeManager.attachRecyclerView(mRecyclerView);

        if (mWrappedAdapter.getItemCount() == 0) {
            mViewTransition.showView(0, false);
        } else {
            mViewTransition.showView(1, false);
        }
    }

    @Override
    protected void onDetachedFromeWindow() {
        super.onDetachedFromeWindow();

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
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        GalleryBase gb = mGalleryBases.get(position);
        Announcer announcer = new Announcer();
        announcer.setAction(GalleryDetailScene.ACTION_GALLERY_BASE);
        announcer.putExtra(GalleryDetailScene.KEY_GALLERY_BASE, gb);
        startScene(GalleryDetailScene.class, announcer);
        return true;
    }

    @Override
    public void onReceive(int id, Object obj) {
        mGalleryBases = DBUtils.getAllHistory();
        mWrappedAdapter.notifyDataSetChanged();
    }

    private class HistoryHolder extends AbstractSwipeableItemViewHolder {

        public LoadImageView thumb;
        public TextView title;
        public TextView uploader;
        public SimpleRatingView rating;
        public TextView category;
        public TextView posted;
        public TextView simpleLanguage;

        public HistoryHolder(View itemView) {
            super(itemView);
            thumb = (LoadImageView) itemView.findViewById(R.id.thumb);
            title = (TextView) itemView.findViewById(R.id.title);
            uploader = (TextView) itemView.findViewById(R.id.uploader);
            rating = (SimpleRatingView) itemView.findViewById(R.id.rating);
            category = (TextView) itemView.findViewById(R.id.category);
            posted = (TextView) itemView.findViewById(R.id.posted);
            simpleLanguage = (TextView) itemView.findViewById(R.id.simple_language);
        }

        @Override
        public View getSwipeableContainerView() {
            return itemView;
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder>
            implements SwipeableItemAdapter<HistoryHolder> {

        @Override
        public HistoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getStageActivity().getLayoutInflater().inflate(R.layout.item_gallery_list_detail, parent, false);
            return new HistoryHolder(view);
        }

        @Override
        public void onBindViewHolder(HistoryHolder holder, int position) {
            GalleryBase gb = mGalleryBases.get(position);

            Conaco conaco = EhApplication.getConaco(getStageActivity());
            holder.thumb.load(conaco, EhCacheKeyFactory.getThumbKey(gb.gid), gb.thumb);
            holder.title.setText(EhUtils.getSuitableTitle(gb));
            holder.uploader.setText(gb.uploader);
            holder.rating.setRating(gb.rating);
            TextView category = holder.category;
            String newCategoryText = EhUtils.getCategory(gb.category);
            if (!newCategoryText.equals(category.getText())) {
                category.setText(newCategoryText);
                category.setBackgroundColor(EhUtils.getCategoryColor(gb.category));
            }
            holder.posted.setText(gb.posted);
            holder.simpleLanguage.setText(gb.simpleLanguage);
        }

        @Override
        public int getItemCount() {
            return mGalleryBases.size();
        }

        @Override
        public long getItemId(int position) {
            return mGalleryBases.get(position).gid;
        }

        @Override
        public int onGetSwipeReactionType(HistoryHolder holder, int position, int x, int y) {
            return RecyclerViewSwipeManager.REACTION_CAN_SWIPE_BOTH;
        }

        @Override
        public void onSetSwipeBackground(HistoryHolder holder, int position, int type) {
            // Empty
        }

        @Override
        public int onSwipeItem(HistoryHolder holder, int position, int result) {
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
        public void onPerformAfterSwipeReaction(HistoryHolder holder, int position, int result, int reaction) {
            if (reaction == RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM) {
                GalleryBase gb = mGalleryBases.remove(position);
                if (gb != null) {
                    DBUtils.removeHistory(gb);
                }
                notifyItemRemoved(position);
                // Try to show tip
                if (mWrappedAdapter.getItemCount() == 0) {
                    mViewTransition.showView(0, true);
                } else {
                    mViewTransition.showView(1, true);
                }
            }
        }
    }
}
