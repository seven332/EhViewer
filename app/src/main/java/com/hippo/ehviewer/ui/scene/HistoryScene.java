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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.easyrecyclerview.HandlerDrawable;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.HistoryInfo;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.scene.TransitionHelper;
import com.hippo.util.ApiHelper;
import com.hippo.util.DrawableManager;
import com.hippo.view.ViewTransition;
import com.hippo.widget.LoadImageView;
import com.hippo.yorozuya.ResourcesUtils;
import com.hippo.yorozuya.ViewUtils;

import de.greenrobot.dao.query.LazyList;

public class HistoryScene extends ToolbarScene
        implements EasyRecyclerView.OnItemClickListener,
        EasyRecyclerView.OnItemLongClickListener{

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private ViewTransition mViewTransition;
    @Nullable
    private HistoryAdapter mAdapter;
    @Nullable
    private LazyList<HistoryInfo> mList;

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_history;
    }

    @Nullable
    @Override
    public View onCreateView2(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_history, container, false);
        View content = ViewUtils.$$(view, R.id.content);
        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(content, R.id.recycler_view);
        FastScroller fastScroller = (FastScroller) ViewUtils.$$(content, R.id.fast_scroller);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        mViewTransition = new ViewTransition(content, tip);

        Drawable drawable = DrawableManager.getDrawable(getContext(), R.drawable.big_history);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);

        Resources resources = getResources();

        mList = EhDB.getHistoryLazyList();
        mAdapter = new HistoryAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.hasFixedSize();
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setOnItemLongClickListener(this);
        int paddingH = resources.getDimensionPixelOffset(R.dimen.list_content_margin_h);
        int paddingV = resources.getDimensionPixelOffset(R.dimen.list_content_margin_v);
        mRecyclerView.setPadding(paddingV, paddingH, paddingV, paddingH);

        fastScroller.attachToRecyclerView(mRecyclerView);
        HandlerDrawable handlerDrawable = new HandlerDrawable();
        handlerDrawable.setColor(ResourcesUtils.getAttrColor(getContext(), R.attr.colorAccent));
        fastScroller.setHandlerDrawable(handlerDrawable);

        if (null != mList && mList.size() > 0) {
            mViewTransition.showView(0);
        } else {
            mViewTransition.showView(1);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.history);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mRecyclerView = null;
        mViewTransition = null;
        mAdapter = null;
        if (null != mList) {
            mList.close();
            mList = null;
        }
    }

    @Override
    public void onNavigationClick() {
        onBackPressed();
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        if (null == mList || null == mRecyclerView) {
            return false;
        }

        Bundle args = new Bundle();
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, mList.get(position));
        Announcer announcer = new Announcer(GalleryDetailScene.class).setArgs(args);
        if (ApiHelper.SUPPORT_TRANSITION) {
            HistoryHolder holder = (HistoryHolder) mRecyclerView.getChildViewHolder(view);
            announcer.setTranHelper(new EnterGalleryDetailTransaction(holder));
        }
        startScene(announcer);
        return true;
    }

    @Override
    public boolean onItemLongClick(EasyRecyclerView parent, View view, int position, long id) {
        if (null == mList) {
            return false;
        }

        final GalleryInfo gi = mList.get(position);
        new AlertDialog.Builder(getContext())
                .setTitle(EhUtils.getSuitableTitle(gi))
                .setItems(R.array.gallery_list_menu_entries, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Download
                                CommonOperations.startDownload(getActivity(), gi);
                                break;
                            case 1: // Favorites
                                CommonOperations.addToFavorites(getActivity(), gi,
                                        new addToFavoriteListener(getContext(),
                                                ((StageActivity) getActivity()).getStageId(), getTag()));
                                break;
                        }
                    }
                }).show();
        return true;
    }

    private class HistoryHolder extends RecyclerView.ViewHolder {

        private final LoadImageView thumb;
        private final TextView title;
        private final TextView uploader;
        private final SimpleRatingView rating;
        private final TextView category;
        private final TextView posted;
        private final TextView simpleLanguage;

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
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder> {

        @Override
        public HistoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new HistoryHolder(getActivity().getLayoutInflater()
                    .inflate(R.layout.item_gallery_list, parent, false));
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void onBindViewHolder(HistoryHolder holder, int position) {
            if (null == mList) {
                return;
            }

            GalleryInfo gi = mList.get(position);
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb, true);
            holder.title.setText(EhUtils.getSuitableTitle(gi));
            holder.uploader.setText(gi.uploader);
            holder.rating.setRating(gi.rating);
            TextView category = holder.category;
            String newCategoryText = EhUtils.getCategory(gi.category);
            if (!newCategoryText.equals(category.getText())) {
                category.setText(newCategoryText);
                category.setBackgroundColor(EhUtils.getCategoryColor(gi.category));
            }
            holder.posted.setText(gi.posted);
            holder.simpleLanguage.setText(gi.simpleLanguage);

            // Update transition name
            if (ApiHelper.SUPPORT_TRANSITION) {
                long gid = gi.gid;
                holder.thumb.setTransitionName(TransitionNameFactory.getThumbTransitionName(gid));
                holder.title.setTransitionName(TransitionNameFactory.getTitleTransitionName(gid));
                holder.uploader.setTransitionName(TransitionNameFactory.getUploaderTransitionName(gid));
                holder.category.setTransitionName(TransitionNameFactory.getCategoryTransitionName(gid));
            }
        }

        @Override
        public int getItemCount() {
            return mList != null ? mList.size() : 0;
        }
    }

    private static class EnterGalleryDetailTransaction implements TransitionHelper {

        private final HistoryHolder mHolder;

        public EnterGalleryDetailTransaction(HistoryHolder holder) {
            mHolder = holder;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onTransition(Context context, FragmentTransaction transaction,
                Fragment exit, Fragment enter) {
            if (mHolder == null || !(enter instanceof GalleryDetailScene)) {
                return false;
            }

            exit.setSharedElementReturnTransition(
                    TransitionInflater.from(context).inflateTransition(R.transition.trans_move));
            exit.setExitTransition(
                    TransitionInflater.from(context).inflateTransition(android.R.transition.fade));
            enter.setSharedElementEnterTransition(
                    TransitionInflater.from(context).inflateTransition(R.transition.trans_move));
            enter.setEnterTransition(
                    TransitionInflater.from(context).inflateTransition(android.R.transition.fade));
            transaction.addSharedElement(mHolder.thumb, mHolder.thumb.getTransitionName());
            transaction.addSharedElement(mHolder.title, mHolder.title.getTransitionName());
            transaction.addSharedElement(mHolder.uploader, mHolder.uploader.getTransitionName());
            transaction.addSharedElement(mHolder.category, mHolder.category.getTransitionName());
            return true;
        }
    }

    private static class addToFavoriteListener extends EhCallback<HistoryScene, Void> {

        public addToFavoriteListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(Void result) {
            showTip(R.string.add_to_favorite_success, LENGTH_SHORT);
        }

        @Override
        public void onFailure(Exception e) {
            showTip(R.string.add_to_favorite_failure, LENGTH_SHORT);
        }

        @Override
        public void onCancel() {}

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof HistoryScene;
        }
    }
}
