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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hippo.app.CheckBoxDialogBuilder;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.easyrecyclerview.HandlerDrawable;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.DownloadLabelRaw;
import com.hippo.ehviewer.download.DownloadInfo;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.download.DownloadService;
import com.hippo.ehviewer.spider.SpiderDen;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.ui.annotation.ViewLifeCircle;
import com.hippo.ehviewer.ui.annotation.WholeLifeCircle;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.TransitionHelper;
import com.hippo.unifile.UniFile;
import com.hippo.util.ActivityHelper;
import com.hippo.util.ApiHelper;
import com.hippo.view.ViewTransition;
import com.hippo.widget.FabLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IntList;
import com.hippo.yorozuya.ObjectUtils;
import com.hippo.yorozuya.ResourcesUtils;
import com.hippo.yorozuya.ViewUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DownloadScene extends ToolbarScene
        implements DownloadManager.DownloadInfoListener,
        EasyRecyclerView.OnItemClickListener,
        EasyRecyclerView.OnItemLongClickListener,
        FabLayout.OnClickFabListener {

    private static final String KEY_LABEL = "label";

    @Nullable
    @ViewLifeCircle
    private EasyRecyclerView mRecyclerView;
    @Nullable
    @ViewLifeCircle
    private ViewTransition mViewTransition;
    @Nullable
    @ViewLifeCircle
    private FabLayout mFabLayout;

    @Nullable
    @ViewLifeCircle
    private DownloadAdapter mAdapter;

    @Nullable
    @WholeLifeCircle
    private String mLabel;
    @Nullable
    @WholeLifeCircle
    private List<DownloadInfo> mList;

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mList = null;
        DownloadManager manager = EhApplication.getDownloadManager(getContext());
        manager.setDownloadInfoListener(null);
    }

    private void updateForLabel() {
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

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        updateTitle();
        Settings.putRecentDownloadLabel(mLabel);
    }

    private void updateTitle() {
        setTitle(getString(R.string.scene_download_title,
                mLabel != null ? mLabel : getString(R.string.default_download_label_name)));
    }

    private void onInit() {
        mLabel = Settings.getRecentDownloadLabel();
        updateForLabel();
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mLabel = savedInstanceState.getString(KEY_LABEL);
        updateForLabel();
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

        View content = ViewUtils.$$(view, R.id.content);
        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(content, R.id.recycler_view);
        FastScroller fastScroller = (FastScroller) ViewUtils.$$(content, R.id.fast_scroller);
        mFabLayout = (FabLayout) ViewUtils.$$(view, R.id.fab_layout);
        View tip = ViewUtils.$$(view, R.id.tip);
        mViewTransition = new ViewTransition(content, tip);

        mAdapter = new DownloadAdapter();
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.hasFixedSize();
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setOnItemLongClickListener(this);
        mRecyclerView.setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM);
        mRecyclerView.setCustomCheckedListener(new DownloadChoiceListener());
        // Cancel change animation
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
        int paddingH = getResources().getDimensionPixelOffset(R.dimen.list_content_margin_h);
        int paddingV = getResources().getDimensionPixelOffset(R.dimen.list_content_margin_v);
        mRecyclerView.setPadding(paddingV, paddingH, paddingV, paddingH);

        fastScroller.attachToRecyclerView(mRecyclerView);
        HandlerDrawable drawable = new HandlerDrawable();
        drawable.setColor(ResourcesUtils.getAttrColor(getContext(), R.attr.colorAccent));
        fastScroller.setHandlerDrawable(drawable);

        mFabLayout.setExpanded(false, false);
        mFabLayout.setHidePrimaryFab(true);
        mFabLayout.setAutoCancel(false);
        mFabLayout.setOnClickFabListener(this);

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
        updateTitle();
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);

        // Clear nav checked item
        setNavCheckedItem(R.id.nav_download);

        // Hide IME
        ActivityHelper.hideSoftInput(getActivity());
    }

    @Override
    public void onNavigationClick() {
        onBackPressed();
    }

    @Override
    public int getMenuResId() {
        return R.menu.scene_download;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // Skip when in choice mode
        if (mRecyclerView == null || mRecyclerView.isInCustomChoice()) {
            return false;
        }

        int id = item.getItemId();
        switch (id) {
            case R.id.action_start_all: {
                Intent intent = new Intent(getActivity(), DownloadService.class);
                intent.setAction(DownloadService.ACTION_START_ALL);
                getActivity().startService(intent);
                return true;
            }
            case R.id.action_stop_all: {
                Intent intent = new Intent(getActivity(), DownloadService.class);
                intent.setAction(DownloadService.ACTION_STOP_ALL);
                getActivity().startService(intent);
                return true;
            }
            case R.id.action_label: {
                openDrawer(Gravity.RIGHT);
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mRecyclerView = null;
        mFabLayout = null;
        mViewTransition = null;
        mAdapter = null;
    }

    @Override
    public View onCreateDrawerView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.drawer_list, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.drawer_download_label_title);
        toolbar.inflateMenu(R.menu.drawer_download);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.action_settings:
                        startScene(new Announcer(DownloadLabelScene.class));
                        return true;
                }
                return false;
            }
        });

        List<DownloadLabelRaw> list = EhApplication.getDownloadManager(getContext()).getLabelList();
        final List<String> labels = new ArrayList<>(list.size() + 1);
        // Add default label name
        labels.add(getString(R.string.default_download_label_name));
        for (DownloadLabelRaw raw: list) {
            labels.add(raw.getLabel());
        }

        // TODO handle download label items update
        ListView listView = (ListView) view.findViewById(R.id.list_view);
        listView.setAdapter(new ArrayAdapter<>(getContext(), R.layout.item_simple_list, labels));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String label;
                if (position == 0) {
                    label = null;
                } else {
                    label = labels.get(position);
                }
                if (!ObjectUtils.equal(label, mLabel)) {
                    mLabel = label;
                    updateForLabel();
                    if (mViewTransition != null) {
                        if (mList == null || mList.size() == 0) {
                            mViewTransition.showView(1);
                        } else {
                            mViewTransition.showView(0);
                        }
                    }
                    closeDrawer(Gravity.RIGHT);
                }
            }
        });

        return view;
    }

    @Override
    public void onBackPressed() {
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            mRecyclerView.outOfCustomChoiceMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        EasyRecyclerView recyclerView = mRecyclerView;
        if (recyclerView == null) {
            return false;
        }

        if (recyclerView.isInCustomChoice()) {
            recyclerView.toggleItemChecked(position);
            return true;
        } else {
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
    }

    @Override
    public boolean onItemLongClick(EasyRecyclerView parent, View view, int position, long id) {
        EasyRecyclerView recyclerView = mRecyclerView;
        if (recyclerView == null) {
            return false;
        }

        if (!recyclerView.isInCustomChoice()) {
            recyclerView.intoCustomChoiceMode();
        }
        recyclerView.toggleItemChecked(position);

        return true;
    }

    @Override
    public void onClickPrimaryFab(FabLayout view, FloatingActionButton fab) {
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            mRecyclerView.outOfCustomChoiceMode();
        }
    }

    @Override
    public void onClickSecondaryFab(FabLayout view, FloatingActionButton fab, int position) {
        EasyRecyclerView recyclerView = mRecyclerView;
        if (recyclerView == null) {
            return;
        }

        if (0 == position) {
            recyclerView.checkAll();
        } else {
            List<DownloadInfo> list = mList;
            if (list == null) {
                return;
            }

            IntList gidList = null;
            List<GalleryInfo> galleryInfoList = null;
            List<DownloadInfo> downloadInfoList = null;
            boolean collectGid = position == 1 || position == 2 || position == 3; // Start, Stop, Delete
            boolean collectGalleryInfo = position == 3; // Delete
            boolean collectDownloadInfo = position == 4; // Move
            if (collectGid) {
                gidList = new IntList();
            }
            if (collectGalleryInfo) {
                galleryInfoList = new LinkedList<>();
            }
            if (collectDownloadInfo) {
                downloadInfoList = new LinkedList<>();
            }

            SparseBooleanArray stateArray = recyclerView.getCheckedItemPositions();
            for (int i = 0, n = stateArray.size(); i < n; i++) {
                if (stateArray.valueAt(i)) {
                    DownloadInfo info = list.get(stateArray.keyAt(i));
                    if (collectDownloadInfo) {
                        downloadInfoList.add(info);
                    }
                    GalleryInfo gi = info.galleryInfo;
                    if (collectGalleryInfo) {
                        galleryInfoList.add(gi);
                    }
                    if (collectGid) {
                        gidList.add(gi.gid);
                    }
                }
            }

            switch (position) {
                case 1: { // Start
                    Intent intent = new Intent(getActivity(), DownloadService.class);
                    intent.setAction(DownloadService.ACTION_START_RANGE);
                    intent.putExtra(DownloadService.KEY_GID_LIST, gidList);
                    getActivity().startService(intent);
                    // Cancel check mode
                    recyclerView.outOfCustomChoiceMode();
                    break;
                }
                case 2: { // Stop
                    Intent intent = new Intent(getActivity(), DownloadService.class);
                    intent.setAction(DownloadService.ACTION_STOP_RANGE);
                    intent.putExtra(DownloadService.KEY_GID_LIST, gidList);
                    getActivity().startService(intent);
                    // Cancel check mode
                    recyclerView.outOfCustomChoiceMode();
                    break;
                }
                case 3: { // Delete
                    CheckBoxDialogBuilder builder = new CheckBoxDialogBuilder(getContext(),
                            getString(R.string.download_remove_dialog_message_2, gidList.size()),
                            getString(R.string.download_remove_dialog_check_text),
                            Settings.getRemoveImageFiles());
                    DeleteRangeDialogHelper helper = new DeleteRangeDialogHelper(
                            galleryInfoList, gidList, builder);
                    builder.setTitle(R.string.download_remove_dialog_title)
                            .setPositiveButton(android.R.string.ok, helper)
                            .show();
                    break;
                }
                case 4: {// Move
                    List<DownloadLabelRaw> labelRawList = EhApplication.getDownloadManager(getContext()).getLabelList();
                    List<String> labelList = new ArrayList<>(labelRawList.size() + 1);
                    labelList.add(getString(R.string.default_download_label_name));
                    for (int i = 0, n = labelRawList.size(); i < n; i++) {
                        labelList.add(labelRawList.get(i).getLabel());
                    }
                    String[] labels = labelList.toArray(new String[labelList.size()]);

                    MoveDialogHelper helper = new MoveDialogHelper(labels, downloadInfoList);

                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.download_move_dialog_title)
                            .setItems(labels, helper)
                            .show();
                    break;
                }
            }
        }
    }

    @Override
    public void onAdd(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list, int position) {
        if (mList != list) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.notifyItemInserted(position);
        }
    }

    @Override
    public void onUpdate(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list) {
        if (mList != list) {
            return;
        }

        int index = list.indexOf(info);
        if (index >= 0 && mAdapter != null) {
            mAdapter.notifyItemChanged(index);
        }
    }

    @Override
    public void onUpdateAll() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onReload() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        if (mViewTransition != null) {
            if (mList != null && mList.size() == 0) {
                mViewTransition.showView(1);
            } else {
                mViewTransition.showView(0);
            }
        }
    }

    @Override
    public void onChange() {
        mLabel = null;
        updateForLabel();
        if (mViewTransition != null) {
            if (mList == null || mList.size() == 0) {
                mViewTransition.showView(1);
            } else {
                mViewTransition.showView(0);
            }
        }
    }

    @Override
    public void onRenameLabel(String from, String to) {
        if (!ObjectUtils.equal(mLabel, from)) {
            return;
        }

        mLabel = to;
        updateForLabel();
        if (mViewTransition != null) {
            if (mList == null || mList.size() == 0) {
                mViewTransition.showView(1);
            } else {
                mViewTransition.showView(0);
            }
        }
    }

    @Override
    public void onRemove(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list, int position) {
        if (mList != list) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.notifyItemRemoved(position);
        }

        if (mList != null && mList.size() == 0 && mViewTransition != null) {
            mViewTransition.showView(1);
        }
    }

    @Override
    public void onUpdateLabels() {
        // TODO
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

        if (info.total <= 0 || info.finished < 0) {
            holder.percent.setText(null);
            holder.progressBar.setIndeterminate(true);
        } else {
            holder.percent.setText(info.finished + "/" + info.total);
            holder.progressBar.setIndeterminate(false);
            holder.progressBar.setMax(info.total);
            holder.progressBar.setProgress(info.finished);
        }
        long speed = info.speed;
        if (speed < 0) {
            speed = 0;
        }
        holder.speed.setText(FileUtils.humanReadableByteCount(speed, false) + "/S");
    }

    private static void deleteFileAsync(UniFile... files) {
        new AsyncTask<UniFile, Void, Void>() {
            @Override
            protected Void doInBackground(UniFile... params) {
                for (UniFile file: params) {
                    file.delete();
                }
                return null;
            }
        }.execute(files);
    }

    private class DeleteDialogHelper implements DialogInterface.OnClickListener {

        private final GalleryInfo mGalleryInfo;
        private final CheckBoxDialogBuilder mBuilder;

        public DeleteDialogHelper(GalleryInfo galleryInfo, CheckBoxDialogBuilder builder) {
            mGalleryInfo = galleryInfo;
            mBuilder = builder;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return;
            }

            // Delete
            Intent intent = new Intent(getActivity(), DownloadService.class);
            intent.setAction(DownloadService.ACTION_DELETE);
            intent.putExtra(DownloadService.KEY_GID, mGalleryInfo.gid);
            getActivity().startService(intent);

            // Delete image files
            boolean checked = mBuilder.isChecked();
            Settings.putRemoveImageFiles(checked);
            if (checked) {
                // Remove download path
                EhDB.removeDownloadDirname(mGalleryInfo.gid);
                // Delete file
                UniFile file = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                deleteFileAsync(file);
            }
        }
    }

    private class DeleteRangeDialogHelper implements DialogInterface.OnClickListener {

        private final List<GalleryInfo> mGalleryInfoList;
        private final IntList mGidList;
        private final CheckBoxDialogBuilder mBuilder;

        public DeleteRangeDialogHelper(List<GalleryInfo> galleryInfoList,
                IntList gidList, CheckBoxDialogBuilder builder) {
            mGalleryInfoList = galleryInfoList;
            mGidList = gidList;
            mBuilder = builder;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return;
            }

            // Cancel check mode
            if (mRecyclerView != null) {
                mRecyclerView.outOfCustomChoiceMode();
            }

            // Delete
            Intent intent = new Intent(getActivity(), DownloadService.class);
            intent.setAction(DownloadService.ACTION_DELETE_RANGE);
            intent.putExtra(DownloadService.KEY_GID_LIST, mGidList);
            getActivity().startService(intent);

            // Delete image files
            boolean checked = mBuilder.isChecked();
            Settings.putRemoveImageFiles(checked);
            if (checked) {
                UniFile[] files = new UniFile[mGalleryInfoList.size()];
                int i = 0;
                for (GalleryInfo gi: mGalleryInfoList) {
                    // Remove download path
                    EhDB.removeDownloadDirname(gi.gid);
                    // Put file
                    files[i] = SpiderDen.getGalleryDownloadDir(gi);
                    i++;
                }
                // Delete file
                deleteFileAsync(files);
            }
        }
    }

    private class MoveDialogHelper implements DialogInterface.OnClickListener {

        private final String[] mLabels;
        private final List<DownloadInfo> mDownloadInfoList;

        public MoveDialogHelper(String[] labels, List<DownloadInfo> downloadInfoList) {
            mLabels = labels;
            mDownloadInfoList = downloadInfoList;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            // Cancel check mode
            if (mRecyclerView != null) {
                mRecyclerView.outOfCustomChoiceMode();
            }

            String label;
            if (which == 0) {
                label = null;
            } else {
                label = mLabels[which];
            }
            EhApplication.getDownloadManager(getContext()).changeLabel(mDownloadInfoList, label);
        }
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

            // TODO cancel on click listener when select items
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
            EasyRecyclerView recyclerView = mRecyclerView;
            if (recyclerView == null || recyclerView.isInCustomChoice()) {
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
                GalleryInfo galleryInfo = list.get(index).galleryInfo;
                CheckBoxDialogBuilder builder = new CheckBoxDialogBuilder(getContext(),
                        getString(R.string.download_remove_dialog_message, galleryInfo.title),
                        getString(R.string.download_remove_dialog_check_text),
                        Settings.getRemoveImageFiles());
                DeleteDialogHelper helper = new DeleteDialogHelper(galleryInfo, builder);
                builder.setTitle(R.string.download_remove_dialog_title)
                        .setPositiveButton(android.R.string.ok, helper)
                        .show();
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

    private class DownloadAdapter extends RecyclerView.Adapter<DownloadHolder> {

        @Override
        public long getItemId(int position) {
            if (mList == null || position < 0 || position >= mList.size()) {
                return 0;
            }
            return mList.get(position).galleryInfo.gid;
        }

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

    private class DownloadChoiceListener implements  EasyRecyclerView.CustomChoiceListener {

        @Override
        public void onIntoCustomChoice(EasyRecyclerView view) {
            if (mRecyclerView != null) {
                mRecyclerView.setOnItemLongClickListener(null);
                mRecyclerView.setLongClickable(false);
            }
            if (mFabLayout != null) {
                mFabLayout.setExpanded(true);
            }
            // Lock drawer
            setDrawerLayoutEnable(false);
        }

        @Override
        public void onOutOfCustomChoice(EasyRecyclerView view) {
            if (mRecyclerView != null) {
                mRecyclerView.setOnItemLongClickListener(DownloadScene.this);
            }
            if (mFabLayout != null) {
                mFabLayout.setExpanded(false);
            }
            // Unlock drawer
            setDrawerLayoutEnable(true);
        }

        @Override
        public void onItemCheckedStateChanged(EasyRecyclerView view, int position, long id, boolean checked) {
            if (view.getCheckedItemCount() == 0) {
                view.outOfCustomChoiceMode();
            }
        }
    }
}
