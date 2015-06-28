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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.hippo.conaco.Conaco;
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhImageKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.RatingView;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.Scene;
import com.hippo.util.Log;

public class GalleryDetailScene extends Scene implements View.OnClickListener {

    public static final String KEY_GALLERY_INFO = "gallery_info";

    private ViewGroup mHeader;
    LoadImageView mThumb;
    TextView mTitle;
    TextView mUploader;
    TextView mCategory;

    private ViewGroup mActionCard;
    private View mRead;
    private View mDownload;

    private ViewGroup mContent;
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

    private View mProgressBar;

    private ViewTransition mViewTransition;

    private void handleAnnouncer(Announcer announcer) {
        GalleryInfo gi;

        if (announcer == null) {
            Log.e("No announcer in GalleryDetailScene, finish itself");
            finish();
            return;
        } else if (null != (gi = announcer.getParcelableExtra(KEY_GALLERY_INFO))){
            Conaco conaco = EhApplication.getConaco(getStageActivity());
            conaco.load(mThumb, EhImageKeyFactory.getThumbKey(gi.gid), gi.thumb);
            mTitle.setText(gi.title);
            mUploader.setText(gi.uploader);
            mCategory.setText(EhUtils.getCategory(gi.category));
            mCategory.setTextColor(EhUtils.getCategoryColor(gi.category));

            EhClient.getInstance().getGalleryDetail(Config.getEhSource(),
                    EhClient.getDetailUrl(Config.getEhSource(), gi.gid, gi.token, 0),
                    new EhDetailListener());
        }

        Context context = getStageActivity();
        Resources resources = context.getResources();

        mViewTransition.showView(1);

        RippleSalon.addRipple(mRead, false);
        RippleSalon.addRipple(mDownload, false);

        mRead.setOnClickListener(this);
        mDownload.setOnClickListener(this);

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
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scene_gallery_detail);

        mHeader = (ViewGroup) findViewById(R.id.header);
        mThumb = (LoadImageView) mHeader.findViewById(R.id.thumb);
        mTitle = (TextView) mHeader.findViewById(R.id.title);
        mUploader = (TextView) mHeader.findViewById(R.id.uploader);
        mCategory = (TextView) mHeader.findViewById(R.id.category);

        mActionCard = (ViewGroup) findViewById(R.id.action_card);
        mRead = mActionCard.findViewById(R.id.read);
        mDownload = mActionCard.findViewById(R.id.download);

        mContent = (ViewGroup) findViewById(R.id.content);
        mLanguage = (TextView) mContent.findViewById(R.id.language);
        mPages = (TextView) mContent.findViewById(R.id.pages);
        mSize = (TextView) mContent.findViewById(R.id.size);
        mPosted = (TextView) mContent.findViewById(R.id.posted);
        mResize = (TextView) mContent.findViewById(R.id.resize);
        mFavoredTimes = (TextView) mContent.findViewById(R.id.favoredTimes);
        mRatingText = (TextSwitcher) mContent.findViewById(R.id.rating_text);
        mRating = (RatingView) mContent.findViewById(R.id.rating);
        mFavorite = (TextView) mContent.findViewById(R.id.favorite);
        mTorrent = (TextView) mContent.findViewById(R.id.torrent);
        mShare = (TextView) mContent.findViewById(R.id.share);
        mRate = (TextView) mContent.findViewById(R.id.rate);

        mProgressBar = findViewById(R.id.progress_bar);

        mViewTransition = new ViewTransition(mContent, mProgressBar);

        handleAnnouncer(getAnnouncer());
    }

    @Override
    public void onClick(View v) {
        if (v == mRead) {

        } else if (v == mDownload) {

        }
    }

    private String getRatingText(float rating) {
        String undefine = "(´_ゝ`)";
        if (rating == Float.NaN) {
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

    private class RatingTextViewFactory implements ViewSwitcher.ViewFactory {
        @Override
        public View makeView() {
            TextView tv = new TextView(getStageActivity());
            tv.setGravity(Gravity.CENTER);
            return tv;
        }
    }

    private class EhDetailListener extends EhClient.OnGetGalleryDetailListener {

        @Override
        public void onSuccess(GalleryDetail galleryDetail) {
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
            mViewTransition.showView(0);
        }

        @Override
        public void onFailure(Exception e) {
            e.printStackTrace();
        }
    }
}
