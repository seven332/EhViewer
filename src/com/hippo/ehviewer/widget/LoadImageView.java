package com.hippo.ehviewer.widget;

import com.hippo.ehviewer.ImageLoadManager;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

public class LoadImageView extends ImageView {
    @SuppressWarnings("unused")
    private static final String TAG = "LoadImageView";
    
    public static final int NONE = 0x0;
    public static final int LOADING = 0x1;
    public static final int LOADED = 0x2;
    public static final int FAIL = 0x3;
    
    private static final int WAIT_IMAGE_ID = R.drawable.ic_launcher;
    private static final int TOUCH_IMAGE_ID = R.drawable.ic_touch;
    
    private static final int DURATION = 300;
    
    private int mState = NONE;
    
    private String mUrl;
    private String mKey;
    
    public LoadImageView(Context context) {
        super(context);
    }
    public LoadImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public LoadImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setLoadInfo(String url, String key) {
        mUrl = url;
        mKey = key;
    }
    
    public synchronized void setState(int state) {
        mState = state;
        if (mState != FAIL)
            setClickable(false);
    }
    
    public void setOnClickListener(ImageLoadManager ilm) {
        setOnClickListener(new OnClickListener(ilm));
    }
    
    public String getUrl() {
        return mUrl;
    }
    
    public String getKey() {
        return mKey;
    }
    
    public synchronized int getState() {
        return mState;
    }
    
    public void setWaitImage() {
        setImageResource(WAIT_IMAGE_ID);
    }
    
    public void setTouchImage() {
        setImageResource(TOUCH_IMAGE_ID);
    }
    
    /**
     * Load target bmp, set progressive animation
     * @param bmp
     */
    public void setContextImage(Bitmap bmp) {
        Drawable oldDrawable = getDrawable();
        if (oldDrawable == null) { // Just set alpha animation
            Drawable drawable = new BitmapDrawable(getContext().getResources(), bmp);
            setImageDrawable(drawable);
            AlphaAnimation aa = new AlphaAnimation(0.0f,1.0f);
            aa.setDuration(DURATION);
            startAnimation(aa);
        } else { // If exists previous drawable, use TransitionDrawable
            if (oldDrawable instanceof TransitionDrawable)
                oldDrawable = ((TransitionDrawable)oldDrawable).getDrawable(1);
            
            Drawable[] layers = new Drawable[2];
            layers[0] = oldDrawable;
            layers[1] = new BitmapDrawable(getContext().getResources(), bmp);
            TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
            setImageDrawable(transitionDrawable);
            transitionDrawable.startTransition(DURATION);
        }
    }
    
    class OnClickListener implements View.OnClickListener {
        private ImageLoadManager mImageLoadManager;
        public OnClickListener(ImageLoadManager imageLoadManager) {
            mImageLoadManager = imageLoadManager;
        }
        
        @Override
        public void onClick(View v) {
            setImageDrawable(null);
            mImageLoadManager.add(LoadImageView.this, true);
        }
    }
}
