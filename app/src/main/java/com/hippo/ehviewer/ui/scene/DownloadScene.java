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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.conaco.Conaco;
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.DownloadInfo;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.service.DownloadManager;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.widget.AppbarRecyclerView;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.AppbarScene;
import com.hippo.scene.SimpleDialog;
import com.hippo.widget.FabLayout;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.Messenger;

import java.util.ArrayList;
import java.util.List;

public class DownloadScene extends AppbarScene implements DrawerProvider,
        EasyRecyclerView.OnItemClickListener, Messenger.Receiver, View.OnClickListener,
        AppbarRecyclerView.Helper {

    private static String LABEL_ALL = null;
    private static String LABEL_DEFAULT = "";

    private static int INDEX_ALL = 0;
    private static int INDEX_DEFAULT = 1;

    private static long ANIMATE_TIME = 300l;

    private View mTip;
    private EasyRecyclerView mRecyclerView;
    private FabLayout mFabLayout;
    private View mStart;
    private View mStop;
    private View mDelete;
    private View mMove;
    private View mCheckAll;
    private View mMainFab;

    private ViewTransition mViewTransition;

    private DownloadAdapter mAdapter;

    private List<DownloadInfo> mDownloadInfos;

    private GidPositionMap mGidPositionMap;

    private int mOriginalRecyclerViewPaddingBottom;
    private int mOriginalFabLayoutPaddingBottom;

    private int mLastUpdateSize = -1;

    private AppbarRecyclerView mRightDrawerView;
    private DownloadLabelAdapter mDownloadLabelAdapter;

    private List<String> mLabels;
    private String mActivatedLabel;
    private int mActivatedLabelPosition;

    @Override
    public int getLaunchMode() {
        return LAUNCH_MODE_SINGLE_TOP;
    }

    private void initLabels() {
        Resources resources = getContext().getResources();
        mLabels = DBUtils.getAllDownloadLabel();
        mLabels.add(0, resources.getString(R.string.download_label_default));
        mLabels.add(0, resources.getString(R.string.download_label_all));
    }

    @Override
    protected void onInit() {
        super.onInit();

        mActivatedLabel = LABEL_DEFAULT;
        mActivatedLabelPosition = INDEX_DEFAULT;
        mDownloadInfos = DownloadManager.getInstance().getDownloadList(null);
        mGidPositionMap = new GidPositionMap();
        Messenger.getInstance().register(Constants.MESSENGER_ID_UPDATE_DOWNLOAD, this);
        Messenger.getInstance().register(Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_SCENE, this);
        Messenger.getInstance().register(Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_MANAGER, this);
    }

    @Override
    protected void onDie() {
        super.onDie();

        Messenger.getInstance().unregister(Constants.MESSENGER_ID_UPDATE_DOWNLOAD, this);
        Messenger.getInstance().unregister(Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_SCENE, this);
        Messenger.getInstance().unregister(Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_MANAGER, this);
    }

    private void updateTitle() {
        String label;
        if (mActivatedLabel == LABEL_ALL) {
            label = getContext().getResources().getString(R.string.download_label_all);
        } else if (mActivatedLabel == LABEL_DEFAULT) {
            label = getContext().getResources().getString(R.string.download_label_default);
        } else {
            label = mActivatedLabel;
        }
        setTitle("Download" + " - " + label); // TODO hardcode
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);
        setContentView(R.layout.scene_download);
        setIcon(getContext().getResources().getDrawable(R.drawable.ic_arrow_left_dark_x24));

        ((ContentActivity) getStageActivity()).setDrawerListActivatedPosition(ContentActivity.DRAWER_LIST_DOWNLOAD);

        initLabels();

        mTip = findViewById(R.id.tip);
        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);
        mFabLayout = (FabLayout) findViewById(R.id.fab_layout);
        mMainFab = mFabLayout.getPrimaryFab();
        mStart = mFabLayout.getSecondaryFabAt(0);
        mStop = mFabLayout.getSecondaryFabAt(1);
        mDelete = mFabLayout.getSecondaryFabAt(2);
        mMove = mFabLayout.getSecondaryFabAt(3);
        mCheckAll = mFabLayout.getSecondaryFabAt(4);

        mAdapter = new DownloadAdapter();
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getStageActivity()));
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM);
        mRecyclerView.setCustomCheckedListener(new DownloadChoiceListener());

        mFabLayout.setAutoCancel(false);
        mFabLayout.setExpanded(false, false);
        mFabLayout.setVisibility(View.INVISIBLE);

        mMainFab.setOnClickListener(this);
        mStart.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mDelete.setOnClickListener(this);
        mMove.setOnClickListener(this);
        mCheckAll.setOnClickListener(this);

        mOriginalRecyclerViewPaddingBottom = mRecyclerView.getPaddingBottom();
        mOriginalFabLayoutPaddingBottom = mFabLayout.getPaddingBottom();

        mRightDrawerView = new AppbarRecyclerView(getStageActivity());
        mRightDrawerView.setTitle("Label"); // TODO hardcode
        mDownloadLabelAdapter = new DownloadLabelAdapter();
        mRightDrawerView.setAdapter(mDownloadLabelAdapter);
        mRightDrawerView.setOnItemClickListener(new DownloadTagClickListener());
        mRightDrawerView.setPlusVisibility(View.GONE);
        mRightDrawerView.setHelper(this);
        mRightDrawerView.showRecyclerView(false);

        updateTitle();

        mViewTransition = new ViewTransition(mTip, mRecyclerView);
        if (mAdapter.getItemCount() == 0) {
            mViewTransition.showView(0, false);
        } else {
            mViewTransition.showView(1, false);
        }
    }

    @Override
    public void onIconClick() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((ContentActivity) getStageActivity()).setDrawerListActivatedPosition(ContentActivity.DRAWER_LIST_DOWNLOAD);
    }

    @Override
    public void onBackPressed() {
        if (mRecyclerView.inCustomChoice()) {
            mRecyclerView.outOfCustomChoiceMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        super.onGetFitPaddingBottom(b);

        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), mRecyclerView.getPaddingTop(),
                mRecyclerView.getPaddingRight(), b + mOriginalRecyclerViewPaddingBottom);
        mFabLayout.setPadding(mFabLayout.getPaddingLeft(), mFabLayout.getPaddingTop(),
                mFabLayout.getPaddingRight(), b + mOriginalFabLayoutPaddingBottom);
    }

    @Override
    public boolean showLeftDrawer() {
        return true;
    }

    @Override
    public void bindRightDrawer(ContentActivity activity) {
        activity.setRightDrawerView(mRightDrawerView);
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        if (parent.inCustomChoice()) {
            parent.toggleItemChecked(position);
        } else {
            DownloadInfo info = mDownloadInfos.get(position);
            Announcer announcer = new Announcer();
            announcer.setAction(GalleryDetailScene.ACTION_GALLERY_BASE);
            announcer.putExtra(GalleryDetailScene.KEY_GALLERY_BASE, info.galleryBase);
            startScene(GalleryDetailScene.class, announcer);
        }
        return true;
    }

    private View.OnClickListener mThumbClickListener = new View.OnClickListener() {

        private int getAdapterPosition(View thumbView) {
            EasyRecyclerView recyclerView = mRecyclerView;
            for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
                View child = recyclerView.getChildAt(i);
                DownloadHolder holder = (DownloadHolder) recyclerView.getChildViewHolder(child);
                if (thumbView == holder.thumb) {
                    return holder.getAdapterPosition();
                }
            }
            return -1;
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition(v);
            if (position >= 0) {
                if (!mRecyclerView.inCustomChoice()) {
                    mRecyclerView.intoCustomChoiceMode();
                }
                mRecyclerView.toggleItemChecked(position);
            }
        }
    };

    private View.OnClickListener mStartListener = new View.OnClickListener() {

        private int getAdapterPosition(View startView) {
            EasyRecyclerView recyclerView = mRecyclerView;
            for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
                View child = recyclerView.getChildAt(i);
                DownloadHolder holder = (DownloadHolder) recyclerView.getChildViewHolder(child);
                if (startView == holder.start) {
                    return holder.getAdapterPosition();
                }
            }
            return -1;
        }

        @Override
        public void onClick(View v) {
            if (mRecyclerView.inCustomChoice()) {
                return;
            }

            int position = getAdapterPosition(v);
            if (position >= 0) {
                // Add to cache
                DownloadInfo info = mDownloadInfos.get(position);
                mGidPositionMap.add(info.galleryBase.gid, position);

                Intent intent = new Intent(getStageActivity(), DownloadService.class);
                intent.setAction(DownloadService.ACTION_START);
                intent.putExtra(DownloadService.KEY_GALLERY_BASE, info.galleryBase);
                getStageActivity().startService(intent);
            }
        }
    };

    private View.OnClickListener mStopListener = new View.OnClickListener() {

        private int getAdapterPosition(View stopView) {
            EasyRecyclerView recyclerView = mRecyclerView;
            for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
                View child = recyclerView.getChildAt(i);
                DownloadHolder holder = (DownloadHolder) recyclerView.getChildViewHolder(child);
                if (stopView == holder.stop) {
                    return holder.getAdapterPosition();
                }
            }
            return -1;
        }

        @Override
        public void onClick(View v) {
            if (mRecyclerView.inCustomChoice()) {
                return;
            }

            int position = getAdapterPosition(v);
            if (position >= 0) {
                // Add to cache
                DownloadInfo info = mDownloadInfos.get(position);
                mGidPositionMap.add(info.galleryBase.gid, position);
                DownloadManager.getInstance().stopDownload(info.galleryBase.gid);
            }
        }
    };

    private View.OnClickListener mDeleteListener = new View.OnClickListener() {

        private int getAdapterPosition(View deleteView) {
            EasyRecyclerView recyclerView = mRecyclerView;
            for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
                View child = recyclerView.getChildAt(i);
                DownloadHolder holder = (DownloadHolder) recyclerView.getChildViewHolder(child);
                if (deleteView == holder.delete) {
                    return holder.getAdapterPosition();
                }
            }
            return -1;
        }

        @Override
        public void onClick(View v) {
            if (mRecyclerView.inCustomChoice()) {
                return;
            }

            int position = getAdapterPosition(v);
            if (position >= 0) {
                // Add to cache
                DownloadInfo info = mDownloadInfos.get(position);
                mGidPositionMap.add(info.galleryBase.gid, position);

                Intent intent = new Intent(getStageActivity(), DownloadService.class);
                intent.setAction(DownloadService.ACTION_DELETE);
                intent.putExtra(DownloadService.KEY_GID, info.galleryBase.gid);
                getStageActivity().startService(intent);
            }
        }
    };

    private int getPositionForGid(int gid) {
        if (mDownloadInfos == null) {
            return -1;
        }

        int position = mGidPositionMap.getPosition(gid);

        if (position == -1) {
            for (int i = 0, n = mDownloadInfos.size(); i < n; i++) {
                DownloadInfo info = mDownloadInfos.get(i);
                if (info.galleryBase.gid == gid) {
                    position = i;
                    break;
                }
            }
        }

        return position;
    }

    @Override
    public void onReceive(int id, Object obj) {
        if (id == Constants.MESSENGER_ID_UPDATE_DOWNLOAD) {
            if (!(obj instanceof Integer)) {
                return;
            }
            if (mAdapter == null || mDownloadInfos == null) {
                return;
            }

            try {
                int mix = (int) obj;
                int ops = DownloadManager.getOps(mix);
                int gid = DownloadManager.getGid(mix);

                if (ops == DownloadManager.OPS_ALL_CHANGE ||
                        ops == DownloadManager.OPS_REMOVE) {
                    // OPS_REMOVE can't find position by gid, just notifyDataSetChanged
                    mGidPositionMap.clear();
                    mAdapter.notifyDataSetChanged();
                    return;
                } else if (ops == DownloadManager.OPS_ADD) {
                    // Add break list, need clean gid position map
                    mGidPositionMap.clear();
                }

                int position = getPositionForGid(gid);
                if (position == -1) {
                    if (mLastUpdateSize != mAdapter.getItemCount()) {
                        // Can't get position, notifyDataSetChanged for safe
                        mAdapter.notifyDataSetChanged();
                    }
                    return;
                }

                if (ops == DownloadManager.OPS_ADD) {
                    mAdapter.notifyItemInserted(position);
                } else if (ops == DownloadManager.OPS_UPDATE) {
                    RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                    if (holder != null) {
                        bindHolder((DownloadHolder) holder, mDownloadInfos.get(position));
                    }
                }
            } finally {
                // Try to show tip
                if (mAdapter.getItemCount() == 0) {
                    mViewTransition.showView(0, true);
                } else {
                    mViewTransition.showView(1, true);
                }
            }

        } else if (id == Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_SCENE ||
                id == Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_MANAGER) {
            AssertUtils.assertInstanceof("Messenger obj must be DownloadLabelOps", obj, DownloadManager.DownloadLabelModify.class);
            DownloadManager.DownloadLabelModify modify = (DownloadManager.DownloadLabelModify) obj;

            switch (modify.ops) {
                case DownloadManager.DownloadLabelModify.OPS_ADD:
                    mLabels.add(modify.value);
                    mDownloadLabelAdapter.notifyDataSetChanged();
                    break;
                case DownloadManager.DownloadLabelModify.OPS_REMOVE:
                    mLabels.remove(modify.label.label);
                    mDownloadLabelAdapter.notifyDataSetChanged();

                    // Update activated info or maybe download info list
                    if (modify.label.label.equals(mActivatedLabel)) {
                        // Select default
                        mActivatedLabel = LABEL_DEFAULT;
                        mActivatedLabelPosition = INDEX_DEFAULT;
                        updateTitle();

                        mDownloadInfos = DownloadManager.getInstance().getDownloadList(null);
                        mGidPositionMap.clear();
                        mAdapter.notifyDataSetChanged();
                        mRecyclerView.scrollToPosition(0);

                        // Try to show tip
                        if (mAdapter.getItemCount() == 0) {
                            mViewTransition.showView(0, true);
                        } else {
                            mViewTransition.showView(1, true);
                        }
                    }
                    break;
                case DownloadManager.DownloadLabelModify.OPS_MOVE:
                    int fromPosition = mLabels.indexOf(modify.label.label);
                    int toPosition = mLabels.indexOf(modify.label.label);
                    if (fromPosition >= 0 && toPosition >= 0) {
                        mLabels.add(toPosition, mLabels.remove(fromPosition));
                        mDownloadLabelAdapter.notifyDataSetChanged();

                        // Update activated info
                        if (fromPosition == mActivatedLabelPosition) {
                            mActivatedLabelPosition = toPosition;
                        }
                    }
                    break;
                case DownloadManager.DownloadLabelModify.OPS_CHANGE:
                    int position = mLabels.indexOf(modify.label.label);
                    if (position >= 0) {
                        mLabels.set(position, modify.value);
                        mDownloadLabelAdapter.notifyDataSetChanged();

                        // Update activated info
                        if (position == mActivatedLabelPosition) {
                            mActivatedLabel = modify.value;
                            updateTitle();
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown download label ops");
            }
        }
    }

    private class MoveDownloadInfoListener implements SimpleDialog.OnClickListener {

        private List<String> mLabels;
        private boolean mHasDefaultLabel;

        public MoveDownloadInfoListener(List<String> labels, boolean hasDefaultLabel) {
            mLabels = labels;
            mHasDefaultLabel = hasDefaultLabel;
        }

        @Override
        public boolean onClick(SimpleDialog dialog, int which) {
            // Target label
            String label;
            if (mHasDefaultLabel && which == 0) {
                label = null;
            } else {
                label = mLabels.get(which);
            }
            // Target download infos
            List<DownloadInfo> infos = new ArrayList<>();
            SparseBooleanArray checkedState = mRecyclerView.getCheckedItemPositions();
            for (int i = 0, n = checkedState.size(); i < n; i++) {
                if (checkedState.valueAt(i)) {
                    DownloadInfo info = mDownloadInfos.get(checkedState.keyAt(i));
                    if (!DownloadManager.labelEquals(info.label, label)) {
                        infos.add(info);
                    }
                }
            }

            // Must out of choice mode here
            mRecyclerView.outOfCustomChoiceMode();

            if (!infos.isEmpty()) {
                DownloadManager.getInstance().moveDownloadInfo(infos, label);
                mAdapter.notifyDataSetChanged();

                // Try to show tip
                if (mAdapter.getItemCount() == 0) {
                    mViewTransition.showView(0, true);
                } else {
                    mViewTransition.showView(1, true);
                }
            }
            return true;
        }
    }

    @Override
    public void onClick(View v) {
        if (mMainFab == v) {
            mRecyclerView.outOfCustomChoiceMode();
        } else if (mStart == v) {
            SparseBooleanArray checkedState = mRecyclerView.getCheckedItemPositions();
            for (int i = 0, n = checkedState.size(); i < n; i++) {
                if (checkedState.valueAt(i)) {
                    int position = checkedState.keyAt(i);
                    DownloadInfo info = mDownloadInfos.get(position);
                    Intent intent = new Intent(getStageActivity(), DownloadService.class);
                    intent.setAction(DownloadService.ACTION_START);
                    intent.putExtra(DownloadService.KEY_GALLERY_BASE, info.galleryBase);
                    getStageActivity().startService(intent);
                }
            }
            // Out of choice mode
            mRecyclerView.outOfCustomChoiceMode();
        } else if (mStop == v) {
            DownloadManager manager = DownloadManager.getInstance();
            SparseBooleanArray checkedState = mRecyclerView.getCheckedItemPositions();
            for (int i = 0, n = checkedState.size(); i < n; i++) {
                if (checkedState.valueAt(i)) {
                    int position = checkedState.keyAt(i);
                    manager.stopDownload(mDownloadInfos.get(position).galleryBase.gid);
                }
            }
            // Out of choice mode
            mRecyclerView.outOfCustomChoiceMode();
        } else if (mDelete == v) {
            // TODO
        } else if (mMove == v) {
            // Get available target label
            boolean hasDefaultLabel = false;
            final List<String> labels = DBUtils.getAllDownloadLabel();
            if (mActivatedLabelPosition > INDEX_DEFAULT) {
                labels.remove(mActivatedLabelPosition - 2);
            }
            if (mActivatedLabelPosition != INDEX_DEFAULT) {
                hasDefaultLabel = true;
                labels.add(getResources().getString(R.string.download_label_default));
            }

            if (labels.isEmpty()) {
                Toast.makeText(getContext(), "No available label", Toast.LENGTH_SHORT).show(); // TODO hardcode
            } else {
                new SimpleDialog.Builder(getContext()).setTitle("Move to") // TODO hardcode
                        .setItems(labels.toArray(new String[labels.size()]),
                                new MoveDownloadInfoListener(labels, hasDefaultLabel)).show(this);
            }
        } else if (mCheckAll == v) {
            SparseBooleanArray checkedState = mRecyclerView.getCheckedItemPositions();
            for (int i = 0, n = mAdapter.getItemCount(); i < n; i++) {
                if (!checkedState.get(i, false)) {
                    mRecyclerView.setItemChecked(i, true);
                }
            }
        }
    }

    @Override
    public void onClickPlusListener() {
    }

    @Override
    public void onClickSettingsListener() {
        startScene(DownloadLabelScene.class);
    }

    private class DownloadHolder extends RecyclerView.ViewHolder {

        public LoadImageView thumb;
        public TextView title;
        public TextView uploader;
        public SimpleRatingView rating;
        public TextView category;
        public View start;
        public View stop;
        public View delete;
        public TextView state;
        public ProgressBar progressBar;
        public TextView percent;
        public TextView speed;

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

            RippleSalon.addRipple(start, false);
            RippleSalon.addRipple(stop, false);
            RippleSalon.addRipple(delete, false);

            thumb.setOnClickListener(mThumbClickListener);
            start.setOnClickListener(mStartListener);
            stop.setOnClickListener(mStopListener);
            delete.setOnClickListener(mDeleteListener);
        }
    }

    private void bindState(DownloadHolder holder, DownloadInfo info, String state) {
        GalleryBase galleryBase = info.galleryBase;
        EhApplication.getConaco(getStageActivity()).load(holder.thumb,
                EhCacheKeyFactory.getThumbKey(galleryBase.gid), galleryBase.thumb);
        holder.title.setText(EhUtils.getSuitableTitle(galleryBase));
        holder.uploader.setVisibility(View.VISIBLE);
        holder.rating.setVisibility(View.VISIBLE);
        holder.category.setVisibility(View.VISIBLE);
        holder.state.setVisibility(View.VISIBLE);
        holder.uploader.setText(galleryBase.uploader);
        holder.rating.setRating(galleryBase.rating);
        TextView category = holder.category;
        String newCategoryText = EhUtils.getCategory(galleryBase.category);
        if (!newCategoryText.equals(category.getText())) {
            category.setText(newCategoryText);
            category.setBackgroundColor(EhUtils.getCategoryColor(galleryBase.category));
        }
        holder.state.setText(state);
        holder.progressBar.setVisibility(View.INVISIBLE);
        holder.percent.setVisibility(View.INVISIBLE);
        holder.speed.setVisibility(View.INVISIBLE);
    }

    private void bindProgress(DownloadHolder holder, DownloadInfo info) {
        GalleryBase galleryBase = info.galleryBase;
        EhApplication.getConaco(getStageActivity()).load(holder.thumb,
                EhCacheKeyFactory.getThumbKey(info.galleryBase.gid), galleryBase.thumb);
        holder.title.setText(EhUtils.getSuitableTitle(galleryBase));
        holder.uploader.setVisibility(View.INVISIBLE);
        holder.rating.setVisibility(View.INVISIBLE);
        holder.category.setVisibility(View.INVISIBLE);
        holder.state.setVisibility(View.INVISIBLE);
        holder.progressBar.setVisibility(View.VISIBLE);

        if (info.total <= 0 || info.speed < 0) {
            holder.progressBar.setIndeterminate(true);
            holder.percent.setVisibility(View.INVISIBLE);
            holder.speed.setVisibility(View.INVISIBLE);
        } else {
            holder.progressBar.setIndeterminate(false);
            holder.progressBar.setMax(info.total);
            holder.progressBar.setProgress(info.download);
            holder.percent.setVisibility(View.VISIBLE);
            holder.speed.setVisibility(View.VISIBLE);
            holder.percent.setText(info.download + "/" + info.total);
            holder.speed.setText(FileUtils.humanReadableByteCount(info.speed, false) + "/S");
        }
    }

    private void bindHolder(DownloadHolder holder, DownloadInfo info) {
        switch (info.state) {
            case DownloadInfo.STATE_NONE:
            default:
                bindState(holder, info, getContext().getResources().getString(R.string.download_state_none));
                break;
            case DownloadInfo.STATE_WAIT:
                bindState(holder, info, getContext().getResources().getString(R.string.download_state_wait));
                break;
            case DownloadInfo.STATE_DOWNLOAD:
                bindProgress(holder, info);
                break;
            case DownloadInfo.STATE_FINISH:
                if (info.legacy == 0) {
                    bindState(holder, info, getContext().getResources().getString(R.string.download_state_done));
                } else if (info.legacy > 0 && info.legacy != Integer.MAX_VALUE) {
                    bindState(holder, info, getContext().getResources().getString(R.string.download_state_incomplete, info.legacy));
                } else {
                    bindState(holder, info, getContext().getResources().getString(R.string.download_state_incomplete));
                }
                break;
        }
    }

    private class DownloadAdapter extends RecyclerView.Adapter<DownloadHolder> {

        Conaco mConaco;

        public DownloadAdapter() {
            mConaco = EhApplication.getConaco(getStageActivity());
        }

        @Override
        public DownloadHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DownloadHolder(getStageActivity().getLayoutInflater().inflate(R.layout.item_download, parent, false));
        }

        @Override
        public void onBindViewHolder(DownloadHolder holder, int position) {
            DownloadInfo info = mDownloadInfos.get(position);
            bindHolder(holder, info);
        }

        @Override
        public long getItemId(int position) {
            return mDownloadInfos.get(position).galleryBase.gid;
        }

        @Override
        public int getItemCount() {
            mLastUpdateSize = mDownloadInfos != null ? mDownloadInfos.size() : 0;
            return mLastUpdateSize;
        }
    }

    private static class GidPositionMap {

        private static final int SIZE = 12;

        private int[] mGids = new int[SIZE];
        private int[] mPosition = new int[SIZE];

        private int mStartIndex = 0;
        private int mSize = 0;

        public void add(int gid, int position) {
            int index = (mStartIndex + mSize) % SIZE;
            mGids[index] = gid;
            mPosition[index] = position;

            // Update start index size and
            mSize++;
            if (mSize > SIZE) {
                mStartIndex = (mStartIndex + mSize) % SIZE;
                mSize = Math.min(mSize, SIZE);
            }
        }

        public void clear() {
            mSize = 0;
        }

        public int getPosition(int gid) {
            for (int i = 0; i < mSize; i++) {
                int index = (mStartIndex + i) % SIZE;
                if (gid == mGids[index]) {
                    return mPosition[index];
                }
            }
            return -1;
        }
    }

    public void openFabLayout() {
        mFabLayout.setVisibility(View.VISIBLE);

        mFabLayout.setExpanded(true);
        PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f);
        PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", 0f, 1f);
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(mMainFab, scaleXPvh, scaleYPvh);
        oa.setDuration(ANIMATE_TIME);
        oa.start();
    }

    public void closeFabLayout() {
        mFabLayout.setExpanded(false);
        PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f);
        PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f);
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(mMainFab, scaleXPvh, scaleYPvh);
        oa.setDuration(ANIMATE_TIME);
        oa.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFabLayout.setVisibility(View.INVISIBLE);
            }
        });
        oa.start();
    }

    public class DownloadChoiceListener implements EasyRecyclerView.CustomChoiceListener {

        @Override
        public void onIntoCustomChoice(EasyRecyclerView view) {
            openFabLayout();
        }

        @Override
        public void onOutOfCustomChoice(EasyRecyclerView view) {
            closeFabLayout();
        }

        @Override
        public void onItemCheckedStateChanged(EasyRecyclerView view, int position, long id, boolean checked) {
            if (view.getCheckedItemCount() == 0) {
                view.outOfCustomChoiceMode();
            }
        }
    }

    public class DownloadTagClickListener implements EasyRecyclerView.OnItemClickListener {

        @Override
        public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
            if (mActivatedLabelPosition != position) {
                mActivatedLabelPosition = position;

                if (position == 0) {
                    mActivatedLabel = LABEL_ALL;
                    mDownloadInfos = DownloadManager.getInstance().getAllDownloadList();
                } else if (position == 1) {
                    mActivatedLabel = LABEL_DEFAULT;
                    mDownloadInfos = DownloadManager.getInstance().getDownloadList(null);
                } else {
                    mActivatedLabel = mLabels.get(position);
                    mDownloadInfos = DownloadManager.getInstance().getDownloadList(mActivatedLabel);
                }
                updateTitle();
                mGidPositionMap.clear();
                mAdapter.notifyDataSetChanged();

                // Try to show tip
                if (mAdapter.getItemCount() == 0) {
                    mViewTransition.showView(0, true);
                } else {
                    mViewTransition.showView(1, true);
                }

                mRecyclerView.scrollToPosition(0);
                ((ContentActivity) getStageActivity()).closeDrawers();
            }
            return true;
        }
    }

    private class SimpleHolder extends RecyclerView.ViewHolder {

        public SimpleHolder(View itemView) {
            super(itemView);
        }
    }

    public class DownloadLabelAdapter extends RecyclerView.Adapter<SimpleHolder> {

        @Override
        public SimpleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SimpleHolder(getStageActivity().getLayoutInflater().inflate(
                    R.layout.item_appbar_recycler_view, parent, false));
        }

        @Override
        public void onBindViewHolder(SimpleHolder holder, int position) {
            ((TextView) holder.itemView).setText(mLabels.get(position));
        }

        @Override
        public int getItemCount() {
            return mLabels == null ? 0 : mLabels.size();
        }
    }
}
