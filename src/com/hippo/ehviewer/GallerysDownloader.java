package com.hippo.ehviewer;

import java.util.ArrayList;
import java.util.List;

import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.DownloadInfo;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.network.HttpHelper;

public class GallerysDownloader {
    
    private AppContext mAppContext;
    
    private Data mData;
    private List<DownloadInfo> mDownloadList;
    private String[] mPageTokes;
    private DownloadInfo curDownloadInfo;
    private OnGalleryDownloadListener mListener;
    
    private static DownloadInfo emptyDownloadInfo = new DownloadInfo(null);
    
    public GallerysDownloader(AppContext appContext) {
        mAppContext = appContext;
        mPageTokes = null;
        mData = mAppContext.getData();
        mDownloadList = mData.getAllDownloads();
    }
    
    public void setOnGalleryDownloadListener(OnGalleryDownloadListener l) {
        mListener = l;
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
    
    private String getUrlContext(HttpHelper hp, String url) {
        int times = 3;
        String pageContent;
        for (int i = 0; i < times; i++) {
            pageContent = hp.get(url);
            if (pageContent != null)
                return pageContent;
        }
        return null;
    }
    
    private class DownloadGallery implements Runnable {
        @Override
        public void run() {
            if ((curDownloadInfo = getFirstWaiting()) != null) {
                curDownloadInfo.setDownloadState(DownloadInfo.DOWNLOADING);
                if (mListener != null)
                    mListener.onGalleryDownloadStart(curDownloadInfo);
                
                // get
                GalleryInfo galleryInfo = curDownloadInfo.getGalleryInfo();
                
                HttpHelper hp = new HttpHelper(mAppContext);
                String url = EhClient.getDetailUrl(galleryInfo.gid, galleryInfo.token,
                        EhClient.getDetailModeForDownloadMode(curDownloadInfo.getMode()));
                String pageContent = getUrlContext(hp, url);
                if (pageContent == null) {
                    if (mListener != null)
                        mListener.onGalleryDownloadFailed(curDownloadInfo, hp.getEMsg());
                    // New turn
                    new Thread(new DownloadGallery()).start();
                } else {
                    
                    
                    
                    
                }
            }
        }
    }
    
    interface OnGalleryDownloadListener {
        void onGalleryDownloadStart(DownloadInfo downloadInfo);
        void onGalleryDownloadStop(DownloadInfo downloadInfo);
        void onGalleryDownloadFailed(DownloadInfo downloadInfo, String eMsg);
        void onGalleryDownloadCompleted(DownloadInfo downloadInfo);
    }
    
    
    
    
    
}
