package com.hippo.ehviewer;

import com.hippo.ehviewer.util.Ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import com.hippo.ehviewer.util.Log;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout.LayoutParams;

@SuppressLint("NewApi")
public class BeautifyScreen {
    private static final String TAG = "BeautifyScreen";
    private static Context mContext;
    private static int mStatusBarHeight = 0;
    private static int mNavigationBarHeight = 0;
    
    private static int sbbId = 0;
    private static int nbbId = 1;
    
    private BeautifyScreen(){}
    
    public static void init(Context context) {
        mContext = context;
        if (Build.VERSION.SDK_INT >= 19) {
            Resources resources = context.getResources();
            int sbhResourceId = resources.getIdentifier("status_bar_height",
                    "dimen", "android");
            if (sbhResourceId > 0)
                mStatusBarHeight = resources
                        .getDimensionPixelSize(sbhResourceId);
            int nbhResourceId = resources.getIdentifier(
                    "navigation_bar_height", "dimen", "android");
            if (nbhResourceId > 0)
                mNavigationBarHeight = resources
                        .getDimensionPixelSize(nbhResourceId);
        }
    }

    public static void ColourfyScreen(Activity activity) {
        if (Build.VERSION.SDK_INT >= 19) {
            Resources resources = activity.getResources();
            Window win = activity.getWindow();
            
            WindowManager.LayoutParams winParams = win.getAttributes();
            winParams.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
            win.setAttributes(winParams);

            ViewGroup decorViewGroup = (ViewGroup) win.getDecorView();
            // Add status bar background
            if (mStatusBarHeight > 0) {
                View statusBarView = new View(mContext);
                LayoutParams params1 = new LayoutParams(
                        LayoutParams.MATCH_PARENT, mStatusBarHeight);
                params1.gravity = Gravity.TOP;
                statusBarView.setLayoutParams(params1);
                statusBarView.setBackgroundColor(Ui.HOLO_BLUE_DARK);
                statusBarView.setId(sbbId);
                decorViewGroup.addView(statusBarView);
            }
            
            // Add navigation bar background
            if (Ui.hasNavigationBar() && mNavigationBarHeight > 0 &&
                    isPortrait(activity)) {
                View navigationBarView = new View(mContext);
                LayoutParams params2 = new LayoutParams(
                        LayoutParams.MATCH_PARENT, mNavigationBarHeight);
                params2.gravity = Gravity.BOTTOM;
                navigationBarView.setLayoutParams(params2);
                navigationBarView.setBackgroundColor(Ui.HOLO_BLUE_DARK);
                navigationBarView.setId(nbbId);
                decorViewGroup.addView(navigationBarView);
            }
        }
    }
    
    private static boolean isPortrait(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels > dm.widthPixels;
    }
    
    public static void fixColour(Activity activity) {
        if (Build.VERSION.SDK_INT < 19)
            return;
        Window win = activity.getWindow();
        ViewGroup decorViewGroup = (ViewGroup) win.getDecorView();
        View navigationBarView = decorViewGroup.findViewById(nbbId);
        if (navigationBarView == null)
            return;
        if (!isPortrait(activity))
            navigationBarView.setVisibility(View.GONE);
        else
            navigationBarView.setVisibility(View.VISIBLE);
    }
}
