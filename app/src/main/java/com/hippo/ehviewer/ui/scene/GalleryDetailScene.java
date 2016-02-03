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
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hippo.ehviewer.EhApplication;
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
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.text.Html;
import com.hippo.text.URLImageGetter;
import com.hippo.utils.ApiHelper;
import com.hippo.utils.ExceptionUtils;
import com.hippo.utils.ReadableTime;
import com.hippo.vector.AnimatedVectorDrawable;
import com.hippo.vector.VectorDrawable;
import com.hippo.widget.AutoWrapLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.SimpleGridLayout;
import com.hippo.widget.SimpleImageView;

import java.util.Locale;

// TODO Update drawer checked item
public class GalleryDetailScene extends BaseScene implements View.OnClickListener {

    public final static String KEY_ACTION = "action";
    public static final String ACTION_GALLERY_INFO = "action_gallery_info";
    public static final String ACTION_GID_TOKEN = "action_gid_token";

    public static final String KEY_GALLERY_INFO = "gallery_info";
    public static final String KEY_GID = "gid";
    public static final String KEY_TOKEN = "token";

    private static final String KEY_GALLERY_DETAIL = "gallery_detail";
    private static final String KEY_REQUEST_ID = "request_id";

    private ScrollView mMainView;
    private View mProgressView;
    private ViewGroup mFailedView;
    private TextView mFailedText;

    // Header
    private View mHeader;
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

    private AnimatedVectorDrawable mHeartDrawable;
    private AnimatedVectorDrawable mHeartOutlineDrawable;
    private PopupMenu mPopupMenu;

    private String mAction;
    private GalleryInfo mGalleryInfo;
    private int mGid;
    private String mToken;

    private GalleryDetail mGalleryDetail;
    private int mRequestId;

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
    private String getGalleryDetailUrl() {
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
        return EhUrl.getGalleryDetailUrl(gid, token);
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
        mMainView = (ScrollView) main.findViewById(R.id.scroll_view);
        mProgressView = main.findViewById(R.id.progress_view);
        mFailedView = (ViewGroup) main.findViewById(R.id.tip);
        mFailedText = (TextView) mFailedView.getChildAt(1);
        mFailedView.setOnClickListener(this);

        View mainView = mMainView;
        mHeader = mainView.findViewById(R.id.header);
        mThumb = (LoadImageView) mHeader.findViewById(R.id.thumb);
        mTitle = (TextView) mHeader.findViewById(R.id.title);
        mUploader = (TextView) mHeader.findViewById(R.id.uploader);
        mCategory = (TextView) mHeader.findViewById(R.id.category);
        mOtherActions = (SimpleImageView) mHeader.findViewById(R.id.other_actions);
        mActionGroup = (ViewGroup) mHeader.findViewById(R.id.action_card);
        mDownload = mActionGroup.findViewById(R.id.download);
        mRead = mActionGroup.findViewById(R.id.read);
        mOtherActions.setDrawable(VectorDrawable.create(getContext(), R.drawable.ic_dots_vertical));
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

        mPreviews = belowHeader.findViewById(R.id.previews);
        mGridLayout = (SimpleGridLayout) mPreviews.findViewById(R.id.grid_layout);
        mPreviewText = (TextView) mPreviews.findViewById(R.id.preview_text);

        mProgress = mainView.findViewById(R.id.progress);

        if (prepareData()) {
            if (mGalleryDetail != null) {
                bindViewSecond();
                setTransitionName();
            } else if (mGalleryInfo != null) {
                bindViewFirst();
                setTransitionName();
            }
        } else {
            mFailedText.setText(R.string.cannot_find_gallery);
        }
        adjustViewVisibility();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mMainView = null;
        mProgressView = null;
        mFailedView = null;
        mFailedText = null;

        mHeader = null;
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
        String url = getGalleryDetailUrl();
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

    private void adjustViewVisibility() {
        if (mGalleryDetail != null) {
            mMainView.setVisibility(View.VISIBLE);
            mProgressView.setVisibility(View.GONE);
            mFailedView.setVisibility(View.GONE);

            mBelowHeader.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.GONE);
        } else if (mGalleryInfo != null) {
            mMainView.setVisibility(View.VISIBLE);
            mProgressView.setVisibility(View.GONE);
            mFailedView.setVisibility(View.GONE);

            mBelowHeader.setVisibility(View.GONE);
            mProgress.setVisibility(View.VISIBLE);
        } else {
            mMainView.setVisibility(View.GONE);
            if (TextUtils.isEmpty(mFailedText.getText())) {
                mProgressView.setVisibility(View.VISIBLE);
                mFailedView.setVisibility(View.GONE);
            } else {
                mProgressView.setVisibility(View.GONE);
                mFailedView.setVisibility(View.VISIBLE);
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
            mTitle.setText(gi.title);
            mUploader.setText(gi.uploader);
            mCategory.setText(EhUtils.getCategory(gi.category));
            mCategory.setTextColor(EhUtils.getCategoryColor(gi.category));
        }
    }

    private void ensureHeartDrawable() {
        if (mHeartDrawable == null) {
            mHeartDrawable = AnimatedVectorDrawable.create(getContext(), R.drawable.ic_heart_animated);
            if (mHeartDrawable == null) {
                throw new IllegalStateException("Can't parse heart drawable");
            }
            mHeartDrawable.setBounds(0, 0, mHeartDrawable.getIntrinsicWidth(),
                    mHeartDrawable.getIntrinsicHeight());
            mHeart.setCompoundDrawables(null, mHeartDrawable, null, null);
        }

        if (mHeartOutlineDrawable == null) {
            mHeartOutlineDrawable = AnimatedVectorDrawable.create(getContext(), R.drawable.ic_heart_outline_animated);
            if (mHeartOutlineDrawable == null) {
                throw new IllegalStateException("Can't parse heart drawable");
            }
            mHeartOutlineDrawable.setBounds(0, 0, mHeartOutlineDrawable.getIntrinsicWidth(),
                    mHeartOutlineDrawable.getIntrinsicHeight());
            mHeartOutline.setCompoundDrawables(null, mHeartOutlineDrawable, null, null);
        }
    }

    private void bindViewSecond() {
        GalleryDetail gd = mGalleryDetail;

        if (gd == null) {
            return;
        }

        Resources resources = getContext().getResources();

        if (TextUtils.isEmpty(mCategory.getText())) {
            mThumb.load(EhCacheKeyFactory.getThumbKey(gd.gid), gd.thumb, true);
            mTitle.setText(gd.title);
            mUploader.setText(gd.uploader);
            mCategory.setText(EhUtils.getCategory(gd.category));
            mCategory.setTextColor(EhUtils.getCategoryColor(gd.category));
        }

        mLanguage.setText(gd.language);
        mPages.setText(resources.getQuantityString(
                R.plurals.page_count, gd.pages, gd.pages));
        mSize.setText(gd.size);
        mPosted.setText(gd.posted);
        mFavoredTimes.setText(resources.getString(R.string.favored_times, gd.favoredTimes));

        mRatingText.setText(getAllRatingText(gd.rating, gd.ratedTimes));
        mRating.setRating(gd.rating);

        ensureHeartDrawable();
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
        RippleSalon.addRipple(mComments, false);
        mComments.setOnClickListener(this);

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
            View view = inflater.inflate(R.layout.item_preview, mGridLayout, false);
            mGridLayout.addView(view);

            int index = previewSet.getIndexAt(i);
            String imageUrl = previewSet.getImageUrlAt(i);
            LoadImageView image = (LoadImageView) view.findViewById(R.id.image);
            image.load(EhCacheKeyFactory.getLargePreviewKey(gd.gid, index), imageUrl, true);
            TextView text = (TextView) view.findViewById(R.id.text);
            text.setText(String.format(Locale.US, "%d", index));
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
        return resources.getString(R.string.rating_text, getRatingText(rating, resources), ratedTimes);
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
        popup.getMenuInflater().inflate(R.menu.gallery_detail_other_action, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_open_in_other_app:
                        // TODO
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
                mFailedText.setText("");
                adjustViewVisibility();
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
            startScene(GalleryInfoScene.class, args);
        } else if (mComments == v) {
            Bundle args = new Bundle();
            args.putParcelableArray(GalleryCommentsScene.KEY_COMMENTS, mGalleryDetail.comments);
            startScene(GalleryCommentsScene.class, args);
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

    private void onGetGalleryDetailSuccess(GalleryDetail result) {
        mGalleryDetail = result;
        adjustViewVisibility();
        bindViewSecond();
    }

    private void onGetGalleryDetailFailure(Exception e) {
        String error = ExceptionUtils.getReadableString(getContext(), e);
        mFailedText.setText(error);
        adjustViewVisibility();
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
}
