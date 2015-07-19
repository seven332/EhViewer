/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.ehviewer.ui.scene;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.hippo.conaco.Conaco;
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhException;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.OffensiveException;
import com.hippo.ehviewer.client.data.Comment;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.data.TagGroup;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Settings;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.PreviewLayout;
import com.hippo.ehviewer.widget.RatingView;
import com.hippo.miscellaneous.StartActivityHelper;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.RippleCurtain;
import com.hippo.scene.Scene;
import com.hippo.scene.SimpleDialog;
import com.hippo.util.URLImageGetter;
import com.hippo.widget.AccurateClick;
import com.hippo.widget.AutoWrapLayout;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.Say;
import com.hippo.yorozuya.ViewUtils;

import java.util.List;

import static com.hippo.ehviewer.R.id.no_tags;

// TODO Use RecyclerView instead of ScrollView. It will improve swipe horizontally and animation
public class GalleryDetailScene extends Scene implements View.OnClickListener,
        AccurateClick.OnAccurateClickListener {

    private static final String TAG = GalleryDetailScene.class.getSimpleName();

    public static final String ACTION_GALLERY_INFO = "action_gallery_info";
    public static final String ACTION_GID_TOKEN = "action_gid_token";
    public static final String ACTION_URL = "action_url";

    public static final String KEY_GALLERY_INFO = "key_gallery_info";
    public static final String KEY_GID = "key_gid";
    public static final String KEY_TOKEN = "key_token";
    public static final String KEY_URL = "key_url";

    private FrameLayout mDetailFrameLayout;

    private ViewGroup mHeader;
    LoadImageView mThumb;
    TextView mTitle;
    TextView mUploader;
    TextView mCategory;

    private ViewGroup mActionCard;
    private View mRead;
    private View mDownload;

    private ViewGroup mContent;
    private ViewGroup mInfo;
    private TextView mLanguage;
    private TextView mPages;
    private TextView mSize;
    private TextView mPosted;
    private TextView mResize;
    private TextView mFavoredTimes;
    private TextSwitcher mRatingText;
    private RatingView mRating;
    private TextView mFavorite;
    private TextView mTorrent;
    private TextView mShare;
    private TextView mRate;
    private LinearLayout mTag;
    private TextView mNoTags;
    private LinearLayout mComment;
    private TextView mCommentMore;
    private PreviewLayout mPreviewLayout;

    private View mProgressBar;

    private ViewTransition mViewTransition;

    private GalleryDetail mGalleryDetail;

    private void requestGalleryDetail(GalleryInfo gi) {
        EhClient client = EhApplication.getEhClient(getStageActivity());
        int source = Settings.getEhSource();
        String url = EhUrl.getDetailUrl(source, gi.gid, gi.token, 0);

        EhRequest request = new EhRequest();
        request.setMethod(EhClient.METHOD_GET_GALLERY_DETAIL);
        request.setEhListener(new EhDetailListener());
        request.setArgs(url, source);
        // TODO request.setEhConfig();
        client.execute(request);
    }

    private void handleAnnouncer(Announcer announcer) {
        if (announcer == null) {
            Say.e(TAG, "No announcer in GalleryDetailScene, finish itself");
            finish();
            return;
        }

        String action = announcer.getAction();
        if (ACTION_GALLERY_INFO.equals(action)) {
            GalleryInfo gi = announcer.getExtra(KEY_GALLERY_INFO, GalleryInfo.class);
            if (gi != null) {

                Conaco conaco = EhApplication.getConaco(getStageActivity());
                mThumb.load(conaco, EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb);
                String title;
                if (Settings.getJpnTitle()) {
                    title = TextUtils.isEmpty(gi.titleJpn) ? gi.title : gi.titleJpn;
                } else {
                    title = gi.title;
                }
                mTitle.setText(title);
                mUploader.setText(gi.uploader);
                mCategory.setText(EhUtils.getCategory(gi.category));
                mCategory.setTextColor(EhUtils.getCategoryColor(gi.category));

                requestGalleryDetail(gi);


                Context context = getStageActivity();
                Resources resources = context.getResources();

                mViewTransition.showView(1, false);

                mRatingText.setFactory(new RatingTextViewFactory());

                Drawable favoriteDrawable = resources.getDrawable(R.drawable.ic_heart_theme_primary);
                Drawable torrentDrawable = resources.getDrawable(R.drawable.ic_water_pump_theme_primary);
                Drawable shareDrawable = resources.getDrawable(R.drawable.ic_share_theme_primary);
                Drawable rateDrawable = resources.getDrawable(R.drawable.ic_thumbs_up_down_theme_primary);

                int drawableSize = resources.getDimensionPixelOffset(R.dimen.detail_action_size);
                favoriteDrawable.setBounds(0, 0, drawableSize, drawableSize);
                torrentDrawable.setBounds(0, 0, drawableSize, drawableSize);
                shareDrawable.setBounds(0, 0, drawableSize, drawableSize);
                rateDrawable.setBounds(0, 0, drawableSize, drawableSize);

                mFavorite.setCompoundDrawables(null, favoriteDrawable, null, null);
                mTorrent.setCompoundDrawables(null, torrentDrawable, null, null);
                mShare.setCompoundDrawables(null, shareDrawable, null, null);
                mRate.setCompoundDrawables(null, rateDrawable, null, null);


                mRead.setOnClickListener(this);
                mDownload.setOnClickListener(this);
                mFavorite.setOnClickListener(this);
                mTorrent.setOnClickListener(this);
                mShare.setOnClickListener(this);
                mRate.setOnClickListener(this);

                AccurateClick.setOnAccurateClickListener(mInfo, this);
                AccurateClick.setOnAccurateClickListener(mComment, this);

                RippleSalon.addRipple(mRead, false);
                RippleSalon.addRipple(mDownload, false);
                RippleSalon.addRipple(mFavorite, false);
                RippleSalon.addRipple(mTorrent, false);
                RippleSalon.addRipple(mShare, false);
                RippleSalon.addRipple(mRate, false);

                mPreviewLayout.setPreviewHelper(new SimplePreviewHelper());

            } else {
                Say.e(TAG, "Can't get GalleryDetail");
                finish();
            }

        } else if (ACTION_GID_TOKEN.equals(action)) {

        } else if (ACTION_URL.equals(action)) {

        } else {
            Say.e(TAG, "Unkonwn action " + action);
            finish();
        }
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);
        setContentView(R.layout.scene_gallery_detail);

        mDetailFrameLayout = (FrameLayout) findViewById(R.id.detail_frame_layout);

        mHeader = (ViewGroup) findViewById(R.id.header);
        mThumb = (LoadImageView) mHeader.findViewById(R.id.thumb);
        mTitle = (TextView) mHeader.findViewById(R.id.title);
        mUploader = (TextView) mHeader.findViewById(R.id.uploader);
        mCategory = (TextView) mHeader.findViewById(R.id.category);

        mActionCard = (ViewGroup) findViewById(R.id.action_card);
        mRead = mActionCard.findViewById(R.id.read);
        mDownload = mActionCard.findViewById(R.id.download);

        mContent = (ViewGroup) findViewById(R.id.content);
        mInfo = (ViewGroup) mContent.findViewById(R.id.info);
        mLanguage = (TextView) mInfo.findViewById(R.id.language);
        mPages = (TextView) mInfo.findViewById(R.id.pages);
        mSize = (TextView) mInfo.findViewById(R.id.size);
        mPosted = (TextView) mInfo.findViewById(R.id.posted);
        mResize = (TextView) mInfo.findViewById(R.id.resize);
        mFavoredTimes = (TextView) mInfo.findViewById(R.id.favoredTimes);
        mRatingText = (TextSwitcher) mContent.findViewById(R.id.rating_text);
        mRating = (RatingView) mContent.findViewById(R.id.rating);
        mFavorite = (TextView) mContent.findViewById(R.id.favorite);
        mTorrent = (TextView) mContent.findViewById(R.id.torrent);
        mShare = (TextView) mContent.findViewById(R.id.share);
        mRate = (TextView) mContent.findViewById(R.id.rate);
        mTag = (LinearLayout) mContent.findViewById(R.id.tag);
        mNoTags = (TextView) mTag.findViewById(no_tags);
        mComment = (LinearLayout) mContent.findViewById(R.id.comment);
        mCommentMore = (TextView) mContent.findViewById(R.id.comment_more);
        mPreviewLayout = (PreviewLayout) mContent.findViewById(R.id.preview);

        mProgressBar = findViewById(R.id.progress_bar);

        mViewTransition = new ViewTransition(mContent, mProgressBar);

        handleAnnouncer(getAnnouncer());
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mDetailFrameLayout.setPadding(0, 0, 0, b);
    }

    @Override
    public void onClick(View v) {
        GalleryDetail gd = mGalleryDetail;
        if (gd == null) {
            return;
        }

        if (v == mRead) {
            Intent intent = new Intent(getStageActivity(), GalleryActivity.class);
            getStageActivity().startActivity(intent);
        } else if (v == mDownload) {

        } else if (v == mFavorite) {

        } else if (v == mTorrent) {

        } else if (v == mShare) {
            StartActivityHelper.share(getStageActivity(),
                    EhUrl.getDetailUrl(Settings.getEhSource(), gd.gid, gd.token, 0));
        }
    }

    @Override
    public void onAccurateClick(View v, float x, float y) {
        GalleryDetail gd = mGalleryDetail;
        if (gd == null) {
            return;
        }

        if (v == mInfo) {
            int[] position = new int[2];
            ViewUtils.getLocationInAncestor(mInfo, position, getSceneView());
            Announcer announcer = new Announcer();
            announcer.putExtra(InfoScene.KEY_INFO, mGalleryDetail);
            startScene(InfoScene.class, announcer,
                    new RippleCurtain((int) x + position[0], (int) y + position[1]));
        } else if (v == mComment) {
            int[] position = new int[2];
            ViewUtils.getLocationInAncestor(mComment, position, getSceneView());
            Announcer announcer = new Announcer();
            announcer.putExtra(CommentScene.KEY_COMMENTS, mGalleryDetail.comments);
            startScene(CommentScene.class, announcer,
                    new RippleCurtain((int) x + position[0], (int) y + position[1]));
        }
    }

    private String getRatingText(float rating) {
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
            return getStageActivity().getString(resId);
        }
    }

    private String getAllRatingText(float rating, int ratedTimes) {
        return String.format(getStageActivity().getString(R.string.we_feel), getRatingText(rating)) +
                " (" + rating + ", " + ratedTimes + ")";
    }

    private void fillTags(List<TagGroup> tags) {
        if (tags.size() != 0) {
            mNoTags.setVisibility(View.GONE);

            Context context = getStageActivity();
            LayoutInflater inflater = getStageActivity().getLayoutInflater();
            int x = LayoutUtils.dp2pix(context, 2);
            int y = LayoutUtils.dp2pix(context, 4);

            for (TagGroup tagGroup : tags) {
                LinearLayout tagGroupLayout = new LinearLayout(context);
                tagGroupLayout.setOrientation(LinearLayout.HORIZONTAL);
                AutoWrapLayout tagLayout = new AutoWrapLayout(context);

                // Group name
                @SuppressLint("InflateParams")
                TextView groupNameView = (TextView) inflater.inflate(R.layout.tag_group, null);
                groupNameView.setText(tagGroup.groupName);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(x, y, x, y);
                tagGroupLayout.addView(groupNameView, lp);

                int count = tagGroup.getTagCount();
                for (int i = 0; i < count; i++) {
                    @SuppressLint("InflateParams")
                    TextView tagView = (TextView) inflater.inflate(R.layout.tag, null);
                    tagView.setText(tagGroup.getTagAt(i));
                /*
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
                */
                    AutoWrapLayout.LayoutParams alp = new AutoWrapLayout.LayoutParams();
                    alp.setMargins(x, y, x, y);
                    tagLayout.addView(tagView, alp);
                }
                tagGroupLayout.addView(tagLayout);
                mTag.addView(tagGroupLayout);
            }
        } else {
            mNoTags.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("InflateParams")
    private View createCommentView(Comment comment) {
         View view = LayoutInflater.from(getStageActivity()).inflate(R.layout.item_comment, null);
        ((TextView) view.findViewById(R.id.user)).setText(comment.user);
        ((TextView) view.findViewById(R.id.time)).setText(comment.time);
        TextView commentText = (TextView) view.findViewById(R.id.comment);
        commentText.setMaxLines(3);
        commentText.setAutoLinkMask(0);
        URLImageGetter p = new URLImageGetter(
                commentText, EhApplication.getConaco(getStageActivity()));
        commentText.setText(Html.fromHtml(comment.comment, p, null));
        return view;
    }

    private class RatingTextViewFactory implements ViewSwitcher.ViewFactory {
        @Override
        public View makeView() {
            TextView tv = new TextView(getStageActivity());
            tv.setGravity(Gravity.CENTER);
            return tv;
        }
    }

    private class EhDetailListener extends EhClient.EhListener<GalleryDetail> {

        @Override
        public void onSuccess(GalleryDetail galleryDetail) {
            mGalleryDetail = galleryDetail;

            Resources resources = getStageActivity().getResources();
            mLanguage.setText(galleryDetail.language);
            mPages.setText(String.format(resources.getString(R.string.page_count),
                    galleryDetail.pageCount));
            mSize.setText(galleryDetail.size);
            mPosted.setText(galleryDetail.posted);
            mResize.setText(galleryDetail.resize);
            mFavoredTimes.setText(String.format(resources.getString(R.string.favorited_times),
                    galleryDetail.favoredTimes));
            mRating.setRating(galleryDetail.rating);
            mRatingText.setText(getAllRatingText(galleryDetail.rating, galleryDetail.ratedTimes));
            fillTags(galleryDetail.tags);

            int commentNum = galleryDetail.comments.size();
            if  (commentNum == 0) {
                mCommentMore.setText(R.string.no_comments);
                // Remove padding interval
                mCommentMore.setPadding(0, 0, 0, 0);
            } else if (commentNum <= 2){
                mCommentMore.setText(R.string.no_more_comments);
            } else {
                mCommentMore.setText(R.string.more_comment);
            }
            int maxShown = Math.min(2, commentNum);
            for (int i = 0; i < maxShown; i++) {
                mComment.addView(createCommentView(galleryDetail.comments.get(i)), i,
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
            }

            mPreviewLayout.setData(galleryDetail.previewSetArray,
                    galleryDetail.gid,
                    getStageActivity().getLayoutInflater(),
                    EhApplication.getConaco(getStageActivity()));
            mPreviewLayout.selectPreviewAt(0);

            mViewTransition.showView(0);
        }

        @Override
        public void onFailure(Exception e) {
            e.printStackTrace();
            if (e instanceof OffensiveException) {

            } else if (e instanceof EhException) {
                new SimpleDialog.Builder(getStageActivity()).setTitle("Error")
                        .setMessage(e.getMessage())
                        .setNegativeButton(android.R.string.cancel)
                        .setOnCloseListener(new SimpleDialog.OnCloseListener() {
                            @Override
                            public void onClose(SimpleDialog dialog, boolean cancel) {
                                finish();
                            }
                        }).show(GalleryDetailScene.this);
            }
        }

        @Override
        public void onCanceled() {

        }
    }

    class SimplePreviewHelper implements PreviewLayout.PreviewHelper {

        @Override
        public void onRequstPreview(PreviewLayout previewLayout, final int index) {
            EhClient client = EhApplication.getEhClient(getStageActivity());
            int source = Settings.getEhSource();
            String url = EhUrl.getDetailUrl(source, mGalleryDetail.gid, mGalleryDetail.token, index);

            EhRequest request = new EhRequest();
            request.setMethod(EhClient.METHOD_GET_PREVIEW_SET);
            // TODO request.setEhConfig();
            request.setArgs(url, source);
            request.setEhListener(new EhClient.EhListener<PreviewSet>() {

                @Override
                public void onSuccess(PreviewSet result) {
                    mPreviewLayout.onGetPreview(result, index);
                }

                @Override
                public void onFailure(Exception e) {
                    e.printStackTrace();
                }

                @Override
                public void onCanceled() {

                }
            });

            client.execute(request);
        }

        @Override
        public void onRequstPreviewIndex(PreviewLayout previewLayout, float x, float y) {
            if (previewLayout.getPreviewPageCount() <= 1) {
                return;
            }

            int count = previewLayout.getPreviewPageCount();
            String[] array = new String[count];
            for (int i = 0; i < count; i++) {
                array[i] = Integer.toString(i + 1);
            }

            int[] position = new int[2];
            ViewUtils.getLocationInAncestor(mPreviewLayout, position, getSceneView());

            new SimpleDialog.Builder(getStageActivity()).setTitle("Select page")
                    .setStartPoint((int) (x + position[0]), (int) (y + position[1]))
                    .setItems(array, new SimpleDialog.OnClickListener() {
                        @Override
                        public boolean onClick(SimpleDialog dialog, int which) {
                            mPreviewLayout.selectPreviewAt(which);
                            return true;
                        }
                    }).show(GalleryDetailScene.this);
        }
    }
}
