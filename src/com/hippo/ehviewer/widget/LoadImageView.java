package com.hippo.ehviewer.widget;

import com.hippo.ehviewer.ImageLoadManager;
import com.hippo.ehviewer.util.Ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class LoadImageView extends ImageView {
    @SuppressWarnings("unused")
    private static final String TAG = "LoadImageView";
    
    public static final int NONE = 0x0;
    public static final int LOADING = 0x1;
    public static final int LOADED = 0x2;
    public static final int FAIL = 0x3;
    
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
    
    class OnClickListener implements View.OnClickListener {
        private ImageLoadManager mImageLoadManager;
        public OnClickListener(ImageLoadManager imageLoadManager) {
            mImageLoadManager = imageLoadManager;
        }
        
        @Override
        public void onClick(View v) {
            LoadImageView.this.setImageBitmap(Ui.BITMAP_LAUNCH);
            mImageLoadManager.add(LoadImageView.this, true);
        }
    }
}
