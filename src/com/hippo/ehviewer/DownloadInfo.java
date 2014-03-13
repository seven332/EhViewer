package com.hippo.ehviewer;

public class DownloadInfo {
    public static final int STOP = 0x0;
    public static final int DOWNLOADING = 0x1;
    public static final int WAITING = 0x2;
    public static final int COMPLETED = 0x3;
    public static final int FAILED = 0x4;
    
    public static final boolean DETAIL_URL = false;
    public static final boolean PAGE_URL = true;
    
    public String gid;
    public String thumb;
    public String title;
    public int status = STOP;
    public boolean type;
    
    public String detailUrlStr;
    
    public int pageSum = 0;
    public int lastStartIndex = 1;
    public String pageUrlStr;
    
    public float totalSize;
    public float downloadSize;
}
