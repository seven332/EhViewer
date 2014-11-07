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

package com.hippo.ehviewer.util;

import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.ListUrls;

public class Ui {
    @SuppressWarnings("unused")
    private static final String TAG = Ui.class.getSimpleName();
    private static Context mContext;
    private static Resources mResources;
    private static final BitmapFactory.Options opt = new BitmapFactory.Options();

    public static float mDensity;

    public static int HOLO_BLUE_DARK;

    public static int DOUJINSHI_BG_COLOR;
    public static int MANGA_BG_COLOR;
    public static int ARTIST_CG_BG_COLOR;
    public static int GAME_CG_BG_COLOR;
    public static int WESTERN_BG_COLOR;
    public static int NON_H_BG_COLOR;
    public static int IMAGE_SET_BG_COLOR;
    public static int COSPLAY_BG_COLOR;
    public static int ASIAN_PORN_BG_COLOR;
    public static int MISC_BG_COLOR;
    public static int UNKNOWN_BG_COLOR;

    public static Drawable transparentDrawable;

    private static String sNavBarOverride;
    public static int STATUS_BAR_HEIGHT;
    public static int ACTION_BAR_HEIGHT_P;
    public static int ACTION_BAR_HEIGHT_L;
    public static int NAV_BAR_HEIGHT;

    public static int ACTION_BAR_HEIGHT;

    static {
        // Android allows a system property to override the presence of the navigation bar.
        // Used by the emulator.
        // See https://github.com/android/platform_frameworks_base/blob/master/policy/src/com/android/internal/policy/impl/PhoneWindowManager.java#L1076
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                Class<?> c = Class.forName("android.os.SystemProperties");
                Method m = c.getDeclaredMethod("get", String.class);
                m.setAccessible(true);
                sNavBarOverride = (String)m.invoke(null, "qemu.hw.mainkeys");
            } catch (Throwable e) {
                sNavBarOverride = null;
            }
        }
    }

    /**
     * Init Crash
     *
     * @param context Application context
     */
    public static void init(Context context) {
        mContext = context;

        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;

        mResources = mContext.getResources();

        mDensity = mResources.getDisplayMetrics().density;

        // init color
        HOLO_BLUE_DARK = mResources.getColor(android.R.color.holo_blue_dark);

        DOUJINSHI_BG_COLOR = mResources.getColor(R.color.doujinshi_bg);
        MANGA_BG_COLOR = mResources.getColor(R.color.manga_bg);
        ARTIST_CG_BG_COLOR = mResources.getColor(R.color.artist_cg_bg);
        GAME_CG_BG_COLOR = mResources.getColor(R.color.game_cg_bg);
        WESTERN_BG_COLOR = mResources.getColor(R.color.western_bg);
        NON_H_BG_COLOR = mResources.getColor(R.color.non_h_bg);
        IMAGE_SET_BG_COLOR = mResources.getColor(R.color.image_set_bg);
        COSPLAY_BG_COLOR = mResources.getColor(R.color.cosplay_bg);
        ASIAN_PORN_BG_COLOR = mResources.getColor(R.color.asian_porn_bg);
        MISC_BG_COLOR = mResources.getColor(R.color.misc_bg);
        UNKNOWN_BG_COLOR = mResources.getColor(R.color.unknown_bg);

        // init drawable
        transparentDrawable = new ColorDrawable(Color.TRANSPARENT);

        // Get height info
        STATUS_BAR_HEIGHT = getInternalDimensionSize(mResources, "status_bar_height");
        ACTION_BAR_HEIGHT_P = Ui.dp2pix(56);
        ACTION_BAR_HEIGHT_L = Ui.dp2pix(56);
        if (hasNavBar(mContext))
            NAV_BAR_HEIGHT = getInternalDimensionSize(mResources, "navigation_bar_height");
        else
            NAV_BAR_HEIGHT = 0;

        ACTION_BAR_HEIGHT = mResources.getDimensionPixelSize(R.dimen.action_bar_height);
    }

    public static int getInternalDimensionSize(Resources res, String key) {
        int result = 0;
        int resourceId = res.getIdentifier(key, "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getThemeDimensionPixelSize(android.content.res.Resources.Theme theme, int attrId) {
        TypedValue tv = new TypedValue();
        theme.resolveAttribute(attrId, tv, true);
        return TypedValue.complexToDimensionPixelSize(tv.data, mContext.getResources().getDisplayMetrics());
    }

    private static boolean hasNavBar(Context context) {
        Resources res = context.getResources();
        int resourceId = res.getIdentifier("config_showNavigationBar", "bool", "android");
        if (resourceId != 0) {
            boolean hasNav = res.getBoolean(resourceId);
            // check override flag (see static block)
            if ("1".equals(sNavBarOverride)) {
                hasNav = false;
            } else if ("0".equals(sNavBarOverride)) {
                hasNav = true;
            }
            return hasNav;
        } else { // fallback
            return !ViewConfiguration.get(context).hasPermanentMenuKey();
        }
    }

    public static String getCategoryText(int category) {
        switch (category) {
        case ListUrls.DOUJINSHI:
            return mResources.getString(R.string.doujinshi);
        case ListUrls.MANGA:
            return mResources.getString(R.string.manga);
        case ListUrls.ARTIST_CG:
            return mResources.getString(R.string.artist_cg);
        case ListUrls.GAME_CG:
            return mResources.getString(R.string.game_cg);
        case ListUrls.WESTERN:
            return mResources.getString(R.string.western);
        case ListUrls.NON_H:
            return mResources.getString(R.string.non_h);
        case ListUrls.IMAGE_SET:
            return mResources.getString(R.string.image_set);
        case ListUrls.COSPLAY:
            return mResources.getString(R.string.cosplay);
        case ListUrls.ASIAN_PORN:
            return mResources.getString(R.string.asian_porn);
        case ListUrls.MISC:
            return mResources.getString(R.string.misc);
        default:
            return mResources.getString(R.string.unknown);
        }
    }

    public static int getCategoryColor(int category) {
        switch (category) {
        case ListUrls.DOUJINSHI:
            return DOUJINSHI_BG_COLOR;
        case ListUrls.MANGA:
            return MANGA_BG_COLOR;
        case ListUrls.ARTIST_CG:
            return ARTIST_CG_BG_COLOR;
        case ListUrls.GAME_CG:
            return GAME_CG_BG_COLOR;
        case ListUrls.WESTERN:
            return WESTERN_BG_COLOR;
        case ListUrls.NON_H:
            return NON_H_BG_COLOR;
        case ListUrls.IMAGE_SET:
            return IMAGE_SET_BG_COLOR;
        case ListUrls.COSPLAY:
            return COSPLAY_BG_COLOR;
        case ListUrls.ASIAN_PORN:
            return ASIAN_PORN_BG_COLOR;
        case ListUrls.MISC:
            return MISC_BG_COLOR;
        default:
            return UNKNOWN_BG_COLOR;
        }
    }

    /**
     * dp conversion to pix
     *
     * @param dp The value you want to conversion
     * @return value in pix
     */
    public static int dp2pix(float dp) {
        return (int) (mDensity * dp + 0.5f);
    }

    public static float pix2dp(int pix) {
        return pix/mDensity;
    }

    /**
     * Get default BitmapFactory.Options
     *
     * @return
     */
    public static BitmapFactory.Options getBitmapOpt() {
        return opt;
    }

    /**
     * Check has navigation bar or not
     * @return
     */
    public static boolean hasNavigationBar() {
        boolean hasNavigationBar = false;
        boolean hasMenuKey = ViewConfiguration.get(mContext).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        if(!hasMenuKey && !hasBackKey)
            hasNavigationBar = true;
        return hasNavigationBar;
    }

    /**
     * Get height of status bar
     * @return
     */
    public static int getStatusBarHeight() {
        Resources resources = mContext.getResources();
        int sbhResourceId = resources.getIdentifier("status_bar_height",
                "dimen", "android");
        if (sbhResourceId > 0)
            return resources.getDimensionPixelSize(sbhResourceId);
        else return 0;
    }

    /**
     * Get height of navigation bar
     * @param force If false, alway return 0 if the device has no navigation bar
     * @return
     */
    public static int getNavigationBarHeight(boolean force) {
        if (!force && !hasNavigationBar())
            return 0;
        int nbhResourceId = mResources.getIdentifier(
                "navigation_bar_height", "dimen", "android");
        if (nbhResourceId > 0)
            return mResources.getDimensionPixelSize(nbhResourceId);
        else
            return 0;
    }

    public static void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
                    MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    /**
     * Update translucent state for API >= 19, KK or more powerful
     *
     * @param activity
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void updateTranslucent(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Configuration c = activity.getResources().getConfiguration();
            Window w = activity.getWindow();

            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (c.orientation == Configuration.ORIENTATION_PORTRAIT)
                w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            else if (c.orientation == Configuration.ORIENTATION_LANDSCAPE)
                w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * Set config Orientation
     *
     * @param activity
     */
    public static void adjustOrientation(Activity activity) {
        int screenOri = Config.getScreenOriMode();
        if (screenOri != activity.getRequestedOrientation())
            activity.setRequestedOrientation(screenOri);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void translucent(Activity activity, int color, int height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View statusBarBgView;
            FrameLayout.LayoutParams lp;
            ViewGroup decorViewGroup = (ViewGroup) activity.getWindow().getDecorView();

            // Try to find old translucent view
            if (decorViewGroup.getChildCount() > 1 &&
                    (statusBarBgView = decorViewGroup.getChildAt(1)) != null &&
                    statusBarBgView.getId() == R.id.translucent_view) {
                lp = (LayoutParams) statusBarBgView.getLayoutParams();
                lp.height = height;
                lp.gravity = Gravity.TOP;
                statusBarBgView.setBackgroundColor(color);
                statusBarBgView.setLayoutParams(lp);

                Log.d(TAG, "translucent new");
            } else {
                statusBarBgView = new View(activity);
                statusBarBgView.setId(R.id.translucent_view);
                statusBarBgView.setBackgroundColor(color);
                lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, height);
                lp.gravity = Gravity.TOP;
                decorViewGroup.addView(statusBarBgView, lp);


                Log.d(TAG, "translucent update");
            }
        }
    }

    /**
     * Get paddingTop and paddingBottom
     *
     * @param resources
     * @param padding
     */
    public static void getWindowPadding(Resources resources, int[] padding) {
        if (resources.getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            padding[0] = ACTION_BAR_HEIGHT_P;
            padding[1] = NAV_BAR_HEIGHT;
        } else {
            padding[0] = ACTION_BAR_HEIGHT_L;
            padding[1] = 0;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            padding[0] += STATUS_BAR_HEIGHT;
    }
}
