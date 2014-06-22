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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.NormalPreviewList;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.ehclient.DetailParser;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.AutoWrapLayout;
import com.hippo.ehviewer.widget.ButtonsDialogBuilder;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.OlScrollView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;
import com.hippo.ehviewer.widget.OnLayoutListener;
import com.hippo.ehviewer.widget.ProgressiveRatingBar;
import com.hippo.ehviewer.widget.SuperToast;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

public class DetailSectionFragment extends Fragment
        implements View.OnClickListener, PreviewList.PreviewHolder {
    private static final String TAG = "DetailSectionFragment";
    
    private AppContext mAppContext;
    private MangaDetailActivity mActivity;
    private EhClient mClient;
    private Data mData;
    private GalleryDetail mGalleryDetail;
    private String mUrl;
    
    private View mRootView;
    private OlScrollView mScrollView;
    private Button mReadButton;
    private Button mRateButton;
    private ProgressBar mWaitPb;
    private Button mRefreshButton;
    private ViewGroup mNormalView;
    private ViewGroup mOffensiveView;
    private ViewGroup mPiningView;
    private ViewGroup mPreviewListMain;
    private AutoWrapLayout mPreviewListLayout;
    private ProgressBar mWaitPreviewList;
    private Button mPreviewRefreshButton;
    private View mBottomPanel;
    private TextView mPreviewNumText;
    private View mCancelButton;
    private View mOnceButton;
    private View mEveryButton;
    private View mBackButton;
    private View mFrontButton;
    private View mKnownButton;
    private View mDivider;
    
    private AlertDialog mGoToDialog;
    private AlertDialog mTagDialog;
    
    private int mCurPage;
    private boolean mShowPreview = false;
    
    private ShapeDrawable getArrowShapeDrawable(int color, boolean isToLeft) {
        Path path = new Path();
        if (isToLeft) {
            path.moveTo(50, 0);
            path.lineTo(50, 100);
            path.lineTo(0, 50);
            path.close();
        } else {
            path.moveTo(0, 0);
            path.lineTo(0, 100);
            path.lineTo(50, 50);
            path.close();
        }
        ShapeDrawable d = new ShapeDrawable(new PathShape(path, 50, 100));
        d.getPaint().setColor(color);
        return d;
    }
    
    private StateListDrawable getArrowClickDrawable(int color, boolean isToLeft) {
        StateListDrawable sld = new StateListDrawable();
        ShapeDrawable normal = getArrowShapeDrawable(color, isToLeft);
        ShapeDrawable grey = getArrowShapeDrawable(Theme.GREY_COLOR, isToLeft);
        ShapeDrawable dark = getArrowShapeDrawable(Theme.getDarkerColor(color), isToLeft);
        sld.addState(new int[]{-android.R.attr.state_enabled}, grey);
        sld.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, dark);
        sld.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, dark);
        sld.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, dark);
        sld.addState(new int[]{}, normal);
        return sld;
    }
    
    @Override
    public int getCurPreviewPage() {
        return mCurPage;
    }
    
    @Override
    public void onGetPreviewImageFailure() {
        mWaitPreviewList.setVisibility(View.GONE);
        mPreviewRefreshButton.setVisibility(View.VISIBLE);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = (MangaDetailActivity)getActivity();
        mAppContext = (AppContext)mActivity.getApplication();
        mClient = mAppContext.getEhClient();
        mData = mAppContext.getData();
        
        GalleryInfo galleryInfo = mActivity.getGalleryInfo();
        mActivity.mGalleryDetail = new GalleryDetail(galleryInfo);
        mGalleryDetail = mActivity.mGalleryDetail;
        mUrl = EhClient.getDetailUrl(
                mGalleryDetail.gid, mGalleryDetail.token);
        
        mRootView = inflater.inflate(
                R.layout.detail, container, false);
        mScrollView = (OlScrollView)mRootView.findViewById(R.id.manga_detail_scrollview);
        mReadButton = (Button)mRootView.findViewById(R.id.detail_read);
        mRateButton = (Button)mRootView.findViewById(R.id.detail_do_rate);
        mWaitPb = (ProgressBar)mRootView.findViewById(R.id.detail_wait);
        mRefreshButton = (Button)mRootView.findViewById(R.id.detail_refresh);
        mNormalView = (ViewGroup)mRootView.findViewById(R.id.manga_detail_normal);
        mOffensiveView = (ViewGroup)mRootView.findViewById(R.id.manga_detail_offensive);
        mPiningView = (ViewGroup)mRootView.findViewById(R.id.manga_detail_pining);
        mPreviewListMain = (ViewGroup)mRootView.findViewById(R.id.page_list);
        mPreviewListLayout = (AutoWrapLayout)mRootView.findViewById(R.id.paper_list_layout);
        mWaitPreviewList = (ProgressBar)mRootView.findViewById(R.id.paper_list_wait);
        mPreviewRefreshButton = (Button)mRootView.findViewById(R.id.preview_button_refresh);
        mBottomPanel = (View)mRootView.findViewById(R.id.bottom_panel);
        mPreviewNumText = (TextView)mBottomPanel.findViewById(R.id.preview_num);
        mCancelButton = mOffensiveView.findViewById(R.id.detail_button_cancel);
        mOnceButton = mOffensiveView.findViewById(R.id.detail_button_once);
        mEveryButton = mOffensiveView.findViewById(R.id.detail_button_every);
        mKnownButton = mPiningView.findViewById(R.id.detail_button_known);
        mBackButton = mPreviewListMain.findViewById(R.id.back);
        mFrontButton = mPreviewListMain.findViewById(R.id.front);
        mDivider = mNormalView.findViewById(R.id.divider);
        
        boolean isRandomColor = Config.getRandomThemeColor();
        int color = Config.getThemeColor();
        // Set random color
        if (isRandomColor)
            color = Theme.getRandomDeepColor();
        mReadButton.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
        mRateButton.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
        if (isRandomColor)
            color = Theme.getRandomDeepColor();
        mRootView.findViewById(R.id.crude_divider).setBackgroundColor(color);
        if (isRandomColor)
            color = Theme.getRandomDeepColor();
        mRefreshButton.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
        mCancelButton.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
        mOnceButton.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
        mEveryButton.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
        mKnownButton.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
        if (isRandomColor)
            color = Theme.getRandomDeepColor();
        mPreviewNumText.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
        mBackButton.setBackgroundDrawable(getArrowClickDrawable(color, true));
        mFrontButton.setBackgroundDrawable(getArrowClickDrawable(color, false));
        
        mRefreshButton.setOnClickListener(this);
        
        FswView align = (FswView)mActivity.findViewById(R.id.alignment);
        int paddingLeft = align.getPaddingLeft();
        int paddingTop = align.getPaddingTop();
        int paddingRight = align.getPaddingRight();
        int paddingBottom = align.getPaddingBottom();
        if (paddingTop != 0 || paddingBottom != 0) {
            mScrollView.setPadding(paddingLeft, paddingTop,
                    paddingRight, paddingBottom);
        }
        align.addOnFitSystemWindowsListener(new OnFitSystemWindowsListener() {
            @Override
            public void onfitSystemWindows(int paddingLeft, int paddingTop,
                    int paddingRight, int paddingBottom) {
                mScrollView.setPadding(paddingLeft, paddingTop,
                        paddingRight, paddingBottom);
            }
        });
        
        mScrollView.setOnLayoutListener(new OnLayoutListener() {
            @Override
            public void onLayout() {
                if (mShowPreview) {
                    mShowPreview = false;
                    mScrollView.scrollTo(0,
                            mNormalView.getTop() + mDivider.getTop());
                }
            }
        });
        
        LoadImageView thumb = (LoadImageView)mRootView.findViewById(R.id.detail_cover);
        thumb.setImageDrawable(null);
        thumb.setLoadInfo(mGalleryDetail.thumb, String.valueOf(mGalleryDetail.gid));
        ImageLoader.getInstance(mActivity).add(mGalleryDetail.thumb, String.valueOf(mGalleryDetail.gid),
                new LoadImageView.SimpleImageGetListener(thumb).setTransitabled(false));
        
        TextView title = (TextView)mRootView.findViewById(R.id.detail_title);
        title.setText(mGalleryDetail.title);
        
        TextView uploader = (TextView)mRootView.findViewById(R.id.detail_uploader);
        uploader.setText(mGalleryDetail.uploader);
        
        TextView category = (TextView)mRootView.findViewById(R.id.detail_category);
        category.setText(Ui.getCategoryText(mGalleryDetail.category));
        category.setBackgroundColor(Ui.getCategoryColor(mGalleryDetail.category));
        
        // Make rate and read button same width
        // Disable them for temp
        Ui.measureView(mReadButton);
        Ui.measureView(mRateButton);
        int readbw = mReadButton.getMeasuredWidth();
        int ratebw = mRateButton.getMeasuredWidth();
        if (readbw > ratebw) {
            mRateButton.setWidth(readbw);
        } else if (ratebw > readbw) {
            mReadButton.setWidth(ratebw);
        }
        mReadButton.setEnabled(false);
        mRateButton.setEnabled(false);
        
        mClient.getGDetail(mUrl, mGalleryDetail, new GDetailGetListener());
        
        return mRootView;
    }
    
    private void refreshPageList() {
        mPreviewListLayout.removeAllViews();
        mPreviewNumText.setText(String.format("%d / %d",
                mCurPage+1, mGalleryDetail.previewSum));
        
        PreviewList pageList = mGalleryDetail.previewLists[mCurPage];
        
        if (pageList == null) {
            // Update layout
            mWaitPreviewList.setVisibility(View.VISIBLE);
            mPreviewRefreshButton.setVisibility(View.GONE);
            
            mUrl = EhClient.getDetailUrl(
                    mGalleryDetail.gid, mGalleryDetail.token, mCurPage);
            mClient.getPreviewList(mUrl, mCurPage,
                    new EhClient.OnGetPreviewListListener() {
                        @Override
                        public void onSuccess(Object checkFlag, NormalPreviewList pageList) {
                            if (mActivity.isFinishing())
                                return;
                            
                            mWaitPreviewList.setVisibility(View.GONE);
                            mPreviewRefreshButton.setVisibility(View.GONE);
                            
                            int page = (Integer)checkFlag;
                            mGalleryDetail.previewLists[page] = pageList;
                            mGalleryDetail.previewLists[page].setData(DetailSectionFragment.this, mActivity, mGalleryDetail, page);
                            mGalleryDetail.previewLists[page].addPreview(mPreviewListLayout);
                            
                            
                            mShowPreview = true;
                        }

                        @Override
                        public void onFailure(Object checkFlag, String eMsg) {
                            if (mActivity.isFinishing())
                                return;
                            
                            int page = (Integer)checkFlag;
                            if (page == mCurPage) {
                                new SuperToast(mActivity).setIcon(R.drawable.ic_warning)
                                        .setMessage(eMsg)
                                        .show();
                                mWaitPreviewList.setVisibility(View.GONE);
                                mPreviewRefreshButton.setVisibility(View.VISIBLE);    
                            }
                        }
                    });
        } else {
            // Update layout
            mWaitPreviewList.setVisibility(View.GONE);
            mPreviewRefreshButton.setVisibility(View.GONE);
            
            mGalleryDetail.previewLists[mCurPage].addPreview(mPreviewListLayout);
            mShowPreview = true;
        }
    }
    
    private AlertDialog createGoToDialog() {
        return new DialogBuilder(mActivity).setTitle(R.string.jump)
                .setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return mGalleryDetail.previewSum;
                    }
                    @Override
                    public Object getItem(int position) {
                        return position;
                    }
                    @Override
                    public long getItemId(int position) {
                        return position;
                    }
                    @Override
                    public View getView(int position, View convertView,
                            ViewGroup parent) {
                        LayoutInflater inflater = mActivity.getLayoutInflater();
                        View view = (View)inflater.inflate(R.layout.list_item_text, null);
                        TextView tv = (TextView)view.findViewById(android.R.id.text1);
                        tv.setText(String.format(getString(R.string.some_page), position + 1));
                        return tv;
                    }
                }, new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int position, long arg3) {
                        mGoToDialog.dismiss();
                        if (position != mCurPage) {
                            mCurPage = position;
                            refreshPageList();
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View paramView) {
                        mGoToDialog.dismiss();
                    }
                }).create();
    }
    
    @SuppressWarnings("deprecation")
    private void addTags() {
        LinearLayout tagsLayout = (LinearLayout)mRootView.findViewById(R.id.tags_layout);
        LinkedHashMap<String, LinkedList<SimpleEntry<String, Integer>>> tagGroups = 
                mGalleryDetail.tags;
        tagsLayout.removeAllViews();
        int x = Ui.dp2pix(4);
        int y = Ui.dp2pix(4);
        Resources resources = getResources();
        // color
        boolean isRandomColor = Config.getRandomThemeColor();
        int color = Config.getThemeColor();
        
        // Get tag view resources
        int tagTextSize = resources.getDimensionPixelOffset(R.dimen.button_small_size);
        ColorStateList tagTextColor = resources.getColorStateList(R.color.blue_bn_text);
        int tagPaddingX = resources.getDimensionPixelOffset(R.dimen.button_tag_padding_x);
        int tagPaddingY = resources.getDimensionPixelOffset(R.dimen.button_tag_padding_y);
        for (Entry<String, LinkedList<SimpleEntry<String, Integer>>> tagGroup : tagGroups.entrySet()) {
            LinearLayout tagGroupLayout = new LinearLayout(mActivity);
            tagGroupLayout.setOrientation(LinearLayout.HORIZONTAL);
            AutoWrapLayout tagLayout = new AutoWrapLayout(mActivity);
            
            // Group name
            final String groupName = tagGroup.getKey();
            TextView groupNameView = new TextView(new ContextThemeWrapper(mActivity, R.style.TextTag));
            groupNameView.setText(groupName);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.setMargins(x, y, x, y);
            tagGroupLayout.addView(groupNameView, lp);
            
            // tags
            // get random color
            if (isRandomColor)
                color = Theme.getRandomDeepColor();
            for (SimpleEntry<String, Integer> tag : tagGroup.getValue()) {
                final String tagText = tag.getKey();
                Button tagView = new Button(mActivity);
                tagView.setTextSize(TypedValue.COMPLEX_UNIT_PX, tagTextSize);
                tagView.setText(String.format("%s (%d)", tagText, tag.getValue()));
                tagView.setTextColor(tagTextColor);
                tagView.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
                tagView.setPadding(tagPaddingX, tagPaddingY, tagPaddingX, tagPaddingY);
                tagView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertButton voteUp = new AlertButton(mActivity);
                        voteUp.setText(getString(R.string.vote_up));
                        voteUp.setOnClickListener(new SimpleVote(groupName, tagText, true));
                        AlertButton voteDown = new AlertButton(mActivity);
                        voteDown.setText(getString(R.string.vote_down));
                        voteDown.setOnClickListener(new SimpleVote(groupName, tagText, false));
                        AlertButton showTagged = new AlertButton(mActivity);
                        showTagged.setText(getString(R.string.show_tagged));
                        showTagged.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mTagDialog.dismiss();
                                Intent intent = new Intent();
                                intent.putExtra(MangaListActivity.KEY_MODE,
                                        MangaListActivity.MODE_TAG);
                                intent.putExtra(MangaListActivity.KEY_GROUP, groupName);
                                intent.putExtra(MangaListActivity.KEY_TAG, tagText);
                                mActivity.setResult(Activity.RESULT_OK, intent);  
                                mActivity.finish();
                            }
                        });
                        mTagDialog = new ButtonsDialogBuilder(mActivity).setTitle(tagText)
                                .addButton(voteUp).addButton(voteDown).addButton(showTagged)
                                .create();
                        mTagDialog.show();
                    }
                });
                AutoWrapLayout.LayoutParams alp = new AutoWrapLayout.LayoutParams();
                alp.setMargins(x, y, x, y);
                tagLayout.addView(tagView, alp);
            }
            tagGroupLayout.addView(tagLayout);
            tagsLayout.addView(tagGroupLayout);
        }
        
        // Add tag
        if (isRandomColor)
            color = Theme.getRandomDeepColor();
        Button addtagView = new Button(mActivity);
        addtagView.setTextSize(TypedValue.COMPLEX_UNIT_PX, tagTextSize);
        addtagView.setText("+");
        addtagView.setTextColor(tagTextColor);
        addtagView.setBackgroundDrawable(Theme.getClickDrawable(mActivity, color));
        addtagView.setPadding(tagPaddingX, tagPaddingY, tagPaddingX, tagPaddingY);
        addtagView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
                new SuperToast(mActivity)
                        .setMessage(R.string.unfinished)
                        .show();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.RIGHT;
        lp.setMargins(x, y, x, y);
        tagsLayout.addView(addtagView, lp);
    }
    
    private void layout(GalleryDetail md) {
        if (mActivity.isFinishing())
            return;
            
        // Delete progress bar
        mWaitPb.setVisibility(View.GONE);

        // Check offensive or not
        if (md.firstPage.equals("offensive")) {
            mOffensiveView.setVisibility(View.VISIBLE);
            mCancelButton.setOnClickListener(this);
            mOnceButton.setOnClickListener(this);
            mEveryButton.setOnClickListener(this);
        }
        // Check pining or not
        else if (md.firstPage.equals("pining")) {
            mPiningView.setVisibility(View.VISIBLE);
            mKnownButton.setOnClickListener(this);
        }
        else {
            // Enable button
            mReadButton.setEnabled(true);
            mReadButton.setOnClickListener(this);
            mRateButton.setEnabled(true);
            mRateButton.setOnClickListener(this);
            
            RatingBar rate = (RatingBar)mRootView.findViewById(R.id.detail_rate);
            rate.setRating(mGalleryDetail.rating);
            
            TextSwitcher averagePeople = (TextSwitcher)mRootView.findViewById(R.id.detail_average_people);
            averagePeople.setFactory(new ViewFactory() {
                @Override
                public View makeView() {
                    LayoutInflater inflater = LayoutInflater.from(mActivity);
                    return inflater.inflate(R.layout.detail_avg_text, null);
                }
            });
            averagePeople.setCurrentText(String.format("%.2f (%d)",
                    mGalleryDetail.rating, mGalleryDetail.people));
            
            TextView posted = (TextView)mRootView.findViewById(R.id.detail_posted);
            posted.setText(mGalleryDetail.posted);
            
            TextView language = (TextView)mRootView.findViewById(R.id.detail_language);
            language.setText(String.format(getString(R.string.detail_language), mGalleryDetail.language));
            
            TextView pagesSize = (TextView)mRootView.findViewById(R.id.detail_pages_size);
            pagesSize.setText(String.format(getString(R.string.detail_pages_size),
                    mGalleryDetail.pages, mGalleryDetail.size));
            
            addTags();
            
            mNormalView.setVisibility(View.VISIBLE);
            
            // paper list
            if (mGalleryDetail.previewLists[0] != null) {
                mPreviewListMain.setVisibility(View.VISIBLE);
                // preview num
                mBottomPanel.setVisibility(View.VISIBLE);
                mPreviewNumText.setText(String.format("%d / %d",
                        1, mGalleryDetail.previewSum));
                mPreviewRefreshButton.setOnClickListener(this);
                
                // Init go to dialog or disable it
                if (mGalleryDetail.previewSum > 1) {
                    mGoToDialog = createGoToDialog();
                    mPreviewNumText.setOnClickListener(this);
                    mBackButton.setOnClickListener(this);
                    mFrontButton.setOnClickListener(this);
                } else {
                    mPreviewNumText.setClickable(false);
                    mBackButton.setClickable(false);
                    mFrontButton.setClickable(false);
                }
                
                mCurPage = 0;
                mGalleryDetail.previewLists[0].setData(this, mActivity, mGalleryDetail, 0);
                mGalleryDetail.previewLists[0].addPreview(mPreviewListLayout);
            } else
                new SuperToast(mActivity)
                        .setMessage(R.string.detail_preview_error)
                        .show();
            
            // Set comments
            mActivity.setComments(mGalleryDetail.comments);
        }
    }
    
    @Override
    public void onClick(View v) {
       if (v == mRateButton) {
           rate(mGalleryDetail.rating);
           
       } else if (v == mReadButton) {
           mData.addRead(mGalleryDetail);
           
           Intent intent = new Intent(mActivity,
                   MangaActivity.class);
           intent.putExtra("url", mGalleryDetail.firstPage);
           intent.putExtra("gid", mGalleryDetail.gid);
           intent.putExtra("title", mGalleryDetail.title);
           intent.putExtra("firstPage", 0);
           intent.putExtra("pageSum", mGalleryDetail.pages);
           startActivity(intent);
           
       } else if (v == mRefreshButton) {
           GDetailGetListener listener = new GDetailGetListener();
           mClient.getGDetail(mUrl, mGalleryDetail, listener);
           // Delete refresh button
           mRefreshButton.setVisibility(View.GONE);
           // Add progressBar
           mWaitPb.setVisibility(View.VISIBLE);
           
       } else if (v == mCancelButton
               | v == mKnownButton) {
           mActivity.finish();
           
       } else if (v == mOnceButton) {
           // TODO create a class uri
           GDetailGetListener listener = new GDetailGetListener();
           mClient.getGDetail(mUrl + "&nw=session", mGalleryDetail, listener);
           // Delete offensiveView
           mOffensiveView.setVisibility(View.GONE);
           // Add progressBar
           mWaitPb.setVisibility(View.VISIBLE);
           
       } else if (v == mEveryButton) {
           GDetailGetListener listener = new GDetailGetListener();
           mClient.getGDetail(mUrl + "&nw=always", mGalleryDetail, listener);
           // Delete offensiveView
           mOffensiveView.setVisibility(View.GONE);
           // Add progressBar
           mWaitPb.setVisibility(View.VISIBLE);
           
       } else if (v == mBackButton) {
           if (mCurPage <= 0)
               return;
           mCurPage--;
           refreshPageList();
           
       } else if (v == mFrontButton) {
           if (mCurPage >= mGalleryDetail.previewSum - 1)
               return;
           mCurPage++;
           refreshPageList();
           
       } else if (v == mPreviewRefreshButton) {
           refreshPageList();
           
       } else if (v == mPreviewNumText) {
           mGoToDialog.show();
       }
    }
    
    private int getSendableRating(float ratingFloat) {
        float dr = ratingFloat*2;        
        return (int)(dr + 0.5);
    }
    
    public void rate(float defaultRating) {
        View view = mActivity.getLayoutInflater().inflate(R.layout.rate, null);
        final TextView tv = (TextView)view.findViewById(R.id.rate_text);
        final ProgressiveRatingBar rb = (ProgressiveRatingBar)view.findViewById(R.id.rate);
        rb.setOnDrawListener(new ProgressiveRatingBar.OnDrawListener() {
            @Override
            public void onDraw(float rating) {
                int textId = getResources().getIdentifier(mAppContext.getPackageName()
                        + ":string/rating" + getSendableRating(rating), null, null);
                if (textId == 0)
                    textId = R.string.rating0;
                tv.setText(textId);
            }
        });
        rb.setRating(defaultRating);
        new DialogBuilder(mActivity).setTitle(R.string.rate)
                .setView(view, true)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int rating = getSendableRating(rb.getRating());
                        if (rating <= 0 || rating > 10)
                            new SuperToast(mActivity)
                                    .setIcon(R.drawable.ic_warning)
                                    .setMessage(R.string.invalid_rating)
                                    .show();
                        else {
                            ((AlertButton)v).dialog.dismiss();
                            mClient.rate(mGalleryDetail.gid, mGalleryDetail.token,
                                    rating, new EhClient.OnRateListener() {
                                        @Override
                                        public void onSuccess(float ratingAvg,
                                                int ratingCnt) {
                                            mGalleryDetail.rating = ratingAvg;
                                            mGalleryDetail.people = ratingCnt;
                                            if (!mActivity.isFinishing()) {
                                                //Reset start
                                                RatingBar rate = (RatingBar)mRootView.findViewById(R.id.detail_rate);
                                                rate.setRating(mGalleryDetail.rating);
                                                
                                                TextSwitcher averagePeople = (TextSwitcher)
                                                        mRootView.findViewById(R.id.detail_average_people);
                                                averagePeople.setText(String.format("%.2f (%d)",
                                                        mGalleryDetail.rating, mGalleryDetail.people));
                                            }
                                            new SuperToast(mActivity).setMessage(R.string.rate_succeeded).show();
                                        }
                                        @Override
                                        public void onFailure(String eMsg) {
                                            new SuperToast(mActivity).setIcon(R.drawable.ic_warning)
                                                    .setMessage(getString(R.string.rate_failed) + "\n" + eMsg)
                                                    .show();
                                        }
                            });
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, new OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                    }
                }).create().show();
    }
    
    
    
    public List<Comment> getComments() {
        if (mGalleryDetail == null)
            return null;
        else
            return mGalleryDetail.comments;
    }
    
    private class GDetailGetListener
            implements EhClient.OnGetGDetailListener {
        @Override
        public void onSuccess(GalleryDetail md) {
            layout(md);
        }
        
        @Override
        public void onFailure(String eMsg) {
            // Delete progress bar
            // Show refresh button
            mWaitPb.setVisibility(View.GONE);
            mRefreshButton.setVisibility(View.VISIBLE);
            new SuperToast(mActivity).setIcon(R.drawable.ic_warning).setMessage(eMsg).show();
        }
    }
    
    private class SimpleVote implements View.OnClickListener {
        
        private String groupName;
        private String tagText;
        private boolean isUp;
        
        public SimpleVote(String groupName, String tagText, boolean isUp) {
            this.groupName = groupName;
            this.tagText = tagText;
            this.isUp = isUp;
        }
        
        @Override
        public void onClick(View v) {
            ((AlertButton)v).dialog.dismiss();
            mClient.vote(mGalleryDetail.gid, mGalleryDetail.token,
                    groupName, tagText, isUp, new SimpleVoteListener());
        }
    }
    
    private class SimpleVoteListener implements EhClient.OnVoteListener {
        @Override
        public void onSuccess(String tagPane) {
            new SuperToast(mActivity).setMessage(R.string.vote_succeeded).show();
            
            DetailParser parser = new DetailParser();
            parser.setMode(DetailParser.TAG);
            if (parser.parser(tagPane) == DetailParser.TAG) {
                mGalleryDetail.tags = parser.tags;
                addTags();
            } else {
                new SuperToast(mActivity).setIcon(R.drawable.ic_warning).setMessage(R.string.em_parser_error).show();
            }
        }

        @Override
        public void onFailure(String eMsg) {
            new SuperToast(mActivity).setIcon(R.drawable.ic_warning).setMessage(getString(R.string.vote_failed) + "\n"+ eMsg).show();
        }
    }
}
