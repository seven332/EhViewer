package com.hippo.ehviewer.util;

import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.os.Build;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.ImageView;

public class Ui {
    @SuppressWarnings("unused")
    private static final String TAG = "Ui";
    private static Context mContext;
    private static Resources mResources;
    private static final BitmapFactory.Options opt = new BitmapFactory.Options();
    
    public static int HOLO_BLUE_DARK;
    public static int BG_WHITE;
    
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
    
    private static boolean mInit = false;
    
    /**
     * Init Crash
     * 
     * @param context Application context
     */
    public static void init(Context context) {
        if (mInit)
            return;
        mInit = true;
        
        mContext = context;
        
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        
        mResources = mContext.getResources();
        // init color
        HOLO_BLUE_DARK = mResources.getColor(android.R.color.holo_blue_dark);
        BG_WHITE = mResources.getColor(R.color.main_background);
        
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
    }
    
    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
    }
    
    public static String getCategoryText(int category) {
        switch (category) {
        case ListUrls.DOUJINSHI:
            return "DOUJINSHI";
        case ListUrls.MANGA:
            return "MANGA";
        case ListUrls.ARTIST_CG:
            return "ARTIST CG";
        case ListUrls.GAME_CG:
            return "GAME CG";
        case ListUrls.WESTERN:
            return "WESTERN";
        case ListUrls.NON_H:
            return "NON H";
        case ListUrls.IMAGE_SET:
            return "IMAGE SET";
        case ListUrls.COSPLAY:
            return "COSPLAY";
        case ListUrls.ASIAN_PORN:
            return "ASIAN PORN";
        case ListUrls.MISC:
            return "MISC";
        default:
            return "UNKNOWN";
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
     * Set type image for a ImageView
     * 
     * @param imageView the ImageView to set type image
     * @param type the type
     */
    public static void setType(ImageView imageView,
            int type) {
        if (!mInit) {
            throw new IllegalStateException("Please init Ui first.");
        }
        if (mContext == null || imageView == null)
            return;
        switch (type) {
        case ListUrls.DOUJINSHI:
            imageView.setImageResource(R.drawable.doujinshi);
            break;
        case ListUrls.MANGA:
            imageView.setImageResource(R.drawable.manga);
            break;
        case ListUrls.ARTIST_CG:
            imageView.setImageResource(R.drawable.artistcg);
            break;
        case ListUrls.GAME_CG:
            imageView.setImageResource(R.drawable.gamecg);
            break;
        case ListUrls.WESTERN:
            imageView.setImageResource(R.drawable.western);
            break;
        case ListUrls.NON_H:
            imageView.setImageResource(R.drawable.non_h);
            break;
        case ListUrls.IMAGE_SET:
            imageView.setImageResource(R.drawable.imageset);
            break;
        case ListUrls.COSPLAY:
            imageView.setImageResource(R.drawable.cosplay);
            break;
        case ListUrls.ASIAN_PORN:
            imageView.setImageResource(R.drawable.asianporn);
            break;
        case ListUrls.MISC:
            imageView.setImageResource(R.drawable.misc);
            break;
        default:
            imageView.setImageResource(R.drawable.unknown);
            break;
        }
    }
    
    /**
     * Add star to a ViewGroup according to the 
     * 
     * @param context
     * @param viewGroup
     * @param rawRateString
     */
    public static void addStar(ViewGroup viewGroup,
            float rawRate) {
        if (!mInit) {
            throw new IllegalStateException("Please init Ui first.");
        }
        if (viewGroup == null)
            return;
        viewGroup.removeAllViews();
        
        int leve = (int)((rawRate + 0.4999)/0.5);
        int starNum = 5;
        
        for (int i = 0; i < leve/2; i++) {
            ImageView IvStar = new ImageView(mContext);
            IvStar.setImageResource(R.drawable.star);
            viewGroup.addView(IvStar);
            starNum--;
        }
        if (leve % 2 != 0) {
            ImageView IvStarHalf = new ImageView(mContext);
            IvStarHalf.setImageResource(R.drawable.star_half);
            viewGroup.addView(IvStarHalf);
            starNum--;
        }
        for (int i = 0; i < starNum; i++) {
            ImageView IvStarEmpty = new ImageView(mContext);
            IvStarEmpty.setImageResource(R.drawable.star_empty);
            viewGroup.addView(IvStarEmpty);
        }
    }
    
    /**
     * dp conversion to pix
     * 
     * @param dp The value you want to conversion
     * @return value in pix
     */
    public static int dp2pix(int dp) {
        return (int) (mContext.getResources().getDisplayMetrics().density * dp + 0.5f);
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
     * Get height of action bar
     * @return
     */
    public static int getActionBarHeight() {
        // TODO
        return Ui.dp2pix(48);
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
}
