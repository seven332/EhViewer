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
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;

public class Ui {
    //private static final String TAG = "Ui";
    private static final Ui INSTANCE = new Ui();
    
    private static Context mContext;
    private static final BitmapFactory.Options opt = new BitmapFactory.Options();
    
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
    }
    
    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
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
            String rawRateString) {
        if (!mInit) {
            throw new IllegalStateException("Please init Ui first.");
        }
        if (viewGroup == null || rawRateString == null)
            return;
        viewGroup.removeAllViews();
        float num = 0;
        try{
            num = Float.parseFloat(rawRateString);
        } catch (Exception e) {
            for (int i = 0; i < 5; i++) {
                ImageView IvStarEmpty = new ImageView(mContext);
                IvStarEmpty.setImageResource(R.drawable.star_empty);
                viewGroup.addView(IvStarEmpty);
            }
            return;
        }
        
        int leve = (int)((num + 0.4999)/0.5);
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
        Resources resources = mContext.getResources();
        int nbhResourceId = resources.getIdentifier(
                "navigation_bar_height", "dimen", "android");
        if (nbhResourceId > 0)
            return resources.getDimensionPixelSize(nbhResourceId);
        else
            return 0;
    }
}
