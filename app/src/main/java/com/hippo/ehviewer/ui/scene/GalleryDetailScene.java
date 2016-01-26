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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.easyrecyclerview.SimpleHolder;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.utils.ApiHelper;
import com.hippo.view.ViewTransition;
import com.hippo.widget.LoadImageView;

// TODO Update drawer checked item
public class GalleryDetailScene extends BaseScene {

    public final static String KEY_ACTION = "action";
    public static final String ACTION_GALLERY_INFO = "action_gallery_info";
    public static final String ACTION_GID_TOKEN = "action_gid_token";

    public static final String KEY_GALLERY_INFO = "gallery_info";
    public static final String KEY_GID = "gid";
    public static final String KEY_TOKEN = "token";

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_BUTTON = 1;
    public static final int TYPE_INFO = 2;
    public static final int TYPE_ACTION = 3;
    public static final int TYPE_TAG = 4;
    public static final int TYPE_COMMENT = 5;
    public static final int TYPE_PREVIEW = 6;
    public static final int TYPE_PROGRESS = 7;

    private EhClient mClient;

    private RecyclerView mRecyclerView;
    private View mMainProgressView;
    private ViewGroup mFailedView;
    private TextView mFailedText;
    private ViewTransition mViewTransition;

    // Header
    private View mHeader;
    private LoadImageView mThumb;
    private TextView mTitle;
    private TextView mUploader;
    private TextView mCategory;

    private String mAction;
    private GalleryInfo mGalleryInfo;
    private int mGid;
    private String mKey;

    public GalleryDetailScene() {

    }

    private void handleArgs(Bundle args) {
        if (args == null) {
            return;
        }

        String action = args.getString(KEY_ACTION);
        mAction = action;
        if (ACTION_GALLERY_INFO.equals(action)) {
            mGalleryInfo = args.getParcelable(KEY_GALLERY_INFO);
        } else if (ACTION_GID_TOKEN.equals(action)) {
            mGid = args.getInt(KEY_GID);
            mKey = args.getString(KEY_TOKEN);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClient = EhApplication.getEhClient(getContext());

        handleArgs(getArguments());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void bindView() {
        int gid = -1;
        boolean notFound = true;
        if (ACTION_GALLERY_INFO.equals(mAction) && mGalleryInfo != null) {
            GalleryInfo gi = mGalleryInfo;
            gid = gi.gid;
            mThumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb, true);
            mTitle.setText(gi.title);
            mUploader.setText(gi.uploader);
            mCategory.setText(EhUtils.getCategory(gi.category));
            mCategory.setTextColor(EhUtils.getCategoryColor(gi.category));
            notFound = false;
        } else if (ACTION_GID_TOKEN.equals(mAction)) {
            gid = mGid;
            // TODO
        }

        if (ApiHelper.SUPPORT_TRANSITION && gid != -1) {
            mThumb.setTransitionName(TransitionNameFactory.getThumbTransitionName(gid));
            mTitle.setTransitionName(TransitionNameFactory.getTitleTransitionName(gid));
            mUploader.setTransitionName(TransitionNameFactory.getUploaderTransitionName(gid));
            mCategory.setTransitionName(TransitionNameFactory.getCategoryTransitionName(gid));
        }

        if (notFound) {
            // TODO Show not found error
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_gallery_detail, container, false);

        ViewGroup main = (ViewGroup) view.findViewById(R.id.main);
        mRecyclerView = (RecyclerView) main.findViewById(R.id.recycler_view);
        mMainProgressView = main.findViewById(R.id.progress_view);
        mFailedView = (ViewGroup) main.findViewById(R.id.tip);
        mFailedText = (TextView) mFailedView.getChildAt(1);
        mViewTransition = new ViewTransition(mRecyclerView, mMainProgressView, mFailedView);

        mRecyclerView.setAdapter(new GalleryDetailAdapter());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mHeader = inflater.inflate(R.layout.gallery_detail_header, mRecyclerView, false);
        mThumb = (LoadImageView) mHeader.findViewById(R.id.thumb);
        mTitle = (TextView) mHeader.findViewById(R.id.title);
        mUploader = (TextView) mHeader.findViewById(R.id.uploader);
        mCategory = (TextView) mHeader.findViewById(R.id.category);

        bindView();

        return view;
    }

    private class GalleryDetailAdapter extends RecyclerView.Adapter<SimpleHolder> {

        @Override
        public SimpleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case TYPE_HEADER:
                    return new SimpleHolder(mHeader);
                /*
                case TYPE_BUTTON:
                    return new SimpleHolder(mButton);
                case TYPE_INFO:
                    return new SimpleHolder(mInfo);
                case TYPE_ACTION:
                    return new SimpleHolder(mAction);
                case TYPE_TAG:
                    return new SimpleHolder(mTag);
                case TYPE_COMMENT:
                    return new SimpleHolder(mComment);
                case TYPE_PREVIEW:
                    return new SimpleHolder(mPreview);
                case TYPE_PROGRESS:
                    return new SimpleHolder(mProgress);
                    */
                default:
                    throw new IllegalStateException("Unknown type " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(SimpleHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int position) {
            return TYPE_HEADER; // TODO
        }
    }
}
