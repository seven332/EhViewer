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

package com.hippo.ehviewer.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.DirGalleryProvider;
import com.hippo.ehviewer.gallery.EhGalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProviderListener;
import com.hippo.ehviewer.gallery.ZipGalleryProvider;
import com.hippo.ehviewer.gallery.gl.GalleryPageView;
import com.hippo.ehviewer.gallery.gl.GalleryView;
import com.hippo.ehviewer.gallery.gl.ImageView;
import com.hippo.ehviewer.widget.GalleryGuideView;
import com.hippo.ehviewer.widget.ReversibleSeekBar;
import com.hippo.gl.glrenderer.ImageTexture;
import com.hippo.gl.view.GLRootView;
import com.hippo.image.Image;
import com.hippo.unifile.UniFile;
import com.hippo.util.SystemUiHelper;
import com.hippo.widget.ColorView;
import com.hippo.yorozuya.AnimationUtils;
import com.hippo.yorozuya.ConcurrentPool;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.SimpleAnimatorListener;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.ViewUtils;

import java.io.File;

public class GalleryActivity extends EhActivity
        implements GalleryProviderListener, SeekBar.OnSeekBarChangeListener,
        GalleryView.Listener {

    public static final String ACTION_DIR = "dir";
    public static final String ACTION_ZIP = "zip";
    public static final String ACTION_EH = "eh";

    public static final String KEY_ACTION = "action";
    public static final String KEY_FILENAME = "filename";
    public static final String KEY_GALLERY_INFO = "gallery_info";
    public static final String KEY_PAGE = "page";
    public static final String KEY_CURRENT_INDEX = "current_index";

    private static final long SLIDER_ANIMATION_DURING = 150;
    private static final long HIDE_SLIDER_DELAY = 3000;

    private String mAction;
    private String mFilename;
    private GalleryInfo mGalleryInfo;
    private int mPage;

    @Nullable
    private GalleryView mGalleryView;

    @Nullable
    private ImageTexture.Uploader mUploader;
    @Nullable
    private GalleryProvider mGalleryProvider;

    @Nullable
    private SystemUiHelper mSystemUiHelper;
    private boolean mShowSystemUi;

    @Nullable
    private ColorView mMaskView;
    @Nullable
    private View mClock;
    @Nullable
    private View mBattery;
    @Nullable
    private View mSeekBarPanel;
    @Nullable
    private TextView mLeftText;
    @Nullable
    private TextView mRightText;
    @Nullable
    private ReversibleSeekBar mSeekBar;

    private ObjectAnimator mSeekBarPanelAnimator;

    private int mLayoutMode;
    private int mSize;
    private int mCurrentIndex;

    private final ConcurrentPool<NotifyTask> mNotifyTaskPool = new ConcurrentPool<>(3);

    private final ValueAnimator.AnimatorUpdateListener mUpdateSliderListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (null != mSeekBarPanel) {
                mSeekBarPanel.requestLayout();
            }
        }
    };

    private final SimpleAnimatorListener mShowSliderListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mSeekBarPanelAnimator = null;
        }
    };

    private final SimpleAnimatorListener mHideSliderListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mSeekBarPanelAnimator = null;
            if (mSeekBarPanel != null) {
                mSeekBarPanel.setVisibility(View.INVISIBLE);
            }
        }
    };

    private final Runnable mHideSliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSeekBarPanel != null) {
                hideSlider(mSeekBarPanel);
            }
        }
    };

    private void buildProvider() {
        if (mGalleryProvider != null) {
            return;
        }

        if (ACTION_DIR.equals(mAction)) {
            if (mFilename != null) {
                mGalleryProvider = new DirGalleryProvider(UniFile.fromFile(new File(mFilename)));
            }
        } else if (ACTION_ZIP.equals(mAction)) {
            if (mFilename != null) {
                mGalleryProvider = new ZipGalleryProvider(new File(mFilename));
            }
        } else if (ACTION_EH.equals(mAction)) {
            if (mGalleryInfo != null) {
                mGalleryProvider = new EhGalleryProvider(this, mGalleryInfo);
            }
        }
    }

    private void onInit() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        mAction = intent.getAction();
        mFilename = intent.getStringExtra(KEY_FILENAME);
        mGalleryInfo = intent.getParcelableExtra(KEY_GALLERY_INFO);
        mPage = intent.getIntExtra(KEY_PAGE, -1);
        buildProvider();
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mAction = savedInstanceState.getString(KEY_ACTION);
        mFilename = savedInstanceState.getString(KEY_FILENAME);
        mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO);
        mPage = savedInstanceState.getInt(KEY_PAGE, -1);
        mCurrentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX);
        buildProvider();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTION, mAction);
        outState.putString(KEY_FILENAME, mFilename);
        if (mGalleryInfo != null) {
            outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo);
        }
        outState.putInt(KEY_PAGE, mPage);
        outState.putInt(KEY_CURRENT_INDEX, mCurrentIndex);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Settings.getReadingFullscreen() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }

        if (mGalleryProvider == null) {
            finish();
            return;
        }
        mGalleryProvider.setGalleryProviderListener(this);
        mGalleryProvider.start();

        setContentView(R.layout.activity_gallery);
        GLRootView glRootView = (GLRootView) ViewUtils.$$(this, R.id.gl_root_view);
        mGalleryProvider.setGLRoot(glRootView);
        mUploader = new ImageTexture.Uploader(glRootView);

        int startPage;
        if (savedInstanceState == null) {
            startPage = mPage >= 0 ? mPage : mGalleryProvider.getStartPage();
        } else {
            startPage = mCurrentIndex;
        }

        mGalleryView = new GalleryView(this, new GalleryAdapter(), this,
                Settings.getReadingDirection(), Settings.getPageScaling(),
                Settings.getStartPosition(), startPage);
        glRootView.setContentPane(mGalleryView);

        // System UI helper
        if (Settings.getReadingFullscreen()) {
            int systemUiLevel;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                systemUiLevel = SystemUiHelper.LEVEL_IMMERSIVE;
            } else {
                systemUiLevel = SystemUiHelper.LEVEL_HIDE_STATUS_BAR;
            }
            mSystemUiHelper = new SystemUiHelper(this, systemUiLevel,
                    SystemUiHelper.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES | SystemUiHelper.FLAG_IMMERSIVE_STICKY);
            mSystemUiHelper.hide();
            mShowSystemUi = false;
        }

        mMaskView = (ColorView) ViewUtils.$$(this, R.id.mask);
        mClock = ViewUtils.$$(this, R.id.clock);
        mBattery = ViewUtils.$$(this, R.id.battery);
        mClock.setVisibility(Settings.getShowClock() ? View.VISIBLE : View.GONE);
        mBattery.setVisibility(Settings.getShowBattery() ? View.VISIBLE : View.GONE);

        mSeekBarPanel = ViewUtils.$$(this, R.id.seek_bar_panel);
        mLeftText = (TextView) ViewUtils.$$(mSeekBarPanel, R.id.left);
        mRightText = (TextView) ViewUtils.$$(mSeekBarPanel, R.id.right);
        mSeekBar = (ReversibleSeekBar) ViewUtils.$$(mSeekBarPanel, R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);

        mSize = mGalleryProvider.size();
        mCurrentIndex = startPage;
        mLayoutMode = mGalleryView.getLayoutMode();
        updateSlider();

        // Update keep screen on
        if (Settings.getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Orientation
        int orientation;
        switch (Settings.getScreenRotation()) {
            default:
            case 0:
                orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                break;
            case 1:
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                break;
            case 2:
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                break;
        }
        setRequestedOrientation(orientation);

        // Screen lightness
        setScreenLightness(Settings.getCustomScreenLightness(), Settings.getScreenLightness());

        if (Settings.getGuideGallery()) {
            FrameLayout mainLayout = (FrameLayout) ViewUtils.$$(this, R.id.main);
            mainLayout.addView(new GalleryGuideView(this));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGalleryView = null;
        if (mUploader != null) {
            mUploader.clear();
            mUploader = null;
        }
        if (mGalleryProvider != null) {
            mGalleryProvider.setGalleryProviderListener(null);
            mGalleryProvider.stop();
            mGalleryProvider = null;
        }

        mMaskView = null;
        mClock = null;
        mBattery = null;
        mSeekBarPanel = null;
        mLeftText = null;
        mRightText = null;
        mSeekBar = null;

        SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && mSystemUiHelper != null) {
            if (mShowSystemUi) {
                mSystemUiHelper.show();
            } else {
                mSystemUiHelper.hide();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mGalleryView == null) {
            return super.onKeyDown(keyCode, event);
        }

        // Check volume
        if (Settings.getVolumePage()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
                    mGalleryView.pageRight();
                } else {
                    mGalleryView.pageLeft();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
                    mGalleryView.pageLeft();
                } else {
                    mGalleryView.pageRight();
                }
                return true;
            }
        }

        // Check keyboard and Dpad
        switch (keyCode) {
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
                    mGalleryView.pageRight();
                } else {
                    mGalleryView.pageLeft();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mGalleryView.pageLeft();
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
                    mGalleryView.pageLeft();
                } else {
                    mGalleryView.pageRight();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mGalleryView.pageRight();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MENU:
                onTapMenuArea();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Check volume
        if (Settings.getVolumePage()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                    keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true;
            }
        }

        // Check keyboard and Dpad
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_SPACE ||
                keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private GalleryPageView findPageByIndex(int index) {
        if (mGalleryView != null) {
            return mGalleryView.findPageByIndex(index);
        } else {
            return null;
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateSlider() {
        if (mSeekBar == null || mRightText == null || mLeftText == null || mSize <= 0 || mCurrentIndex < 0) {
            return;
        }

        TextView start;
        TextView end;
        if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
            start = mRightText;
            end = mLeftText;
            mSeekBar.setReverse(true);
        } else {
            start = mLeftText;
            end = mRightText;
            mSeekBar.setReverse(false);
        }
        start.setText(Integer.toString(mCurrentIndex + 1));
        end.setText(Integer.toString(mSize));
        mSeekBar.setMax(mSize - 1);
        mSeekBar.setProgress(mCurrentIndex);
    }

    @Override
    public void onDataChanged() {
        if (mGalleryView != null) {
            mGalleryView.onDataChanged();
        }

        if (mGalleryProvider != null) {
            int size = mGalleryProvider.size();
            NotifyTask task = mNotifyTaskPool.pop();
            if (task == null) {
                task = new NotifyTask();
            }
            task.setData(NotifyTask.KEY_SIZE, size);
            SimpleHandler.getInstance().post(task);
        }
    }

    @Override
    public void onPageWait(int index) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null) {
            page.showInfo();
            page.setImage(null);
            page.setPage(index + 1);
            page.setProgress(GalleryPageView.PROGRESS_INDETERMINATE);
            page.setError(null, null);
        }
    }

    @Override
    public void onPagePercent(int index, float percent) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null) {
            page.showInfo();
            page.setImage(null);
            page.setPage(index + 1);
            page.setProgress(percent);
            page.setError(null, null);
        }
    }

    @Override
    public void onPageSucceed(int index, Image image) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null) {
            ImageTexture imageTexture = new ImageTexture(image);
            if (mUploader != null) {
                mUploader.addTexture(imageTexture);
            }
            page.showImage();
            page.setImage(imageTexture);
            page.setPage(index + 1);
            page.setProgress(GalleryPageView.PROGRESS_GONE);
            page.setError(null, null);
        } else {
            image.recycle();
        }
    }

    @Override
    public void onPageFailed(int index, String error) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null && mGalleryView != null) {
            page.showInfo();
            page.setImage(null);
            page.setPage(index + 1);
            page.setProgress(GalleryPageView.PROGRESS_GONE);
            page.setError(error, mGalleryView);
        }
    }

    @Override
    public void onDataChanged(int index) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null && mGalleryProvider != null) {
            mGalleryProvider.request(index);
        }
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView start;
        if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
            start = mRightText;
        } else {
            start = mLeftText;
        }
        if (fromUser && null != start) {
            start.setText(Integer.toString(progress + 1));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        SimpleHandler.getInstance().postDelayed(mHideSliderRunnable, HIDE_SLIDER_DELAY);
        int progress = seekBar.getProgress();
        if (progress != mCurrentIndex && null != mGalleryView) {
            mGalleryView.setCurrentPage(progress);
        }
    }

    @Override
    public void onUpdateCurrentIndex(int index) {
        if (null != mGalleryProvider) {
            mGalleryProvider.putStartPage(index);
        }

        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_CURRENT_INDEX, index);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapSliderArea() {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_SLIDER_AREA, 0);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapMenuArea() {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_MENU_AREA, 0);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onLongPressPage(int index) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_LONG_PRESS_PAGE, index);
        SimpleHandler.getInstance().post(task);
    }

    private void showSlider(View sliderPanel) {
        if (null != mSeekBarPanelAnimator) {
            mSeekBarPanelAnimator.cancel();
            mSeekBarPanelAnimator = null;
        }

        sliderPanel.setTranslationY(sliderPanel.getHeight());
        sliderPanel.setVisibility(View.VISIBLE);

        mSeekBarPanelAnimator = ObjectAnimator.ofFloat(sliderPanel, "translationY", 0.0f);
        mSeekBarPanelAnimator.setDuration(SLIDER_ANIMATION_DURING);
        mSeekBarPanelAnimator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
        mSeekBarPanelAnimator.addUpdateListener(mUpdateSliderListener);
        mSeekBarPanelAnimator.addListener(mShowSliderListener);
        mSeekBarPanelAnimator.start();

        if (null != mSystemUiHelper) {
            mSystemUiHelper.show();
            mShowSystemUi = true;
        }
    }

    private void hideSlider(View sliderPanel) {
        if (null != mSeekBarPanelAnimator) {
            mSeekBarPanelAnimator.cancel();
            mSeekBarPanelAnimator = null;
        }

        mSeekBarPanelAnimator = ObjectAnimator.ofFloat(sliderPanel, "translationY", sliderPanel.getHeight());
        mSeekBarPanelAnimator.setDuration(SLIDER_ANIMATION_DURING);
        mSeekBarPanelAnimator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
        mSeekBarPanelAnimator.addUpdateListener(mUpdateSliderListener);
        mSeekBarPanelAnimator.addListener(mHideSliderListener);
        mSeekBarPanelAnimator.start();

        if (null != mSystemUiHelper) {
            mSystemUiHelper.hide();
            mShowSystemUi = false;
        }
    }

    /**
     * @param lightness 0 - 200
     */
    private void setScreenLightness(boolean enable, int lightness) {
        if (null == mMaskView) {
            return;
        }

        Window w = getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        if (enable) {
            lightness = MathUtils.clamp(lightness, 0, 200);
            if (lightness > 100) {
                mMaskView.setColor(0);
                // Avoid BRIGHTNESS_OVERRIDE_OFF,
                // screen may be off when lp.screenBrightness is 0.0f
                lp.screenBrightness = Math.max((lightness - 100) / 100.0f, 0.01f);
            } else {
                mMaskView.setColor(MathUtils.lerp(0xde, 0x00, lightness / 100.0f) << 24);
                lp.screenBrightness = 0.01f;
            }
        } else {
            mMaskView.setColor(0);
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        }
        w.setAttributes(lp);
    }

    private class GalleryMenuHelper implements DialogInterface.OnClickListener {

        private final View mView;
        private final Spinner mScreenRotation;
        private final Spinner mReadingDirection;
        private final Spinner mScaleMode;
        private final Spinner mStartPosition;
        private final SwitchCompat mKeepScreenOn;
        private final SwitchCompat mShowClock;
        private final SwitchCompat mShowBattery;
        private final SwitchCompat mVolumePage;
        private final SwitchCompat mReadingFullscreen;
        private final SwitchCompat mCustomScreenLightness;
        private final SeekBar mScreenLightness;

        @SuppressLint("InflateParams")
        public GalleryMenuHelper(Context context) {
            mView = LayoutInflater.from(context).inflate(R.layout.dialog_gallery_menu, null);
            mScreenRotation = (Spinner) mView.findViewById(R.id.screen_rotation);
            mReadingDirection = (Spinner) mView.findViewById(R.id.reading_direction);
            mScaleMode = (Spinner) mView.findViewById(R.id.page_scaling);
            mStartPosition = (Spinner) mView.findViewById(R.id.start_position);
            mKeepScreenOn = (SwitchCompat) mView.findViewById(R.id.keep_screen_on);
            mShowClock = (SwitchCompat) mView.findViewById(R.id.show_clock);
            mShowBattery = (SwitchCompat) mView.findViewById(R.id.show_battery);
            mVolumePage = (SwitchCompat) mView.findViewById(R.id.volume_page);
            mReadingFullscreen = (SwitchCompat) mView.findViewById(R.id.reading_fullscreen);
            mCustomScreenLightness = (SwitchCompat) mView.findViewById(R.id.custom_screen_lightness);
            mScreenLightness = (SeekBar) mView.findViewById(R.id.screen_lightness);

            mScreenRotation.setSelection(Settings.getScreenRotation());
            mReadingDirection.setSelection(Settings.getReadingDirection());
            mScaleMode.setSelection(Settings.getPageScaling());
            mStartPosition.setSelection(Settings.getStartPosition());
            mKeepScreenOn.setChecked(Settings.getKeepScreenOn());
            mShowClock.setChecked(Settings.getShowClock());
            mShowBattery.setChecked(Settings.getShowBattery());
            mVolumePage.setChecked(Settings.getVolumePage());
            mReadingFullscreen.setChecked(Settings.getReadingFullscreen());
            mCustomScreenLightness.setChecked(Settings.getCustomScreenLightness());
            mScreenLightness.setProgress(Settings.getScreenLightness());
            mScreenLightness.setEnabled(Settings.getCustomScreenLightness());

            mCustomScreenLightness.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mScreenLightness.setEnabled(isChecked);
                }
            });
        }

        public View getView() {
            return mView;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int screenRotation = mScreenRotation.getSelectedItemPosition();
            int layoutMode = GalleryView.sanitizeLayoutMode(mReadingDirection.getSelectedItemPosition());
            int scaleMode = ImageView.sanitizeScaleMode(mScaleMode.getSelectedItemPosition());
            int startPosition = ImageView.sanitizeStartPosition(mStartPosition.getSelectedItemPosition());
            boolean keepScreenOn = mKeepScreenOn.isChecked();
            boolean showClock = mShowClock.isChecked();
            boolean showBattery = mShowBattery.isChecked();
            boolean volumePage = mVolumePage.isChecked();
            boolean readingFullscreen = mReadingFullscreen.isChecked();
            boolean customScreenLightness = mCustomScreenLightness.isChecked();
            int screenLightness = mScreenLightness.getProgress();

            boolean oldReadingFullscreen = Settings.getReadingFullscreen();

            Settings.putScreenRotation(screenRotation);
            Settings.putReadingDirection(layoutMode);
            Settings.putPageScaling(scaleMode);
            Settings.putStartPosition(startPosition);
            Settings.putKeepScreenOn(keepScreenOn);
            Settings.putShowClock(showClock);
            Settings.putShowBattery(showBattery);
            Settings.putVolumePage(volumePage);
            Settings.putReadingFullscreen(readingFullscreen);
            Settings.putCustomScreenLightness(customScreenLightness);
            Settings.putScreenLightness(screenLightness);

            int orientation;
            switch (screenRotation) {
                default:
                case 0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                    break;
                case 1:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                    break;
                case 2:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                    break;
            }
            setRequestedOrientation(orientation);
            if (mGalleryView != null) {
                mGalleryView.setLayoutMode(layoutMode);
                mGalleryView.setScaleMode(scaleMode);
                mGalleryView.setStartPosition(startPosition);
            }
            if (keepScreenOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            if (mClock != null) {
                mClock.setVisibility(showClock ? View.VISIBLE : View.GONE);
            }
            if (mBattery != null) {
                mBattery.setVisibility(showBattery ? View.VISIBLE : View.GONE);
            }
            setScreenLightness(customScreenLightness, screenLightness);

            // Update slider
            mLayoutMode = layoutMode;
            updateSlider();

            if (oldReadingFullscreen != readingFullscreen) {
                recreate();
            }
        }
    }

    private void shareImage(int page) {
        if (null == mGalleryProvider) {
            return;
        }

        File dir = AppConfig.getExternalTempDir();
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show();
            return;
        }
        UniFile file;
        if (null == (file = mGalleryProvider.save(page, UniFile.fromFile(dir), Long.toString(System.currentTimeMillis())))) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO Create ContentProvider for some app are not grand storage write permission
        Uri uri = file.getUri();
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "image/jpeg";
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType(mimeType);
        startActivity(Intent.createChooser(intent, getString(R.string.share_image)));
    }

    private void saveImage(int page) {
        if (null == mGalleryProvider) {
            return;
        }

        File dir = AppConfig.getExternalImageDir();
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        UniFile file;
        if (null == (file = mGalleryProvider.save(page, UniFile.fromFile(dir), mGalleryProvider.getImageFilename(page)))) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, getString(R.string.image_saved, file.getUri()), Toast.LENGTH_SHORT).show();
    }

    private void showPageDialog(final int page) {
        Resources resources = GalleryActivity.this.getResources();
        new AlertDialog.Builder(GalleryActivity.this)
                .setTitle(resources.getString(R.string.page_menu_title, page + 1))
                .setItems(R.array.page_menu_entries, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mGalleryProvider == null) {
                            return;
                        }

                        switch (which) {
                            case 0: // Refresh
                                mGalleryProvider.forceRequest(page);
                                break;
                            case 1: // Share
                                shareImage(page);
                                break;
                            case 2: // Save
                                saveImage(page);
                                break;
                            case 3: // Add a bookmark
                                break;
                        }
                    }
                }).show();
    }

    private class NotifyTask implements Runnable {

        public static final int KEY_LAYOUT_MODE = 0;
        public static final int KEY_SIZE = 1;
        public static final int KEY_CURRENT_INDEX = 2;
        public static final int KEY_TAP_SLIDER_AREA = 3;
        public static final int KEY_TAP_MENU_AREA = 4;
        public static final int KEY_LONG_PRESS_PAGE = 5;

        private int mKey;
        private int mValue;

        public void setData(int key, int value) {
            mKey = key;
            mValue = value;
        }

        private void onTapMenuArea() {
            AlertDialog.Builder builder = new AlertDialog.Builder(GalleryActivity.this);
            GalleryMenuHelper helper = new GalleryMenuHelper(builder.getContext());
            builder.setTitle(R.string.gallery_menu_title)
                    .setView(helper.getView())
                    .setPositiveButton(android.R.string.ok, helper).show();
        }

        private void onTapSliderArea() {
            if (mSeekBarPanel == null || mSize <= 0 || mCurrentIndex < 0) {
                return;
            }

            SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);

            if (mSeekBarPanel.getVisibility() == View.VISIBLE) {
                hideSlider(mSeekBarPanel);
            } else {
                showSlider(mSeekBarPanel);
                SimpleHandler.getInstance().postDelayed(mHideSliderRunnable, HIDE_SLIDER_DELAY);
            }
        }

        private void onLongPressPage(final int index) {
            showPageDialog(index);
        }

        @Override
        public void run() {
            switch (mKey) {
                case KEY_LAYOUT_MODE:
                    GalleryActivity.this.mLayoutMode = mValue;
                    updateSlider();
                    break;
                case KEY_SIZE:
                    GalleryActivity.this.mSize = mValue;
                    updateSlider();
                    break;
                case KEY_CURRENT_INDEX:
                    GalleryActivity.this.mCurrentIndex = mValue;
                    updateSlider();
                    break;
                case KEY_TAP_MENU_AREA:
                    onTapMenuArea();
                    break;
                case KEY_TAP_SLIDER_AREA:
                    onTapSliderArea();
                    break;
                case KEY_LONG_PRESS_PAGE:
                    onLongPressPage(mValue);
                    break;
            }
            mNotifyTaskPool.push(this);
        }
    }

    private class GalleryAdapter extends GalleryView.Adapter {

        @Override
        public String getError() {
            if (mGalleryProvider == null) {
                return getString(R.string.error_no_provider);
            } else if (mGalleryProvider.size() <= GalleryProvider.STATE_ERROR) {
                return mGalleryProvider.getError();
            }
            return null;
        }

        @Override
        public int size() {
            if (mGalleryProvider == null) {
                return GalleryProvider.STATE_ERROR;
            } else {
                return mGalleryProvider.size();
            }
        }

        @Override
        public void onBind(GalleryPageView view, int index) {
            if (mGalleryProvider != null && mGalleryView != null) {
                mGalleryProvider.request(index);
                view.showInfo();
                view.setImage(null);
                view.setPage(index + 1);
                view.setProgress(GalleryPageView.PROGRESS_INDETERMINATE);
                view.setError(null, null);
            }
        }

        @Override
        public void onUnbind(GalleryPageView view, int index) {
            if (mGalleryProvider != null) {
                mGalleryProvider.cancelRequest(index);
            }
            view.setImage(null);
            view.setError(null, null);
        }
    }
}
