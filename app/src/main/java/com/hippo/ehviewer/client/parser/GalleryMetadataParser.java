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

package com.hippo.ehviewer.client.parser;

/*
 * Created by Hippo on 5/17/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.util.JSONUtils;
import com.hippo.yorozuya.NumberUtils;
import com.hippo.yorozuya.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class GalleryMetadataParser {
  private GalleryMetadataParser() {}

  private static final String DEFAULT_GROUP = "misc";

  /**
   * Parses gallery metadata json to a list of {@link GalleryInfo}.
   * <p>
   * Gid and token must be filled.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static List<GalleryInfo> parseGalleryMetadata(@NonNull String body) throws ParseException {
    List<GalleryInfo> galleryInfoList = new ArrayList<>();

    try {
      JSONObject jo = new JSONObject(body);
      JSONArray ja = jo.getJSONArray("gmetadata");
      for (int i = 0, n = ja.length(); i < n; ++i) {
        GalleryInfo info = parseGalleryInfo(ja.getJSONObject(i));
        if (info != null) {
          galleryInfoList.add(info);
        }
      }
    } catch (JSONException e) {
      throw new ParseException("Can't parse gallery metadata json", body, e);
    }

    return galleryInfoList;
  }

  @Nullable
  private static GalleryInfo parseGalleryInfo(JSONObject jo) {
    GalleryInfo info = new GalleryInfo();

    info.gid = jo.optLong("gid");
    if (info.gid == 0) return null;

    info.token = JSONUtils.optString(jo, "token");
    if (info.token == null) return null;

    info.title = ParserUtils.unescape(JSONUtils.optString(jo, "title"));
    if (info.title != null) {
      info.language = EhUtils.guessLang(info.title);
    }
    info.titleJpn = JSONUtils.optString(jo, "title_jpn");
    if (info.language == EhUtils.LANG_UNKNOWN && info.titleJpn != null) {
      info.language = EhUtils.guessLang(info.titleJpn);
    }
    if (StringUtils.isEmpty(info.title) || StringUtils.isEmpty(info.titleJpn)) return null;

    info.archiverKey = JSONUtils.optString(jo, "archiver_key");
    info.category = EhUtils.getCategory(JSONUtils.optString(jo, "category"));
    info.coverUrl = ParserUtils.unescape(JSONUtils.optString(jo, "thumb"));
    info.cover = EhUrl.getImageFingerprint(info.coverUrl);
    info.uploader = JSONUtils.optString(jo, "uploader");
    info.date = NumberUtils.parseLong(JSONUtils.optString(jo, "posted"), 0) * 1000;
    info.pages = NumberUtils.parseInt(JSONUtils.optString(jo, "filecount"), 0);
    info.size = NumberUtils.parseLong(JSONUtils.optString(jo, "filesize"), 0);
    info.invalid = jo.optBoolean("expunged", false);
    info.rating = NumberUtils.parseFloat(JSONUtils.optString(jo, "rating"), 0.0f);
    info.torrentCount = NumberUtils.parseInt(JSONUtils.optString(jo, "torrentcount"), 0);

    JSONArray tags = jo.optJSONArray("tags");
    if (tags != null) {
      for (int i = 0, n = tags.length(); i < n; ++i) {
        String tag = JSONUtils.optString(tags, i);
        if (tag == null) continue;

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

        info.tagSet.add(group, name);
      }
    }

    return info;
  }
}
