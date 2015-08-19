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

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hippo.conaco.Conaco;
import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.DownloadInfo;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.service.DownloadManager;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Scene;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.Messenger;

import java.util.List;

public class DownloadScene extends Scene implements DrawerProvider,
        EasyRecyclerView.OnItemClickListener, Messenger.Receiver {

    private EasyRecyclerView mRecyclerView;

    private DownloadAdapter mAdapter;

    private List<DownloadInfo> mDownloadInfos;

    private GidPositionMap mGidPositionMap;

    private int mLastUpdateSize = -1;

    @Override
    protected void onInit() {
        super.onInit();

        mDownloadInfos = DownloadManager.getInstance().getDownloadList(null);
        mGidPositionMap = new GidPositionMap();
        Messenger.getInstance().register(Constants.MESSENGER_ID_UPDATE_DOWNLOAD, this);
    }

    @Override
    protected void onDie() {
        super.onDie();

        Messenger.getInstance().unregister(Constants.MESSENGER_ID_UPDATE_DOWNLOAD, this);
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);
        setContentView(R.layout.scene_download);

        ((ContentActivity) getStageActivity()).setDrawerListActivatedPosition(ContentActivity.DRAWER_LIST_DOWNLOAD);

        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);

        mAdapter = new DownloadAdapter();
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getStageActivity()));
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setHasFixedSize(true);
    }


    @Override
    protected void onResume() {
        super.onResume();

        ((ContentActivity) getStageActivity()).setDrawerListActivatedPosition(ContentActivity.DRAWER_LIST_DOWNLOAD);
    }

    @Override
    public boolean showLeftDrawer() {
        return true;
    }

    @Override
    public void bindRightDrawer(ContentActivity activity) {
        // TODO
        activity.clearRightDrawerView();
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        return false;
    }

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
            int position = getAdapterPosition(v);
            if (position >= 0) {
                // Add to cache
                DownloadInfo info = mDownloadInfos.get(position);
                mGidPositionMap.add(info.galleryBase.gid, position);

                Intent intent = new Intent(getStageActivity(), DownloadService.class);
                intent.setAction(DownloadService.ACTION_STOP);
                intent.putExtra(DownloadService.KEY_GID, info.galleryBase.gid);
                getStageActivity().startService(intent);
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

            int mix = (int) obj;
            int ops = DownloadManager.getOps(mix);
            int gid = DownloadManager.getGid(mix);

            if (ops == DownloadManager.OPS_ALL_CHANGE) {
                mAdapter.notifyDataSetChanged();
                return;
            }

            int position = getPositionForGid(gid);
            if (position == -1 && mLastUpdateSize != mAdapter.getItemCount()) {
                // Can't get position, notifyDataSetChanged for safe
                mAdapter.notifyDataSetChanged();
                return;
            }

            if (ops == DownloadManager.OPS_ADD) {
                mAdapter.notifyItemInserted(position);
            } else if (ops == DownloadManager.OPS_REMOVE) {
                mAdapter.notifyItemRemoved(position);
            } else if (ops == DownloadManager.OPS_UPDATE) {
                RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                if (holder != null) {
                    bindHolder((DownloadHolder) holder, mDownloadInfos.get(position));
                }
            }
        }
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
}
