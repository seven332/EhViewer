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

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.LinearDividerItemDecoration;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UrlOpener;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.ehviewer.client.parser.VoteCommentParser;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.reveal.ViewAnimationUtils;
import com.hippo.ripple.Ripple;
import com.hippo.scene.SceneFragment;
import com.hippo.text.Html;
import com.hippo.text.URLImageGetter;
import com.hippo.util.DrawableManager;
import com.hippo.util.ReadableTime;
import com.hippo.util.TextUrl;
import com.hippo.view.ViewTransition;
import com.hippo.widget.FabLayout;
import com.hippo.widget.LinkifyTextView;
import com.hippo.widget.ObservedTextView;
import com.hippo.yorozuya.AnimationUtils;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ResourcesUtils;
import com.hippo.yorozuya.SimpleAnimatorListener;
import com.hippo.yorozuya.StringUtils;
import com.hippo.yorozuya.ViewUtils;
import com.hippo.yorozuya.collect.IntList;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public final class GalleryCommentsScene extends ToolbarScene
        implements EasyRecyclerView.OnItemClickListener,
        View.OnClickListener {

    public static final String TAG = GalleryCommentsScene.class.getSimpleName();

    public static final String KEY_API_UID = "api_uid";
    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_GID = "gid";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_COMMENTS = "comments";

    private long mApiUid;
    private String mApiKey;
    private long mGid;
    private String mToken;
    @Nullable
    private GalleryComment[] mComments;

    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private FabLayout mFabLayout;
    @Nullable
    private FloatingActionButton mFab;
    @Nullable
    private View mEditPanel;
    @Nullable
    private ImageView mSendImage;
    @Nullable
    private EditText mEditText;
    @Nullable
    private CommentAdapter mAdapter;
    @Nullable
    private ViewTransition mViewTransition;

    private boolean mInAnimation = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void handleArgs(Bundle args) {
        if (args == null) {
            return;
        }

        mApiUid = args.getLong(KEY_API_UID, -1L);
        mApiKey = args.getString(KEY_API_KEY);
        mGid = args.getLong(KEY_GID, -1L);
        mToken = args.getString(KEY_TOKEN, null);
        Parcelable[] parcelables = args.getParcelableArray(KEY_COMMENTS);
        if (parcelables instanceof GalleryComment[]) {
            mComments = (GalleryComment[]) parcelables;
        }
    }

    private void onInit() {
        handleArgs(getArguments());
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mApiUid = savedInstanceState.getLong(KEY_API_UID, -1L);
        mApiKey = savedInstanceState.getString(KEY_API_KEY);
        mGid = savedInstanceState.getLong(KEY_GID, -1L);
        mToken = savedInstanceState.getString(KEY_TOKEN, null);
        Parcelable[] parcelables = savedInstanceState.getParcelableArray(KEY_COMMENTS);
        if (parcelables instanceof GalleryComment[]) {
            mComments = (GalleryComment[]) parcelables;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_API_UID, mApiUid);
        outState.putString(KEY_API_KEY, mApiKey);
        outState.putLong(KEY_GID, mGid);
        outState.putString(KEY_TOKEN, mToken);
        outState.putParcelableArray(KEY_COMMENTS, mComments);
    }

    @Nullable
    @Override
    public View onCreateView3(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_gallery_comments, container, false);
        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(view, R.id.recycler_view);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        mEditPanel = ViewUtils.$$(view, R.id.edit_panel);
        mSendImage = (ImageView) ViewUtils.$$(mEditPanel, R.id.send);
        mEditText = (EditText) ViewUtils.$$(mEditPanel, R.id.edit_text);
        mFabLayout = (FabLayout) ViewUtils.$$(view, R.id.fab_layout);
        mFab = (FloatingActionButton) ViewUtils.$$(view, R.id.fab);

        Context context = getContext2();
        Assert.assertNotNull(context);
        Resources resources = context.getResources();
        int paddingBottomFab = resources.getDimensionPixelOffset(R.dimen.gallery_padding_bottom_fab);

        Drawable drawable = DrawableManager.getDrawable(context, R.drawable.big_weird_face);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);

        mAdapter = new CommentAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false));
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL, context.getResources().getColor(R.color.divider),
                LayoutUtils.dp2pix(context, 1));
        decoration.setShowLastDivider(true);
        mRecyclerView.addItemDecoration(decoration);
        mRecyclerView.setSelector(Ripple.generateRippleDrawable(context, false));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), mRecyclerView.getPaddingTop(),
                mRecyclerView.getPaddingRight(), mRecyclerView.getPaddingBottom() + paddingBottomFab);
        // Cancel change animator
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }

        mSendImage.setOnClickListener(this);
        mFab.setOnClickListener(this);

        addAboveSnackView(mEditPanel);
        addAboveSnackView(mFabLayout);

        mViewTransition = new ViewTransition(mRecyclerView, tip);

        updateView(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mRecyclerView) {
            mRecyclerView.stopScroll();
            mRecyclerView = null;
        }
        if (null != mEditPanel) {
            removeAboveSnackView(mEditPanel);
            mEditPanel = null;
        }
        if (null != mFabLayout) {
            removeAboveSnackView(mFabLayout);
            mFabLayout = null;
        }

        mFab = null;
        mSendImage = null;
        mEditText = null;
        mAdapter = null;
        mViewTransition = null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.gallery_comments);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    public void onNavigationClick() {
        onBackPressed();
    }

    private void voteComment(long id, int vote) {
        Context context = getContext2();
        MainActivity activity = getActivity2();
        if (null == context || null == activity) {
            return;
        }

        EhRequest request = new EhRequest()
                .setMethod(EhClient.METHOD_VOTE_COMMENT)
                .setArgs(mApiUid, mApiKey, mGid, mToken, id, vote)
                .setCallback(new VoteCommentListener(context,
                        activity.getStageId(), getTag()));
        EhApplication.getEhClient(context).execute(request);
    }

    private class InfoHolder extends RecyclerView.ViewHolder {

        private final TextView key;
        private final TextView value;

        public InfoHolder(View itemView) {
            super(itemView);
            key = (TextView) ViewUtils.$$(itemView, R.id.key);
            value = (TextView) ViewUtils.$$(itemView, R.id.value);
        }
    }

    @SuppressLint("InflateParams")
    public void showVoteStatusDialog(Context context, String voteStatus) {
        String[] temp = StringUtils.split(voteStatus, ',');
        final int length = temp.length;
        final String[] userArray = new String[length];
        final String[] voteArray = new String[length];
        for (int i = 0; i < length; i++) {
            String str = StringUtils.trim(temp[i]);
            int index = str.lastIndexOf(' ');
            if (index < 0) {
                Log.d(TAG, "Something wrong happened about vote state");
                userArray[i] = str;
                voteArray[i] = "";
            } else {
                userArray[i] = StringUtils.trim(str.substring(0, index));
                voteArray[i] = StringUtils.trim(str.substring(index + 1));
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        context = builder.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        EasyRecyclerView rv = (EasyRecyclerView) inflater.inflate(R.layout.dialog_recycler_view, null);
        rv.setAdapter(new RecyclerView.Adapter<InfoHolder>() {
            @Override
            public InfoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new InfoHolder(inflater.inflate(R.layout.item_favorite_info_data, parent, false));
            }

            @Override
            public void onBindViewHolder(InfoHolder holder, int position) {
                holder.key.setText(userArray[position]);
                holder.value.setText(voteArray[position]);
            }

            @Override
            public int getItemCount() {
                return length;
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(context));
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL, context.getResources().getColor(R.color.divider),
                LayoutUtils.dp2pix(context, 1));
        decoration.setPadding(ResourcesUtils.getAttrDimensionPixelOffset(context, R.attr.dialogPreferredPadding));
        rv.addItemDecoration(decoration);
        rv.setSelector(Ripple.generateRippleDrawable(context, false));
        rv.setClipToPadding(false);
        builder.setView(rv).show();
    }

    private void showCommentDialog(int position) {
        final Context context = getContext2();
        if (null == context || null == mComments || position >= mComments.length || position < 0) {
            return;
        }

        final GalleryComment comment = mComments[position];
        List<String> menu = new ArrayList<>();
        final IntList menuId = new IntList();
        Resources resources = context.getResources();
        if (0 == comment.id || mApiUid < 0) {
            // 0 id is uploader comment, can't vote
            // Not sign in, can't vote
            menu.add(resources.getString(R.string.copy_comment_text));
            menuId.add(R.id.copy);
        } else {
            menu.add(resources.getString(R.string.copy_comment_text));
            menuId.add(R.id.copy);
            menu.add(resources.getString(comment.voteUp ? R.string.cancel_vote_up : R.string.vote_up));
            menuId.add(R.id.vote_up);
            menu.add(resources.getString(comment.voteDown ? R.string.cancel_vote_down : R.string.vote_down));
            menuId.add(R.id.vote_down);
        }
        if (!TextUtils.isEmpty(comment.voteState)) {
            menu.add(resources.getString(R.string.check_vote_status));
            menuId.add(R.id.check_vote_status);
        }

        new AlertDialog.Builder(context)
                .setItems(menu.toArray(new String[menu.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < 0 || which >= menuId.size()) {
                           return;
                        }
                        int id = menuId.get(which);
                        switch (id) {
                            case R.id.copy:
                                ClipboardManager cmb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                cmb.setPrimaryClip(ClipData.newPlainText(null, comment.comment));
                                showTip(R.string.copied_to_clipboard, LENGTH_SHORT);
                                break;
                            case R.id.vote_up:
                                voteComment(comment.id, 1);
                                break;
                            case R.id.vote_down:
                                voteComment(comment.id, -1);
                                break;
                            case R.id.check_vote_status:
                                showVoteStatusDialog(context, comment.voteState);
                                break;
                        }
                    }
                }).show();
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        Activity activity = getActivity2();
        if (null == activity) {
            return false;
        }

        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
        if (holder instanceof CommentHolder) {
            CommentHolder commentHolder = (CommentHolder) holder;
            ClickableSpan span = commentHolder.comment.getCurrentSpan();
            commentHolder.comment.clearCurrentSpan();

            if (span instanceof URLSpan) {
                UrlOpener.openUrl(activity, ((URLSpan) span).getURL(), true);
                return true;
            }
        }

        showCommentDialog(position);

        return true;
    }

    private void updateView(boolean animation) {
        if (null == mViewTransition) {
            return;
        }

        if (mComments == null || mComments.length <= 0) {
            mViewTransition.showView(1, animation);
        } else {
            mViewTransition.showView(0, animation);
        }
    }

    private void showEditPanelWithAnimation() {
        if (null == mFab || null == mEditPanel) {
            return;
        }

        mInAnimation = true;
        mFab.setTranslationX(0.0f);
        mFab.setTranslationY(0.0f);
        mFab.setScaleX(1.0f);
        mFab.setScaleY(1.0f);
        int fabEndX = mEditPanel.getLeft() + (mEditPanel.getWidth() / 2) - (mFab.getWidth() / 2);
        int fabEndY = mEditPanel.getTop() + (mEditPanel.getHeight() / 2) - (mFab.getHeight() / 2);
        mFab.animate().x(fabEndX).y(fabEndY).scaleX(0.0f).scaleY(0.0f)
                .setInterpolator(AnimationUtils.SLOW_FAST_SLOW_INTERPOLATOR)
                .setDuration(300L).setListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (null == mFab || null == mEditPanel) {
                    return;
                }

                mFab.setVisibility(View.INVISIBLE);
                mEditPanel.setVisibility(View.VISIBLE);
                int halfW = mEditPanel.getWidth() / 2;
                int halfH = mEditPanel.getHeight() / 2;
                Animator animator = ViewAnimationUtils.createCircularReveal(mEditPanel, halfW, halfH, 0,
                        (float) Math.hypot(halfW, halfH)).setDuration(300L);
                animator.addListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator a) {
                        mInAnimation = false;
                    }
                });
                animator.start();
            }
        }).start();
    }

    private void showEditPanel(boolean animation) {
        if (animation) {
            showEditPanelWithAnimation();
        } else {
            if (null == mFab || null == mEditPanel) {
                return;
            }

            mFab.setVisibility(View.INVISIBLE);
            mEditPanel.setVisibility(View.VISIBLE);
        }
    }

    private void hideEditPanelWithAnimation() {
        if (null == mFab || null == mEditPanel) {
            return;
        }

        mInAnimation = true;
        int halfW = mEditPanel.getWidth() / 2;
        int halfH = mEditPanel.getHeight() / 2;
        Animator animator = ViewAnimationUtils.createCircularReveal(mEditPanel, halfW, halfH,
                (float) Math.hypot(halfW, halfH), 0.0f).setDuration(300L);
        animator.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (null == mFab || null == mEditPanel) {
                    return;
                }

                mEditPanel.setVisibility(View.GONE);
                mFab.setVisibility(View.VISIBLE);
                int fabStartX = mEditPanel.getLeft() + (mEditPanel.getWidth() / 2) - (mFab.getWidth() / 2);
                int fabStartY = mEditPanel.getTop() + (mEditPanel.getHeight() / 2) - (mFab.getHeight() / 2);
                mFab.setX(fabStartX);
                mFab.setY(fabStartY);
                mFab.setScaleX(0.0f);
                mFab.setScaleY(0.0f);
                mFab.setRotation(-45.0f);
                mFab.animate().translationX(0.0f).translationY(0.0f).scaleX(1.0f).scaleY(1.0f).rotation(0.0f)
                        .setInterpolator(AnimationUtils.SLOW_FAST_SLOW_INTERPOLATOR)
                        .setDuration(300L).setListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mInAnimation = false;
                    }
                }).start();
            }
        });
        animator.start();
    }

    private void hideEditPanel(boolean animation) {
        if (animation) {
            hideEditPanelWithAnimation();
        } else {
            if (null == mFab || null == mEditPanel) {
                return;
            }

            mFab.setVisibility(View.VISIBLE);
            mEditPanel.setVisibility(View.INVISIBLE);
        }
    }

    @Nullable
    private String getGalleryDetailUrl() {
        if (mGid != -1 && mToken != null) {
            return EhUrl.getGalleryDetailUrl(mGid, mToken, 0, false);
        } else {
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        Context context = getContext2();
        MainActivity activity = getActivity2();
        if (null == context || null == activity || null == mEditText) {
            return;
        }

        if (mFab == v) {
            if (!mInAnimation) {
                showEditPanel(true);
            }
        } else if (mSendImage == v) {
            if (!mInAnimation) {
                String comment = mEditText.getText().toString();
                if (TextUtils.isEmpty(comment)) {
                    // Comment is empty
                    return;
                }
                String url = getGalleryDetailUrl();
                if (url == null) {
                    return;
                }
                // Request
                EhRequest request = new EhRequest()
                        .setMethod(EhClient.METHOD_GET_COMMENT_GALLERY)
                        .setArgs(url, comment)
                        .setCallback(new CommentGalleryListener(context,
                                activity.getStageId(), getTag()));
                EhApplication.getEhClient(context).execute(request);
                hideSoftInput();
                hideEditPanel(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mInAnimation) {
            return;
        }
        if (null != mEditPanel && mEditPanel.getVisibility() == View.VISIBLE) {
            hideEditPanel(true);
        } else {
            finish();
        }
    }

    private class CommentHolder extends RecyclerView.ViewHolder {

        public final TextView user;
        public final TextView time;
        public final LinkifyTextView comment;

        public CommentHolder(View itemView) {
            super(itemView);
            user = (TextView) itemView.findViewById(R.id.user);
            time = (TextView) itemView.findViewById(R.id.time);
            comment = (LinkifyTextView) itemView.findViewById(R.id.comment);
        }
    }

    private class CommentAdapter extends RecyclerView.Adapter<CommentHolder> {

        private final LayoutInflater mInflater;

        public CommentAdapter() {
            mInflater = getLayoutInflater2();
            Assert.assertNotNull(mInflater);
        }

        @Override
        public CommentHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new CommentHolder(mInflater.inflate(R.layout.item_gallery_comment, parent, false));
        }

        public CharSequence generateComment(Context context, ObservedTextView textView, GalleryComment comment) {
            SpannableStringBuilder ssb = Html.fromHtml(comment.comment, new URLImageGetter(textView,
                    EhApplication.getConaco(context)), null);

            if (0 != comment.id && 0 != comment.score) {
                int score = comment.score;
                String scoreString = score > 0 ? "+" + score : Integer.toString(score);
                SpannableString ss = new SpannableString(scoreString);
                ss.setSpan(new RelativeSizeSpan(0.8f), 0, scoreString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, scoreString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.secondary_text_default_light))
                        , 0, scoreString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append("  ").append(ss);
            }

            return TextUrl.handleTextUrl(ssb);
        }

        @Override
        public void onBindViewHolder(CommentHolder holder, int position) {
            Context context = getContext2();
            if (null == context || null == mComments) {
                return;
            }

            GalleryComment comment = mComments[position];
            holder.user.setText(comment.user);
            holder.time.setText(ReadableTime.getTimeAgo(comment.time));
            holder.comment.setText(generateComment(context, holder.comment, comment));
        }

        @Override
        public int getItemCount() {
            return mComments == null ? 0 : mComments.length;
        }
    }

    private void onCommentGallerySuccess(GalleryComment[] result) {
        if (null == mAdapter) {
            return;
        }

        mComments = result;
        mAdapter.notifyDataSetChanged();
        Bundle re = new Bundle();
        re.putParcelableArray(KEY_COMMENTS, result);
        setResult(SceneFragment.RESULT_OK, re);

        // Remove text
        if (mEditText != null) {
            mEditText.setText("");
        }

        updateView(true);
    }

    private void onVoteCommentSuccess(VoteCommentParser.Result result) {
        if (null == mAdapter || null == mComments) {
            return;
        }

        int position = -1;
        for (int i = 0, n = mComments.length; i < n; i++) {
            GalleryComment comment = mComments[i];
            if (comment.id == result.id) {
                position = i;
                break;
            }
        }

        if (-1 == position) {
            Log.d(TAG, "Can't find comment with id " + result.id);
            return;
        }

        // Update comment
        GalleryComment comment = mComments[position];
        comment.score = result.score;
        if (result.expectVote > 0) {
            comment.voteUp = 0 != result.vote;
            comment.voteDown = false;
        } else {
            comment.voteDown = 0 != result.vote;
            comment.voteUp = false;
        }

        mAdapter.notifyItemChanged(position);

        Bundle re = new Bundle();
        re.putParcelableArray(KEY_COMMENTS, mComments);
        setResult(SceneFragment.RESULT_OK, re);
    }

    private static class CommentGalleryListener extends EhCallback<GalleryCommentsScene, GalleryComment[]> {

        public CommentGalleryListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(GalleryComment[] result) {
            showTip(R.string.comment_successfully, LENGTH_SHORT);

            GalleryCommentsScene scene = getScene();
            if (scene != null) {
                scene.onCommentGallerySuccess(result);
            }
        }

        @Override
        public void onFailure(Exception e) {
            showTip(R.string.comment_failed, LENGTH_SHORT);
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryCommentsScene;
        }
    }

    private static class VoteCommentListener extends EhCallback<GalleryCommentsScene, VoteCommentParser.Result> {

        public VoteCommentListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(VoteCommentParser.Result result) {
            showTip(result.expectVote > 0 ?
                    (0 != result.vote ? R.string.vote_up_successfully : R.string.cancel_vote_up_successfully) :
                    (0 != result.vote ? R.string.vote_down_successfully : R.string.cancel_vote_down_successfully),
                    LENGTH_SHORT);

            GalleryCommentsScene scene = getScene();
            if (scene != null) {
                scene.onVoteCommentSuccess(result);
            }
        }

        @Override
        public void onFailure(Exception e) {
            showTip(R.string.vote_failed, LENGTH_SHORT);
        }

        @Override
        public void onCancel() {}

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryCommentsScene;
        }
    }
}
