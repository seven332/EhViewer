/*
 * Copyright 2015 Hippo Seven
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
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.GalleryDetailUrlParser;
import com.hippo.ehviewer.client.data.Comment;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.client.data.GalleryDetail;
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
import com.hippo.scene.OffsetCurtain;
import com.hippo.scene.RippleCurtain;
import com.hippo.scene.Scene;
import com.hippo.scene.SimpleDialog;
import com.hippo.scene.UnionCurtain;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.URLImageGetter;
import com.hippo.widget.AccurateClick;
import com.hippo.widget.AutoWrapLayout;
import com.hippo.widget.NoopLayout;
import com.hippo.widget.recyclerview.LinearDividerItemDecoration;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.Say;
import com.hippo.yorozuya.ViewUtils;

import java.util.List;

public class GalleryDetailScene extends Scene implements View.OnClickListener,
        AccurateClick.OnAccurateClickListener, LinearDividerItemDecoration.ShowDividerHelper {

    private static final String TAG = GalleryDetailScene.class.getSimpleName();

    public static final String ACTION_GALLERY_INFO = "action_gallery_info";
    public static final String ACTION_GID_TOKEN = "action_gid_token";
    public static final String ACTION_URL = "action_url";

    public static final String KEY_GALLERY_INFO = "key_gallery_info";
    public static final String KEY_GID = "key_gid";
    public static final String KEY_TOKEN = "key_token";
    public static final String KEY_URL = "key_url";

    public static final int STATE_TOTALLY_LOADING = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_LOADED = 2;
    public static final int STATE_FAILED = 3;

    public static final int COUNT_LOADING = 3;
    public static final int COUNT_LOADED = 7;

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_BUTTON = 1;
    public static final int TYPE_INFO = 2;
    public static final int TYPE_ACTION = 3;
    public static final int TYPE_TAG = 4;
    public static final int TYPE_COMMENT = 5;
    public static final int TYPE_PREVIEW = 6;
    public static final int TYPE_PROGRESS = 7;

    private EhConfig mEhConfig;
    private int mSource;

    private String mActionStr;
    private GalleryBase mGalleryBase;
    private int mGid;
    private String mToken;
    private GalleryDetail mGalleryDetail;
    private String mErrorMessage;

    private RecyclerView mRecyclerView;
    private View mMainProgressView;
    private ViewGroup mFailedView;
    private TextView mFailedText;
    private ViewTransition mViewTransition;

    private View mHeader;
    LoadImageView mThumb;
    TextView mTitle;
    TextView mUploader;
    TextView mCategory;

    private View mButton;
    private View mDownload;
    private View mRead;

    private View mInfo;
    private TextView mLanguage;
    private TextView mPages;
    private TextView mSize;
    private TextView mPosted;
    private TextView mResize;
    private TextView mFavoredTimes;

    private View mAction;
    private TextView mRatingText;
    private RatingView mRating;
    private TextView mFavorite;
    private TextView mTorrent;
    private TextView mShare;
    private TextView mRate;

    private ViewGroup mTag;
    private TextView mNoTagText;

    private ViewGroup mComment;
    private TextView mCommentText;

    private PreviewLayout mPreview;

    private View mProgress;
    private View mProgressView;

    private DetailAdapter mAdapter;

    private int mState;

    private EhRequest mEhRequest;

    @Override
    protected void onInit() {
        super.onInit();

        mEhConfig = EhApplication.getEhHttpClient(getStageActivity()).getEhConfigClone();
        mSource = Settings.getEhSource();
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);
        setContentView(R.layout.scene_gallery_detail);

        ViewGroup main = (ViewGroup) findViewById(R.id.main);
        mRecyclerView = (RecyclerView) main.getChildAt(0);
        mMainProgressView = main.getChildAt(1);
        mFailedView = (ViewGroup) main.getChildAt(2);
        mFailedText = (TextView) mFailedView.getChildAt(1);
        mViewTransition = new ViewTransition(mRecyclerView, mMainProgressView, mFailedView);

        LayoutInflater inflater = getStageActivity().getLayoutInflater();
        NoopLayout noopLayout = new NoopLayout(getStageActivity());

        mHeader = inflater.inflate(R.layout.gallery_detail_header, noopLayout, false);
        mThumb = (LoadImageView) mHeader.findViewById(R.id.thumb);
        mTitle = (TextView) mHeader.findViewById(R.id.title);
        mUploader = (TextView) mHeader.findViewById(R.id.uploader);
        mCategory = (TextView) mHeader.findViewById(R.id.category);

        mButton = inflater.inflate(R.layout.gallery_detail_button, noopLayout, false);
        mDownload = mButton.findViewById(R.id.download);
        mRead = mButton.findViewById(R.id.read);

        mInfo = inflater.inflate(R.layout.gallery_detail_info, noopLayout, false);
        mLanguage = (TextView) mInfo.findViewById(R.id.language);
        mPages = (TextView) mInfo.findViewById(R.id.pages);
        mSize = (TextView) mInfo.findViewById(R.id.size);
        mPosted = (TextView) mInfo.findViewById(R.id.posted);
        mResize = (TextView) mInfo.findViewById(R.id.resize);
        mFavoredTimes = (TextView) mInfo.findViewById(R.id.favoredTimes);

        mAction = inflater.inflate(R.layout.gallery_detail_action, noopLayout, false);
        mRatingText = (TextView) mAction.findViewById(R.id.rating_text);
        mRating = (RatingView) mAction.findViewById(R.id.rating);
        mFavorite = (TextView) mAction.findViewById(R.id.favorite);
        mTorrent = (TextView) mAction.findViewById(R.id.torrent);
        mShare = (TextView) mAction.findViewById(R.id.share);
        mRate = (TextView) mAction.findViewById(R.id.rate);

        mTag = (ViewGroup) inflater.inflate(R.layout.gallery_detail_tag, noopLayout, false);
        mNoTagText = (TextView) mTag.getChildAt(0);

        mComment = (ViewGroup) inflater.inflate(R.layout.gallery_detail_comment, noopLayout, false);
        mCommentText = (TextView) mComment.getChildAt(0);

        mPreview = (PreviewLayout) inflater.inflate(R.layout.gallery_detail_preview, noopLayout, false);

        mProgress = inflater.inflate(R.layout.gallery_detail_progress, noopLayout, false);
        mProgressView = mProgress.findViewById(R.id.progress_view);

        Resources resources = getStageActivity().getResources();

        mAdapter = new DetailAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getStageActivity()));
        mRecyclerView.hasFixedSize();
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL, resources.getColor(R.color.divider_light),
                LayoutUtils.dp2pix(getStageActivity(), 1));
        decoration.setShowDividerHelper(this);
        decoration.setPadding(resources.getDimensionPixelOffset(R.dimen.keyline_margin_horizontal));
        mRecyclerView.addItemDecoration(decoration);

        mDownload.setOnClickListener(this);
        mRead.setOnClickListener(this);
        mFavorite.setOnClickListener(this);
        mTorrent.setOnClickListener(this);
        mShare.setOnClickListener(this);
        mRate.setOnClickListener(this);
        mFailedView.setOnClickListener(this);

        AccurateClick.setOnAccurateClickListener(mInfo, this);
        AccurateClick.setOnAccurateClickListener(mComment, this);

        RippleSalon.addRipple(mDownload, false);
        RippleSalon.addRipple(mRead, false);
        RippleSalon.addRipple(mFavorite, false);
        RippleSalon.addRipple(mTorrent, false);
        RippleSalon.addRipple(mShare, false);
        RippleSalon.addRipple(mRate, false);

        mPreview.setPreviewHelper(new SimplePreviewHelper());
    }

    private void requestGalleryDetail(int gid, String token) {
        EhClient client = EhApplication.getEhClient(getStageActivity());
        int source = Settings.getEhSource();
        String url = EhUrl.getDetailUrl(source, gid, token, 0);

        mEhRequest = new EhRequest();
        mEhRequest.setMethod(EhClient.METHOD_GET_GALLERY_DETAIL);
        mEhRequest.setEhListener(new EhDetailListener());
        mEhRequest.setArgs(url, mSource);
        mEhRequest.setEhConfig(mEhConfig);
        client.execute(mEhRequest);
    }

    private void bindFirst(GalleryBase gb) {
        mThumb.load(EhApplication.getConaco(getStageActivity()), EhCacheKeyFactory.getThumbKey(gb.gid), gb.thumb);
        mTitle.setText(EhUtils.getSuitableTitle(gb));
        mUploader.setText(gb.uploader);
        mCategory.setText(EhUtils.getCategory(gb.category));
        mCategory.setTextColor(EhUtils.getCategoryColor(gb.category));
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
            mNoTagText.setVisibility(View.GONE);

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
            mNoTagText.setVisibility(View.VISIBLE);
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

    private void fillComment(List<Comment> comments) {
        int commentNum = comments.size();
        if  (commentNum == 0) {
            mCommentText.setText(R.string.no_comments);
            // Remove padding interval
            mCommentText.setPadding(0, 0, 0, 0);
        } else if (commentNum <= 2){
            mCommentText.setText(R.string.no_more_comments);
        } else {
            mCommentText.setText(R.string.more_comment);
        }
        int maxShown = Math.min(2, commentNum);
        for (int i = 0; i < maxShown; i++) {
            mComment.addView(createCommentView(comments.get(i)), i,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private void bindSecond(GalleryDetail gd) {
        Resources resources = getStageActivity().getResources();
        mLanguage.setText(gd.language);
        mPages.setText(String.format(resources.getString(R.string.page_count),
                gd.pageCount));
        mSize.setText(gd.size);
        mPosted.setText(gd.posted);
        mResize.setText(gd.resize);
        mFavoredTimes.setText(String.format(resources.getString(R.string.favorited_times),
                gd.favoredTimes));

        Drawable favoriteDrawable;
        if (gd.isFavored) {
            favoriteDrawable = resources.getDrawable(R.drawable.ic_heart_theme_primary);
        } else {
            favoriteDrawable = resources.getDrawable(R.drawable.ic_heart_outline_theme_primary);
        }
        if (favoriteDrawable == null) {
            throw new IllegalStateException("Can't get favorite drawable");
        }
        favoriteDrawable.setBounds(0, 0, favoriteDrawable.getIntrinsicWidth(), favoriteDrawable.getIntrinsicHeight());
        mFavorite.setCompoundDrawables(null, favoriteDrawable, null, null);

        mTorrent.setText(resources.getString(R.string.torrent) + " " + gd.torrentCount);
        mRating.setRating(gd.rating);
        mRatingText.setText(getAllRatingText(gd.rating, gd.ratedTimes));

        fillTags(gd.tags);

        fillComment(gd.comments);

        mPreview.setData(gd.previewSetArray,
                gd.gid,
                getStageActivity().getLayoutInflater(),
                EhApplication.getConaco(getStageActivity()));
        mPreview.selectPreviewAt(0);
    }

    private void bindGalleryInfo(GalleryBase galleryBase) {
        mViewTransition.showView(0); // Show recycler view

        bindFirst(galleryBase);
        mState = STATE_LOADING;
        mAdapter.notifyDataSetChanged();
        requestGalleryDetail(galleryBase.gid, galleryBase.token);
    }

    private void bindGidToken(int gid, String token) {
        mViewTransition.showView(1); // Show progress view

        mState = STATE_TOTALLY_LOADING;
        mAdapter.notifyDataSetChanged();
        requestGalleryDetail(gid, token);
    }

    @Override
    protected void onBind() {
        super.onBind();

        Announcer announcer = getAnnouncer();
        if (announcer != null) {
            String action = announcer.getAction();
            mActionStr = action;
            if (ACTION_GALLERY_INFO.equals(action)) {
                mGalleryBase = announcer.getExtra(KEY_GALLERY_INFO, GalleryBase.class);
                if (mGalleryBase != null) {
                    bindGalleryInfo(mGalleryBase);
                    return;
                }
            } else if (ACTION_GID_TOKEN.equals(action)) {
                mGid = announcer.getIntExtra(KEY_GID, 0);
                mToken = announcer.getStringExtra(KEY_TOKEN, null);
                if (mGid != 0 && mToken != null) {
                    bindGidToken(mGid, mToken);
                    return;
                }
            } else if (ACTION_URL.equals(action)) {
                GalleryDetailUrlParser.Result result = GalleryDetailUrlParser.parser(
                        announcer.getStringExtra(KEY_URL, null));
                if (result != null) {
                    result.gid = mGid;
                    result.token = mToken;
                    bindGidToken(mGid, mToken);
                    return;
                }
            }
        }

        finish();
    }

    @Override
    protected void onRestore() {
        super.onRestore();

        switch (mState) {
            case STATE_TOTALLY_LOADING:
                mViewTransition.showView(1); // Show progress view
                break;
            case STATE_LOADING:
                mViewTransition.showView(0); // Show recycler view
                bindFirst(mGalleryBase);
                break;
            case STATE_LOADED:
                mViewTransition.showView(0); // Show recycler view
                bindFirst(mGalleryDetail);
                bindSecond(mGalleryDetail);
                break;
            case STATE_FAILED:
                mViewTransition.showView(2); // Show failed view
                mFailedText.setText(mErrorMessage);
                break;
            default:
                throw new IllegalStateException("Unknown state " + mState);
        }
    }

    @Override
    protected void onDie() {
        super.onDie();

        if (mEhRequest != null) {
            mEhRequest.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        if (mFailedView == v) {
            mViewTransition.showView(1); // Show progress view
            mState = STATE_TOTALLY_LOADING;
            mAdapter.notifyDataSetChanged();
            if (ACTION_GALLERY_INFO.equals(mActionStr)) {
                requestGalleryDetail(mGalleryBase.gid, mGalleryBase.token);
            } else {
                requestGalleryDetail(mGid, mToken);
            }
        } else if (mDownload == v) {
            // TODO
        } else if (mRead == v) {
            Intent intent = new Intent(getStageActivity(), GalleryActivity.class);
            intent.setAction(GalleryActivity.ACTION_GALLERY_FROM_GALLERY_BASE);
            intent.putExtra(GalleryActivity.KEY_GALLERY_BASE, mGalleryDetail);
            //intent.setAction(GalleryActivity.ACTION_GALLERY_FROM_ARCHIVE);
            //intent.putExtra(GalleryActivity.KEY_ARCHIVE_URI,
            //        Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "a.zip")));
            getStageActivity().startActivity(intent);
        } else if (mFavorite == v) {
            // TODO
        } else if (mTorrent == v) {
            if (mGalleryDetail.torrentCount > 0) {
                // TODO
            }
        } else if (mShare == v) {
            StartActivityHelper.share(getStageActivity(),
                    EhUrl.getDetailUrl(Settings.getEhSource(), mGalleryDetail.gid, mGalleryDetail.token, 0));
        } else if (mRate == v) {
            // TODO
        }
    }

    @Override
    public void onAccurateClick(View v, float x, float y) {
        GalleryBase gb = mGalleryBase;
        if (gb == null) {
            gb = mGalleryDetail;
        }
        if (gb == null) {
            return;
        }

        if (v == mInfo) {
            int[] position = new int[2];
            ViewUtils.getLocationInAncestor(mInfo, position, getSceneView());
            Announcer announcer = new Announcer();
            announcer.putExtra(InfoScene.KEY_INFO, mGalleryDetail);
            startScene(InfoScene.class, announcer,
                    new UnionCurtain(new RippleCurtain((int) x + position[0], (int) y + position[1]),
                            new OffsetCurtain(OffsetCurtain.DIRECTION_BOTTOM)));
        } else if (v == mComment) {
            int[] position = new int[2];
            ViewUtils.getLocationInAncestor(mComment, position, getSceneView());
            Announcer announcer = new Announcer();
            announcer.putExtra(CommentScene.KEY_COMMENTS, mGalleryDetail.comments);
            startScene(CommentScene.class, announcer,
                    new UnionCurtain(new RippleCurtain((int) x + position[0], (int) y + position[1]),
                            new OffsetCurtain(OffsetCurtain.DIRECTION_BOTTOM)));
        }
    }

    @Override
    public boolean showDivider(int index) {
        if (mState == STATE_LOADED) {
            return index >= 3 && index <= 6;
        } else {
            return false;
        }
    }

    private class EhDetailListener extends EhClient.EhListener<GalleryDetail> {

        @Override
        public void onSuccess(GalleryDetail result) {
            mEhRequest = null;

            int oldState = mState;
            mGalleryDetail = result;
            if (oldState == STATE_TOTALLY_LOADING) {
                bindFirst(result);
            }
            bindSecond(result);

            mState = STATE_LOADED;
            mAdapter.notifyDataSetChanged();
            mViewTransition.showView(0); // Show recycler view
        }

        @Override
        public void onFailure(Exception e) {
            mEhRequest = null;

            Say.d(TAG, "Get gallery detail failed " + e.getClass().getName() + " " + e.getMessage());
            String readableError = ExceptionUtils.getReadableString(getStageActivity(), e);
            String reason = ExceptionUtils.getReasonString(getStageActivity(), e);
            if (reason != null) {
                readableError += '\n' + reason;
            }
            mErrorMessage = readableError;
            mFailedText.setText(readableError);

            mState = STATE_FAILED;
            mAdapter.notifyDataSetChanged();
            mViewTransition.showView(2); // Show failed view
        }

        @Override
        public void onCanceled() {
            mEhRequest = null;
        }
    }

    private class DetailHolder extends RecyclerView.ViewHolder {

        public DetailHolder(View itemView) {
            super(itemView);
        }
    }

    private class DetailAdapter extends RecyclerView.Adapter<DetailHolder> {

        @Override
        public DetailHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case TYPE_HEADER:
                    return new DetailHolder(mHeader);
                case TYPE_BUTTON:
                    return new DetailHolder(mButton);
                case TYPE_INFO:
                    return new DetailHolder(mInfo);
                case TYPE_ACTION:
                    return new DetailHolder(mAction);
                case TYPE_TAG:
                    return new DetailHolder(mTag);
                case TYPE_COMMENT:
                    return new DetailHolder(mComment);
                case TYPE_PREVIEW:
                    return new DetailHolder(mPreview);
                case TYPE_PROGRESS:
                    return new DetailHolder(mProgress);
                default:
                    throw new IllegalStateException("Unknown type " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(DetailHolder holder, int position) {
            // Empty
        }

        @Override
        public int getItemViewType(int position) {
            if (mState == STATE_LOADING) {
                if (position >= 2) {
                    return TYPE_PROGRESS;
                } else {
                    return position;
                }
            } else if (mState == STATE_LOADED) {
                return position;
            } else {
                return -1;
            }
        }

        @Override
        public int getItemCount() {
            if (mState == STATE_TOTALLY_LOADING || mState == STATE_FAILED) {
                return 0;
            } else if (mState == STATE_LOADING) {
                return COUNT_LOADING;
            } else if (mState == STATE_LOADED) {
                return COUNT_LOADED;
            } else {
                throw new IllegalStateException("Unknown state " + mState);
            }
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
            request.setEhConfig(mEhConfig);
            request.setArgs(url, mSource);
            request.setEhListener(new EhClient.EhListener<PreviewSet>() {

                @Override
                public void onSuccess(PreviewSet result) {
                    mPreview.onGetPreview(result, index);
                }

                @Override
                public void onFailure(Exception e) {
                    // TODO
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
            ViewUtils.getLocationInAncestor(mPreview, position, getSceneView());

            new SimpleDialog.Builder(getStageActivity()).setTitle("Select page")
                    .setStartPoint((int) (x + position[0]), (int) (y + position[1]))
                    .setItems(array, new SimpleDialog.OnClickListener() {
                        @Override
                        public boolean onClick(SimpleDialog dialog, int which) {
                            mPreview.selectPreviewAt(which);
                            return true;
                        }
                    }).show(GalleryDetailScene.this);
        }
    }
}
