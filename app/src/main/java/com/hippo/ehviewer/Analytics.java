/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.hippo.scene.SceneFragment;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.IoThreadPoolExecutor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public final class Analytics {

    private static FirebaseAnalytics analytics;
    private static String userId;
    private static OkHttpClient client;

    private Analytics() {}

    public static void start(Context context) {
        analytics = FirebaseAnalytics.getInstance(context);
        userId = Settings.getUserID();
        analytics.setUserId(userId);
        client = EhApplication.getOkHttpClient(context);
    }

    public static boolean isEnabled() {
        return analytics != null && Settings.getEnableAnalytics();
    }

    public static void onSceneView(SceneFragment scene) {
        if (isEnabled()) {
            Bundle bundle = new Bundle();
            bundle.putString("scene_simple_class", scene.getClass().getSimpleName());
            bundle.putString("scene_class", scene.getClass().getName());
            analytics.logEvent("scene_view", bundle);
        }
    }

    public static void signIn() {
        addRecord("sign_in", null, null, null);
    }

    public static void viewGalleryList() {
        addRecord("view_gallery_list", null, null, null);
    }

    public static void imageSearch() {
        addRecord("image_search", null, null, null);
    }

    public static void viewWhatsHot() {
        addRecord("view_whats_hot", null, null, null);
    }

    public static void viewHistory() {
        addRecord("view_history", null, null, null);
    }

    public static void viewFavourite() {
        addRecord("view_favourite", null, null, null);
    }

    public static void viewGallery(long gid, String token) {
        addRecord("view_gallery", gid, token, null);
    }

    public static void readGallery(long gid, String token) {
        addRecord("read_gallery", gid, token, null);
    }

    public static void downloadGallery(long gid, String token) {
        addRecord("download_gallery", gid, token, null);
    }

    public static void rateGallery(long gid, String token) {
        addRecord("rate_gallery", gid, token, null);
    }

    public static void commentGallery(long gid, String token) {
        addRecord("comment_gallery", gid, token, null);
    }

    public static void voteComment(long gid, String token) {
        addRecord("vote_comment", gid, token, null);
    }

    public static void addGalleryToFavourite(long gid, String token) {
        addRecord("add_gallery_to_favourite", gid, token, null);
    }

    public static void downloadArchive(long gid, String token) {
        addRecord("download_archive", gid, token, null);
    }

    private static void addRecord(String action, Long gid, String token, Integer page) {
        if (isEnabled()) {
            IoThreadPoolExecutor.getInstance().execute(() -> {
                try {
                    StringBuilder sb = new StringBuilder(150)
                            .append("http://155.138.199.206:8080/addRecord?userId=")
                            .append(userId)
                            .append("&action=")
                            .append(action);
                    if (gid != null) {
                        sb.append("&gid=").append(gid);
                    }
                    if (token != null) {
                        sb.append("&token=").append(token);
                    }
                    if (page != null) {
                        sb.append("&page=").append(page);
                    }
                    String url = sb.toString();

                    Request request = new Request.Builder().url(url).build();
                    client.newCall(request).execute().close();
                } catch (Throwable t) {
                    ExceptionUtils.throwIfFatal(t);
                }
            });
        }
    }
}
