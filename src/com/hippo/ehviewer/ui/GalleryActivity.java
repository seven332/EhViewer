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

import java.io.File;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
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

import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.DirSelectDialog;
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
import com.hippo.ehviewer.widget.MaterialToast;
import com.hippo.ehviewer.widget.SlidingLayout;

public class GalleryActivity extends AbsActivity
        implements GalleryView.GalleryViewListener, SeekBar.OnSeekBarChangeListener,
        FullScreenHelper.OnFullScreenBrokenListener, CompoundButton.OnCheckedChangeListener,
        SlidingLayout.OnChildHideListener, AdapterView.OnItemSelectedListener,
        View.OnClickListener, Utils.OnCopyOverListener {

    @SuppressWarnings("unused")
    private final static String TAG = GalleryActivity.class.getSimpleName();

    public final static String KEY_GALLERY_INFO = "gallery_info";
    public final static String KEY_START_INDEX = "start_index";

    public final static String KEY_LIGHTNESS = "lightness";
    public final static float DEFAULT_LIGHTNESS = 0.5f;

    public final static String KEY_FIRST_READ_CONFIG_V1 = "first_read_config_v1";
    public final static boolean DEFAULT_FIRST_READ_CONFIG_V1 = true;

    public final static String KEY_LAST_SAVE_IMAGE_FILE_PATH = "last_save_image_file_path";
    public final static String DEFAULT_LAST_SAVE_IMAGE_FILE_PATH = null;

    private View mClock;
    private View mBattery;

    private SlidingLayout mConfigSliding;
    /** It store index, start from 0, so max is samller than size **/
    private TextView mCurrentPage;
    private TextView mPageSum;
    private SeekBar mPageSeeker;
    private TextView mRefresh;
    private TextView mSend;
    private TextView mMoreSettings;
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

    private File mCurImageFile;

    private Dialog mSendDialog;

    private final Runnable mFullScreenTask = new Runnable() {
        @Override public void run() {
            mFullScreenHelper.setFullScreen(true);
        }
    };

    private void startFullScreenTask() {
        AppHandler.getInstance().postDelayed(mFullScreenTask, 2000);
    }

    private void cancelFullScreenTask() {
        AppHandler.getInstance().removeCallbacks(mFullScreenTask);
    }

    @Override
    public void onCopyOver(boolean success, File src, File dst) {
        MaterialToast.showToast(success ? String.format(getString(R.string.save_successful), dst.getPath())
                : getString(R.string.save_failed));
        if (success)
            Config.setString(KEY_LAST_SAVE_IMAGE_FILE_PATH, dst.getParent());
    }

    private Dialog createSaveImageDialog() {
        return DirSelectDialog.create(
                new MaterialAlertDialog.Builder(GalleryActivity.this)
                        .setDrakTheme(true).setTitle(R.string.select_save_dir)
                        .setDefaultButton(MaterialAlertDialog.POSITIVE | MaterialAlertDialog.NEGATIVE)
                        .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                            @Override
                            public boolean onClick(MaterialAlertDialog dialog, int which) {
                                if (which == MaterialAlertDialog.POSITIVE) {
                                    String targetPath = ((DirSelectDialog) dialog).getCurrentPath();
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(mImageSet.getTitle()).append("-").append(mGalleryView.getCurIndex() + 1)
                                            .append(".").append(Utils.getExtension(mCurImageFile.getName(), "jpg"));
                                    Utils.copyInNewThread(mCurImageFile, new File(targetPath, Utils.standardizeFilename(sb.toString())),
                                            GalleryActivity.this);
                                }
                                return true;
                            }
                        }),
                Config.getString(KEY_LAST_SAVE_IMAGE_FILE_PATH, null));
    }

    private Dialog createSendDialog() {
        return new MaterialAlertDialog.Builder(this).setDrakTheme(true).setTitle(R.string.send)
                .setItems(R.array.send_entries, new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        // Check current image file again
                        if (mCurImageFile == null || !mCurImageFile.exists()) {
                            MaterialToast.showToast(R.string.current_image_not_downloaded);
                            return false;
                        }

                        switch (which) {
                        case 0:
                            createSaveImageDialog().show();
                            break;
                        case 1:
                            Uri uri = Uri.fromFile(mCurImageFile);
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                            shareIntent.setType(Utils.getMimeType(uri.toString()));
                            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_image)));
                            break;
                        }
                        return true;
                    }
                }).setNegativeButton(android.R.string.cancel).create();
    }


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
                // Avoid BRIGHTNESS_OVERRIDE_OFF,
                // screen may be off when lp.screenBrightness is 0.0f
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
        mRefresh = (TextView) mConfigSliding.findViewById(R.id.refresh);
        mSend = (TextView) mConfigSliding.findViewById(R.id.send);
        mMoreSettings = (TextView) mConfigSliding.findViewById(R.id.more_settings);
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

        mSendDialog = createSendDialog();

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
            mRefresh.setOnClickListener(this);
            mSend.setOnClickListener(this);
            mMoreSettings.setOnClickListener(this);
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
    public void onBackPressed() {
        if (mConfigSliding.isInHideAnimate())
            return;

        if (mConfigSliding.isShowing())
            mConfigSliding.toBaseLevel();
        else
            finish();
    }

    @Override
    public void onClick(View v) {
        if (v == mRefresh) {
            mImageSet.redownload(mGalleryView.getCurIndex());
        } else if (v == mSend) {
            mCurImageFile = mImageSet.getImageFile(mGalleryView.getCurIndex());
            if (mCurImageFile == null || !mCurImageFile.exists())
                MaterialToast.showToast(R.string.current_image_not_downloaded);
            else
                mSendDialog.show();

        } else if (v == mMoreSettings) {
            Intent intent = new Intent(GalleryActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onTapCenter() {
        if (!mConfigSliding.isShowing()) {
            if (mFullScreenHelper.willHideNavBar())
                cancelFullScreenTask();
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
        if (mFullScreenHelper.willHideNavBar())
            startFullScreenTask();
        else
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
