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
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.transition.TransitionInflater;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.hippo.drawable.RoundSideRectDrawable;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UrlOpener;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryTagGroup;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.parser.RateGalleryParser;
import com.hippo.ehviewer.dao.DownloadInfo;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.ui.annotation.WholeLifeCircle;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.scene.TransitionHelper;
import com.hippo.text.Html;
import com.hippo.text.URLImageGetter;
import com.hippo.util.ActivityHelper;
import com.hippo.util.ApiHelper;
import com.hippo.util.DrawableManager;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.LayoutUtils2;
import com.hippo.util.ReadableTime;
import com.hippo.view.ViewTransition;
import com.hippo.widget.AutoWrapLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.ProgressView;
import com.hippo.widget.ProgressiveRatingBar;
import com.hippo.widget.SimpleGridLayout;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.ViewUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Locale;

public class GalleryDetailScene extends BaseScene implements View.OnClickListener,
        com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener,
        View.OnLongClickListener{

    @IntDef({STATE_INIT, STATE_NORMAL, STATE_REFRESH, STATE_REFRESH_HEADER, STATE_FAILED})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {}

    private static final int REQUEST_CODE_COMMENT_GALLERY = 0;

    private static final int STATE_INIT = -1;
    private static final int STATE_NORMAL = 0;
    private static final int STATE_REFRESH = 1;
    private static final int STATE_REFRESH_HEADER = 2;
    private static final int STATE_FAILED = 3;

    public final static String KEY_ACTION = "action";
    public static final String ACTION_GALLERY_INFO = "action_gallery_info";
    public static final String ACTION_GID_TOKEN = "action_gid_token";

    public static final String KEY_GALLERY_INFO = "gallery_info";
    public static final String KEY_GID = "gid";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_PAGE = "page";

    private static final String KEY_GALLERY_DETAIL = "gallery_detail";
    private static final String KEY_REQUEST_ID = "request_id";

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private TextView mTip;
    @Nullable
    private ViewTransition mViewTransition;
    // Header
    @Nullable
    private View mHeader;
    @Nullable
    private View mColorBg;
    @Nullable
    private LoadImageView mThumb;
    @Nullable
    private TextView mTitle;
    @Nullable
    private TextView mUploader;
    @Nullable
    private TextView mCategory;
    @Nullable
    private ImageView mOtherActions;
    @Nullable
    private ViewGroup mActionGroup;
    @Nullable
    private TextView mDownload;
    @Nullable
    private View mRead;
    // Below header
    @Nullable
    private View mBelowHeader;
    // Info
    @Nullable
    private View mInfo;
    @Nullable
    private TextView mLanguage;
    @Nullable
    private TextView mPages;
    @Nullable
    private TextView mSize;
    @Nullable
    private TextView mPosted;
    @Nullable
    private TextView mFavoredTimes;
    // Actions
    @Nullable
    private View mActions;
    @Nullable
    private TextView mRatingText;
    @Nullable
    private RatingBar mRating;
    @Nullable
    private View mHeartGroup;
    @Nullable
    private TextView mHeart;
    @Nullable
    private TextView mHeartOutline;
    @Nullable
    private TextView mTorrent;
    @Nullable
    private TextView mShare;
    @Nullable
    private TextView mRate;
    // Tags
    @Nullable
    private LinearLayout mTags;
    @Nullable
    private TextView mNoTags;
    // Comments
    @Nullable
    private LinearLayout mComments;
    @Nullable
    private TextView mCommentsText;
    // Previews
    @Nullable
    private View mPreviews;
    @Nullable
    private SimpleGridLayout mGridLayout;
    @Nullable
    private TextView mPreviewText;
    // Progress
    @Nullable
    private View mProgress;
    @Nullable
    private ViewTransition mViewTransition2;
    @Nullable
    private PopupMenu mPopupMenu;

    @WholeLifeCircle
    private int mDownloadState;

    @Nullable
    private String mAction;
    @Nullable
    private GalleryInfo mGalleryInfo;
    private long mGid;
    private String mToken;

    @Nullable
    private GalleryDetail mGalleryDetail;
    private int mRequestId;

    private Pair<String, String>[] mTorrentList;

    @State
    private int mState = STATE_INIT;

    private boolean mModifingFavorites;

    private void handleArgs(Bundle args) {
        if (args == null) {
            return;
        }

        String action = args.getString(KEY_ACTION);
        mAction = action;
        if (ACTION_GALLERY_INFO.equals(action)) {
            mGalleryInfo = args.getParcelable(KEY_GALLERY_INFO);
            // Add history
            if (null != mGalleryInfo) {
                EhDB.putHistoryInfo(mGalleryInfo);
            }
        } else if (ACTION_GID_TOKEN.equals(action)) {
            mGid = args.getLong(KEY_GID);
            mToken = args.getString(KEY_TOKEN);
        }
    }

    @Nullable
    private String getGalleryDetailUrl(boolean allComment) {
        long gid;
        String token;
        if (mGalleryDetail != null) {
            gid = mGalleryDetail.gid;
            token = mGalleryDetail.token;
        } else if (mGalleryInfo != null) {
            gid = mGalleryInfo.gid;
            token = mGalleryInfo.token;
        } else if (ACTION_GID_TOKEN.equals(mAction)) {
            gid = mGid;
            token = mToken;
        } else {
            return null;
        }
        return EhUrl.getGalleryDetailUrl(gid, token, 0, allComment);
    }

    // -1 for error
    private long getGid() {
        if (mGalleryDetail != null) {
            return mGalleryDetail.gid;
        } else if (mGalleryInfo != null) {
            return mGalleryInfo.gid;
        } else if (ACTION_GID_TOKEN.equals(mAction)) {
            return mGid;
        } else {
            return -1;
        }
    }

    private String getToken() {
        if (mGalleryDetail != null) {
            return mGalleryDetail.token;
        } else if (mGalleryInfo != null) {
            return mGalleryInfo.token;
        } else if (ACTION_GID_TOKEN.equals(mAction)) {
            return mToken;
        } else {
            return null;
        }
    }

    private String getUploader() {
        if (mGalleryDetail != null) {
            return mGalleryDetail.uploader;
        } else if (mGalleryInfo != null) {
            return mGalleryInfo.uploader;
        } else {
            return null;
        }
    }

    // -1 for error
    private int getCategory() {
        if (mGalleryDetail != null) {
            return mGalleryDetail.category;
        } else if (mGalleryInfo != null) {
            return mGalleryInfo.category;
        } else {
            return -1;
        }
    }

    private GalleryInfo getGalleryInfo() {
        if (null != mGalleryDetail) {
            return mGalleryDetail;
        } else if (null != mGalleryInfo) {
            return mGalleryInfo;
        } else {
            return null;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }

        long gid = getGid();
        if (gid != -1) {
            mDownloadState = EhApplication.getDownloadManager(getContext()).getDownloadState(gid);
        } else {
            mDownloadState = DownloadInfo.STATE_INVALID;
        }
    }

    private void onInit() {
        handleArgs(getArguments());
    }

    private void onRestore(Bundle savedInstanceState) {
        mAction = savedInstanceState.getString(KEY_ACTION);
        mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO);
        mGid = savedInstanceState.getLong(KEY_GID);
        mToken = savedInstanceState.getString(KEY_TOKEN);
        mGalleryDetail = savedInstanceState.getParcelable(KEY_GALLERY_DETAIL);
        mRequestId = savedInstanceState.getInt(KEY_REQUEST_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAction != null) {
            outState.putString(KEY_ACTION, mAction);
        }
        if (mGalleryInfo != null) {
            outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo);
        }
        outState.putLong(KEY_GID, mGid);
        if (mToken != null) {
            outState.putString(KEY_TOKEN, mAction);
        }
        if (mGalleryDetail != null) {
            outState.putParcelable(KEY_GALLERY_DETAIL, mGalleryDetail);
        }
        outState.putInt(KEY_REQUEST_ID, mRequestId);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_gallery_detail, container, false);

        ViewGroup main = (ViewGroup) ViewUtils.$$(view, R.id.main);
        View mainView = ViewUtils.$$(main, R.id.scroll_view);
        View progressView = ViewUtils.$$(main, R.id.progress_view);
        mTip = (TextView) ViewUtils.$$(main, R.id.tip);
        mViewTransition = new ViewTransition(mainView, progressView, mTip);

        Drawable drawable = DrawableManager.getDrawable(getContext(), R.drawable.big_weird_face);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        mTip.setCompoundDrawables(null, drawable, null, null);
        mTip.setOnClickListener(this);

        mHeader = ViewUtils.$$(mainView, R.id.header);
        mColorBg = ViewUtils.$$(mHeader, R.id.color_bg);
        mThumb = (LoadImageView) ViewUtils.$$(mHeader, R.id.thumb);
        mTitle = (TextView) ViewUtils.$$(mHeader, R.id.title);
        mUploader = (TextView) ViewUtils.$$(mHeader, R.id.uploader);
        mCategory = (TextView) ViewUtils.$$(mHeader, R.id.category);
        mOtherActions = (ImageView) ViewUtils.$$(mHeader, R.id.other_actions);
        mActionGroup = (ViewGroup) ViewUtils.$$(mHeader, R.id.action_card);
        mDownload = (TextView) ViewUtils.$$(mActionGroup, R.id.download);
        mRead = ViewUtils.$$(mActionGroup, R.id.read);
        RippleSalon.addRipple(mOtherActions, false);
        RippleSalon.addRipple(mDownload, false);
        RippleSalon.addRipple(mRead, false);
        mUploader.setOnClickListener(this);
        mCategory.setOnClickListener(this);
        mOtherActions.setOnClickListener(this);
        mDownload.setOnClickListener(this);
        mDownload.setOnLongClickListener(this);
        mRead.setOnClickListener(this);

        mBelowHeader = mainView.findViewById(R.id.below_header);
        View belowHeader = mBelowHeader;

        mInfo = ViewUtils.$$(belowHeader, R.id.info);
        mLanguage = (TextView) ViewUtils.$$(mInfo, R.id.language);
        mPages = (TextView) ViewUtils.$$(mInfo, R.id.pages);
        mSize = (TextView) ViewUtils.$$(mInfo, R.id.size);
        mPosted = (TextView) ViewUtils.$$(mInfo, R.id.posted);
        mFavoredTimes = (TextView) ViewUtils.$$(mInfo, R.id.favoredTimes);
        RippleSalon.addRipple(mInfo, false);
        mInfo.setOnClickListener(this);

        mActions = ViewUtils.$$(belowHeader, R.id.actions);
        mRatingText = (TextView) ViewUtils.$$(mActions, R.id.rating_text);
        mRating = (RatingBar) ViewUtils.$$(mActions, R.id.rating);
        mHeartGroup = ViewUtils.$$(mActions, R.id.heart_group);
        mHeart = (TextView) ViewUtils.$$(mHeartGroup, R.id.heart);
        mHeartOutline = (TextView) ViewUtils.$$(mHeartGroup, R.id.heart_outline);
        mTorrent = (TextView) ViewUtils.$$(mActions, R.id.torrent);
        mShare = (TextView) ViewUtils.$$(mActions, R.id.share);
        mRate = (TextView) ViewUtils.$$(mActions, R.id.rate);
        RippleSalon.addRipple(mHeartGroup, false);
        RippleSalon.addRipple(mTorrent, false);
        RippleSalon.addRipple(mShare, false);
        RippleSalon.addRipple(mRate, false);
        mHeartGroup.setOnClickListener(this);
        mTorrent.setOnClickListener(this);
        mShare.setOnClickListener(this);
        mRate.setOnClickListener(this);
        ensureActionDrawable();

        mTags = (LinearLayout) ViewUtils.$$(belowHeader, R.id.tags);
        mNoTags = (TextView) ViewUtils.$$(mTags, R.id.no_tags);

        mComments = (LinearLayout) ViewUtils.$$(belowHeader, R.id.comments);
        mCommentsText = (TextView) ViewUtils.$$(mComments, R.id.comments_text);
        RippleSalon.addRipple(mComments, false);
        mComments.setOnClickListener(this);

        mPreviews = ViewUtils.$$(belowHeader, R.id.previews);
        mGridLayout = (SimpleGridLayout) ViewUtils.$$(mPreviews, R.id.grid_layout);
        mPreviewText = (TextView) ViewUtils.$$(mPreviews, R.id.preview_text);
        RippleSalon.addRipple(mPreviews, false);
        mPreviews.setOnClickListener(this);

        mProgress = ViewUtils.$$(mainView, R.id.progress);

        mViewTransition2 = new ViewTransition(mBelowHeader, mProgress);

        if (prepareData()) {
            if (mGalleryDetail != null) {
                bindViewSecond();
                setTransitionName();
                adjustViewVisibility(STATE_NORMAL, false);
            } else if (mGalleryInfo != null) {
                bindViewFirst();
                setTransitionName();
                adjustViewVisibility(STATE_REFRESH_HEADER, false);
            } else {
                adjustViewVisibility(STATE_REFRESH, false);
            }
        } else {
            mTip.setText(R.string.error_cannot_find_gallery);
            adjustViewVisibility(STATE_FAILED, false);
        }

        EhApplication.getDownloadManager(getContext()).addDownloadInfoListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EhApplication.getDownloadManager(getContext()).removeDownloadInfoListener(this);

        mTip = null;
        mViewTransition = null;

        mHeader = null;
        mColorBg = null;
        mThumb = null;
        mTitle = null;
        mUploader = null;
        mCategory = null;
        mOtherActions = null;
        mActionGroup = null;
        mDownload = null;
        mRead = null;
        mBelowHeader = null;

        mInfo = null;
        mLanguage = null;
        mPages = null;
        mSize = null;
        mPosted = null;
        mFavoredTimes = null;

        mActions = null;
        mRatingText = null;
        mRating = null;
        mHeartGroup = null;
        mHeart = null;
        mHeartOutline = null;
        mTorrent = null;
        mShare = null;
        mRate = null;

        mTags = null;
        mNoTags = null;

        mComments = null;
        mCommentsText = null;

        mPreviews = null;
        mGridLayout = null;
        mPreviewText = null;

        mProgress = null;

        mViewTransition2 = null;

        mPopupMenu = null;
    }

    private boolean prepareData() {
        if (mGalleryDetail != null) {
            return true;
        }

        long gid = getGid();
        if (gid == -1) {
            return false;
        }

        // Get from cache
        mGalleryDetail = EhApplication.getGalleryDetailCache(getContext()).get(gid);
        if (mGalleryDetail != null) {
            return true;
        }

        EhApplication application = (EhApplication) getContext().getApplicationContext();
        if (application.containGlobalStuff(mRequestId)) {
            // request exist
            return true;
        }

        // Do request
        return request();
    }

    private boolean request() {
        String url = getGalleryDetailUrl(true);
        if (url == null) {
            return false;
        }

        EhClient.Callback callback = new GetGalleryDetailListener(getContext(),
                ((StageActivity) getActivity()).getStageId(), getTag());
        mRequestId = ((EhApplication) getContext().getApplicationContext()).putGlobalStuff(callback);
        EhRequest request = new EhRequest()
                .setMethod(EhClient.METHOD_GET_GALLERY_DETAIL)
                .setArgs(url)
                .setCallback(callback);
        EhApplication.getEhClient(getContext()).execute(request);

        return true;
    }

    private void setActionDrawable(TextView text, Drawable drawable) {
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        text.setCompoundDrawables(null, drawable, null, null);
    }

    private void ensureActionDrawable() {
        Drawable heart = DrawableManager.getDrawable(getContext(), R.drawable.v_heart_primary_x48);
        setActionDrawable(mHeart, heart);
        Drawable heartOutline = DrawableManager.getDrawable(getContext(), R.drawable.v_heart_outline_primary_x48);
        setActionDrawable(mHeartOutline, heartOutline);
        Drawable torrent = DrawableManager.getDrawable(getContext(), R.drawable.v_utorrent_primary_x48);
        setActionDrawable(mTorrent, torrent);
        Drawable share = DrawableManager.getDrawable(getContext(), R.drawable.v_share_primary_x48);
        setActionDrawable(mShare, share);
        Drawable rate = DrawableManager.getDrawable(getContext(), R.drawable.v_thumb_up_primary_x48);
        setActionDrawable(mRate, rate);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean createCircularReveal() {
        if (mColorBg == null) {
            return false;
        }

        int w = mColorBg.getWidth();
        int h = mColorBg.getHeight();
        if (mColorBg.isAttachedToWindow() && w != 0 && h != 0) {
            ViewAnimationUtils.createCircularReveal(mColorBg, w / 2, h / 2, 0,
                    (float) Math.hypot(w / 2, h / 2)).setDuration(300).start();
            return true;
        } else {
            return false;
        }
    }

    private void adjustViewVisibility(int state, boolean animation) {
        if (state == mState) {
            return;
        }
        if (mViewTransition == null || mViewTransition2 == null) {
            return;
        }

        int oldState = mState;
        mState = state;

        switch (state) {
            case STATE_NORMAL:
                // Show mMainView
                mViewTransition.showView(0, animation);
                // Show mBelowHeader
                mViewTransition2.showView(0, animation);
                break;
            case STATE_REFRESH:
                // Show mProgressView
                mViewTransition.showView(1, animation);
                break;
            case STATE_REFRESH_HEADER:
                // Show mMainView
                mViewTransition.showView(0, animation);
                // Show mProgress
                mViewTransition2.showView(1, animation);
                break;
            default:
            case STATE_INIT:
            case STATE_FAILED:
                // Show mFailedView
                mViewTransition.showView(2, animation);
                break;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP &&
                (oldState == STATE_INIT || oldState == STATE_FAILED || oldState == STATE_REFRESH) &&
                (state == STATE_NORMAL || state == STATE_REFRESH_HEADER)) {
            if (!createCircularReveal()) {
                SimpleHandler.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        createCircularReveal();
                    }
                });
            }
        }
    }

    private void bindViewFirst() {
        if (mGalleryDetail != null) {
            return;
        }
        if (mThumb == null || mTitle == null || mUploader == null || mCategory == null) {
            return;
        }

        if (ACTION_GALLERY_INFO.equals(mAction) && mGalleryInfo != null) {
            GalleryInfo gi = mGalleryInfo;
            mThumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb, true);
            mTitle.setText(EhUtils.getSuitableTitle(gi));
            mUploader.setText(gi.uploader);
            mCategory.setText(EhUtils.getCategory(gi.category));
            mCategory.setTextColor(EhUtils.getCategoryColor(gi.category));
            updateDownloadText();
        }
    }

    private void updateFavoriteDrawable() {
        GalleryDetail gd = mGalleryDetail;
        if (gd == null) {
            return;
        }
        if (mHeart == null || mHeartOutline == null ) {
            return;
        }

        if (gd.isFavored || EhDB.containLocalFavorites(gd.gid)) {
            mHeart.setVisibility(View.VISIBLE);
            mHeartOutline.setVisibility(View.GONE);
        } else {
            mHeart.setVisibility(View.GONE);
            mHeartOutline.setVisibility(View.VISIBLE);
        }
    }

    private void bindViewSecond() {
        GalleryDetail gd = mGalleryDetail;
        if (gd == null) {
            return;
        }
        if (mThumb == null || mTitle == null || mUploader == null || mCategory == null ||
                mLanguage == null || mPages == null || mSize == null || mPosted == null ||
                mFavoredTimes == null || mRatingText == null || mRating == null || mTorrent == null) {
            return;
        }

        Resources resources = getContext().getResources();

        mThumb.load(EhCacheKeyFactory.getThumbKey(gd.gid), gd.thumb, true);
        mTitle.setText(EhUtils.getSuitableTitle(gd));
        mUploader.setText(gd.uploader);
        mCategory.setText(EhUtils.getCategory(gd.category));
        mCategory.setTextColor(EhUtils.getCategoryColor(gd.category));
        updateDownloadText();

        mLanguage.setText(gd.language);
        mPages.setText(resources.getQuantityString(
                R.plurals.page_count, gd.pages, gd.pages));
        mSize.setText(gd.size);
        mPosted.setText(gd.posted);
        mFavoredTimes.setText(resources.getString(R.string.favored_times, gd.favoredTimes));

        mRatingText.setText(getAllRatingText(gd.rating, gd.ratedTimes));
        mRating.setRating(gd.rating);

        updateFavoriteDrawable();

        mTorrent.setText(resources.getString(R.string.torrent_count, gd.torrentCount));

        bindTags(gd.tags);
        bindComments(gd.comments);
        bindPreviews(gd);
    }

    @SuppressWarnings("deprecation")
    private void bindTags(GalleryTagGroup[] tagGroups) {
        if (mTags == null || mNoTags == null) {
            return;
        }

        mTags.removeViews(1, mTags.getChildCount() - 1);
        if (tagGroups == null || tagGroups.length == 0) {
            mNoTags.setVisibility(View.VISIBLE);
            return;
        } else {
            mNoTags.setVisibility(View.GONE);
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        int colorTag = getResources().getColor(R.color.colorPrimary);
        int colorName = getResources().getColor(R.color.purple_a400);
        for (GalleryTagGroup tg : tagGroups) {
            LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.gallery_tag_group, mTags, false);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            mTags.addView(ll);

            TextView tgName = (TextView) inflater.inflate(R.layout.item_gallery_tag, ll, false);
            ll.addView(tgName);
            tgName.setText(tg.groupName);
            tgName.setBackgroundDrawable(new RoundSideRectDrawable(colorName));

            AutoWrapLayout awl = new AutoWrapLayout(getContext());
            ll.addView(awl, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            for (int j = 0, z = tg.size(); j < z; j++) {
                TextView tag = (TextView) inflater.inflate(R.layout.item_gallery_tag, awl, false);
                awl.addView(tag);
                String tagStr = tg.getTagAt(j);
                tag.setText(tagStr);
                tag.setBackgroundDrawable(new RoundSideRectDrawable(colorTag));
                tag.setTag(R.id.tag, tg.groupName + ":" + tagStr);
                tag.setOnClickListener(this);
            }
        }
    }

    private void bindComments(GalleryComment[] comments) {
        if (mComments == null || mCommentsText == null) {
            return;
        }

        mComments.removeViews(0, mComments.getChildCount() - 1);

        final int maxShowCount = 2;
        if (comments == null || comments.length == 0) {
            mCommentsText.setText(R.string.no_comments);
            return;
        } else if (comments.length <= maxShowCount) {
            mCommentsText.setText(R.string.no_more_comments);
        } else {
            mCommentsText.setText(R.string.more_comment);
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        int length = Math.min(maxShowCount, comments.length);
        for (int i = 0; i < length; i++) {
            GalleryComment comment = comments[i];
            View v = inflater.inflate(R.layout.item_gallery_comment, mComments, false);
            mComments.addView(v, i);
            TextView user = (TextView) v.findViewById(R.id.user);
            user.setText(comment.user);
            TextView time = (TextView) v.findViewById(R.id.time);
            time.setText(ReadableTime.getTimeAgo(comment.time));
            TextView c = (TextView) v.findViewById(R.id.comment);
            c.setMaxLines(5);
            c.setText(Html.fromHtml(comment.comment,
                    new URLImageGetter(c, EhApplication.getConaco(getContext())), null));
        }
    }

    private void bindPreviews(GalleryDetail gd) {
        if (mGridLayout == null || mPreviewText == null) {
            return;
        }

        mGridLayout.removeAllViews();
        LargePreviewSet previewSet = gd.previewSet;
        if (gd.previewPages <= 0 || previewSet == null || previewSet.size() == 0) {
            mPreviewText.setText(R.string.no_previews);
            return;
        } else if (gd.previewPages == 1) {
            mPreviewText.setText(R.string.no_more_previews);
        } else {
            mPreviewText.setText(R.string.more_previews);
        }

        int minWidth = getResources().getDimensionPixelOffset(R.dimen.preview_grid_min_width);
        int spanCount = LayoutUtils2.calculateSpanCount(getContext(), minWidth);
        mGridLayout.setColumnCount(spanCount);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        for (int i = 0, size = previewSet.size(); i < size; i++) {
            View view = inflater.inflate(R.layout.item_gallery_preview, mGridLayout, false);
            mGridLayout.addView(view);

            int index = previewSet.getIndexAt(i);
            String imageUrl = previewSet.getImageUrlAt(i);
            LoadImageView image = (LoadImageView) view.findViewById(R.id.image);
            image.load(EhCacheKeyFactory.getLargePreviewKey(gd.gid, index), imageUrl, true);
            image.setTag(R.id.index, i);
            image.setOnClickListener(this);
            TextView text = (TextView) view.findViewById(R.id.text);
            text.setText(String.format(Locale.US, "%d", index + 1));
        }
    }

    private static String getRatingText(float rating, Resources resources) {
        String undefine = "(´_ゝ`)";
        if (Float.isNaN(rating)) {
            return undefine;
        }

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

        if (resId == 0) {
            return undefine;
        } else {
            return resources.getString(resId);
        }
    }

    private String getAllRatingText(float rating, int ratedTimes) {
        Resources resources = getResources();
        return resources.getString(R.string.rating_text, getRatingText(rating, resources), rating, ratedTimes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTransitionName() {
        long gid = getGid();

        if (gid != -1 && ApiHelper.SUPPORT_TRANSITION && mThumb != null &&
                mTitle != null && mUploader != null && mCategory != null) {
            mThumb.setTransitionName(TransitionNameFactory.getThumbTransitionName(gid));
            mTitle.setTransitionName(TransitionNameFactory.getTitleTransitionName(gid));
            mUploader.setTransitionName(TransitionNameFactory.getUploaderTransitionName(gid));
            mCategory.setTransitionName(TransitionNameFactory.getCategoryTransitionName(gid));
        }
    }

    private void ensurePopMenu() {
        if (mPopupMenu != null) {
            return;
        }

        PopupMenu popup = new PopupMenu(getContext(), mOtherActions, Gravity.TOP);
        mPopupMenu = popup;
        popup.getMenuInflater().inflate(R.menu.scene_gallery_detail, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_open_in_other_app:
                        String url = getGalleryDetailUrl(false);
                        if (url != null) {
                            UrlOpener.openUrl(getActivity(), url, false, false);
                        }
                        break;
                    case R.id.action_refresh:
                        if (mState != STATE_REFRESH && mState != STATE_REFRESH_HEADER) {
                            adjustViewVisibility(STATE_REFRESH, true);
                            request();
                        }
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (mTip == v) {
            if (request()) {
                adjustViewVisibility(STATE_REFRESH, true);
            }
        } else if (mOtherActions == v) {
            ensurePopMenu();
            if (mPopupMenu != null) {
                mPopupMenu.show();
            }
        } else if (mUploader == v) {
            String uploader = getUploader();
            if (TextUtils.isEmpty(uploader)) {
                return;
            }
            ListUrlBuilder lub = new ListUrlBuilder();
            lub.setMode(ListUrlBuilder.MODE_UPLOADER);
            lub.setKeyword(uploader);
            GalleryListScene.startScene(this, lub);
        } else if (mCategory == v) {
            int category = getCategory();
            if (category == -1) {
                return;
            }
            ListUrlBuilder lub = new ListUrlBuilder();
            lub.setCategory(category);
            GalleryListScene.startScene(this, lub);
        } else if (mDownload == v) {
            GalleryInfo galleryInfo = getGalleryInfo();
            if (galleryInfo != null) {
                CommonOperations.startDownload(getActivity(), galleryInfo, false);
            }
        } else if (mRead == v) {
            GalleryInfo galleryInfo = null;
            if (mGalleryInfo != null) {
                galleryInfo = mGalleryInfo;
            } else if (mGalleryDetail != null) {
                galleryInfo = mGalleryDetail;
            }
            if (galleryInfo != null) {
                Intent intent = new Intent(getActivity(), GalleryActivity.class);
                intent.setAction(GalleryActivity.ACTION_EH);
                intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, galleryInfo);
                startActivity(intent);
            }
        } else if (mInfo == v) {
            Bundle args = new Bundle();
            args.putParcelable(GalleryInfoScene.KEY_GALLERY_DETAIL, mGalleryDetail);
            startScene(new Announcer(GalleryInfoScene.class).setArgs(args));
        } else if (mHeartGroup == v) {
            if (mGalleryDetail != null && !mModifingFavorites) {
                boolean remove = false;
                if (EhDB.containLocalFavorites(mGalleryDetail.gid)) {
                    EhDB.removeLocalFavorites(mGalleryDetail.gid);
                    remove = true;
                }
                if (mGalleryDetail.isFavored) {
                    mModifingFavorites = true;
                    CommonOperations.removeFromFavorites(getActivity(), mGalleryDetail,
                            new ModifyFavoritesListener(getContext(),
                                    ((StageActivity) getActivity()).getStageId(), getTag(), true));
                    remove = true;
                }
                if (!remove) {
                    mModifingFavorites = true;
                    CommonOperations.addToFavorites(getActivity(), mGalleryDetail,
                            new ModifyFavoritesListener(getContext(),
                                    ((StageActivity) getActivity()).getStageId(), getTag(), false));
                }
                // Update UI
                updateFavoriteDrawable();
            }
        } else if (mShare == v) {
            String url = getGalleryDetailUrl(false);
            if (url != null) {
                ActivityHelper.share(getActivity(), url);
            }
        } else if (mTorrent == v) {
            if (mGalleryDetail != null) {
                TorrentListDialogHelper helper = new TorrentListDialogHelper(mGalleryDetail.torrentUrl);
                Dialog dialog = new AlertDialog.Builder(getContext())
                        .setTitle(R.string.torrents)
                        .setView(helper.getView())
                        .setOnDismissListener(helper)
                        .show();
                helper.setDialog(dialog);
            }
        } else if (mRate == v) {
            if (mGalleryDetail == null) {
                return;
            }
            RateDialogHelper helper = new RateDialogHelper(mGalleryDetail.rating);
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.rate)
                    .setView(helper.view)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, helper)
                    .show();
        } else if (mComments == v) {
            if (mGalleryDetail == null) {
                return;
            }
            Bundle args = new Bundle();
            args.putLong(GalleryCommentsScene.KEY_GID, mGalleryDetail.gid);
            args.putString(GalleryCommentsScene.KEY_TOKEN, mGalleryDetail.token);
            args.putParcelableArray(GalleryCommentsScene.KEY_COMMENTS, mGalleryDetail.comments);
            startScene(new Announcer(GalleryCommentsScene.class)
                    .setArgs(args)
                    .setRequestCode(this, REQUEST_CODE_COMMENT_GALLERY));
        } else if (mPreviews == v) {
            if (null != mGalleryDetail) {
                Bundle args = new Bundle();
                args.putParcelable(GalleryPreviewsScene.KEY_GALLERY_INFO, mGalleryDetail);
                startScene(new Announcer(GalleryPreviewsScene.class).setArgs(args));
            }
        } else {
            Object o = v.getTag(R.id.tag);
            if (o instanceof String) {
                String tag = (String) o;
                ListUrlBuilder lub = new ListUrlBuilder();
                lub.setMode(ListUrlBuilder.MODE_TAG);
                lub.setKeyword(tag);
                GalleryListScene.startScene(this, lub);
                return;
            }

            Context context = getContext();
            GalleryInfo galleryInfo = getGalleryInfo();
            o = v.getTag(R.id.index);
            if (null != context && null != galleryInfo && o instanceof Integer) {
                int index = (Integer) o;
                Intent intent = new Intent(context, GalleryActivity.class);
                intent.setAction(GalleryActivity.ACTION_EH);
                intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, galleryInfo);
                intent.putExtra(GalleryActivity.KEY_PAGE, index);
                startActivity(intent);
                return;
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mDownload == v) {
            GalleryInfo galleryInfo = getGalleryInfo();
            if (galleryInfo != null) {
                CommonOperations.startDownload(getActivity(), galleryInfo, true);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (ApiHelper.SUPPORT_TRANSITION && mViewTransition != null && mThumb != null &&
                mViewTransition.getShownViewIndex() == 0 && mThumb.isShown()) {
            int[] location = new int[2];
            mThumb.getLocationInWindow(location);
            // Only show transaction when thumb can be seen
            if (location[1] + mThumb.getHeight() > 0) {
                setTransitionName();
                finish(new ExitTransaction(mThumb));
                return;
            }
        }
        finish();
    }

    @Override
    protected void onSceneResult(int requestCode, int resultCode, Bundle data) {
        switch (requestCode) {
            case REQUEST_CODE_COMMENT_GALLERY:
                if (resultCode != RESULT_OK || data == null){
                    break;
                }
                Parcelable[] array = data.getParcelableArray(GalleryCommentsScene.KEY_COMMENTS);
                if (!(array instanceof GalleryComment[])) {
                    break;
                }
                GalleryComment[] comments = (GalleryComment[]) array;
                if (mGalleryDetail == null) {
                    break;
                }
                mGalleryDetail.comments = comments;
                if (!isViewCreated()) {
                    break;
                }
                bindComments(comments);
                break;
            default:
                super.onSceneResult(requestCode, resultCode, data);
        }
    }

    private void updateDownloadText() {
        if (null == mDownload) {
            return;
        }
        switch (mDownloadState) {
            default:
            case DownloadInfo.STATE_INVALID:
            case DownloadInfo.STATE_NONE:
                mDownload.setText(R.string.download);
                break;
            case DownloadInfo.STATE_WAIT:
            case DownloadInfo.STATE_DOWNLOAD:
                mDownload.setText(R.string.download_state_downloading);
                break;
            case DownloadInfo.STATE_FINISH:
            case DownloadInfo.STATE_FAILED:
                mDownload.setText(R.string.download_state_downloaded);
                break;
        }
    }

    private void updateDownloadState() {
        long gid = getGid();
        if (-1L == gid) {
            return;
        }

        int downloadState = EhApplication.getDownloadManager(getContext()).getDownloadState(gid);
        if (downloadState == mDownloadState) {
            return;
        }
        mDownloadState = downloadState;
        updateDownloadText();
    }

    @Override
    public void onAdd(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list, int position) {
        updateDownloadState();
    }

    @Override
    public void onUpdate(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list) {
        updateDownloadState();
    }

    @Override
    public void onUpdateAll() {
        updateDownloadState();
    }

    @Override
    public void onReload() {
        updateDownloadState();
    }

    @Override
    public void onChange() {
        updateDownloadState();
    }

    @Override
    public void onRemove(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list, int position) {
        updateDownloadState();
    }

    @Override
    public void onRenameLabel(String from, String to) {}

    @Override
    public void onUpdateLabels() {}

    private static class ExitTransaction implements TransitionHelper {

        private final View mThumb;

        public ExitTransaction(View thumb) {
            mThumb = thumb;
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public boolean onTransition(Context context,
                FragmentTransaction transaction, Fragment exit, Fragment enter) {
            if (!(enter instanceof GalleryListScene) && !(enter instanceof DownloadsScene) &&
                    !(enter instanceof FavoritesScene) && !(enter instanceof HistoryScene)) {
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
            transaction.addSharedElement(mThumb, mThumb.getTransitionName());
            return true;
        }
    }

    private void onGetGalleryDetailSuccess(GalleryDetail result) {
        mGalleryDetail = result;
        if (isViewCreated()) {
            updateDownloadState();
            adjustViewVisibility(STATE_NORMAL, true);
            bindViewSecond();
        }
    }

    private void onGetGalleryDetailFailure(Exception e) {
        e.printStackTrace();
        if (isViewCreated()) {
            String error = ExceptionUtils.getReadableString(getContext(), e);
            if (mTip != null) {
                mTip.setText(error);
                adjustViewVisibility(STATE_FAILED, true);
            }
        }
    }

    private void onRateGallerySuccess(RateGalleryParser.Result result) {
        if (mGalleryDetail != null) {
            mGalleryDetail.rating = result.rating;
            mGalleryDetail.ratedTimes = result.ratedTimes;
        }

        // Update UI
        if (mRatingText != null && mRating != null) {
            mRatingText.setText(getAllRatingText(result.rating, result.ratedTimes));
            mRating.setRating(result.rating);
        }
    }

    private void onModifyFavoritesSuccess(boolean addOrRemove) {
        mModifingFavorites = false;
        if (mGalleryDetail != null) {
            mGalleryDetail.isFavored = !addOrRemove;
            updateFavoriteDrawable();
        }
    }

    private class ModifyFavoritesListener extends EhCallback<GalleryDetailScene, Void> {

        private final boolean mAddOrRemove;

        /**
         * @param addOrRemove false for add, true for remove
         */
        public ModifyFavoritesListener(Context context, int stageId, String sceneTag, boolean addOrRemove) {
            super(context, stageId, sceneTag);
            mAddOrRemove = addOrRemove;
        }

        @Override
        public void onSuccess(Void result) {
            showTip(mAddOrRemove ? R.string.remove_from_favorite_success :
                    R.string.add_to_favorite_success, LENGTH_SHORT);
            GalleryDetailScene scene = getScene();
            if (scene != null) {
                scene.onModifyFavoritesSuccess(mAddOrRemove);
            }
        }

        @Override
        public void onFailure(Exception e) {
            showTip(mAddOrRemove ? R.string.remove_from_favorite_failure :
                    R.string.add_to_favorite_failure, LENGTH_SHORT);
        }

        @Override
        public void onCancel() {}

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryDetailScene;
        }
    }

    private class TorrentListDialogHelper implements AdapterView.OnItemClickListener,
            DialogInterface.OnDismissListener, EhClient.Callback<Pair<String, String>[]> {

        private final View mView;
        private final ProgressView mProgressView;
        private final TextView mErrorText;
        private final ListView mListView;

        private EhRequest mRequest;
        private Dialog mDialog;

        @SuppressLint("InflateParams")
        public TorrentListDialogHelper(String url) {
            mView = getActivity().getLayoutInflater().inflate(R.layout.dialog_torrent_list, null);
            mProgressView = (ProgressView) ViewUtils.$$(mView, R.id.progress);
            mErrorText = (TextView) ViewUtils.$$(mView, R.id.text);
            mListView = (ListView) ViewUtils.$$(mView, R.id.list_view);
            mListView.setOnItemClickListener(this);

            if (mTorrentList == null) {
                mErrorText.setVisibility(View.GONE);
                mListView.setVisibility(View.GONE);
                mRequest = new EhRequest().setMethod(EhClient.METHOD_GET_TORRENT_LIST)
                        .setArgs(url)
                        .setCallback(this);
                EhApplication.getEhClient(getContext()).execute(mRequest);
            } else {
                bind(mTorrentList);
            }
        }

        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        private void bind(Pair<String, String>[] data) {
            if (0 == data.length) {
                mProgressView.setVisibility(View.GONE);
                mErrorText.setVisibility(View.VISIBLE);
                mListView.setVisibility(View.GONE);
                mErrorText.setText(R.string.no_torrents);
            } else {
                String[] nameArray = new String[data.length];
                for (int i = 0, n = data.length; i < n; i++) {
                    nameArray[i] = data[i].second;
                }
                mProgressView.setVisibility(View.GONE);
                mErrorText.setVisibility(View.GONE);
                mListView.setVisibility(View.VISIBLE);
                mListView.setAdapter(new ArrayAdapter<>(mDialog.getContext(), R.layout.item_select_dialog, nameArray));
            }
        }

        public View getView() {
            return mView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mTorrentList != null && position < mTorrentList.length) {
                String url = mTorrentList[position].first;
                String name = mTorrentList[position].second;
                // Use system download service
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name + ".torrent");
                r.allowScanningByMediaScanner();
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager dm = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                dm.enqueue(r);
            }

            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (mRequest != null) {
                mRequest.cancel();
                mRequest = null;
            }
            mDialog = null;
        }

        @Override
        public void onSuccess(Pair<String, String>[] result) {
            if (mRequest != null) {
                mRequest = null;
                mTorrentList = result;
                bind(result);
            }
        }

        @Override
        public void onFailure(Exception e) {
            if (mRequest != null) {
                mRequest = null;
                mProgressView.setVisibility(View.GONE);
                mErrorText.setVisibility(View.VISIBLE);
                mListView.setVisibility(View.GONE);
                mErrorText.setText(ExceptionUtils.getReadableString(getContext(), e));
            }
        }

        @Override
        public void onCancel() {
            mRequest = null;
        }
    }

    private static class GetGalleryDetailListener extends EhCallback<GalleryDetailScene, GalleryDetail> {

        public GetGalleryDetailListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(GalleryDetail result) {
            getApplication().removeGlobalStuff(this);

            // Put gallery detail to cache
            EhApplication.getGalleryDetailCache(getApplication()).put(result.gid, result);
            EhApplication.getLargePreviewSetCache(getApplication()).put(
                    EhCacheKeyFactory.getLargePreviewSetKey(result.gid, 0), result.previewSet);
            EhApplication.getPreviewPagesCache(getApplication()).put(result.gid, result.previewPages);

            // Add history
            EhDB.putHistoryInfo(result);

            // Notify success
            GalleryDetailScene scene = getScene();
            if (scene != null) {
                scene.onGetGalleryDetailSuccess(result);
            }
        }

        @Override
        public void onFailure(Exception e) {
            getApplication().removeGlobalStuff(this);
            GalleryDetailScene scene = getScene();
            if (scene != null) {
                scene.onGetGalleryDetailFailure(e);
            }
        }

        @Override
        public void onCancel() {
            getApplication().removeGlobalStuff(this);
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryDetailScene;
        }
    }

    private class RateDialogHelper implements ProgressiveRatingBar.OnUserRateListener,
            DialogInterface.OnClickListener {

        public View view;
        private final ProgressiveRatingBar mRatingBar;
        private final TextView mRatingText;

        @SuppressLint("InflateParams")
        private RateDialogHelper(float rating) {
            view = getActivity().getLayoutInflater().inflate(R.layout.dialog_rate, null);
            mRatingText = (TextView) view.findViewById(R.id.rating_text);
            mRatingBar = (ProgressiveRatingBar) view.findViewById(R.id.rating_view);
            mRatingText.setText(getRatingText(rating, getResources()));
            mRatingBar.setRating(rating);
            mRatingBar.setOnUserRateListener(this);
        }

        @Override
        public void onUserRate(float rating) {
            mRatingText.setText(getRatingText(rating, getResources()));
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE || mGalleryDetail == null) {
                return;
            }

            EhRequest request = new EhRequest()
                    .setMethod(EhClient.METHOD_GET_RATE_GALLERY)
                    .setArgs(mGalleryDetail.gid, mGalleryDetail.token, mRatingBar.getRating())
                    .setCallback(new RateGalleryListener(getContext(),
                            ((StageActivity) getActivity()).getStageId(), getTag(), mGalleryDetail.gid));
            EhApplication.getEhClient(getContext()).execute(request);
        }
    }

    private static class RateGalleryListener extends EhCallback<GalleryDetailScene, RateGalleryParser.Result> {

        private final long mGid;

        public RateGalleryListener(Context context, int stageId, String sceneTag, long gid) {
            super(context, stageId, sceneTag);
            mGid = gid;
        }

        @Override
        public void onSuccess(RateGalleryParser.Result result) {
            showTip(R.string.rate_successfully, LENGTH_SHORT);

            GalleryDetailScene scene = getScene();
            if (scene != null) {
                scene.onRateGallerySuccess(result);
            } else {
                // Update rating in cache
                GalleryDetail gd = EhApplication.getGalleryDetailCache(getApplication()).get(mGid);
                if (gd != null) {
                    gd.rating = result.rating;
                    gd.ratedTimes = result.ratedTimes;
                }
            }
        }

        @Override
        public void onFailure(Exception e) {
            e.printStackTrace();
            showTip(R.string.rate_failed, LENGTH_SHORT);
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryDetailScene;
        }
    }
}
