/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.ehviewer.client;

import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PopularParser {

    public List<GalleryInfo> galleryInfoList;
    public long timeStamp;

    public void parse(String body) throws Exception {
        galleryInfoList = new ArrayList<>();
        JSONObject js = new JSONObject(body);
        js = js.getJSONObject("popular");

        if (!js.has("galleries")) {
            if (js.has("error")) {
                throw new EhException(js.getString("error"));
            } else {
                throw new EhException("Parser error"); // TODO
            }
        }

        JSONArray ja = js.getJSONArray("galleries");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject j = ja.getJSONObject(i);
            GalleryInfo gi = new GalleryInfo();
            gi.gid = j.getInt("gid");
            gi.token = j.getString("token");
            gi.title = Utils.unescapeXml(j.getString("title"));
            gi.posted = EhUtils.formatPostDate(Long.parseLong(j.getString("posted")) * 1000);
            gi.thumb = j.getString("thumb");
            gi.category = EhUtils.getCategory(j.getString("category"));
            gi.uploader = j.getString("uploader");
            gi.rating = Float.parseFloat(j.getString("rating"));
            gi.generateSLang();
            galleryInfoList.add(gi);
        }

        if (js.has("time")) {
            timeStamp = js.getLong("time");
        } else {
            timeStamp = -1;
        }
    }
}
