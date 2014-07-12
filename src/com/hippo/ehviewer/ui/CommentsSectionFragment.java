/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

import java.util.List;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.LinkifyTextView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;
import com.hippo.ehviewer.widget.SuperToast;

public class CommentsSectionFragment extends Fragment
        implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = "CommentsSectionFragment";

    private MangaDetailActivity mActivity;

    private View mRootView;
    private ListView mList;
    private View mWaitView;
    private View mRefreshButton;

    private List<Comment> mComments;

    private final BaseAdapter adapter = new CommentsAdapter();

    private AlertDialog mLongClickDialog;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mActivity = (MangaDetailActivity)getActivity();

        mRootView = inflater.inflate(
                R.layout.comments, container, false);
        mList = (ListView)mRootView.findViewById(R.id.commtens_list);
        mWaitView = mRootView.findViewById(R.id.commtens_wait);
        mRefreshButton = mRootView.findViewById(R.id.commtens_refresh);

        //mRefreshButton.setOnClickListener(this);

        FswView align = (FswView)mActivity.findViewById(R.id.alignment);
        int paddingLeft = align.getPaddingLeft();
        int paddingTop = align.getPaddingTop();
        int paddingRight = align.getPaddingRight();
        int paddingBottom = align.getPaddingBottom();
        if (paddingTop != 0 || paddingBottom != 0) {
            mList.setPadding(paddingLeft, paddingTop,
                    paddingRight, paddingBottom);
        }
        align.addOnFitSystemWindowsListener(new OnFitSystemWindowsListener() {
            @Override
            public void onfitSystemWindows(int paddingLeft, int paddingTop,
                    int paddingRight, int paddingBottom) {
                mList.setPadding(paddingLeft, paddingTop,
                        paddingRight, paddingBottom);
            }
        });

        mList.setAdapter(adapter);
        mList.setOnItemClickListener(this);
        mList.setOnItemLongClickListener(this);

        mComments = mActivity.getComments();
        if (mComments != null) {
            adapter.notifyDataSetChanged();
            mWaitView.setVisibility(View.GONE);
        }

        return mRootView;
    }

    public void setComments(List<Comment> comments) {
        mComments = comments;
        adapter.notifyDataSetChanged();
        if (mWaitView != null)
            mWaitView.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        // Handler url here
        LinkifyTextView comment = ((LinkifyTextView)view.findViewById(R.id.comment));
        String url = comment.getTouchedUrl();
        if (url != null) {
            comment.clearTouchedUrl();
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        final Comment c = (Comment)parent.getItemAtPosition(position);

        mLongClickDialog = new DialogBuilder(mActivity)
                .setTitle(R.string.what_to_do).setItems(R.array.comment_long_click,
                        new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        mLongClickDialog.dismiss();
                        switch (position) {
                        case 0:
                            ClipboardManager cm = (ClipboardManager)mActivity
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(ClipData.newPlainText(null, c.comment));
                            new SuperToast(mActivity).setMessage(R.string.copyed).show();
                            break;

                        case 1:
                            mActivity.finish();
                            Intent intent = new Intent(mActivity, MangaListActivity.class);
                            intent.setAction(MangaListActivity.ACTION_GALLERY_LIST);
                            intent.putExtra(MangaListActivity.KEY_MODE, ListUrls.UPLOADER);
                            intent.putExtra(MangaListActivity.KEY_UPLOADER, c.user);
                            startActivity(intent);
                            break;
                        }
                    }
                }).setNegativeButton(android.R.string.cancel,
                        new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mLongClickDialog.dismiss();
                    }
                }).create();
        mLongClickDialog.show();
        return true;
    }

    private class CommentsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (mComments == null)
                return 0;
            else
                return mComments.size();
        }

        @Override
        public Object getItem(int position) {
            return mComments.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            boolean isUserSame = true;
            boolean isTimeSame = true;

            if (convertView == null) {
                convertView = LayoutInflater.from(mActivity)
                        .inflate(R.layout.comments_item, null);
            }
            Comment c = mComments.get(position);

            TextView user = (TextView)convertView.findViewById(R.id.user);
            if (!c.user.equals(user.getText())) {
                isUserSame = false;
                user.setText(c.user);
            }

            TextView time = (TextView)convertView.findViewById(R.id.time);
            if (!c.time.equals(time.getText())) {
                isTimeSame = false;
                time.setText(c.time);
            }

            // No one can post one comment at same this time
            if (!isUserSame || !isTimeSame) {
                TextView comment = ((TextView)convertView.findViewById(R.id.comment));
                comment.setText(Html.fromHtml(c.comment));
            }
            return convertView;
        }
    }
}
