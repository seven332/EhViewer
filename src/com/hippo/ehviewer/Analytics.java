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

import org.json.JSONObject;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.util.Config;

import android.content.Context;

public class Analytics {
    
    private static final String KEY_GALLERY = "gallery";
    private static final String KEY_OPEN = "open";
    private static final String KEY_READ = "read";
    private static final String KEY_DOWNLOAD = "download";
    private static final String KEY_ADD_TO_FAVORITE = "add_to_favorite";
    
    private static void doGallery(Context context, int gid, String token, String method) {
        if (!Config.getAllowAnalyics())
            return;
            
        try {
            JSONObject js = new JSONObject();
            js.put("gid", gid).put("token", token);
            EasyTracker easyTracker = EasyTracker.getInstance(context);
            easyTracker.send(MapBuilder.createEvent(KEY_GALLERY, method, js.toString(), null).build());
        } catch (Exception e) {}
    }
    
    public static void openGallery(Context context, int gid, String token) {
        doGallery(context, gid, token, KEY_OPEN);
    }
    
    public static void readGallery(Context context, int gid, String token) {
        doGallery(context, gid, token, KEY_READ);
    }
    
    public static void downloadGallery(Context context, int gid, String token) {
        doGallery(context, gid, token, KEY_DOWNLOAD);
    }
    
    public static void addToFavoriteGallery(Context context, int gid, String token) {
        doGallery(context, gid, token, KEY_ADD_TO_FAVORITE);
    }
}
