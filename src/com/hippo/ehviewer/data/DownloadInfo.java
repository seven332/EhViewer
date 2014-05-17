package com.hippo.ehviewer.data;

/**
 * 
 * Use byte[] to store download detail,
 * each bit for each page, 0 for uncompleted,
 * 1 for completed.
 * 
 * @author Hippo
 *
 */

// TODO get downloadMode

public class DownloadInfo {
    public static final int UNCOMPLETED = 0x0;
    public static final int FAILED = 0x1;
    public static final int COMPLETED = 0x2;
    
    public static final int NONE = 0x0;
    public static final int WAITING = 0x1;
    public static final int DOWNLOADING = 0x2;
    
    // TODO delete it use EhClient
    public static final int G = 0x0;
    public static final int EX = 0x1;
    public static final int LOFI_460x = 0x2;
    public static final int LOFI_780x = 0x3;
    public static final int LOFI_980x = 0x4;
    
    private GalleryInfo mGalleryInfo;
    private int mState;
    private int mDownloadState;
    private int mPages;
    private byte[] mDetail;
    private int mStartPage;
    private int mMode;
    
    public DownloadInfo(GalleryInfo galleryInfo) {
        mGalleryInfo = galleryInfo;
        mState = UNCOMPLETED;
        mDownloadState = NONE;
        mPages = 0;
        mDetail = null;
        mStartPage = 0;
    }
    
    public DownloadInfo(GalleryInfo galleryInfo, int pages) {
        mGalleryInfo = galleryInfo;
        mState = UNCOMPLETED;
        mDownloadState = NONE;
        mPages = pages;
        mDetail = new byte[(mPages + 7)/8];
        mStartPage = 0;
    }
    
    public DownloadInfo(GalleryInfo galleryInfo, int state, int pages, byte[] detail, int startPage) {
        mGalleryInfo = galleryInfo;
        mState = state;
        mDownloadState = NONE;
        mPages = pages;
        mDetail = detail;
        mStartPage = startPage;
    }
    
    public boolean finishPage(int index) {
        int position; // byte index
        int offset;   // bit offset
        if (mDetail == null)
            return false;
        
        position = index / 8;
        
        if (position >= mDetail.length)
            return false;
        
        offset = index % 8;
        int mask = 0x80 >> offset;
        mDetail[position] |= mask;
        return true;
    }
    
    public GalleryInfo getGalleryInfo() {
        return mGalleryInfo;
    }
    
    public int getMode() {
        return mMode;
    }
    
    public int getState() {
        return mState;
    }
    
    public int getDownloadState() {
        return mDownloadState;
    }
    
    public byte[] getDetail() {
        return mDetail;
    }
    
    public int getStartPage() {
        return mStartPage;
    }
    
    /**
     * If you pass detail in constructor return true,
     * if you have called it setPages return true.
     * @return
     */
    public boolean isInit() {
        return mDetail != null;
    }
    
    /**
     * If you pass detail in constructor return false,
     * if you call it before return false.
     * @param pages
     * @return
     */
    public boolean setPages(int pages) {
        if (mDetail == null)
            return false;
        mPages = pages;
        mDetail = new byte[(pages + 7) / 8];
        return true;
    }
    
    public void setState(int state) {
        mState = state;
    }
    
    public void setDownloadState(int downloadState) {
        mDownloadState = downloadState;
    }
    
    public void setStartPage(int startPage) {
        mStartPage = startPage;
    }
}
