/*
 * Copyright 2017 Hippo Seven
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

package com.hippo.ehviewer.client.converter;

/*
 * Created by Hippo on 2/24/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.result.GalleryMetadataResult;
import com.hippo.ehviewer.util.JSONUtils;
import com.hippo.yorozuya.NumberUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GalleryMetadataConverter extends EhConverter<GalleryMetadataResult> {

  private static final String DEFAULT_GROUP = "misc";

  @NonNull
  @Override
  public GalleryMetadataResult convert(String body) throws Exception {
    List<GalleryInfo> galleryInfoList = new ArrayList<>();
    try {
      JSONObject jo = new JSONObject(body);
      JSONArray ja = jo.getJSONArray("gmetadata");
      for (int i = 0, n = ja.length(); i < n; ++i) {
        galleryInfoList.add(parseGalleryInfo(ja.getJSONObject(i)));
      }
      return new GalleryMetadataResult(galleryInfoList);
    } catch (JSONException e) {
      throw new ParseException("Can't parse gallery metadata json", body);
    }
  }

  private static GalleryInfo parseGalleryInfo(JSONObject jo) {
    GalleryInfo info = new GalleryInfo();
    info.gid = jo.optLong("gid");
    if (info.gid == 0) {
      return null;
    }
    info.token = JSONUtils.optString(jo, "token");
    if (info.token == null) {
      return null;
    }
    info.title = ConverterUtils.unescapeXml(JSONUtils.optString(jo, "title"));
    if (info.title != null) {
      info.language = EhUtils.guessLang(info.title);
    }
    info.titleJpn = JSONUtils.optString(jo, "title_jpn");
    if (info.language == EhUtils.LANG_UNKNOWN && info.titleJpn != null) {
      info.language = EhUtils.guessLang(info.titleJpn);
    }
    info.archiverKey = JSONUtils.optString(jo, "archiver_key");
    info.category = EhUtils.getCategory(JSONUtils.optString(jo, "category"));
    info.coverUrl = ConverterUtils.unescapeXml(JSONUtils.optString(jo, "thumb"));
    info.cover = EhUrl.getImageFingerprint(info.coverUrl);
    info.uploader = JSONUtils.optString(jo, "uploader");
    info.date = NumberUtils.parseLong(JSONUtils.optString(jo, "posted"), 0) * 1000;
    info.pages = NumberUtils.parseInt(JSONUtils.optString(jo, "filecount"), -1);
    info.size = NumberUtils.parseLong(JSONUtils.optString(jo, "filesize"), -1);
    info.invalid = jo.optBoolean("expunged", false);
    info.rating = NumberUtils.parseFloat(JSONUtils.optString(jo, "rating"), 0.0f);
    info.torrentCount = NumberUtils.parseInt(JSONUtils.optString(jo, "torrentcount"), 0);
    JSONArray tags = jo.optJSONArray("tags");
    if (tags != null) {
      for (int i = 0, n = tags.length(); i < n; ++i) {
        String tag = JSONUtils.optString(tags, i);
        if (tag == null) {
          continue;
        }

        String group;
        String name;
        int index = tag.indexOf(":");
        if (index == -1) {
          // Default group
          group = DEFAULT_GROUP;
          name = tag;
        } else {
          group = tag.substring(0, index);
          name = tag.substring(index + 1);
        }

        Map<String, List<String>> map = new HashMap<>();
        info.tags = map;
        List<String> list = map.get(group);
        if (list == null) {
          list = new LinkedList<>();
          map.put(group, list);
        }
        list.add(name);
      }
    }

    return info;
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public GalleryMetadataResult error(Throwable t) {
    return GalleryMetadataResult.error(t);
  }
}
