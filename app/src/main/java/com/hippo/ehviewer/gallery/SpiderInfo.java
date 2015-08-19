/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.ehviewer.gallery;

import android.util.Log;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.yorozuya.AutoExpandArray;
import com.hippo.yorozuya.IOUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SpiderInfo {

    private static final String TAG = SpiderInfo.class.getSimpleName();

    private static final int VERSION = 1;

    public GalleryBase galleryBase;
    public int pages = -1;
    public String previewSize;
    public int previewPages;
    public int previewCountPerPage;
    public AutoExpandArray<String> tokens;

    /**
     * version
     * galleryBase.gid
     * galleryBase.token
     * galleryBase.title
     * galleryBase.titleJpn
     * galleryBase.thumb
     * galleryBase.category
     * galleryBase.posted
     * galleryBase.uploader
     * galleryBase.rating
     * pages
     * previewSize
     * previewPages
     * previewCountPerPage
     * index token
     * index token
     * ...
     *
     * @param os the output stream
     */
    public void write(OutputStream os) throws IOException {
        if (galleryBase == null || pages == -1 || previewSize == null) {
            // No need to write
            return;
        }

        GalleryBase gb = galleryBase;
        JSONObject jo;
        try {
            jo = new JSONObject();
            jo.put("version", VERSION);
            jo.put("gid", gb.gid);
            jo.put("token", gb.token);
            jo.put("title", gb.title);
            jo.put("titleJpn", gb.titleJpn);
            jo.put("thumb", gb.thumb);
            jo.put("category", gb.category);
            jo.put("posted", gb.posted);
            jo.put("uploader", gb.uploader);
            jo.put("rating", gb.rating);
            jo.put("pages", pages);
            jo.put("previewSize", previewSize);
            jo.put("previewPages", previewPages);
            jo.put("previewCountPerPage", previewCountPerPage);

            JSONArray tokenJa = new JSONArray();
            int length = Math.min(tokens.maxValidIndex() + 1, pages);
            for (int i = 0; i < length; i++) {
                tokenJa.put(tokens.get(i));
            }

            jo.put("tokens", tokenJa);
        } catch (JSONException e) {
            Log.e(TAG, "Can't create SpiderInfo JSON to write", e);
            jo = null;
        }

        if (jo != null) {
            OutputStreamWriter osWrite = new OutputStreamWriter(os, "UTF-8");
            osWrite.append(jo.toString());
            osWrite.flush();
            osWrite.close();
        }
    }


    public static SpiderInfo read(InputStream is) throws Exception {
        String str = IOUtils.readString(is, "UTF-8");

        JSONObject jo = new JSONObject(str);

        int version = jo.optInt("version", 0);
        if (VERSION != version){
            throw new IllegalStateException("Version should be " + VERSION +
                    ", but it is " + version);
        }

        SpiderInfo spiderInfo = new SpiderInfo();

        GalleryBase gb = new GalleryBase();
        gb.gid = jo.getInt("gid");
        gb.token = jo.getString("token");
        gb.title = jo.getString("title");
        gb.titleJpn = jo.optString("titleJpn", null);
        gb.thumb = jo.getString("thumb");
        gb.category = jo.getInt("category");
        gb.posted = jo.getString("posted");
        gb.uploader = jo.getString("uploader");
        gb.rating = (float) jo.getDouble("rating");
        spiderInfo.galleryBase = gb;

        spiderInfo.pages = jo.getInt("pages");
        spiderInfo.previewSize = jo.getString("previewSize");
        if (!spiderInfo.previewSize.equals(EhConfig.PREVIEW_SIZE_NORMAL) &&
                !spiderInfo.previewSize.equals(EhConfig.PREVIEW_SIZE_LARGE)) {
            throw new IllegalStateException("Unknown preview size " + spiderInfo.previewSize);
        }
        spiderInfo.previewPages = jo.getInt("previewPages");
        spiderInfo.previewCountPerPage = jo.getInt("previewCountPerPage");

        JSONArray ja = jo.getJSONArray("tokens");
        AutoExpandArray<String> tokens = new AutoExpandArray<>(ja.length());
        spiderInfo.tokens = tokens;
        for (int i = 0, n = ja.length(); i < n; i++) {
            tokens.set(i, ja.getString(i));
        }

        return spiderInfo;
    }
}
