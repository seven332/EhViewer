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

import android.annotation.TargetApi;
import android.app.ActionBar;
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
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable;

public class Ui {
    @SuppressWarnings("unused")
    private static final String TAG = Ui.class.getSimpleName();
    private static Context mContext;
    private static Resources mResources;
    private static final BitmapFactory.Options opt = new BitmapFactory.Options();

    public static float mDensity;
    public static float mScaledDensity;

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

    public static int THEME_COLOR;

    public static Drawable transparentDrawable;

    public static int ACTION_BAR_HEIGHT;

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
        mScaledDensity = mResources.getDisplayMetrics().scaledDensity;

        // init color

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

        THEME_COLOR = mResources.getColor(R.color.theme);

        // init drawable
        transparentDrawable = new ColorDrawable(Color.TRANSPARENT);

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

    public static int sp2pix(float sp) {
        return (int) (sp * mScaledDensity + 0.5f);
    }

    public static float pix2sp(float pix) {
        return pix / mScaledDensity;
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

            if (c.orientation == Configuration.ORIENTATION_PORTRAIT ||
                    activity.getResources().getBoolean(R.bool.is_table))
                w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            else
                w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

            // For KK set a color view at top to make color status bar
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
                w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * Return Configuration.ORIENTATION_PORTRAIT only
     * if orientation != Configuration.ORIENTATION_LANDSCAPE
     *
     * @param orientation
     * @return
     */
    public static int filterOrientation(int orientation) {
        if (orientation != Configuration.ORIENTATION_LANDSCAPE)
            orientation = Configuration.ORIENTATION_PORTRAIT;
        return orientation;
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

    public static void colorStatusBarKK(Activity activity, int color, int height) {
        int darkColor = Theme.getDarkerColor(color);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
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
                statusBarBgView.setBackgroundColor(darkColor);
                statusBarBgView.setLayoutParams(lp);
            } else {
                statusBarBgView = new View(activity);
                statusBarBgView.setId(R.id.translucent_view);
                statusBarBgView.setBackgroundColor(darkColor);
                lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, height);
                lp.gravity = Gravity.TOP;
                decorViewGroup.addView(statusBarBgView, lp);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void colorStatusBarL(Activity activity, int color) {
        int darkColor = Theme.getDarkerColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = activity.getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.setStatusBarColor(Theme.getDarkerColor(darkColor));
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void setMaterialIndicator(ActionBar actionBar, MaterialIndicatorDrawable d) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setIcon(d);
        } else {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(d);
            actionBar.setIcon(null);
            actionBar.setElevation(dp2pix(4.0f));
        }
    }
}
