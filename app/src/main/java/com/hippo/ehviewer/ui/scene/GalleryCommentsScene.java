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

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.LinearDividerItemDecoration;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.rippleold.RippleSalon;
import com.hippo.text.Html;
import com.hippo.text.URLImageGetter;
import com.hippo.ehviewer.OpenUrlHelper;
import com.hippo.util.ReadableTime;
import com.hippo.util.TextUrl;
import com.hippo.vector.VectorDrawable;
import com.hippo.view.ViewTransition;
import com.hippo.widget.LinkifyTextView;
import com.hippo.widget.SimpleImageView;
import com.hippo.yorozuya.LayoutUtils;

public final class GalleryCommentsScene extends ToolbarScene
        implements EasyRecyclerView.OnItemClickListener,
        EasyRecyclerView.OnItemLongClickListener {

    public static final String KEY_COMMENTS = "comments";

    private GalleryComment[] mComments;

    private CommentAdapter mAdapter;
    private ViewTransition mViewTransition;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void handleArgs(Bundle args) {
        if (args == null) {
            return;
        }

        Parcelable[] parcelables = args.getParcelableArray(KEY_COMMENTS);
        if (parcelables instanceof GalleryComment[]) {
            mComments = (GalleryComment[]) parcelables;
        }
    }

    private void onInit() {
        handleArgs(getArguments());
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        Parcelable[] parcelables = savedInstanceState.getParcelableArray(KEY_COMMENTS);
        if (parcelables instanceof GalleryComment[]) {
            mComments = (GalleryComment[]) parcelables;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray(KEY_COMMENTS, mComments);
    }

    @Nullable
    @Override
    public View onCreateView2(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_gallery_comments, container, false);
        EasyRecyclerView recyclerView = (EasyRecyclerView) view.findViewById(R.id.recycler_view);
        View tip = view.findViewById(R.id.tip);
        SimpleImageView tipImage = (SimpleImageView) tip.findViewById(R.id.tip_image);
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);

        mAdapter = new CommentAdapter();
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL, getResources().getColor(R.color.divider),
                LayoutUtils.dp2pix(getContext(), 1));
        decoration.setShowLastDivider(true);
        recyclerView.addItemDecoration(decoration);
        recyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnItemClickListener(this);
        recyclerView.setOnItemLongClickListener(this);

        tipImage.setDrawable(VectorDrawable.create(getContext(), R.drawable.sadpanda_head));

        fab.setImageDrawable(VectorDrawable.create(getContext(), R.drawable.ic_reply));

        mViewTransition = new ViewTransition(recyclerView, tip);

        if (mComments == null || mComments.length <= 0) {
            mViewTransition.showView(1);
        } else {
            mViewTransition.showView(0);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAdapter = null;
        mViewTransition = null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.gallery_comments);
        setNavigationIcon(VectorDrawable.create(getContext(), R.drawable.ic_arrow_left));
    }

    @Override
    public void onNavigationClick() {
        finish();
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
        if (holder instanceof CommentHolder) {
            CommentHolder commentHolder = (CommentHolder) holder;
            ClickableSpan span = commentHolder.comment.getCurrentSpan();
            commentHolder.comment.clearCurrentSpan();

            if (span instanceof URLSpan) {
                OpenUrlHelper.openUrl(getActivity(), ((URLSpan) span).getURL(), true, true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onItemLongClick(EasyRecyclerView parent, View view, int position, long id) {
        return false;
    }

    private class CommentHolder extends RecyclerView.ViewHolder {

        public TextView user;
        public TextView time;
        public LinkifyTextView comment;

        public CommentHolder(View itemView) {
            super(itemView);
            user = (TextView) itemView.findViewById(R.id.user);
            time = (TextView) itemView.findViewById(R.id.time);
            comment = (LinkifyTextView) itemView.findViewById(R.id.comment);
        }
    }

    private class CommentAdapter extends RecyclerView.Adapter<CommentHolder> {

        @Override
        public CommentHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new CommentHolder(getActivity().getLayoutInflater()
                    .inflate(R.layout.item_gallery_comment, parent, false));
        }

        @Override
        public void onBindViewHolder(CommentHolder holder, int position) {
            GalleryComment comment = mComments[position];
            holder.user.setText(comment.user);
            holder.time.setText(ReadableTime.getTimeAgo(comment.time));
            holder.comment.setText(TextUrl.handleTextUrl(Html.fromHtml(comment.comment,
                    new URLImageGetter(holder.comment, EhApplication.getConaco(getContext())), null)));
        }

        @Override
        public int getItemCount() {
            return mComments == null ? 0 : mComments.length;
        }
    }
}
