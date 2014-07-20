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

package com.hippo.ehviewer;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.Constants;

public class AppHandler extends Handler {

    public static final int IMAGE_LOADER_TAG = 0x0;
    public static final int HTTP_HELPER_TAG = 0x1;

    private static Handler sInstance;

    public AppHandler(Looper mainLooper) {
        super(mainLooper);
    }

    public final static Handler getInstance() {
        if (sInstance == null)
            sInstance = new AppHandler(Looper.getMainLooper());
        return sInstance;
    }

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what) {
        case IMAGE_LOADER_TAG:
            ImageLoader.LoadTask task = (ImageLoader.LoadTask)msg.obj;
            task.listener.onGetImage(task.key, task.bitmap);
            break;

        case HTTP_HELPER_TAG:
            HttpHelper.Package p = (HttpHelper.Package)msg.obj;
            HttpHelper.OnRespondListener listener = p.listener;
            Object obj = p.obj;
            if (msg.arg1 == Constants.TRUE) {
                listener.onSuccess(obj);
            } else {
                listener.onFailure((String)obj);
            }
            break;
        }

    }

}
