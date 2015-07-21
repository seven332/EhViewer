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

package com.hippo.ehviewer.client;

import com.hippo.ehviewer.client.data.GalleryApiDetail;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GalleryApiParser {

    public static List<GalleryApiDetail> parse(String body) throws JSONException {
        JSONObject jo = new JSONObject(body);
        JSONArray ja = jo.getJSONArray("gmetadata");
        List<GalleryApiDetail> list = new ArrayList<>();
        int length = ja.length();
        for (int i = 0; i < length; i++) {
            GalleryApiDetail gad = new GalleryApiDetail();
            JSONObject g = ja.getJSONObject(i);
            gad.gid = g.getInt("gid");
            gad.token = g.getString("token");
            gad.archiverKey = g.getString("archiver_key");
            gad.title = StringUtils.unescapeXml(g.getString("title"));
            gad.titleJpn = StringUtils.unescapeXml(g.getString("title_jpn"));
            gad.category = EhUtils.getCategory(g.getString("category"));
            gad.thumb = StringUtils.unescapeXml(g.getString("thumb"));
            gad.uploader = g.getString("uploader");
            gad.posted = GalleryBase.DEFAULT_DATE_FORMAT.format(new Date(Long.parseLong(g.getString("posted")) * 1000));
            gad.pageCount = Integer.parseInt(g.getString("filecount"));
            gad.size = FileUtils.humanReadableByteCount(g.getLong("filesize"), false);
            gad.expunged = g.getBoolean("expunged");
            gad.rating = Float.parseFloat(g.getString("rating"));
            gad.torrentcount = Integer.parseInt(g.getString("torrentcount"));
            JSONArray tagJa = g.getJSONArray("tags");
            int length2 = tagJa.length();
            String[] tags = new String[length2];
            for (int j = 0; j < length2; j++) {
                tags[j] = tagJa.getString(j);
            }
            gad.tags = tags;
            list.add(gad);
        }
        return list;
    }
}
