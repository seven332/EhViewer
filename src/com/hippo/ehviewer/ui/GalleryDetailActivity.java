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

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.widget.ScrollView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.ApiGalleryDetail;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.LofiGalleryDetail;
import com.hippo.ehviewer.data.LofiGalleryInfo;
import com.hippo.ehviewer.drawable.OvalDrawable;
import com.hippo.ehviewer.ehclient.DetailUrlParser;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.ProgressiveRatingBar;
import com.hippo.ehviewer.widget.RefreshTextView;
import com.hippo.ehviewer.widget.SuperButton;
import com.hippo.ehviewer.widget.SuperToast;
import com.hippo.ehviewer.windowsanimate.WindowsAnimate;

public class GalleryDetailActivity extends AbstractActivity
        implements View.OnClickListener, FswView.OnFitSystemWindowsListener,
        View.OnTouchListener , ViewSwitcher.ViewFactory,
        ProgressiveRatingBar.OnUserRateListener {

    @SuppressWarnings("unused")
    private static final String TAG = GalleryDetailActivity.class.getSimpleName();

    public static final String KEY_G_INFO = "gallery_info";

    private Resources mResources;
    private EhClient mClient;
    private WindowsAnimate mWindowsAnimate;
    private GalleryInfo mGalleryInfo;

    private int mRunningAnimateNum = 0;
    private boolean isCheckmark = false;
    private boolean isSetDrawable = false;

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

    private ScrollView mDetailScroll;
    private ScrollView mMoreDetailScroll;

    private View mDetailHeader;
    private View mDetailButtons;
    private TextView mWarning;
    private View mDetailActions;
    private View mDividerAM;
    private View mDetailMore;
    private View mDividerMR;
    private View mDetailRate;

    private OvalDrawable mCategoryDrawable;
    private Drawable mRateDrawable;
    private Drawable mCheckmarkDrawable;

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
            gi = (GalleryInfo)(intent.getParcelableExtra(KEY_G_INFO));
            if (gi != null) {
                if (gi instanceof LofiGalleryInfo)
                    gi = new LofiGalleryDetail((LofiGalleryInfo)gi);
                else
                    gi = new GalleryDetail(gi);
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

        mClient = EhClient.getInstance();
        mWindowsAnimate = new WindowsAnimate();
        mWindowsAnimate.init(this);

        mResources = getResources();
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Get view
        mDetailScroll = (ScrollView)findViewById(R.id.detail_scroll);
        mMoreDetailScroll = (ScrollView)findViewById(R.id.more_detail_scroll);
        mRefreshText = (RefreshTextView)findViewById(R.id.refresh_text);

        mDetailHeader = findViewById(R.id.detail_header);
        mDetailButtons = findViewById(R.id.detail_buttons);
        mWarning = (TextView)findViewById(R.id.warning);
        mDetailActions = findViewById(R.id.detail_actions);
        mDividerAM = findViewById(R.id.detail_divider_a_m);
        mDetailMore = findViewById(R.id.detail_more);
        mDividerMR = findViewById(R.id.detail_divider_m_r);
        mDetailRate = findViewById(R.id.detail_rate);

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

        // Get temp view
        FswView alignment = (FswView)findViewById(R.id.alignment);

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

        // Set ripple
        mWindowsAnimate.addRippleEffect(mCategory, false);
        mWindowsAnimate.addRippleEffect(mMoreOfUploader, false);
        mWindowsAnimate.addRippleEffect(mSimilar, false);
        mWindowsAnimate.addRippleEffect(mFavorite, false);

        // Init
        mRatingText.setFactory(this);

        // Set random color
        int color = Config.getRandomThemeColor() ? Theme.getRandomDarkColor() : Config.getThemeColor();
        int actionBarColor = color & 0x00ffffff | 0xdd000000;
        Drawable drawable = new ColorDrawable(actionBarColor);
        actionBar.setBackgroundDrawable(drawable);
        Ui.translucent(this, actionBarColor);
        mDownloadButton.setRoundBackground(true, mResources.getColor(R.color.background_light), color);
        mDownloadButton.setTextColor(color);
        mReadButton.setRoundBackground(true, color, 0);
        ((TextView)findViewById(R.id.detail_more_text)).setTextColor(color);

        // Set listener
        mDownloadButton.setOnClickListener(this);
        mReadButton.setOnClickListener(this);
        mCategory.setOnClickListener(this);
        mMoreOfUploader.setOnClickListener(this);
        mSimilar.setOnClickListener(this);
        mRating.setOnUserRateListener(this);
        mFavorite.setOnClickListener(this);
        mRate.setOnClickListener(this);
        mDetailMore.setOnTouchListener(this);
        alignment.addOnFitSystemWindowsListener(this);

        doPreLayout();
    }

    private void doPreLayout() {
        String detailUrl = mClient.getDetailUrl(
                mGalleryInfo.gid, mGalleryInfo.token);

        if (mGalleryInfo instanceof GalleryDetail) {
            GalleryDetail galleryDetail = (GalleryDetail)mGalleryInfo;
            if (galleryDetail.title == null) {
                // TODO
                // We should get info from detail page
            } else {
                mDetailScroll.setVisibility(View.VISIBLE);
                mDetailHeader.setVisibility(View.VISIBLE);

                mThumb.setLoadInfo(galleryDetail.thumb, String.valueOf(galleryDetail.gid));
                ImageLoader.getInstance(this).add(galleryDetail.thumb, String.valueOf(galleryDetail.gid),
                        new LoadImageView.SimpleImageGetListener(mThumb).setTransitabled(false));
                mTitle.setText(galleryDetail.title);
                mUploader.setText(galleryDetail.uploader);

                mClient.getGDetail(detailUrl, galleryDetail, new GDetailGetListener());
            }
        } else {

        }
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
        return new StringBuilder().append("大家觉得").append(" ") // TODO
                .append(getRatingText(rating))
                .append(" (").append(rating).append(", ").append(people).append(")").toString();
    }

    /**
     * GalleryDetail
     * LofiGalleryDetail
     * ApiGalleryDetail
     */
    private void doLayout() {
        if (mDetailHeader.getVisibility() == View.GONE) {
            // If not set header in doPreLayout
            mDetailHeader.setVisibility(View.VISIBLE);
            mThumb.setLoadInfo(mGalleryInfo.thumb, String.valueOf(mGalleryInfo.gid));
            ImageLoader.getInstance(this).add(mGalleryInfo.thumb, String.valueOf(mGalleryInfo.gid),
                    new LoadImageView.SimpleImageGetListener(mThumb).setTransitabled(false));
            mTitle.setText(mGalleryInfo.title);
            mUploader.setText(mGalleryInfo.uploader);
        }

        mDetailButtons.setVisibility(View.VISIBLE);
        mWarning.setVisibility(View.VISIBLE);
        mDetailActions.setVisibility(View.VISIBLE);
        mDividerAM.setVisibility(View.VISIBLE);
        mDetailMore.setVisibility(View.VISIBLE);
        mDividerMR.setVisibility(View.VISIBLE);
        mDetailRate.setVisibility(View.VISIBLE);

        mCategory.setText(Ui.getCategoryText(mGalleryInfo.category));
        mCategoryDrawable.setColor(Ui.getCategoryColor(mGalleryInfo.category));

        if (mGalleryInfo instanceof GalleryDetail) {
            GalleryDetail galleryDetail = (GalleryDetail)mGalleryInfo;

            // TODO set language
            ((TextView)mDetailMore.findViewById(R.id.language)).setText("语言: " + galleryDetail.language); // TODO
            ((TextView)mDetailMore.findViewById(R.id.posted)).setText(galleryDetail.posted);
            ((TextView)mDetailMore.findViewById(R.id.pages)).setText("页面: " + String.valueOf(galleryDetail.pages));
            ((TextView)mDetailMore.findViewById(R.id.size)).setText("大小: " + galleryDetail.size);
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
            mRatingText.setCurrentText(getAllRatingText(galleryDetail.rating, galleryDetail.people));
            mRating.setRating(galleryDetail.rating);
            mRating.setEnableRate(false);

        } else if (mGalleryInfo instanceof GalleryDetail) {

        } else if (mGalleryInfo instanceof ApiGalleryDetail) {

        }
    }

    @Override
    public void onBackPressed() {
        if (mRunningAnimateNum != 0 || mWindowsAnimate.isRunningAnimate())
            return;

        if (mDetailScroll.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        } else if (mMoreDetailScroll.getVisibility() == View.VISIBLE) {
            mDetailScroll.setVisibility(View.VISIBLE);

            ValueAnimator animation = ValueAnimator.ofInt(mMoreDetailScroll.getLeft(), mMoreDetailScroll.getRight());
            animation.setDuration(Constants.ANIMATE_TIME);
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int left = (Integer)animation.getAnimatedValue();
                    mMoreDetailScroll.setLeft(left);
                }
            });
            animation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMoreDetailScroll.setVisibility(View.GONE);
                    mRunningAnimateNum--;
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
            });
            animation.setInterpolator(new  AccelerateInterpolator());
            animation.start();
            mRunningAnimateNum++;
        }
    }

    @Override
    public void onClick(View v) {
        if (mRunningAnimateNum != 0 || mWindowsAnimate.isRunningAnimate())
            return;

        if (v == mDownloadButton) {
        } else if (v == mReadButton) {
            //mData.addRead(mGalleryInfo);
            if (mGalleryInfo instanceof GalleryDetail) {
                GalleryDetail galleryDetail = (GalleryDetail)mGalleryInfo;
                Intent intent = new Intent(this,
                        GalleryActivity.class);
                intent.putExtra("url", galleryDetail.firstPage);
                intent.putExtra("gid", mGalleryInfo.gid);
                intent.putExtra("title", mGalleryInfo.title);
                intent.putExtra("firstPage", 0);
                intent.putExtra("pageSum", galleryDetail.pages);
                startActivity(intent);
            } else {
                // TODO
            }
        } else if (v == mFavorite) {
            Favorite.addToFavorite(this, mGalleryInfo);
        } else if (v == mRate) {
            if (isCheckmark) {
                int rating = (int)(mRating.getRating() * 2 + 0.5);
                if (rating <= 0 || rating > 10) {
                    new SuperToast(R.string.invalid_rating, SuperToast.ERROR).show();
                    return;
                }
                mClient.rate(mGalleryInfo.gid, mGalleryInfo.token, rating, new EhClient.OnRateListener() {
                    @Override
                    public void onSuccess(float ratingAvg, int ratingCnt) {
                        mGalleryInfo.rating = ratingAvg;
                        // TODO mGalleryInfo.people = ratingCnt;
                        mRatingText.setText(getAllRatingText(mGalleryInfo.rating, ratingCnt));
                        mRating.setRating(mGalleryInfo.rating);
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        mRatingText.setText("评分失败"); // TODO
                        new SuperToast(eMsg, SuperToast.WARNING).show();
                    }
                });
                mRatingText.setText("感谢评分"); // TODO
                mRating.setEnableRate(false);
            } else {
                mRatingText.setText("请理智评分"); // TODO
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
        }
    }

    @Override
    public void onfitSystemWindows(int paddingLeft, int paddingTop,
            int paddingRight, int paddingBottom) {
        mDetailScroll.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        mMoreDetailScroll.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        if (v == mDetailMore) {
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    System.nanoTime() / 1000000 - event.getDownTime() < 100) {
                mWindowsAnimate.addCircleTransitions(mDetailMore, (int)event.getX(),
                        (int)event.getY(), mResources.getColor(R.color.background_light),
                        new WindowsAnimate.OnAnimationEndListener() {
                            @Override
                            public void onAnimationEnd() {
                                mDetailScroll.setVisibility(View.GONE);
                                mMoreDetailScroll.setVisibility(View.VISIBLE);
                                mMoreDetailScroll.scrollTo(0, 0);
                                AlphaAnimation aa = new AlphaAnimation(0.0f,1.0f);
                                aa.setDuration(Constants.ANIMATE_TIME);
                                mMoreDetailScroll.startAnimation(aa);
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
    public View makeView() {
        TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private class GDetailGetListener
            implements EhClient.OnGetGDetailListener {
        @Override
        public void onSuccess(GalleryDetail md) {
            doLayout();
        }

        @Override
        public void onFailure(String eMsg) {
            // TODO
            new SuperToast(eMsg, SuperToast.ERROR).show();
       }
    }
}
