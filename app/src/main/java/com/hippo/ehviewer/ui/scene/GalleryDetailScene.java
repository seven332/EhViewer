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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.transition.TransitionInflater;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.UrlOpener;
import com.hippo.ehviewer.R;
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
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.scene.TransitionHelper;
import com.hippo.text.Html;
import com.hippo.text.URLImageGetter;
import com.hippo.util.ActivityHelper;
import com.hippo.util.ApiHelper;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.ReadableTime;
import com.hippo.vector.AnimatedVectorDrawable;
import com.hippo.vector.VectorDrawable;
import com.hippo.view.ViewTransition;
import com.hippo.widget.AutoWrapLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.ProgressiveRatingBar;
import com.hippo.widget.SimpleGridLayout;
import com.hippo.widget.SimpleImageView;
import com.hippo.yorozuya.SimpleHandler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

// TODO Update drawer checked item
public class GalleryDetailScene extends BaseScene implements View.OnClickListener {

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

    private ViewGroup mFailedView;
    private TextView mFailedText;
    private ViewTransition mViewTransition;

    // Header
    private View mHeader;
    private View mColorBg;
    private LoadImageView mThumb;
    private TextView mTitle;
    private TextView mUploader;
    private TextView mCategory;
    private SimpleImageView mOtherActions;
    private ViewGroup mActionGroup;
    private View mDownload;
    private View mRead;
    // Below header
    private View mBelowHeader;
    // Info
    private View mInfo;
    private TextView mLanguage;
    private TextView mPages;
    private TextView mSize;
    private TextView mPosted;
    private TextView mFavoredTimes;
    // Actions
    private View mActions;
    private TextView mRatingText;
    private RatingBar mRating;
    private View mHeartGroup;
    private TextView mHeart;
    private TextView mHeartOutline;
    private TextView mTorrent;
    private TextView mShare;
    private TextView mRate;
    // Tags
    private LinearLayout mTags;
    private TextView mNoTags;
    // Comments
    private LinearLayout mComments;
    private TextView mCommentsText;
    // Previews
    private View mPreviews;
    private SimpleGridLayout mGridLayout;
    private TextView mPreviewText;
    // Progress
    private View mProgress;

    private ViewTransition mViewTransition2;

    private AnimatedVectorDrawable mHeartDrawable;
    private AnimatedVectorDrawable mHeartOutlineDrawable;
    private PopupMenu mPopupMenu;

    @Nullable
    private String mAction;
    @Nullable
    private GalleryInfo mGalleryInfo;
    private int mGid;
    private String mToken;

    @Nullable
    private GalleryDetail mGalleryDetail;
    private int mRequestId;

    @State
    private int mState = STATE_INIT;

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
            mToken = args.getString(KEY_TOKEN);
        }
    }

    @Nullable
    private String getGalleryDetailUrl(boolean allComment) {
        int gid;
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
    private int getGid() {
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void onInit() {
        handleArgs(getArguments());
    }

    private void onRestore(Bundle savedInstanceState) {
        mAction = savedInstanceState.getString(KEY_ACTION);
        mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO);
        mGid = savedInstanceState.getInt(KEY_GID);
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
        outState.putInt(KEY_GID, mGid);
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

        ViewGroup main = (ViewGroup) view.findViewById(R.id.main);
        View mainView = main.findViewById(R.id.scroll_view);
        View progressView = main.findViewById(R.id.progress_view);
        mFailedView = (ViewGroup) main.findViewById(R.id.tip);
        SimpleImageView mFailedImage = (SimpleImageView) mFailedView.getChildAt(0);
        mFailedText = (TextView) mFailedView.getChildAt(1);
        mFailedView.setOnClickListener(this);
        mFailedImage.setDrawable(VectorDrawable.create(getContext(), R.xml.sadpanda_head));
        mViewTransition = new ViewTransition(mainView, progressView, mFailedView);

        mHeader = mainView.findViewById(R.id.header);
        mColorBg = mainView.findViewById(R.id.color_bg);
        mThumb = (LoadImageView) mHeader.findViewById(R.id.thumb);
        mTitle = (TextView) mHeader.findViewById(R.id.title);
        mUploader = (TextView) mHeader.findViewById(R.id.uploader);
        mCategory = (TextView) mHeader.findViewById(R.id.category);
        mOtherActions = (SimpleImageView) mHeader.findViewById(R.id.other_actions);
        mActionGroup = (ViewGroup) mHeader.findViewById(R.id.action_card);
        mDownload = mActionGroup.findViewById(R.id.download);
        mRead = mActionGroup.findViewById(R.id.read);
        mOtherActions.setDrawable(VectorDrawable.create(getContext(), R.xml.ic_dots_vertical_secondary_dark_x24));
        RippleSalon.addRipple(mOtherActions, false);
        RippleSalon.addRipple(mDownload, false);
        RippleSalon.addRipple(mRead, false);
        mUploader.setOnClickListener(this);
        mCategory.setOnClickListener(this);
        mOtherActions.setOnClickListener(this);
        mDownload.setOnClickListener(this);
        mRead.setOnClickListener(this);

        mBelowHeader = mainView.findViewById(R.id.below_header);
        View belowHeader = mBelowHeader;

        mInfo = belowHeader.findViewById(R.id.info);
        mLanguage = (TextView) mInfo.findViewById(R.id.language);
        mPages = (TextView) mInfo.findViewById(R.id.pages);
        mSize = (TextView) mInfo.findViewById(R.id.size);
        mPosted = (TextView) mInfo.findViewById(R.id.posted);
        mFavoredTimes = (TextView) mInfo.findViewById(R.id.favoredTimes);
        RippleSalon.addRipple(mInfo, false);
        mInfo.setOnClickListener(this);

        mActions = belowHeader.findViewById(R.id.actions);
        mRatingText = (TextView) mActions.findViewById(R.id.rating_text);
        mRating = (RatingBar) mActions.findViewById(R.id.rating);
        mHeartGroup = mActions.findViewById(R.id.heart_group);
        mHeart = (TextView) mHeartGroup.findViewById(R.id.heart);
        mHeartOutline = (TextView) mHeartGroup.findViewById(R.id.heart_outline);
        mTorrent = (TextView) mActions.findViewById(R.id.torrent);
        mShare = (TextView) mActions.findViewById(R.id.share);
        mRate = (TextView) mActions.findViewById(R.id.rate);
        RippleSalon.addRipple(mHeartGroup, false);
        RippleSalon.addRipple(mTorrent, false);
        RippleSalon.addRipple(mShare, false);
        RippleSalon.addRipple(mRate, false);
        mHeartGroup.setOnClickListener(this);
        mTorrent.setOnClickListener(this);
        mShare.setOnClickListener(this);
        mRate.setOnClickListener(this);

        mTags = (LinearLayout) belowHeader.findViewById(R.id.tags);
        mNoTags = (TextView) mTags.findViewById(R.id.no_tags);

        mComments = (LinearLayout) belowHeader.findViewById(R.id.comments);
        mCommentsText = (TextView) mComments.findViewById(R.id.comments_text);
        RippleSalon.addRipple(mComments, false);
        mComments.setOnClickListener(this);

        mPreviews = belowHeader.findViewById(R.id.previews);
        mGridLayout = (SimpleGridLayout) mPreviews.findViewById(R.id.grid_layout);
        mPreviewText = (TextView) mPreviews.findViewById(R.id.preview_text);
        RippleSalon.addRipple(mPreviews, false);
        mPreviews.setOnClickListener(this);

        mProgress = mainView.findViewById(R.id.progress);

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
            mFailedText.setText(R.string.error_cannot_find_gallery);
            adjustViewVisibility(STATE_FAILED, false);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Clear nav checked item
        setNavCheckedItem(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mFailedView = null;
        mFailedText = null;
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

        mHeartDrawable = null;
        mHeartOutlineDrawable = null;
        mPopupMenu = null;
    }

    private boolean prepareData() {
        if (mGalleryDetail != null) {
            return true;
        }

        int gid = getGid();
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean createCircularReveal() {
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

        if (ACTION_GALLERY_INFO.equals(mAction) && mGalleryInfo != null) {
            GalleryInfo gi = mGalleryInfo;
            mThumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb, true);
            mTitle.setText(EhUtils.getSuitableTitle(gi));
            mUploader.setText(gi.uploader);
            mCategory.setText(EhUtils.getCategory(gi.category));
            mCategory.setTextColor(EhUtils.getCategoryColor(gi.category));
        }
    }

    private void setActionDrawable(TextView text, Drawable drawable) {
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        text.setCompoundDrawables(null, drawable, null, null);
    }

    private void ensureActionDrawable() {
        if (mHeartDrawable != null) {
            return;
        }

        mHeartDrawable = AnimatedVectorDrawable.create(getContext(), R.xml.ic_heart_animated);
        setActionDrawable(mHeart, mHeartDrawable);

        mHeartOutlineDrawable = AnimatedVectorDrawable.create(getContext(), R.xml.ic_heart_outline_animated);
        setActionDrawable(mHeartOutline, mHeartOutlineDrawable);

        VectorDrawable torrent = VectorDrawable.create(getContext(), R.xml.ic_utorrent_primary_x48);
        setActionDrawable(mTorrent, torrent);

        VectorDrawable share = VectorDrawable.create(getContext(), R.xml.ic_share_primary_x48);
        setActionDrawable(mShare, share);

        VectorDrawable rate = VectorDrawable.create(getContext(), R.xml.ic_thumb_up_primary_x48);
        setActionDrawable(mRate, rate);
    }

    private void bindViewSecond() {
        GalleryDetail gd = mGalleryDetail;

        if (gd == null) {
            return;
        }

        Resources resources = getContext().getResources();

        mThumb.load(EhCacheKeyFactory.getThumbKey(gd.gid), gd.thumb, true);
        mTitle.setText(EhUtils.getSuitableTitle(gd));
        mUploader.setText(gd.uploader);
        mCategory.setText(EhUtils.getCategory(gd.category));
        mCategory.setTextColor(EhUtils.getCategoryColor(gd.category));

        mLanguage.setText(gd.language);
        mPages.setText(resources.getQuantityString(
                R.plurals.page_count, gd.pages, gd.pages));
        mSize.setText(gd.size);
        mPosted.setText(gd.posted);
        mFavoredTimes.setText(resources.getString(R.string.favored_times, gd.favoredTimes));

        mRatingText.setText(getAllRatingText(gd.rating, gd.ratedTimes));
        mRating.setRating(gd.rating);

        ensureActionDrawable();
        if (gd.isFavored) {
            mHeart.setVisibility(View.VISIBLE);
            mHeartOutline.setVisibility(View.GONE);
        } else {
            mHeart.setVisibility(View.GONE);
            mHeartOutline.setVisibility(View.VISIBLE);
        }
        mTorrent.setText(resources.getString(R.string.torrent_count, gd.torrentCount));

        bindTags(gd.tags);
        bindComments(gd.comments);
        bindPreviews(gd);
    }

    @SuppressWarnings("deprecation")
    private void bindTags(GalleryTagGroup[] tagGroups) {
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
        for (int i = 0, s = tagGroups.length; i < s; i++) {
            GalleryTagGroup tg = tagGroups[i];

            LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.gallery_tag_group, mTags, false);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            mTags.addView(ll);

            TextView tgName = (TextView) inflater.inflate(R.layout.item_gallery_tag, ll, false);
            ll.addView(tgName);
            tgName.setText(tg.groupName);
            tgName.setBackgroundColor(colorName);

            AutoWrapLayout awl = new AutoWrapLayout(getContext());
            ll.addView(awl, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            for (int j = 0, z = tg.size(); j < z; j++) {
                TextView tag = (TextView) inflater.inflate(R.layout.item_gallery_tag, awl, false);
                awl.addView(tag);
                String tagStr = tg.getTagAt(j);
                tag.setText(tagStr);
                tag.setBackgroundColor(colorTag);
                tag.setTag(R.id.tag, tagStr);
                tag.setOnClickListener(this);
            }
        }
    }

    private void bindComments(GalleryComment[] comments) {
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

        LayoutInflater inflater = getActivity().getLayoutInflater();
        for (int i = 0, size = previewSet.size(); i < size; i++) {
            View view = inflater.inflate(R.layout.item_gallery_preview, mGridLayout, false);
            mGridLayout.addView(view);

            int index = previewSet.getIndexAt(i);
            String imageUrl = previewSet.getImageUrlAt(i);
            LoadImageView image = (LoadImageView) view.findViewById(R.id.image);
            image.load(EhCacheKeyFactory.getLargePreviewKey(gd.gid, index), imageUrl, true);
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
        int gid = getGid();

        if (gid != -1 && ApiHelper.SUPPORT_TRANSITION) {
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
        popup.getMenuInflater().inflate(R.menu.gallery_detail, popup.getMenu());
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
        if (mFailedView == v) {
            if (request()) {
                adjustViewVisibility(STATE_REFRESH, true);
            }
        } else if (mOtherActions == v) {
            ensurePopMenu();
            mPopupMenu.show();
        } else if (mUploader == v) {
            String uploader = getUploader();
            if (TextUtils.isEmpty(uploader)) {
                return;
            }
            ListUrlBuilder lub = new ListUrlBuilder();
            lub.setMode(ListUrlBuilder.MODE_UPLOADER);
            lub.setKeyword(EhUtils.getSpecifyUploaderKeyword(uploader));
            GalleryListScene.startScene(this, lub);
        } else if (mCategory == v) {
            int category = getCategory();
            if (category == -1) {
                return;
            }
            ListUrlBuilder lub = new ListUrlBuilder();
            lub.setCategory(category);
            GalleryListScene.startScene(this, lub);
        } else if (mInfo == v) {
            Bundle args = new Bundle();
            args.putParcelable(GalleryInfoScene.KEY_GALLERY_DETAIL, mGalleryDetail);
            startScene(new Announcer(GalleryInfoScene.class).setArgs(args));
        } else if (mShare == v) {
            String url = getGalleryDetailUrl(false);
            if (url != null) {
                ActivityHelper.share(getActivity(), url);
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
            args.putInt(GalleryCommentsScene.KEY_GID, mGalleryDetail.gid);
            args.putString(GalleryCommentsScene.KEY_TOKEN, mGalleryDetail.token);
            args.putParcelableArray(GalleryCommentsScene.KEY_COMMENTS, mGalleryDetail.comments);
            startScene(new Announcer(GalleryCommentsScene.class)
                    .setArgs(args)
                    .setRequestCode(this, REQUEST_CODE_COMMENT_GALLERY));
        } else if (mPreviews == v) {
            int gid = getGid();
            String token = getToken();
            if (gid == -1 || token == null) {
                return;
            }
            Bundle args = new Bundle();
            args.putInt(GalleryPreviewsScene.KEY_GID, gid);
            args.putString(GalleryPreviewsScene.KEY_TOKEN, token);
            startScene(new Announcer(GalleryPreviewsScene.class).setArgs(args));
        } else {
            Object tag;
            tag = v.getTag(R.id.tag);
            if (tag instanceof String) {
                String tagStr = (String) tag;
                ListUrlBuilder lub = new ListUrlBuilder();
                lub.setMode(ListUrlBuilder.MODE_TAG);
                lub.setKeyword(tagStr);
                GalleryListScene.startScene(this, lub);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mViewTransition.getShownViewIndex() == 0 && ApiHelper.SUPPORT_TRANSITION) {
            int[] location = new int[2];
            mThumb.getLocationInWindow(location);
            // Only show transaction when thumb can be seen
            if (location[1] + mThumb.getHeight() > 0) {
                finish(new EnterGalleryListTransaction(mThumb, mTitle, mUploader, mCategory));
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

    private static class EnterGalleryListTransaction implements TransitionHelper {

        private View mThumb;
        private View mTitle;
        private View mUploader;
        private View mCategory;

        public EnterGalleryListTransaction(View thumb, View title, View uploader, View category) {
            mThumb = thumb;
            mTitle = title;
            mUploader = uploader;
            mCategory = category;
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public boolean onTransition(Context context,
                FragmentTransaction transaction, Fragment exit, Fragment enter) {
            if (!(enter instanceof GalleryListScene)) {
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
            transaction.addSharedElement(mTitle, mTitle.getTransitionName());
            transaction.addSharedElement(mUploader, mUploader.getTransitionName());
            transaction.addSharedElement(mCategory, mCategory.getTransitionName());
            return true;
        }
    }

    private void onGetGalleryDetailSuccess(GalleryDetail result) {
        mGalleryDetail = result;
        if (isViewCreated()) {
            adjustViewVisibility(STATE_NORMAL, true);
            bindViewSecond();
        }
    }

    private void onGetGalleryDetailFailure(Exception e) {
        e.printStackTrace();
        if (isViewCreated()) {
            String error = ExceptionUtils.getReadableString(getContext(), e);
            mFailedText.setText(error);
            adjustViewVisibility(STATE_FAILED, true);
        }
    }

    private void onRateGallerySuccess(RateGalleryParser.Result result) {
        if (mGalleryDetail != null) {
            mGalleryDetail.rating = result.rating;
            mGalleryDetail.ratedTimes = result.ratedTimes;
        }

        // Update UI
        if (isViewCreated()) {
            mRatingText.setText(getAllRatingText(result.rating, result.ratedTimes));
            mRating.setRating(result.rating);
        }
    }

    private static class GetGalleryDetailListener implements EhClient.Callback<GalleryDetail> {

        private EhApplication mApplication;
        private int mStageId;
        private String mSceneTag;

        public GetGalleryDetailListener(Context context, int stageId, String sceneTag) {
            mApplication = (EhApplication) context.getApplicationContext();
            mStageId = stageId;
            mSceneTag = sceneTag;
        }

        private GalleryDetailScene getScene() {
            StageActivity stage = mApplication.findStageActivityById(mStageId);
            if (stage == null) {
                return null;
            }
            SceneFragment scene = stage.findSceneByTag(mSceneTag);
            if (scene instanceof GalleryDetailScene) {
                return (GalleryDetailScene) scene;
            } else {
                return null;
            }
        }

        @Override
        public void onSuccess(GalleryDetail result) {
            mApplication.removeGlobalStuff(this);

            // Put gallery detail to cache
            EhApplication.getGalleryDetailCache(mApplication).put(result.gid, result);
            EhApplication.getLargePreviewSetCache(mApplication).put(
                    EhCacheKeyFactory.getLargePreviewSetKey(result.gid, 0), result.previewSet);
            EhApplication.getPreviewPagesCache(mApplication).put(result.gid, result.previewPages);

            // Notify success
            GalleryDetailScene scene = getScene();
            if (scene != null) {
                scene.onGetGalleryDetailSuccess(result);
            }
        }

        @Override
        public void onFailure(Exception e) {
            mApplication.removeGlobalStuff(this);
            GalleryDetailScene scene = getScene();
            if (scene != null) {
                scene.onGetGalleryDetailFailure(e);
            }
        }

        @Override
        public void onCancel() {
            mApplication.removeGlobalStuff(this);
        }
    }

    private class RateDialogHelper implements ProgressiveRatingBar.OnUserRateListener,
            DialogInterface.OnClickListener {

        public View view;
        private ProgressiveRatingBar mRatingBar;
        private TextView mRatingText;

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

    private static class RateGalleryListener implements EhClient.Callback<RateGalleryParser.Result> {

        private EhApplication mApplication;
        private int mStageId;
        private String mSceneTag;
        private int mGid;

        public RateGalleryListener(Context context, int stageId, String sceneTag, int gid) {
            mApplication = (EhApplication) context.getApplicationContext();
            mStageId = stageId;
            mSceneTag = sceneTag;
            mGid = gid;
        }

        private GalleryDetailScene getScene() {
            StageActivity stage = mApplication.findStageActivityById(mStageId);
            if (stage == null) {
                return null;
            }
            SceneFragment scene = stage.findSceneByTag(mSceneTag);
            if (scene instanceof GalleryDetailScene) {
                return (GalleryDetailScene) scene;
            } else {
                return null;
            }
        }

        @Override
        public void onSuccess(RateGalleryParser.Result result) {
            // Show toast
            Toast.makeText(mApplication, R.string.rate_successfully, Toast.LENGTH_SHORT).show();

            GalleryDetailScene scene = getScene();
            if (scene != null) {
                scene.onRateGallerySuccess(result);
            } else {
                // Update rating in cache
                GalleryDetail gd = EhApplication.getGalleryDetailCache(mApplication).get(mGid);
                if (gd != null) {
                    gd.rating = result.rating;
                    gd.ratedTimes = result.ratedTimes;
                }
            }
        }

        @Override
        public void onFailure(Exception e) {
            e.printStackTrace();
            // Show toast
            Toast.makeText(mApplication, R.string.rate_failed, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel() {
        }
    }
}
