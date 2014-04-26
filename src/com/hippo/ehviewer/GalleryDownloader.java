package com.hippo.ehviewer;

import java.util.ArrayList;
import java.util.List;

import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.DownloadInfo;
import com.hippo.ehviewer.data.GalleryInfo;

public class GalleryDownloader {
    
    private AppContext mAppContext;
    
    private Data mData;
    private List<DownloadInfo> mDownloadList;
    private ArrayList<String> mPageTokes;
    private DownloadInfo curDownloadInfo;
    
    private static DownloadInfo emptyDownloadInfo = new DownloadInfo(null);
    
    public GalleryDownloader(AppContext appContext) {
        mAppContext = appContext;
        mPageTokes = new ArrayList<String>();
        mData = mAppContext.getData();
        mDownloadList = mData.getAllDownloads();
    }
    
    public synchronized void add(GalleryInfo galleryInfo) {
        DownloadInfo downloadInfo = new DownloadInfo(galleryInfo);
        mData.addDownload(downloadInfo);
        downloadInfo.setDownloadState(DownloadInfo.WAITING);
        
        checkDownloadThread();
    }
    
    public synchronized boolean start(int index) {
        DownloadInfo downloadInfo = mDownloadList.get(index);
        if (downloadInfo.getDownloadState() == DownloadInfo.NONE
                && downloadInfo.getState() != DownloadInfo.COMPLETED) {
            downloadInfo.setDownloadState(DownloadInfo.WAITING);
            
            checkDownloadThread();
            return true;
        } else {
            return false;
        }
    }
    
    private synchronized void checkDownloadThread() {
        if (curDownloadInfo == null) {
            curDownloadInfo = emptyDownloadInfo;
            new Thread(new DownloadGallery()).start();
        }
    }
    
    private synchronized DownloadInfo getFirstWaiting() {
        for (DownloadInfo downloadInfo : mDownloadList) {
            if (downloadInfo.getDownloadState() == DownloadInfo.WAITING)
                return downloadInfo;
        }
        return null;
    }
    
    private class DownloadGallery implements Runnable {
        @Override
        public void run() {
            while ((curDownloadInfo = getFirstWaiting()) != null) {
                curDownloadInfo.setDownloadState(DownloadInfo.DOWNLOADING);
                
                // Check need get 
            }
        }
    }
    
    
    
}
