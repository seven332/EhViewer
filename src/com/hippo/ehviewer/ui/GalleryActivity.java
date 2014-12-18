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

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.ExDownloader;
import com.hippo.ehviewer.gallery.GalleryView;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.gallery.ui.GLRootView;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.FullScreenHelper;
import com.hippo.ehviewer.util.MathUtils;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.widget.ColorView;
import com.hippo.ehviewer.widget.SlidingLayout;

public class GalleryActivity extends AbsActivity
        implements GalleryView.GalleryViewListener, SeekBar.OnSeekBarChangeListener,
        FullScreenHelper.OnFullScreenBrokenListener, CompoundButton.OnCheckedChangeListener,
        SlidingLayout.OnChildHideListener, AdapterView.OnItemSelectedListener {

    @SuppressWarnings("unused")
    private final static String TAG = GalleryActivity.class.getSimpleName();

    public final static String KEY_GALLERY_INFO = "gallery_info";
    public final static String KEY_START_INDEX = "start_index";

    public final static String KEY_LIGHTNESS = "lightness";
    public final static float DEFAULT_LIGHTNESS = 0.5f;

    public final static String KEY_FIRST_READ_CONFIG_V1 = "first_read_config_v1";
    public final static boolean DEFAULT_FIRST_READ_CONFIG_V1 = true;

    private View mClock;
    private View mBattery;

    private SlidingLayout mConfigSliding;
    /** It store index, start from 0, so max is samller than size **/
    private TextView mCurrentPage;
    private TextView mPageSum;
    private SeekBar mPageSeeker;
    private Spinner mReadingDirection;
    private Spinner mPageScaling;
    private Spinner mStartPosition;
    private CheckBox mKeepScreenOn;
    private CheckBox mShowClock;
    private CheckBox mShowBattery;
    private CheckBox mCustomLightness;
    private SeekBar mCustomLightnessValue;
    private CheckBox mCustomCodec;
    private Spinner mDecodeFormat;

    private ImageSet mImageSet;

    private GalleryView mGalleryView;

    private ColorView mMaskView;

    private GalleryInfo mGi;

    private FullScreenHelper mFullScreenHelper;

    @Override
    public void onFullScreenBroken(boolean fullScreen) {
        // TODO For some MIUI system, onSystemUiVisibilityChange() do not work fine
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateState();
        mFullScreenHelper.setFullScreen(!mConfigSliding.isShowing());
    }

    private void setScreenLightness(boolean enable, int lightness) {
        Window w = getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        if (enable) {
            lightness = MathUtils.clamp(lightness, 0, 200);
            if (lightness > 100) {
                mMaskView.setColor(0);
                lp.screenBrightness = Math.max((lightness - 100) / 100.0f, 0.05f);
            } else {
                mMaskView.setColor(MathUtils.lerp(0xde, 0x00, lightness / 100.0f) << 24);
                lp.screenBrightness = 0.05f;
            }
        } else {
            mMaskView.setColor(0);
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        }
        w.setAttributes(lp);
    }

    private void updateState() {
        mReadingDirection.setSelection(Config.getReadingDirection());
        mPageScaling.setSelection(Config.getPageScaling());
        mStartPosition.setSelection(Config.getStartPosition());
        mGalleryView.setReadingDirection(Config.getReadingDirection());
        mGalleryView.setPageScaling(Config.getPageScaling());
        mGalleryView.setStartPosition(Config.getStartPosition());

        mKeepScreenOn.setChecked(Config.getKeepSreenOn());
        if (Config.getKeepSreenOn())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mShowClock.setChecked(Config.getShowClock());
        mShowBattery.setChecked(Config.getShowBattery());
        mClock.setVisibility(Config.getShowClock() ? View.VISIBLE : View.GONE);
        mBattery.setVisibility(Config.getShowBattery() ? View.VISIBLE : View.GONE);

        mCustomLightness.setChecked(Config.getCustomLightness());
        if (mCustomLightness.isChecked())
            mCustomLightnessValue.setEnabled(true);
        else
            mCustomLightnessValue.setEnabled(false);
        mCustomLightnessValue.setProgress(Config.getCustomLightnessValue());
        setScreenLightness(mCustomLightness.isChecked(), mCustomLightnessValue.getProgress());

        if (Utils.SUPPORT_IMAGE)
            mCustomCodec.setEnabled(true);
        else
            mCustomCodec.setEnabled(false);
        mCustomCodec.setChecked(Config.getCustomCodec());
        if (Utils.SUPPORT_IMAGE && mCustomCodec.isChecked())
            mDecodeFormat.setEnabled(true);
        else
            mDecodeFormat.setEnabled(false);
        mDecodeFormat.setSelection(Config.getDecodeFormatIndex());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gl_root_group);

        mFullScreenHelper = new FullScreenHelper(this);
        mFullScreenHelper.setOnFullScreenBrokenListener(this);
        mFullScreenHelper.setFullScreen(true);

        mMaskView = (ColorView)findViewById(R.id.mask);
        mClock = findViewById(R.id.clock);
        mBattery = findViewById(R.id.battery);

        mConfigSliding = (SlidingLayout) findViewById(R.id.config_sliding);
        mCurrentPage = (TextView) mConfigSliding.findViewById(R.id.current_page);
        mPageSum = (TextView) mConfigSliding.findViewById(R.id.page_sum);
        mPageSeeker = (SeekBar) mConfigSliding.findViewById(R.id.page_seeker);
        mReadingDirection = (Spinner) mConfigSliding.findViewById(R.id.reading_direction);
        mPageScaling = (Spinner) mConfigSliding.findViewById(R.id.page_scaling);
        mStartPosition = (Spinner) mConfigSliding.findViewById(R.id.start_position);
        mKeepScreenOn = (CheckBox) mConfigSliding.findViewById(R.id.keep_screen_on);
        mShowClock = (CheckBox) mConfigSliding.findViewById(R.id.gallery_show_clock);
        mShowBattery = (CheckBox) mConfigSliding.findViewById(R.id.gallery_show_battery);
        mCustomLightness = (CheckBox) mConfigSliding.findViewById(R.id.custom_lightness);
        mCustomLightnessValue = (SeekBar) mConfigSliding.findViewById(R.id.custom_lightness_value);
        mCustomCodec = (CheckBox) mConfigSliding.findViewById(R.id.custom_codec);
        mDecodeFormat = (Spinner) mConfigSliding.findViewById(R.id.decode_format);

        mConfigSliding.hide();

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

            mImageSet = new ImageSet(mGi.gid, mGi.token, mGi.title, startIndex);
            mGalleryView = new GalleryView(getApplicationContext(), mImageSet, startIndex);

            GLRootView glrv= (GLRootView)findViewById(R.id.gl_root_view);
            glrv.setContentPane(mGalleryView);

            // Set listener
            mConfigSliding.setOnChildHideListener(this);
            mGalleryView.setGalleryViewListener(this);
            mPageSeeker.setOnSeekBarChangeListener(this);
            mReadingDirection.setOnItemSelectedListener(this);
            mPageScaling.setOnItemSelectedListener(this);
            mStartPosition.setOnItemSelectedListener(this);
            mDecodeFormat.setOnItemSelectedListener(this);
            mKeepScreenOn.setOnCheckedChangeListener(this);
            mShowClock.setOnCheckedChangeListener(this);
            mShowBattery.setOnCheckedChangeListener(this);
            mCustomLightness.setOnCheckedChangeListener(this);
            mCustomLightnessValue.setOnSeekBarChangeListener(this);
            mCustomCodec.setOnCheckedChangeListener(this);

            mKeepScreenOn = (CheckBox) mConfigSliding.findViewById(R.id.keep_screen_on);
            mShowClock = (CheckBox) mConfigSliding.findViewById(R.id.gallery_show_clock);
            mShowBattery = (CheckBox) mConfigSliding.findViewById(R.id.gallery_show_battery);
            mCustomCodec = (CheckBox) mConfigSliding.findViewById(R.id.custom_codec);

            mPageSeeker.setMax(mGalleryView.getSize() - 1);
            mPageSeeker.setProgress(startIndex);

            mCurrentPage.setText(Integer.toString(startIndex + 1));
            mPageSum.setText(Integer.toString(mGalleryView.getSize()));

            onRightToLeftChanged(mGalleryView.isRightToLeft());

            // Show first tip
            String keyGalleryFirst = "gallery_first";
            if (Config.getBoolean(keyGalleryFirst, true)) {
                Config.setBoolean(keyGalleryFirst, false);
                new MaterialAlertDialog.Builder(this).setDrakTheme(true).setTitle(R.string.tip)
                        .setMessage(R.string.gallery_tip)
                        .setNegativeButton(android.R.string.cancel)
                        .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                            @Override
                            public boolean onClick(MaterialAlertDialog dialog, int which) {
                                mFullScreenHelper.setFullScreen(true);
                                return true;
                            }
                        }).show();
            }

            // Update state
            updateState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mImageSet != null)
            mImageSet.free();
        if (mGalleryView != null)
            mGalleryView.free();
    }

    @Override
    public void onTapCenter() {
        if (!mConfigSliding.isShowing()) {
            mFullScreenHelper.setFullScreen(false);

            boolean firstTime = Config.getBoolean(KEY_FIRST_READ_CONFIG_V1, DEFAULT_FIRST_READ_CONFIG_V1);
            mConfigSliding.toReservedLevel(firstTime);
            if (firstTime)
                Config.setBoolean(KEY_FIRST_READ_CONFIG_V1, false);
        }
    }

    @Override
    public void onPageChanged(int index) {
        mPageSeeker.setProgress(index);
        mCurrentPage.setText(Integer.toString(index + 1));
    }

    @Override
    public void onSizeUpdate(int size) {
        mPageSeeker.setMax(size - 1);
        mPageSeeker.setProgress(mGalleryView.getCurIndex() + 1);
        mCurrentPage.setText(Integer.toString(mGalleryView.getCurIndex() + 1));
        mPageSum.setText(Integer.toString(size));
    }

    @Override
    public void onSlideBottom(float dx) {
        // Empty
    }

    @Override
    public void onSlideBottomOver() {
        // Empty
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void onRightToLeftChanged(boolean value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            int nevLd = value ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
            if (mPageSeeker.getLayoutDirection() != nevLd) {
                mPageSeeker.setLayoutDirection(nevLd);
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        if (seekBar == mPageSeeker) {
            mCurrentPage.setText(Integer.toString(progress + 1));
        } else if (seekBar == mCustomLightnessValue) {
            setScreenLightness(mCustomLightness.isChecked(), progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Empty
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar == mPageSeeker) {
            mGalleryView.goToPage(seekBar.getProgress());
        } else if (seekBar == mCustomLightnessValue) {
            Config.setCustomLightnessValue(seekBar.getProgress());
        }
    }

    @Override
    public void onChildHide() {
        mFullScreenHelper.setFullScreen(true);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        if (parent == mReadingDirection) {
            Config.setReadingDirection(mReadingDirection.getSelectedItemPosition());
            mGalleryView.setReadingDirection(position);
        } else if (parent == mPageScaling) {
            Config.setPageScaling(mPageScaling.getSelectedItemPosition());
            mGalleryView.setPageScaling(position);
        } else if (parent == mStartPosition) {
            Config.setStartPosition(mStartPosition.getSelectedItemPosition());
            mGalleryView.setStartPosition(position);
        } else if (parent == mDecodeFormat) {
            Config.setDecodeFormatFromIndex(mDecodeFormat.getSelectedItemPosition());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Empty
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mShowClock) {
            Config.setShowClock(isChecked);
            mClock.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        } else if (buttonView == mShowBattery) {
            Config.setShowBattery(isChecked);
            mBattery.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        } else if (buttonView == mKeepScreenOn) {
            Config.setKeepSreenOn(isChecked);
            if (isChecked)
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if (buttonView == mCustomLightness) {
            Config.setCustomLightness(isChecked);
            if (isChecked)
                mCustomLightnessValue.setEnabled(true);
            else
                mCustomLightnessValue.setEnabled(false);
            setScreenLightness(isChecked, mCustomLightnessValue.getProgress());
        } else if (buttonView == mCustomCodec) {
            Config.setCustomCodec(isChecked);
            if (Utils.SUPPORT_IMAGE && mCustomCodec.isChecked())
                mDecodeFormat.setEnabled(true);
            else
                mDecodeFormat.setEnabled(false);
        }
    }
}
