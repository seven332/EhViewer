/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
