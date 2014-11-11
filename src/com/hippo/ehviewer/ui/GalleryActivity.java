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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.ExDownloader;
import com.hippo.ehviewer.gallery.GalleryView;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.gallery.ui.GLRootView;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.FullScreenHelper;
import com.hippo.ehviewer.util.Ui;

public class GalleryActivity extends Activity
        implements GalleryView.GalleryViewListener, SeekBar.OnSeekBarChangeListener {
    @SuppressWarnings("unused")
    private final static String TAG = GalleryActivity.class.getSimpleName();

    public final static String KEY_GALLERY_INFO = "gallery_info";
    public final static String KEY_START_INDEX = "start_index";

    private View mFooter;
    private TextView mSeekerBubble;

    private ActionBar mActionBar;

    /** It store index, start from 0, so max is samller than size **/
    private SeekBar mPageSeeker;
    private ImageSet mImageSet;

    private GalleryView mGalleryView;

    private GalleryInfo mGi;

    private FullScreenHelper mFullScreenHelper;

    private final Runnable mFullScreenTask = new Runnable() {
        @Override public void run() {
            setFullScreen(true);
        }
    };

    private void startFullScreenTask() {
        AppHandler.getInstance().postDelayed(mFullScreenTask, 2000);
    }

    private void cancelFullScreenTask() {
        AppHandler.getInstance().removeCallbacks(mFullScreenTask);
    }

    void setFullScreen(final boolean fullScreen) {
        mFullScreenHelper.setFullScreen(fullScreen);

        AlphaAnimation aa = fullScreen ? new AlphaAnimation(1.0f, 0.0f) : new AlphaAnimation(0.0f, 1.0f);
        aa.setDuration(Constants.ANIMATE_TIME);
        aa.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (!fullScreen)
                    mFooter.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (fullScreen)
                    mFooter.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Empty
            }
        });
        mFooter.startAnimation(aa);

        if (!fullScreen)
            startFullScreenTask();
        else
            cancelFullScreenTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFullScreen(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gl_root_group);

        mFullScreenHelper = new FullScreenHelper(this);
        mFullScreenHelper.setFullScreen(true);

        mFooter = findViewById(R.id.footer);
        mPageSeeker = (SeekBar) findViewById(R.id.page_seeker);
        mSeekerBubble = (TextView) findViewById(R.id.seeker_bubble);
        mActionBar = getActionBar();

        Intent intent = getIntent();
        mGi = (GalleryInfo) intent.getParcelableExtra(KEY_GALLERY_INFO);

        if (mGi == null) {
            // Get error force finish
            new MaterialAlertDialog.Builder(this).setTitle(R.string.error)
                    .setMessage(R.string.ga_data_error)
                    .setPositiveButton(R.string.close)
                    .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                        @Override
                        public boolean onClick(MaterialAlertDialog dialog, int which) {
                            finish();
                            return true;
                        }
                    }).show();
        } else {

            Data.getInstance().addHistory(mGi, Data.READ);

            int startIndex = intent.getIntExtra(KEY_START_INDEX, 0);
            if (startIndex == -1)
                startIndex = ExDownloader.readCurReadIndex(mGi.gid, mGi.title);

            // Keep screen on
            if (Config.getKeepSreenOn())
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            mImageSet = new ImageSet(mGi.gid, mGi.token, mGi.title, startIndex);
            mGalleryView = new GalleryView(getApplicationContext(), mImageSet, startIndex);

            GLRootView glrv= (GLRootView)findViewById(R.id.gl_root_view);
            glrv.setContentPane(mGalleryView);

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
            String keyGalleryFirst = "gallery_first";
            if (Config.getBoolean(keyGalleryFirst, true)) {
                Config.setBoolean(keyGalleryFirst, false);
                new MaterialAlertDialog.Builder(this).setTitle(R.string.tip)
                        .setMessage(R.string.gallery_tip)
                        .setNegativeButton(android.R.string.cancel)
                        .show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_refresh:
            mImageSet.redownload(mGalleryView.getCurIndex());
            cancelFullScreenTask();
            startFullScreenTask();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void updateTitle() {
        StringBuilder sb = new StringBuilder(30);
        sb.append(mGalleryView.getCurIndex() + 1).append("/")
                .append(mGalleryView.getSize())
                .append(" - ").append(mGi.gid);
        mActionBar.setTitle(sb.toString());
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
        setFullScreen(!mFullScreenHelper.getFullScreen());
    }

    @Override
    public void onPageChanged(int index) {
        updateTitle();
        mPageSeeker.setProgress(index);
    }

    @Override
    public void onSizeUpdate(int size) {
        updateTitle();
        mPageSeeker.setMax(size - 1);
        mPageSeeker.setProgress(mGalleryView.getCurIndex() + 1);
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
        int x = (getWindow().getDecorView().getWidth() - 2 * thumbOffset) * progress / max + thumbOffset;
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
        cancelFullScreenTask();
        showSeekBubble();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        startFullScreenTask();
        hideSeekBubble();
        mGalleryView.goToPage(seekBar.getProgress());
    }
}
