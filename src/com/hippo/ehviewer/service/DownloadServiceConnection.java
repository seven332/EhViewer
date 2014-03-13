package com.hippo.ehviewer.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class DownloadServiceConnection implements ServiceConnection{
    
    private DownloadService mService;
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((DownloadService.ServiceBinder)service).getService();
        
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }
    
    public DownloadService getService() {
        return mService;
    }
}
