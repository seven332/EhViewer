package com.hippo.ehviewer.activity;

import java.util.List;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class CommentsSectionFragment extends Fragment {
    
    private static final String TAG = "CommentsSectionFragment";
    
    private MangaDetailActivity mActivity;
    
    private View mRootView;
    private ListView mList;
    private View mWaitView;
    private View mRefreshButton;
    
    private List<Comment> mComments;
    
    private BaseAdapter adapter = new CommentsAdapter();
    
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
            if (convertView == null)
                convertView = LayoutInflater.from(mActivity)
                        .inflate(R.layout.comments_item, null);
            Comment c = mComments.get(position);
            ((TextView)convertView.findViewById(R.id.user)).setText(c.user);
            ((TextView)convertView.findViewById(R.id.time)).setText(c.time);
            ((TextView)convertView.findViewById(R.id.comment)).setText(Html.fromHtml(c.comment));
            return convertView;
        }
    };
}
