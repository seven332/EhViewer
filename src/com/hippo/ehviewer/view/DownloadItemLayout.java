package com.hippo.ehviewer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class DownloadItemLayout extends LinearLayout {
    
    public String gid;
    public int status;
    public boolean type;
    public int lastStartIndex;
    public float downloadSize;
    public boolean isWait;
    
    public DownloadItemLayout(Context context) {
        super(context);
    }
    
    public DownloadItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public DownloadItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
