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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.download.DownloadInfo;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.download.DownloadService;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.TransitionHelper;
import com.hippo.util.ActivityHelper;
import com.hippo.util.ApiHelper;
import com.hippo.vector.VectorDrawable;
import com.hippo.view.ViewTransition;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.SimpleImageView;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.ObjectUtils;

import java.util.List;

public class DownloadScene extends ToolbarScene
        implements DownloadManager.DownloadInfoListener,
        EasyRecyclerView.OnItemClickListener {

    private static final String KEY_LABEL = "label";

    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private ViewTransition mViewTransition;

    @Nullable
    private DownloadAdapter mAdapter;

    @Nullable
    private String mLabel;
    @Nullable
    private List<DownloadInfo> mList;

    // TODO Only single instance
    @Override
    public int getLaunchMode() {
        return LAUNCH_MODE_SINGLE_TOP;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DownloadManager manager = EhApplication.getDownloadManager(getContext());
        manager.setDownloadInfoListener(this);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void onInit() {
        mLabel = Settings.getRecentDownloadLabel();
        DownloadManager manager = EhApplication.getDownloadManager(getContext());
        if (mLabel == null) {
            mList = manager.getDefaultDownloadInfoList();
        } else {
            mList = manager.getLabelDownloadInfoList(mLabel);
            if (mList == null) {
                mLabel = null;
                mList = manager.getDefaultDownloadInfoList();
            }
        }
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mLabel = savedInstanceState.getString(KEY_LABEL);
        DownloadManager manager = EhApplication.getDownloadManager(getContext());
        if (mLabel == null) {
            mList = manager.getDefaultDownloadInfoList();
        } else {
            mList = manager.getLabelDownloadInfoList(mLabel);
            if (mList == null) {
                mLabel = null;
                mList = manager.getDefaultDownloadInfoList();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_LABEL, mLabel);
    }

    @Nullable
    @Override
    public View onCreateView2(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_download, container, false);
        mRecyclerView = (EasyRecyclerView) view.findViewById(R.id.recycler_view);
        View tip = view.findViewById(R.id.tip);
        SimpleImageView tipImage = (SimpleImageView) tip.findViewById(R.id.tip_image);
        tipImage.setDrawable(VectorDrawable.create(getContext(), R.xml.sadpanda_head));
        mViewTransition = new ViewTransition(mRecyclerView, tip);

        if (mRecyclerView != null) {
            mAdapter = new DownloadAdapter();
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
            mRecyclerView.setDrawSelectorOnTop(true);
            mRecyclerView.hasFixedSize();
            mRecyclerView.setClipToPadding(false);
            mRecyclerView.setOnItemClickListener(this);
            int paddingH = getResources().getDimensionPixelOffset(R.dimen.list_content_margin_h);
            int paddingV = getResources().getDimensionPixelOffset(R.dimen.list_content_margin_v);
            mRecyclerView.setPadding(paddingV, paddingH, paddingV, paddingH);
        }

        if (mList == null || mList.isEmpty()) {
            mViewTransition.showView(1);
        } else {
            mViewTransition.showView(0);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.download);
        setNavigationIcon(VectorDrawable.create(getContext(), R.xml.ic_arrow_left_dark_x24));

        // Clear nav checked item
        setNavCheckedItem(R.id.nav_download);

        // Hide IME
        ActivityHelper.hideSoftInput(getActivity());
    }

    @Override
    public void onNavigationClick() {
        finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        DownloadManager manager = EhApplication.getDownloadManager(getContext());
        manager.setDownloadInfoListener(null);
        mRecyclerView = null;
        mViewTransition = null;
        mAdapter = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mList = null;
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        List<DownloadInfo> list = mList;
        if (list == null) {
            return false;
        }
        if (position < 0 && position >= list.size()) {
            return false;
        }

        Intent intent = new Intent(getActivity(), GalleryActivity.class);
        intent.setAction(GalleryActivity.ACTION_EH);
        intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, list.get(position).galleryInfo);
        startActivity(intent);
        return true;
    }

    @Override
    public void onAdd(DownloadInfo info) {
        // TODO
    }

    @Override
    public void onUpdate(DownloadInfo info) {
        if (!ObjectUtils.equal(info.label, mLabel)) {
            return;
        }
        RecyclerView recyclerView = mRecyclerView;
        if (recyclerView == null) {
            return;
        }
        List<DownloadInfo> list = mList;
        if (list == null) {
            return;
        }

        int size = list.size();
        for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
            View child = recyclerView.getChildAt(i);
            int index = recyclerView.getChildAdapterPosition(child);
            if (index >= 0 && index < size && list.get(index) == info) {
                DownloadHolder holder = (DownloadHolder) recyclerView.getChildViewHolder(child);
                if (holder != null) {
                    bindForState(holder, info);
                }
                break;
            }
        }
    }

    @Override
    public void onUpdateAll() {
        RecyclerView recyclerView = mRecyclerView;
        if (recyclerView == null) {
            return;
        }
        List<DownloadInfo> list = mList;
        if (list == null) {
            return;
        }

        int size = list.size();
        for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
            View child = recyclerView.getChildAt(i);
            int index = recyclerView.getChildAdapterPosition(child);
            if (index >= 0 && index < size) {
                DownloadHolder holder = (DownloadHolder) recyclerView.getChildViewHolder(child);
                bindForState(holder, list.get(index));
            }
        }
    }

    @Override
    public void onRemove(DownloadInfo info) {

    }

    private void bindForState(DownloadHolder holder, DownloadInfo info) {
        switch (info.state) {
            case DownloadInfo.STATE_NONE:
                bindState(holder, info, getContext().getString(R.string.download_state_none));
                break;
            case DownloadInfo.STATE_WAIT:
                bindState(holder, info, getContext().getString(R.string.download_state_wait));
                break;
            case DownloadInfo.STATE_DOWNLOAD:
                bindProgress(holder, info);
                break;
            case DownloadInfo.STATE_FAILED:
                String text;
                if (info.legacy <= 0) {
                    text = getContext().getString(R.string.download_state_failed);
                } else {
                    text = getContext().getString(R.string.download_state_failed_2, info.legacy);
                }
                bindState(holder, info, text);
                break;
            case DownloadInfo.STATE_FINISH:
                bindState(holder, info, getContext().getString(R.string.download_state_finish));
                break;
        }
    }

    private void bindState(DownloadHolder holder, DownloadInfo info, String state) {
        holder.uploader.setVisibility(View.VISIBLE);
        holder.rating.setVisibility(View.VISIBLE);
        holder.category.setVisibility(View.VISIBLE);
        holder.state.setVisibility(View.VISIBLE);
        holder.progressBar.setVisibility(View.INVISIBLE);
        holder.percent.setVisibility(View.INVISIBLE);
        holder.speed.setVisibility(View.INVISIBLE);

        holder.state.setText(state);
    }

    @SuppressLint("SetTextI18n")
    private void bindProgress(DownloadHolder holder, DownloadInfo info) {
        holder.uploader.setVisibility(View.INVISIBLE);
        holder.rating.setVisibility(View.INVISIBLE);
        holder.category.setVisibility(View.INVISIBLE);
        holder.state.setVisibility(View.INVISIBLE);
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.percent.setVisibility(View.VISIBLE);
        holder.speed.setVisibility(View.VISIBLE);

        if (info.total <= 0 || info.download < 0) {
            holder.percent.setText(null);
            holder.progressBar.setIndeterminate(true);
        } else {
            holder.percent.setText(info.download + "/" + info.total);
            holder.progressBar.setIndeterminate(false);
            holder.progressBar.setMax(info.total);
            holder.progressBar.setProgress(info.download);
        }
        long speed = info.speed;
        if (speed < 0) {
            speed = 0;
        }
        holder.speed.setText(FileUtils.humanReadableByteCount(speed, false) + "/S");
    }

    private class DownloadHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final LoadImageView thumb;
        public final TextView title;
        public final TextView uploader;
        public final SimpleRatingView rating;
        public final TextView category;
        public final View start;
        public final View stop;
        public final View delete;
        public final TextView state;
        public final ProgressBar progressBar;
        public final TextView percent;
        public final TextView speed;

        public DownloadHolder(View itemView) {
            super(itemView);

            thumb = (LoadImageView) itemView.findViewById(R.id.thumb);
            title = (TextView) itemView.findViewById(R.id.title);
            uploader = (TextView) itemView.findViewById(R.id.uploader);
            rating = (SimpleRatingView) itemView.findViewById(R.id.rating);
            category = (TextView) itemView.findViewById(R.id.category);
            start = itemView.findViewById(R.id.start);
            stop = itemView.findViewById(R.id.stop);
            delete = itemView.findViewById(R.id.delete);
            state = (TextView) itemView.findViewById(R.id.state);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
            percent = (TextView) itemView.findViewById(R.id.percent);
            speed = (TextView) itemView.findViewById(R.id.speed);

            thumb.setOnClickListener(this);
            start.setOnClickListener(this);
            stop.setOnClickListener(this);
            delete.setOnClickListener(this);
            RippleSalon.addRipple(start, false);
            RippleSalon.addRipple(stop, false);
            RippleSalon.addRipple(delete, false);
        }

        @Override
        public void onClick(View v) {
            RecyclerView recyclerView = mRecyclerView;
            if (recyclerView == null) {
                return;
            }
            List<DownloadInfo> list = mList;
            if (list == null) {
                return;
            }
            int size = list.size();
            int index = recyclerView.getChildAdapterPosition(itemView);
            if (index < 0 && index >= size) {
                return;
            }

            if (thumb == v) {
                Bundle args = new Bundle();
                args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
                args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, list.get(index).galleryInfo);
                Announcer announcer = new Announcer(GalleryDetailScene.class).setArgs(args);
                if (ApiHelper.SUPPORT_TRANSITION) {
                    DownloadHolder holder = (DownloadHolder) recyclerView.getChildViewHolder(itemView);
                    announcer.setTranHelper(new EnterGalleryDetailTransaction(holder));
                }
                startScene(announcer);
            } else if (start == v) {
                Intent intent = new Intent(getActivity(), DownloadService.class);
                intent.setAction(DownloadService.ACTION_START);
                intent.putExtra(DownloadService.KEY_GALLERY_INFO, list.get(index).galleryInfo);
                getActivity().startService(intent);
            } else if (stop == v) {
                Intent intent = new Intent(getActivity(), DownloadService.class);
                intent.setAction(DownloadService.ACTION_STOP);
                intent.putExtra(DownloadService.KEY_GID, list.get(index).galleryInfo.gid);
                getActivity().startService(intent);
            } else if (delete == v) {
                // TODO
            }
        }
    }

    private static class EnterGalleryDetailTransaction implements TransitionHelper {

        private final DownloadHolder mHolder;

        public EnterGalleryDetailTransaction(DownloadHolder holder) {
            mHolder = holder;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onTransition(Context context, FragmentTransaction transaction,
                Fragment exit, Fragment enter) {
            if (!(enter instanceof GalleryDetailScene)) {
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

    private class DownloadAdapter extends RecyclerView.Adapter<DownloadHolder> {

        @Override
        public DownloadHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DownloadHolder(getActivity().getLayoutInflater().inflate(R.layout.item_download, parent, false));
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onBindViewHolder(DownloadHolder holder, int position) {
            if (mList == null) {
                return;
            }
            DownloadInfo info = mList.get(position);
            GalleryInfo gi = info.galleryInfo;
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb, true);
            holder.title.setText(gi.title);
            holder.uploader.setText(gi.uploader);
            holder.rating.setRating(gi.rating);
            TextView category = holder.category;
            String newCategoryText = EhUtils.getCategory(gi.category);
            if (!newCategoryText.equals(category.getText())) {
                category.setText(newCategoryText);
                category.setBackgroundColor(EhUtils.getCategoryColor(gi.category));
            }
            bindForState(holder, info);

            // Update transition name
            if (ApiHelper.SUPPORT_TRANSITION) {
                int gid = gi.gid;
                holder.thumb.setTransitionName(TransitionNameFactory.getThumbTransitionName(gid));
                holder.title.setTransitionName(TransitionNameFactory.getTitleTransitionName(gid));
                holder.uploader.setTransitionName(TransitionNameFactory.getUploaderTransitionName(gid));
                holder.category.setTransitionName(TransitionNameFactory.getCategoryTransitionName(gid));
            }
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }
    }
}
