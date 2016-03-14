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

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.easyrecyclerview.HandlerDrawable;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.HistoryInfo;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.util.ApiHelper;
import com.hippo.util.DrawableManager;
import com.hippo.view.ViewTransition;
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
        EasyRecyclerView recyclerView = (EasyRecyclerView) ViewUtils.$$(content, R.id.recycler_view);
        FastScroller fastScroller = (FastScroller) ViewUtils.$$(content, R.id.fast_scroller);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        mViewTransition = new ViewTransition(content, tip);

        Drawable drawable = DrawableManager.getDrawable(getContext(), R.drawable.big_history);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);

        mList = EhDB.getHistoryLazyList();
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        recyclerView.setDrawSelectorOnTop(true);
        recyclerView.hasFixedSize();
        recyclerView.setClipToPadding(false);
        recyclerView.setOnItemClickListener(this);
        recyclerView.setOnItemLongClickListener(this);
        mAdapter = new HistoryAdapter(LayoutInflater.from(getContext()),
                getContext(), recyclerView, layoutManager, Settings.getListMode());
        recyclerView.setAdapter(mAdapter);
        mAdapter.register();

        fastScroller.attachToRecyclerView(recyclerView);
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

        if (null != mList) {
            mList.close();
            mList = null;
        }
        if (null != mAdapter) {
            mAdapter.unregister();
            mAdapter = null;
        }

        mViewTransition = null;
        mAdapter = null;
    }

    @Override
    public void onNavigationClick() {
        onBackPressed();
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        if (null == mList) {
            return false;
        }

        Bundle args = new Bundle();
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, mList.get(position));
        Announcer announcer = new Announcer(GalleryDetailScene.class).setArgs(args);
        View thumb;
        if (ApiHelper.SUPPORT_TRANSITION && null != (thumb = view.findViewById(R.id.thumb))) {
            announcer.setTranHelper(new EnterGalleryDetailTransaction(thumb));
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
                                CommonOperations.startDownload(getActivity(), gi, false);
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

    private class HistoryAdapter extends GalleryAdapter {

        public HistoryAdapter(LayoutInflater inflater, Context context,
                RecyclerView recyclerView, GridLayoutManager layoutManager, int type) {
            super(inflater, context, recyclerView, layoutManager, type);
        }

        @Nullable
        @Override
        public GalleryInfo getDataAt(int position) {
            return null != mList ? mList.get(position) : null;
        }

        @Override
        public int getItemCount() {
            return null != mList ? mList.size() : 0;
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
