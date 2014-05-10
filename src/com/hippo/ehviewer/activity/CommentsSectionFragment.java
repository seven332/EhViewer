package com.hippo.ehviewer.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;

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
import android.widget.Toast;

public class CommentsSectionFragment extends Fragment
        implements AdapterView.OnItemLongClickListener {
    
    @SuppressWarnings("unused")
    private static final String TAG = "CommentsSectionFragment";
    
    private MangaDetailActivity mActivity;
    
    private View mRootView;
    private ListView mList;
    private View mWaitView;
    private View mRefreshButton;
    
    private List<Comment> mComments;
    
    private BaseAdapter adapter = new CommentsAdapter();
    
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
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        final Comment c = (Comment)parent.getItemAtPosition(position);
        final List<String> urls = new ArrayList<String>();
        urls.add(getString(android.R.string.copy));
        urls.add("该用户上传的其他内容");
        
        Pattern p = Pattern.compile("[a-zA-Z]+://[^\\s<>\"]*");
        Matcher m = p.matcher(c.comment);
        
        while (m.find()) {
            String url = m.group();
            if (!urls.contains(url))
                urls.add(url);
        }
        
        // Long Click dialog
        mLongClickDialog = new DialogBuilder(mActivity)
                .setTitle(R.string.what_to_do)
                .setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return urls.size();
                    }
                    @Override
                    public Object getItem(int position) {
                        return urls.get(position);
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View convertView,
                            ViewGroup parent) {
                        LayoutInflater inflater = LayoutInflater.from(mActivity);
                        if (position < 2) {
                            convertView = inflater.inflate(R.layout.list_item_text, null);
                        } else {
                            convertView = inflater.inflate(R.layout.list_url_text, null);
                        }
                        ((TextView)convertView.findViewById(
                                android.R.id.text1)).setText(urls.get(position));
                        return convertView;
                    }
                    
                }, new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        if (position == 0) {
                            ClipboardManager cm = (ClipboardManager)mActivity
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(ClipData.newPlainText(null, c.comment));
                            Toast.makeText(mActivity, "已复制", Toast.LENGTH_SHORT).show(); // TODO
                        } else if (position == 1){
                            
                        } else if (position > 1) {
                            String str = (String)parent.getItemAtPosition(position);
                            Uri uri = Uri.parse(str);
                            Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                            startActivity(intent);
                        }
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
            
            if (convertView == null)
                convertView = LayoutInflater.from(mActivity)
                        .inflate(R.layout.comments_item, null);
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
            if (!isUserSame || !isTimeSame)
                ((TextView)convertView.findViewById(R.id.comment))
                        .setText(Html.fromHtml(c.comment));
                
            return convertView;
        }
    }
}
