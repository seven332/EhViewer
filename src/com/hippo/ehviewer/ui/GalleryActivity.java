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
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.GalleryView;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.gallery.ui.GLRootView;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;

public class GalleryActivity extends AbsActivity
        implements GalleryView.GalleryViewListener, SeekBar.OnSeekBarChangeListener {
    @SuppressWarnings("unused")
    private final static String TAG = GalleryActivity.class.getSimpleName();

    private static final Interpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final int ACTION_BAR_HEIGHT = 48;
    private static final int NAV_BAR_HEIGHT = 48;

    public final static String KEY_GID = "gid";
    public final static String KEY_TOKEN = "token";
    public final static String KEY_TITLE = "title";
    public final static String KEY_START_INDEX = "start_index";

    private View mainView;
    private View mActionBar;
    private View mNavBar;
    private TextView mTitleView;
    private TextView mSeekerBubble;

    /** It store index, start from 0, so max is samller than size **/
    private SeekBar mPageSeeker;
    private ImageSet mImageSet;

    private GalleryView mGalleryView;

    private int mGid;
    private String mTitle;

    private ValueAnimator mShowAnimator;
    private ValueAnimator mHideAnimator;

    @Override
    public void onOrientationChanged(int paddingTop, int paddingBottom) {
        // Empty
    }

    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                hasFocus && mainView != null) {
            mainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                mainView != null) {
            mainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    private void initAnimator() {
        mShowAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        mShowAnimator.setDuration(Constants.ANIMATE_TIME);
        mShowAnimator.setInterpolator(ACCELERATE_INTERPOLATOR);
        mShowAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float)animation.getAnimatedValue();
                mActionBar.setY(- value * Ui.dp2pix(ACTION_BAR_HEIGHT));
                mNavBar.setY(mainView.getHeight() - (1.0f - value) * Ui.dp2pix(NAV_BAR_HEIGHT));
                mActionBar.requestLayout();
                mNavBar.requestLayout();
            }
        });
        mShowAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mActionBar.setVisibility(View.VISIBLE);
                mNavBar.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {}
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        mHideAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mHideAnimator.setDuration(Constants.ANIMATE_TIME);
        mHideAnimator.setInterpolator(ACCELERATE_INTERPOLATOR);
        mHideAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float)animation.getAnimatedValue();
                mActionBar.setY(- value * Ui.dp2pix(ACTION_BAR_HEIGHT));
                mNavBar.setY(mainView.getHeight() - (1.0f - value) * Ui.dp2pix(NAV_BAR_HEIGHT));
                mActionBar.requestLayout();
                mNavBar.requestLayout();
            }
        });
        mHideAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mActionBar.setVisibility(View.VISIBLE);
                mNavBar.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mActionBar.setVisibility(View.GONE);
                mNavBar.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gl_root_group);

        mainView = findViewById(R.id.main);
        mActionBar = findViewById(R.id.action_bar);
        mNavBar = findViewById(R.id.nav_bar);
        mTitleView = (TextView)findViewById(R.id.title);
        mPageSeeker = (SeekBar)findViewById(R.id.page_seeker);
        mSeekerBubble = (TextView)findViewById(R.id.seeker_bubble);

        // FullScreen
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            mainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        Intent intent = getIntent();
        mGid = intent.getIntExtra(KEY_GID, -1);
        String token = intent.getStringExtra(KEY_TOKEN);
        mTitle = intent.getStringExtra(KEY_TITLE);
        int startIndex = intent.getIntExtra(KEY_START_INDEX, 0);

        if (mGid == -1 || token == null || mTitle == null) {
            new DialogBuilder(this).setTitle(R.string.error)
                    .setMessage("数据错误！").setPositiveButton("关闭",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((AlertButton)v).dialog.dismiss();
                            finish();
                        }
                    }).create().show();
        } else {
            mImageSet = new ImageSet(mGid, token, mTitle, startIndex);
            mGalleryView = new GalleryView(getApplicationContext(), mImageSet, startIndex);
            GLRootView glrv= (GLRootView)findViewById(R.id.gl_root_view);
            glrv.setContentPane(mGalleryView);

            initAnimator();

            // Hide or show
            if (!Config.getGShowTime())
                findViewById(R.id.clock).setVisibility(View.GONE);
            if (!Config.getGShowBattery())
                findViewById(R.id.battery).setVisibility(View.GONE);

            // Set listener
            mGalleryView.setGalleryViewListener(this);
            mPageSeeker.setOnSeekBarChangeListener(this);

            updateTitle();
            mPageSeeker.setMax(mGalleryView.getSize() - 1);
            mPageSeeker.setProgress(startIndex);

            // Show first tip
            if (Config.getGalleryFirst()) {
                Config.setGalleryFirst(false);
                new DialogBuilder(this).setTitle(R.string.tip)
                        .setMessage(R.string.gallery_tip)
                        .setSimpleNegativeButton().create().show();
            }
        }
    }

    private void updateTitle() {
        StringBuilder sb = new StringBuilder(30);
        sb.append(mGalleryView.getCurIndex() + 1).append("/")
                .append(mGalleryView.getSize())
                .append(" - ").append(mGid);
        mTitleView.setText(sb.toString());
    }

    private void startHideTask() {
        AppHandler.getInstance().postDelayed(mHideTask, 2000);
    }

    private void cancelHideTask() {
        AppHandler.getInstance().removeCallbacks(mHideTask);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mImageSet != null)
            mImageSet.free();
        if (mGalleryView != null)
            mGalleryView.free();
    }

    @Override
    public void onTapCenter() {
        if (mShowAnimator.isRunning() || mHideAnimator.isRunning())
            return;

        if (mActionBar.getVisibility() == View.GONE) {
            mShowAnimator.start();
            startHideTask();
        } else if (mActionBar.getVisibility() == View.VISIBLE) {
            mHideAnimator.start();
        }
    }

    private final Runnable mHideTask = new Runnable() {
        @Override
        public void run() {
            if (!mShowAnimator.isRunning() && !mHideAnimator.isRunning()
                    && mActionBar.getVisibility() == View.VISIBLE)
                mHideAnimator.start();
        }
    };

    @Override
    public void onPageChanged(int index) {
        updateTitle();
        mPageSeeker.setProgress(index);
    }

    @Override
    public void onSizeUpdate(int size) {
        updateTitle();
        mPageSeeker.setMax(size - 1);
    }

    private void showSeekBubble() {
        mSeekerBubble.setVisibility(View.VISIBLE);
    }

    private void hideSeekBubble() {
        mSeekerBubble.setVisibility(View.GONE);
    }

    public void updateSeekBubble(int progress, int max, int thumbOffset) {
        if (max == 0)
            // Avoid divide by zero
            return;

        thumbOffset = thumbOffset + Ui.dp2pix(12);
        int x = (mainView.getWidth() - 2 * thumbOffset) * progress / max + thumbOffset;
        mSeekerBubble.setX(x);
        mSeekerBubble.setText(String.valueOf(progress + 1));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        updateSeekBubble(progress, seekBar.getMax(), seekBar.getThumbOffset());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        cancelHideTask();
        showSeekBubble();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        startHideTask();
        hideSeekBubble();
        mGalleryView.goToPage(seekBar.getProgress());
    }
}
