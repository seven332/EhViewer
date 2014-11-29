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
import java.util.List;
import java.util.Map.Entry;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.cache.ImageCache;
import com.hippo.ehviewer.data.ApiGalleryDetail;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.ListUrls;
import com.hippo.ehviewer.data.LofiDetailImpl;
import com.hippo.ehviewer.data.LofiGalleryDetail;
import com.hippo.ehviewer.data.LofiGalleryInfo;
import com.hippo.ehviewer.data.PreviewImpl;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable.Stroke;
import com.hippo.ehviewer.drawable.OvalDrawable;
import com.hippo.ehviewer.ehclient.DetailUrlParser;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.ViewUtils;
import com.hippo.ehviewer.widget.AutoWrapLayout;
import com.hippo.ehviewer.widget.FitWindowView;
import com.hippo.ehviewer.widget.FloatingActionButton;
import com.hippo.ehviewer.widget.LinkifyTextView;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.MaterialProgress;
import com.hippo.ehviewer.widget.MaterialToast;
import com.hippo.ehviewer.widget.ProgressiveRatingBar;
import com.hippo.ehviewer.widget.RefreshTextView;
import com.hippo.ehviewer.widget.ResponedScrollView;
import com.hippo.ehviewer.widget.SimpleGridLayout;
import com.hippo.ehviewer.widget.SuperButton;
import com.hippo.ehviewer.windowsanimate.WindowsAnimate;

public class GalleryDetailActivity extends AbsTranslucentActivity
        implements View.OnClickListener,
        ResponedScrollView.OnScrollStateChangedListener,
        View.OnTouchListener , ViewSwitcher.ViewFactory,
        ProgressiveRatingBar.OnUserRateListener, PreviewList.PreviewHolder,
        View.OnLayoutChangeListener, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, RefreshTextView.OnRefreshListener,
        FitWindowView.OnFitSystemWindowsListener {

    @SuppressWarnings("unused")
    private static final String TAG = GalleryDetailActivity.class.getSimpleName();

    public static final String KEY_G_INFO = "gallery_info";

    private AppContext mAppContext;
    private Resources mResources;
    private EhClient mClient;
    private WindowsAnimate mWindowsAnimate;
    private GalleryInfo mGalleryInfo;

    private int mRunningAnimateNum = 0;
    private boolean isCheckmark = false;
    private boolean isSetDrawable = false;

    private FitWindowView mStandard;
    private RefreshTextView mRefreshText;
    private LoadImageView mThumb;
    private TextView mTitle;
    private TextView mUploader;
    private SuperButton mDownloadButton;
    private SuperButton mReadButton;
    private TextView mCategory;
    private TextView mMoreOfUploader;
    private TextView mSimilar;
    private TextSwitcher mRatingText;
    private ProgressiveRatingBar mRating;
    private TextView mFavorite;
    private TextView mRate;
    private TextView mCommentMoreText;
    private SimpleGridLayout mPreview;
    private SuperButton mPreviewPage;
    private SuperButton mPreviewBack;
    private SuperButton mPreviewFront;
    private MaterialProgress mPreviewWait;
    private SuperButton mPreviewRefresh;

    private ResponedScrollView mDetailScroll;
    private ScrollView mMoreDetailScroll;
    private View mMoreComment;

    private View mDetailHeader;
    private View mDetailButtons;
    private TextView mWarning;
    private View mDetailActions;
    private View mDividerAM;
    private View mDetailMore;
    private View mDividerMR;
    private View mDetailRate;
    private View mDividerRT;
    private LinearLayout mDetailTag;
    private View mDividerTC;
    private LinearLayout mDetailComment;
    private View mDividerCP;
    private LinearLayout mDetailPreview;

    private ListView mCommentList;
    private FloatingActionButton mReply;

    private OvalDrawable mCategoryDrawable;
    private Drawable mRateDrawable;
    private Drawable mCheckmarkDrawable;

    private CommentAdapter mCommentAdapter;

    private int mPaddingTop = -1;
    private int mThemeColor;
    private int mCurPreviewPage = 0;
    private boolean mShowPreview = false;
    private boolean mGetPreview = false;

    private String mOldReply;
    private boolean mReplyPositive;

    private AlertDialog createGoToDialog() {
        CharSequence[] items = new CharSequence[((PreviewImpl)mGalleryInfo).getPreviewPageNum()];
        for (int i = 0; i < items.length; i++)
            items[i] = Integer.toString(i + 1);

        // TODO too slow

        return new MaterialAlertDialog.Builder(this).setTitle(R.string.jump)
                .setNegativeButton(android.R.string.cancel)
                .setItems(items, new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int position) {
                        if (position != mCurPreviewPage) {
                            mCurPreviewPage = position;
                            refreshPreview();
                        }
                        return true;
                    }
                }).create();
    }

    private GalleryInfo handleIntent(Intent intent) {
        GalleryInfo gi = null;
        if (intent.getAction() == "android.intent.action.VIEW") {
            // Start from url
            DetailUrlParser parser = new DetailUrlParser();
            if (parser.parser(intent.getData().getPath())) {
                if (Config.getMode() == EhClient.MODE_LOFI) {
                    // We can't get info from lofi detail, we need get info from api
                    gi = new ApiGalleryDetail();
                    gi.gid = parser.gid;
                    gi.token = parser.token;
                } else {
                    gi = new GalleryDetail();
                    gi.gid = parser.gid;
                    gi.token = parser.token;
                }
            } else {
                // If can not parser url, just keep it null
            }
        } else {
            //
            gi = (GalleryInfo)(intent.getParcelableExtra(KEY_G_INFO));
            if (gi != null) {
                // Add history
                Data.getInstance().addHistory(gi, Data.BROWSE);

                if (gi instanceof LofiGalleryInfo) {
                    if (Config.getMode() == EhClient.MODE_LOFI) {
                        gi = new LofiGalleryDetail((LofiGalleryInfo)gi);
                    } else {
                        gi = new GalleryDetail(gi);
                    }
                } else {
                    if (Config.getMode() == EhClient.MODE_LOFI) {
                        gi = new LofiGalleryDetail(gi); // TAG is null
                    } else {
                        gi = new GalleryDetail(gi);
                    }
                }
            }
        }
        return gi;
    }

    /*
    @Override
    protected void onNewIntent(Intent intent) {
        if (mGalleryInfo != null)
            mGiStack.push(mGalleryInfo);
        setIntent(intent);
        mGalleryInfo = handleIntent(intent);

        if (mGalleryInfo == null) {
            // TODO need a toast ?
            if (mGiStack.isEmpty())
                finish();
            else
                mGalleryInfo = mGiStack.pop();
        } else {
            doPreLayout();
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWindowsAnimate.free();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGalleryInfo = handleIntent(getIntent());
        if (mGalleryInfo == null)
            // If no GalleryInfo pass, just finish
            finish();

        setContentView(R.layout.gallery_detail);
        setTitle(String.valueOf(mGalleryInfo.gid));

        mAppContext = (AppContext) getApplication();
        mClient = EhClient.getInstance();
        mWindowsAnimate = new WindowsAnimate();
        mWindowsAnimate.init(this);

        mResources = getResources();

        // Menu
        MaterialIndicatorDrawable materialIndicator = new MaterialIndicatorDrawable(this, Color.WHITE, Stroke.THIN);
        materialIndicator.setIconState(MaterialIndicatorDrawable.IconState.ARROW);
        Ui.setMaterialIndicator(getActionBar(), materialIndicator);

        // Get view
        mStandard = (FitWindowView) findViewById(R.id.standard);

        mDetailScroll = (ResponedScrollView)findViewById(R.id.detail_scroll);
        mMoreDetailScroll = (ScrollView)findViewById(R.id.more_detail_scroll);
        mMoreComment = findViewById(R.id.more_comment);
        mRefreshText = (RefreshTextView)findViewById(R.id.refresh_text);

        mDetailHeader = findViewById(R.id.detail_header);
        mDetailButtons = findViewById(R.id.detail_buttons);
        mWarning = (TextView)findViewById(R.id.warning);
        mDetailActions = findViewById(R.id.detail_actions);
        mDividerAM = findViewById(R.id.detail_divider_a_m);
        mDetailMore = findViewById(R.id.detail_more);
        mDividerMR = findViewById(R.id.detail_divider_m_r);
        mDetailRate = findViewById(R.id.detail_rate);
        mDividerRT = findViewById(R.id.detail_divider_r_t);
        mDetailTag = (LinearLayout)findViewById(R.id.detail_tag);
        mDividerTC = findViewById(R.id.detail_divider_t_c);
        mDetailComment = (LinearLayout)findViewById(R.id.detail_comment);
        mDividerCP = findViewById(R.id.detail_divider_c_p);
        mDetailPreview = (LinearLayout)findViewById(R.id.detail_preview);

        mThumb = (LoadImageView)findViewById(R.id.thumb);
        mTitle = (TextView)findViewById(R.id.title);
        mUploader = (TextView)findViewById(R.id.uploader);
        mDownloadButton = (SuperButton)findViewById(R.id.download);
        mReadButton = (SuperButton)findViewById(R.id.read);
        mCategory = (TextView)findViewById(R.id.category);
        mMoreOfUploader = (TextView)findViewById(R.id.more_of_uploader);
        mSimilar = (TextView)findViewById(R.id.similar);
        mRatingText = (TextSwitcher)mDetailRate.findViewById(R.id.rating_text);
        mRating = (ProgressiveRatingBar)mDetailRate.findViewById(R.id.rating);
        mFavorite = (TextView)findViewById(R.id.favorite);
        mRate = (TextView)findViewById(R.id.rate);
        mCommentMoreText = (TextView)findViewById(R.id.comment_more_text);
        mPreview = (SimpleGridLayout)findViewById(R.id.preview);
        mPreviewPage = (SuperButton)findViewById(R.id.preview_num);
        mPreviewBack = (SuperButton)findViewById(R.id.back);
        mPreviewFront = (SuperButton)findViewById(R.id.front);
        mPreviewWait = (MaterialProgress)findViewById(R.id.preview_wait);
        mPreviewRefresh = (SuperButton)findViewById(R.id.preview_refresh);

        mCommentList = (ListView)findViewById(R.id.comment_list);
        mReply = (FloatingActionButton)findViewById(R.id.reply);

        // Set Drawable
        Rect rect = new Rect(0, 0, Ui.dp2pix(48), Ui.dp2pix(48));
        mCategoryDrawable = new OvalDrawable(0);
        mCategoryDrawable.setBounds(0, 0, Ui.dp2pix(48), Ui.dp2pix(48));
        mCategory.setCompoundDrawables(null, mCategoryDrawable, null, null);
        Drawable moreOfUploaderDrawable = mResources.getDrawable(R.drawable.ic_more_of_uploader);
        moreOfUploaderDrawable.setBounds(rect);
        mMoreOfUploader.setCompoundDrawables(null, moreOfUploaderDrawable, null, null);
        Drawable similarDrawable = mResources.getDrawable(R.drawable.ic_similar);
        similarDrawable.setBounds(rect);
        mSimilar.setCompoundDrawables(null, similarDrawable, null, null);
        Drawable favoriteDrawable = mResources.getDrawable(R.drawable.ic_favorite);
        favoriteDrawable.setBounds(rect);
        mFavorite.setCompoundDrawables(null, favoriteDrawable, null, null);
        mRateDrawable = mResources.getDrawable(R.drawable.ic_rate);
        mRateDrawable.setBounds(rect);
        mCheckmarkDrawable = mResources.getDrawable(R.drawable.ic_checkmark);
        mCheckmarkDrawable.setBounds(rect);
        mRate.setCompoundDrawables(null, mRateDrawable, null, null);

        // Init
        mRatingText.setFactory(this);
        mPreview.setColumnCountPortrait(
                Config.getPreviewColumnsPortrait());
        mPreview.setColumnCountLandscape(
                Config.getPreviewColumnsLandscape());

        // Set theme color
        setThemeColor(mThemeColor = Config.getCustomThemeColor() ? Config.getThemeColor() :
            (mGalleryInfo.title == null ? Ui.THEME_COLOR : Ui.getCategoryColor(mGalleryInfo.category)));

        // Set ripple
        mWindowsAnimate.addRippleEffect(mDownloadButton, true);
        mWindowsAnimate.addRippleEffect(mFavorite, false);
        mWindowsAnimate.addRippleEffect(mRate, false);
        mWindowsAnimate.addRippleEffect(mPreviewPage, true);
        mWindowsAnimate.addRippleEffect(mPreviewBack, true);
        mWindowsAnimate.addRippleEffect(mPreviewFront, true);

        // Set listener
        mStandard.addOnFitSystemWindowsListener(this);
        mDownloadButton.setOnClickListener(this);
        mReadButton.setOnClickListener(this);
        mCategory.setOnClickListener(this);
        mMoreOfUploader.setOnClickListener(this);
        mSimilar.setOnClickListener(this);
        mRating.setOnUserRateListener(this);
        mFavorite.setOnClickListener(this);
        mRate.setOnClickListener(this);
        mDetailMore.setOnTouchListener(this);
        mDetailComment.setOnTouchListener(this);
        mMoreDetailScroll.addOnLayoutChangeListener(this);
        mDetailScroll.addOnLayoutChangeListener(this);
        mDetailScroll.setOnScrollStateChangedListener(this);
        mCommentList.addOnLayoutChangeListener(this);
        mPreviewPage.setOnClickListener(this);
        mPreviewBack.setOnClickListener(this);
        mPreviewFront.setOnClickListener(this);
        mPreviewRefresh.setOnClickListener(this);
        mReply.setOnClickListener(this);
        mRefreshText.setDefaultRefresh("点击重试", this);

        doPreLayout();
    }

    private void setThemeColor(int themeColor) {
        mThemeColor = themeColor;
        getActionBar().setBackgroundDrawable(new ColorDrawable(mThemeColor));
        mDownloadButton.setRoundBackground(true, false, mResources.getColor(R.color.background_light), mThemeColor);
        mDownloadButton.setTextColor(mThemeColor);
        mReadButton.setRoundBackground(true, mThemeColor, 0);
        ((TextView)findViewById(R.id.detail_more_text)).setTextColor(mThemeColor);
        mCommentMoreText.setTextColor(mThemeColor);
        mPreviewPage.setRoundBackground(true, false, mResources.getColor(R.color.background_light), mThemeColor);
        mPreviewPage.setTextColor(mThemeColor);
        mPreviewBack.setRoundBackground(true, false, mResources.getColor(R.color.background_light), mThemeColor);
        mPreviewBack.setTextColor(mThemeColor);
        mPreviewFront.setRoundBackground(true, false, mResources.getColor(R.color.background_light), mThemeColor);
        mPreviewFront.setTextColor(mThemeColor);
        mPreviewWait.setColor(mThemeColor);
        mPreviewRefresh.setRoundBackground(true, mThemeColor, 0);
        mReply.setColor(mThemeColor);
        Ui.colorStatusBarL(this, mThemeColor);
        if (mPaddingTop != -1)
            Ui.colorStatusBarKK(this, mPaddingTop - Ui.ACTION_BAR_HEIGHT, mThemeColor);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getDetailInfo() {
        String detailUrl = mClient.getDetailUrl(
                mGalleryInfo.gid, mGalleryInfo.token);
        if (mGalleryInfo instanceof GalleryDetail)
            mClient.getGDetail(detailUrl, (GalleryDetail)mGalleryInfo, new GDetailGetListener());
        else if (mGalleryInfo instanceof LofiGalleryDetail)
            mClient.getLGDetail(detailUrl, (LofiGalleryDetail)mGalleryInfo, new GLDetailGetListener());
        else if (mGalleryInfo instanceof ApiGalleryDetail)
            ; // TODO
    }

    private void doPreLayout() {
        mRefreshText.setRefreshing(true);

        if (mGalleryInfo.title != null) {
            // Not from url
            mDetailScroll.setVisibility(View.VISIBLE);
            mDetailHeader.setVisibility(View.VISIBLE);

            mThumb.setLoadInfo(mGalleryInfo.thumb, String.valueOf(mGalleryInfo.gid));
            ImageLoader.getInstance(this).add(mGalleryInfo.thumb, String.valueOf(mGalleryInfo.gid),
                    new LoadImageView.SimpleImageGetListener(mThumb).setTransitabled(false));
            mTitle.setText(mGalleryInfo.title);
            mUploader.setText(mGalleryInfo.uploader);
        }

        getDetailInfo();
    }

    private String getRatingText(float rating) {
        if (rating == Float.NaN)
            return "(´_ゝ`)";

        int resId = 0;
        switch (Math.round(rating * 2)) {
        case 0:
            resId = R.string.rating0; break;
        case 1:
            resId = R.string.rating1; break;
        case 2:
            resId = R.string.rating2; break;
        case 3:
            resId = R.string.rating3; break;
        case 4:
            resId = R.string.rating4; break;
        case 5:
            resId = R.string.rating5; break;
        case 6:
            resId = R.string.rating6; break;
        case 7:
            resId = R.string.rating7; break;
        case 8:
            resId = R.string.rating8; break;
        case 9:
            resId = R.string.rating9; break;
        case 10:
            resId = R.string.rating10; break;
        }

        if (resId == 0)
            return "(´_ゝ`)";
        else
            return getString(resId);
    }

    private String getAllRatingText(float rating, int people) {
        StringBuilder sb = new StringBuilder()
                .append(String.format(getString(R.string.we_feel), getRatingText(rating)))
                .append(" (").append(rating);
        if (people != -1)
            sb.append(", ").append(people).append(")").toString();
        else
            sb.append(")");
        return sb.toString();
    }

    private void addTag(LinkedHashMap<String, LinkedList<String>> tags) {
        int x = Ui.dp2pix(2);
        int y = Ui.dp2pix(4);
        ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.TextTag);
        for (Entry<String, LinkedList<String>> tagGroup : tags.entrySet()) {

            LinearLayout tagGroupLayout = new LinearLayout(this);
            tagGroupLayout.setOrientation(LinearLayout.HORIZONTAL);
            AutoWrapLayout tagLayout = new AutoWrapLayout(this);


            // Group name
            final String groupName = tagGroup.getKey();
            TextView groupNameView = new TextView(ctw);
            groupNameView.setText(groupName);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(x, y, x, y);
            tagGroupLayout.addView(groupNameView, lp);

            for (final String tag : tagGroup.getValue()) {
                TextView tagView = new TextView(ctw);
                tagView.setText(tag);
                tagView.setBackgroundColor(mThemeColor);
                tagView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(GalleryDetailActivity.this,
                                GalleryListActivity.class);
                        intent.setAction(GalleryListActivity.ACTION_GALLERY_LIST);
                        intent.putExtra(GalleryListActivity.KEY_MODE,
                                ListUrls.MODE_TAG);
                        intent.putExtra(GalleryListActivity.KEY_TAG, groupName + ":" + tag);
                        intent.setAction(GalleryListActivity.ACTION_GALLERY_LIST);
                        startActivity(intent);
                    }
                });
                AutoWrapLayout.LayoutParams alp = new AutoWrapLayout.LayoutParams();
                alp.setMargins(x, y, x, y);
                tagLayout.addView(tagView, alp);
            }
            tagGroupLayout.addView(tagLayout);
            mDetailTag.addView(tagGroupLayout);
        }
    }

    private void addLofiTag(String[] tags) {
        int x = Ui.dp2pix(2);
        int y = Ui.dp2pix(4);
        ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.TextTag);
        AutoWrapLayout tagLayout = new AutoWrapLayout(this);
        for (String tag : tags) {
            TextView tagView = new TextView(ctw);
            tagView.setText(tag);
            tagView.setBackgroundColor(mThemeColor);
            AutoWrapLayout.LayoutParams alp = new AutoWrapLayout.LayoutParams();
            alp.setMargins(x, y, x, y);
            tagLayout.addView(tagView, alp);
        }
        mDetailTag.addView(tagLayout);
    }

    private String getPreviewPageNum(int pageNum) {
        return pageNum == Integer.MAX_VALUE ? "??" : String.valueOf(pageNum);
    }

    /**
     * GalleryDetail
     * LofiGalleryDetail
     * ApiGalleryDetail
     */
    private void doLayout() {
        if (mDetailScroll.getVisibility() == View.GONE) {
            // If not set header in doPreLayout
            // Set theme
            setThemeColor(Ui.getCategoryColor(mGalleryInfo.category));
            // Add history first
            Data.getInstance().addHistory(mGalleryInfo, Data.BROWSE);
            mDetailScroll.setVisibility(View.VISIBLE);
            mDetailHeader.setVisibility(View.VISIBLE);
            mThumb.setLoadInfo(mGalleryInfo.thumb, String.valueOf(mGalleryInfo.gid));
            ImageLoader.getInstance(this).add(mGalleryInfo.thumb, String.valueOf(mGalleryInfo.gid),
                    new LoadImageView.SimpleImageGetListener(mThumb));
            mTitle.setText(mGalleryInfo.title);
            mUploader.setText(mGalleryInfo.uploader);
        }

        // Button
        mDetailButtons.setVisibility(View.VISIBLE);

        // waring
        mWarning.setVisibility(View.VISIBLE);

        // Action
        mDetailActions.setVisibility(View.VISIBLE);
        mCategory.setText(Ui.getCategoryText(mGalleryInfo.category));
        mCategoryDrawable.setColor(Ui.getCategoryColor(mGalleryInfo.category));

        // Detail
        mDividerAM.setVisibility(View.VISIBLE);
        mDetailMore.setVisibility(View.VISIBLE);
        if (mGalleryInfo instanceof GalleryDetail) {
            GalleryDetail galleryDetail = (GalleryDetail)mGalleryInfo;
            ((TextView)mDetailMore.findViewById(R.id.language)).setText(
                    getString(R.string.language) + ": " + galleryDetail.language);
            ((TextView)mDetailMore.findViewById(R.id.posted)).setText(galleryDetail.posted);
            ((TextView)mDetailMore.findViewById(R.id.pages)).setText(
                    getString(R.string.pages) + ": " + String.valueOf(galleryDetail.pages));
            ((TextView)mDetailMore.findViewById(R.id.size)).setText(
                    getString(R.string.size) + ": " + galleryDetail.size);
            StringBuilder sb = new StringBuilder();
            sb.append("gid: ").append(galleryDetail.gid).append("\n\n")
                    .append("token: ").append(galleryDetail.token).append("\n\n")
                    .append("title: ").append(galleryDetail.title).append("\n\n")
                    .append("title_jpn: ").append(galleryDetail.title_jpn).append("\n\n")
                    .append("thumb: ").append(galleryDetail.thumb).append("\n\n")
                    .append("category: ").append(Ui.getCategoryText(galleryDetail.category)).append("\n\n")
                    .append("uploader: ").append(galleryDetail.uploader).append("\n\n")
                    .append("posted: ").append(galleryDetail.posted).append("\n\n")
                    .append("pages: ").append(galleryDetail.pages).append("\n\n")
                    .append("size: ").append(galleryDetail.size).append("\n\n")
                    .append("resized: ").append(galleryDetail.resized).append("\n\n")
                    .append("parent: ").append(galleryDetail.parent).append("\n\n")
                    .append("visible: ").append(galleryDetail.visible).append("\n\n")
                    .append("language: ").append(galleryDetail.language).append("\n\n")
                    .append("people: ").append(galleryDetail.people).append("\n\n")
                    .append("rating: ").append(galleryDetail.rating);
            ((TextView)mMoreDetailScroll.findViewById(R.id.more_detail)).setText(sb.toString());
        } else if (mGalleryInfo instanceof LofiGalleryDetail) {
            LofiGalleryDetail lofiGalleryDetail = (LofiGalleryDetail)mGalleryInfo;
            ((TextView)mDetailMore.findViewById(R.id.language)).setVisibility(View.GONE);
            ((TextView)mDetailMore.findViewById(R.id.posted)).setVisibility(View.GONE);
            ((TextView)mDetailMore.findViewById(R.id.pages)).setVisibility(View.GONE);
            ((TextView)mDetailMore.findViewById(R.id.size)).setText(lofiGalleryDetail.posted);
            StringBuilder sb = new StringBuilder();
            sb.append("gid: ").append(lofiGalleryDetail.gid).append("\n\n")
                    .append("token: ").append(lofiGalleryDetail.token).append("\n\n")
                    .append("title: ").append(lofiGalleryDetail.title).append("\n\n")
                    .append("thumb: ").append(lofiGalleryDetail.thumb).append("\n\n")
                    .append("category: ").append(Ui.getCategoryText(lofiGalleryDetail.category)).append("\n\n")
                    .append("uploader: ").append(lofiGalleryDetail.uploader).append("\n\n")
                    .append("posted: ").append(lofiGalleryDetail.posted).append("\n\n")
                    .append("simpleLanguage: ").append(lofiGalleryDetail.simpleLanguage).append("\n\n")
                    .append("rating: ").append(lofiGalleryDetail.rating).append("\n\n");
            ((TextView)mMoreDetailScroll.findViewById(R.id.more_detail)).setText(sb.toString());
        } else if (mGalleryInfo instanceof ApiGalleryDetail) {
            // TODO
        }

        // Rate
        mDividerMR.setVisibility(View.VISIBLE);
        mDetailRate.setVisibility(View.VISIBLE);
        int people = mGalleryInfo instanceof GalleryDetail ? ((GalleryDetail)mGalleryInfo).people : -1;
        mRatingText.setCurrentText(getAllRatingText(mGalleryInfo.rating, people));
        mRating.setRating(mGalleryInfo.rating);
        mRating.setEnableRate(false);

        // Tag
        if (mGalleryInfo instanceof GalleryDetail) {
            GalleryDetail galleryDetail = (GalleryDetail)mGalleryInfo;
            if (galleryDetail.tags != null && galleryDetail.tags.size() > 0) {
                mDividerRT.setVisibility(View.VISIBLE);
                mDetailTag.setVisibility(View.VISIBLE);
                addTag(galleryDetail.tags);
            }
        } else if (mGalleryInfo instanceof LofiDetailImpl) {
            LofiDetailImpl lofiDetailImpl = (LofiDetailImpl)mGalleryInfo;
            String[] tags = lofiDetailImpl.getTags();
            if (tags != null && tags.length > 0) {
                mDividerRT.setVisibility(View.VISIBLE);
                mDetailTag.setVisibility(View.VISIBLE);
                addLofiTag(tags);
            }
        }

        // Comment
        GalleryDetail galleryDetail;
        if (mGalleryInfo instanceof GalleryDetail &&
                (galleryDetail = (GalleryDetail)mGalleryInfo) != null &&
                galleryDetail.comments != null) {
            mDividerTC.setVisibility(View.VISIBLE);
            mDetailComment.setVisibility(View.VISIBLE);

            int commentNum = galleryDetail.comments.size();
            if (commentNum == 0)
                mCommentMoreText.setText(R.string.no_comment);
            else if (commentNum <= 2)
                mCommentMoreText.setText(R.string.no_more_comment);
            else
                mCommentMoreText.setText(R.string.click_more);
            int maxShown = Math.min(2, commentNum);
            for (int i = 0; i < maxShown; i++) {
                mDetailComment.addView(getCommentView(null, galleryDetail.comments.get(i), true),
                        i, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            mCommentAdapter = new CommentAdapter();
            mCommentList.setAdapter(mCommentAdapter);
            mCommentList.setOnItemClickListener(this);
            mCommentList.setOnItemLongClickListener(this);
        }

        // Add preview
        if (mGalleryInfo instanceof PreviewImpl) {
            PreviewImpl previewImpl = (PreviewImpl)mGalleryInfo;
            PreviewList pl = previewImpl.getPreview(0);
            if (pl != null) {
                mDividerCP.setVisibility(View.VISIBLE);
                mDetailPreview.setVisibility(View.VISIBLE);

                mPreviewPage.setText((mCurPreviewPage + 1) + "/" +
                            getPreviewPageNum(previewImpl.getPreviewPageNum()));
                pl.setData(this, this, previewImpl, mCurPreviewPage);
                pl.addPreview(mPreview);
            }
        }
    }

    private void refreshPreview() {
        if (!(mGalleryInfo instanceof PreviewImpl))
            return;

        PreviewImpl previewImpl = (PreviewImpl)mGalleryInfo;
        mPreview.removeAllViews();
        mPreviewPage.setText((mCurPreviewPage + 1) + "/" + getPreviewPageNum(previewImpl.getPreviewPageNum()));

        PreviewList previewList = previewImpl.getPreview(mCurPreviewPage);
        if (previewList == null) {
            mPreviewWait.setVisibility(View.VISIBLE);
            mPreviewRefresh.setVisibility(View.GONE);

            String url = mClient.getDetailUrl(
                    mGalleryInfo.gid, mGalleryInfo.token, mCurPreviewPage);
            mClient.getPreviewList(url, Config.getMode(), mCurPreviewPage,
                    new PListGetListener());

            mGetPreview = true;
        } else {
            mPreviewWait.setVisibility(View.GONE);
            mPreviewRefresh.setVisibility(View.GONE);
            previewList.addPreview(mPreview);

            mShowPreview = true;
        }
    }

    @SuppressLint("InflateParams")
    private View getCommentView(View contentView, Comment comment, boolean restrict) {
        if (contentView == null) {
            contentView = LayoutInflater.from(this).inflate(R.layout.comments_item, null);
            if (!restrict) {
                mWindowsAnimate.addRippleEffect(contentView, true);
            }
        }
        ((TextView)contentView.findViewById(R.id.user)).setText(comment.user);
        ((TextView)contentView.findViewById(R.id.time)).setText(comment.time);
        TextView commentText = (TextView)contentView.findViewById(R.id.comment);
        if (restrict) {
            commentText.setMaxLines(3);
            // Cancel auto link
            commentText.setAutoLinkMask(0);
        }
        commentText.setText(Html.fromHtml(comment.comment));
        return contentView;
    }

    @Override
    public void onBackPressed() {
        if (mRunningAnimateNum != 0 || mWindowsAnimate.isRunningAnimate())
            return;

        if (mMoreDetailScroll.getVisibility() == View.VISIBLE) {
            mDetailScroll.setVisibility(View.VISIBLE);
            mWindowsAnimate.addMoveExitTransitions(mMoreDetailScroll, null);
        } else if (mMoreComment.getVisibility() == View.VISIBLE) {
            mDetailScroll.setVisibility(View.VISIBLE);
            mWindowsAnimate.addMoveExitTransitions(mMoreComment, null);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRefresh() {
        getDetailInfo();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onClick(View v) {
        if (mRunningAnimateNum != 0 || mWindowsAnimate.isRunningAnimate())
            return;

        if (v == mDownloadButton) {
            Intent it = new Intent(GalleryDetailActivity.this, DownloadService.class);
            startService(it);
            mAppContext.getDownloadServiceConnection().getService().add(mGalleryInfo);
            MaterialToast.showToast(R.string.toast_add_download);
        } else if (v == mReadButton) {
            //mData.addRead(mGalleryInfo);
            Intent intent = new Intent(this,
                    GalleryActivity.class);
            intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, mGalleryInfo);
            intent.putExtra(GalleryActivity.KEY_START_INDEX, -1);
            startActivity(intent);
        } else if (v == mCategory) {
            Intent intent = new Intent(this, GalleryListActivity.class);
            intent.setAction(GalleryListActivity.ACTION_GALLERY_LIST);
            intent.putExtra(GalleryListActivity.KEY_MODE, ListUrls.MODE_NORMAL);
            intent.putExtra(GalleryListActivity.KEY_CATEGORY, mGalleryInfo.category);
            startActivity(intent);
        } else if (v == mMoreOfUploader) {
            Intent intent = new Intent(this, GalleryListActivity.class);
            intent.setAction(GalleryListActivity.ACTION_GALLERY_LIST);
            intent.putExtra(GalleryListActivity.KEY_MODE, ListUrls.MODE_UPLOADER);
            intent.putExtra(GalleryListActivity.KEY_UPLOADER, mGalleryInfo.uploader);
            startActivity(intent);
        } else if (v == mSimilar) {
            Intent intent = new Intent(this, GalleryListActivity.class);
            intent.setAction(GalleryListActivity.ACTION_GALLERY_LIST);
            intent.putExtra(GalleryListActivity.KEY_MODE, ListUrls.MODE_IMAGE_SEARCH);
            intent.putExtra(GalleryListActivity.KEY_IMAGE_KEY, String.valueOf(mGalleryInfo.gid));
            intent.putExtra(GalleryListActivity.KEY_IMAGE_URL, mGalleryInfo.thumb);
            startActivity(intent);
        } else if (v == mFavorite) {
            Favorite.addToFavorite(this, mGalleryInfo);
        } else if (v == mRate) {
            if (isCheckmark) {
                int rating = (int)(mRating.getRating() * 2 + 0.5);
                if (rating <= 0 || rating > 10) {
                    MaterialToast.showToast(R.string.invalid_rating);
                    return;
                }
                mClient.rate(mGalleryInfo.gid, mGalleryInfo.token, rating, new EhClient.OnRateListener() {
                    @Override
                    public void onSuccess(float ratingAvg, int ratingCnt) {
                        mGalleryInfo.rating = ratingAvg;
                        mRatingText.setText(getAllRatingText(mGalleryInfo.rating, ratingCnt));
                        mRating.setRating(mGalleryInfo.rating);
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        mRatingText.setText(getString(R.string.rate_failed));
                        MaterialToast.showToast(eMsg);
                    }
                });
                mRatingText.setText(getString(R.string.rate_succeeded));
                mRating.setEnableRate(false);
            } else {
                mRatingText.setText(getString(R.string.rate_sensibly));
                mRating.setEnableRate(true);
            }

            // Animate to change
            ValueAnimator animation = ValueAnimator.ofFloat(0.0f, 90.0f, 180.0f);
            animation.setDuration(Constants.ANIMATE_TIME);
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float rotationY = (Float)animation.getAnimatedValue();
                    if (rotationY > 90) {
                        rotationY = 180 - rotationY;
                        if (!isSetDrawable) {
                            mRate.setCompoundDrawables(null, isCheckmark ? mRateDrawable : mCheckmarkDrawable, null, null);
                            mRate.setText(isCheckmark ? R.string.rate : android.R.string.ok);
                            isSetDrawable = true;
                            isCheckmark = !isCheckmark;
                        }
                    }
                    mRate.setRotationY(rotationY);
                }
            });
            animation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRunningAnimateNum--;
                    isSetDrawable = false;
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
            });
            animation.start();
            mRunningAnimateNum++;
        } else if (v == mReply) {
            mReplyPositive = false;
            final EditText et = new EditText(this);
            et.setGravity(Gravity.TOP);
            et.setHint("评论内容");
            et.setTextColor(getResources().getColor(R.color.secondary_text_dark));
            et.setBackgroundDrawable(null);
            et.setText(mOldReply);
            if (mOldReply != null)
                et.setSelection(mOldReply.length());

            new MaterialAlertDialog.Builder(this).setTitle(R.string.reply)
                    .setView(et, true, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                    .setNegativeButton(android.R.string.cancel)
                    .setPositiveButton(android.R.string.ok)
                    .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                        @Override
                        public boolean onClick(MaterialAlertDialog dialog, int which) {
                            if (which == MaterialAlertDialog.POSITIVE) {
                                mReplyPositive = true;
                                mClient.comment(EhClient.getDetailUrl(mGalleryInfo.gid,
                                        mGalleryInfo.token, 0, Config.getApiMode()), et.getText().toString(),
                                        new EhClient.OnCommentListener() {
                                            @Override
                                            public void onSuccess(List<Comment> comments) {
                                                ((GalleryDetail)mGalleryInfo).comments = comments;
                                                mCommentAdapter.notifyDataSetChanged();
                                                mOldReply = null;
                                            }
                                            @Override
                                            public void onFailure(String eMsg) {
                                                MaterialToast.showToast(eMsg);
                                                mOldReply = et.getText().toString();
                                            }
                                        });
                            }
                            return true;
                        }
                    }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (mReplyPositive)
                                mOldReply = null;
                            else
                                mOldReply = et.getText().toString();
                        }
                    }).show();

        } else if (v == mPreviewPage) {
            if (mGalleryInfo instanceof PreviewImpl &&
                    ((PreviewImpl)mGalleryInfo).getPreviewPageNum() != Integer.MAX_VALUE &&
                    ((PreviewImpl)mGalleryInfo).getPreviewPageNum() > 1)
                createGoToDialog().show();
        } else if (v == mPreviewBack) {
            if (!(mGalleryInfo instanceof PreviewImpl) ||
                    (mGalleryInfo instanceof LofiGalleryDetail && mGetPreview))
                return;

            if (mCurPreviewPage <= 0)
                return;
            mCurPreviewPage--;
            refreshPreview();
        } else if (v == mPreviewFront) {
            if (!(mGalleryInfo instanceof PreviewImpl) ||
                    (mGalleryInfo instanceof LofiGalleryDetail && mGetPreview))
                return;

            if (mCurPreviewPage >= ((PreviewImpl)mGalleryInfo).getPreviewPageNum() - 1)
                return;
            mCurPreviewPage++;
            refreshPreview();
        } else if (v == mPreviewRefresh) {
            refreshPreview();
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right,
            int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (v == mMoreDetailScroll) {
            mMoreDetailScroll.scrollTo(0, 0);
            System.out.print("sssss");
        } else if (v == mCommentList) {
            mCommentList.setSelection(0);
        } else if (v == mDetailScroll) {
            if (mShowPreview) {
                mShowPreview = false;
                mDetailScroll.smoothScrollTo(0, mDividerCP.getTop());
            }
        }
    }

    @Override
    public void onFitSystemWindows(int l, int t, int r, int b) {
        mDetailScroll.setPadding(mDetailScroll.getPaddingLeft(), t,
                mDetailScroll.getPaddingRight(), b);
        mMoreDetailScroll.setPadding(mMoreDetailScroll.getPaddingLeft(), t,
                mMoreDetailScroll.getPaddingRight(), b);
        mCommentList.setPadding(mCommentList.getPaddingLeft(), t,
                mCommentList.getPaddingRight(), b + Ui.dp2pix(16 + 56 + 16));
        ((FrameLayout.LayoutParams)mReply.getLayoutParams()).bottomMargin = Ui.dp2pix(16) + b;

        mPaddingTop = t;
        Ui.colorStatusBarKK(this, mThemeColor, mPaddingTop - Ui.ACTION_BAR_HEIGHT);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        if (v == mDetailMore) {
            if (ViewUtils.isClickAction(event)) {
                mWindowsAnimate.addCircleTransitions(mDetailMore, (int)event.getX(),
                        (int)event.getY(), mResources.getColor(R.color.background_light),
                        new WindowsAnimate.OnAnimationEndListener() {
                            @Override
                            public void onAnimationEnd() {
                                mDetailScroll.setVisibility(View.GONE);
                                mMoreDetailScroll.setVisibility(View.VISIBLE);
                                AlphaAnimation aa = new AlphaAnimation(0.0f,1.0f);
                                aa.setDuration(Constants.ANIMATE_TIME);
                                mMoreDetailScroll.startAnimation(aa);
                            }
                        });
            }
        } else if (v == mDetailComment) {
            if (ViewUtils.isClickAction(event)) {
                mWindowsAnimate.addCircleTransitions(mDetailComment, (int)event.getX(),
                        (int)event.getY(), mResources.getColor(R.color.background_light),
                        new WindowsAnimate.OnAnimationEndListener() {
                            @Override
                            public void onAnimationEnd() {
                                mDetailScroll.setVisibility(View.GONE);
                                mMoreComment.setVisibility(View.VISIBLE);
                                // Add reply icon animation
                                mReply.setVisibility(View.INVISIBLE);
                                AlphaAnimation aa = new AlphaAnimation(0.0f,1.0f);
                                aa.setDuration(Constants.ANIMATE_TIME);
                                aa.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {}
                                    @Override
                                    public void onAnimationRepeat(Animation animation) {}
                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        mWindowsAnimate.addOvershootEnterTransitions(mReply, null);
                                    }
                                });
                                mMoreComment.startAnimation(aa);
                            }
                        });
            }
        }
        return true;
    }

    @Override
    public void onUserRate(float rating) {
        mRatingText.setText(getRatingText(rating));
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

        new MaterialAlertDialog.Builder(this).setTitle(R.string.what_to_do)
                .setNegativeButton(android.R.string.cancel)
                .setItems(R.array.comment_long_click, new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int position) {
                        switch (position) {
                        case 0:
                            ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(ClipData.newPlainText(null, c.comment));
                            MaterialToast.showToast(R.string.copyed);
                            break;

                        case 1:
                            Intent intent = new Intent(GalleryDetailActivity.this, GalleryListActivity.class);
                            intent.setAction(GalleryListActivity.ACTION_GALLERY_LIST);
                            intent.putExtra(GalleryListActivity.KEY_MODE, ListUrls.MODE_UPLOADER);
                            intent.putExtra(GalleryListActivity.KEY_UPLOADER, c.user);
                            startActivity(intent);
                            break;
                        }
                        return true;
                    }
                }).show();
        return true;
    }

    @Override
    public View makeView() {
        TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    @Override
    public int getCurPreviewPage() {
        return mCurPreviewPage;
    }

    @Override
    public void onGetPreviewImageFailure() {
        mPreviewWait.setVisibility(View.GONE);
        mPreviewRefresh.setVisibility(View.VISIBLE);
    }

    @Override
    public void onScrollStateChanged(ResponedScrollView view, int state) {
        if (view == mDetailScroll) {
            ImageCache imageCache = ImageCache.getInstance(this);
            if (state == ResponedScrollView.SCROLL_START)
                imageCache.setPauseDiskCache(true);
            else
                imageCache.setPauseDiskCache(false);
        }
    }

    private class GDetailGetListener
            implements EhClient.OnGetGDetailListener {
        @Override
        public void onSuccess(GalleryDetail md) {
            mRefreshText.setRefreshing(false);
            doLayout();
        }

        @Override
        public void onFailure(String eMsg) {
            mRefreshText.setEmesg(eMsg, true);
       }
    }

    private class GLDetailGetListener
            implements EhClient.OnGetLGDetailListener {
        @Override
        public void onSuccess(LofiGalleryDetail md, boolean isLastPage) {
            if (isLastPage)
                ((LofiDetailImpl)mGalleryInfo).setPreviewPageNum(mCurPreviewPage + 1);
            mRefreshText.setRefreshing(false);
            doLayout();
        }

        @Override
        public void onFailure(String eMsg) {
            mRefreshText.setEmesg(eMsg, true);
        }
    }

    private class PListGetListener
            implements EhClient.OnGetPreviewListListener {
        @Override
        public void onSuccess(Object checkFlag, PreviewList pageList, boolean isLastPage) {
            if (isFinishing())
                return;
            if (!(mGalleryInfo instanceof PreviewImpl))
                return;

            mPreviewWait.setVisibility(View.GONE);
            mPreviewRefresh.setVisibility(View.GONE);

            int page = (Integer)checkFlag;
            PreviewImpl previewImpl = (PreviewImpl)mGalleryInfo;
            previewImpl.setPreview(page, pageList);
            pageList.setData(GalleryDetailActivity.this,
                    GalleryDetailActivity.this, previewImpl, page);
            pageList.addPreview(mPreview);

            if (isLastPage && mGalleryInfo instanceof LofiDetailImpl) {
                ((LofiDetailImpl)mGalleryInfo).setPreviewPageNum(mCurPreviewPage + 1);
                mPreviewPage.setText((mCurPreviewPage + 1) + "/" +
                        getPreviewPageNum(previewImpl.getPreviewPageNum()));
            }

            mGetPreview = false;
            mShowPreview = true;
        }

        @Override
        public void onFailure(Object checkFlag, String eMsg) {
            if (isFinishing())
                return;
            if (!(mGalleryInfo instanceof PreviewImpl))
                return;

            int page = (Integer)checkFlag;
            if (page == mCurPreviewPage) {
                MaterialToast.showToast(eMsg);
                mPreviewWait.setVisibility(View.GONE);
                mPreviewRefresh.setVisibility(View.VISIBLE);
            }

            // TODO what to do with mGetPreview
        }
    }

    private class CommentAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (mGalleryInfo instanceof GalleryDetail) {
                GalleryDetail galleryDetail = (GalleryDetail)mGalleryInfo;
                if (galleryDetail.comments == null)
                    return 0;
                else
                    return galleryDetail.comments.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            return ((GalleryDetail)mGalleryInfo).comments.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = getCommentView(convertView,
                    ((GalleryDetail)mGalleryInfo).comments.get(position), false);
            return view;
        }
    }
}
