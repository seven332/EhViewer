package com.hippo.ehviewer.gallery.data;

import java.io.File;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.hippo.ehviewer.service.DownloadService;

public class DownloadImageSet extends ImageSet {
    
    public DownloadImageSet(Context context, int gid, File folder, int size,
            int startIndex, int endIndex, Set<Integer> failIndexSet) {
        super(context, gid, folder, size, startIndex, endIndex, failIndexSet);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_UPDATE);
        mContext.registerReceiver(mReceiver, filter);
    }
    
    public void unregisterReceiver() {
        mContext.unregisterReceiver(mReceiver);
    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String gid;
            if(intent.getAction().equals(DownloadService.ACTION_UPDATE)
                    && (gid = intent.getStringExtra(DownloadService.KEY_GID)) != null
                    && gid.equals(mGid)) {
                int index = intent.getIntExtra(DownloadService.KEY_INDEX, -1);
                int state = intent.getIntExtra(DownloadService.KEY_STATE, -1);
                if (index != -1 && state != -1) {
                    changeState(index, state);
                }
            }
        }
    };
    
}
