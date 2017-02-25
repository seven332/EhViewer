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
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.result.WhatsHotResult;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A {@link retrofit2.Converter} to parse whats hot.
 */
public class WhatsHotConverter extends EhConverter<WhatsHotResult> {

  private static final String LOG_TAG = WhatsHotConverter.class.getSimpleName();

  private static final int DEFAULT_WHATS_HOT_SIZE = 15;

  @NonNull
  @Override
  public WhatsHotResult convert(String body) throws ParseException {
    List<GalleryInfo> galleryInfoList = new ArrayList<>(DEFAULT_WHATS_HOT_SIZE);

    Document d = Jsoup.parse(body);
    Element pp = d.getElementById("pp");
    if (pp == null) {
      throw new ParseException("Can't get element pp", body);
    }
    Elements id1s = pp.getElementsByClass("id1");
    for (Element e: id1s) {
      GalleryInfo gi = parseGalleryInfo(e);
      if (gi != null) {
        galleryInfoList.add(gi);
      }
    }

    return new WhatsHotResult(galleryInfoList);
  }

  @Nullable
  private static GalleryInfo parseGalleryInfo(Element e) {
    GalleryInfo galleryInfo = new GalleryInfo();
    Element id3 = e.getElementsByClass("id3").first();
    if (id3 == null) {
      Log.w(LOG_TAG, "Can't find id3 element");
      return null;
    }

    Element a = id3.getElementsByTag("a").first();
    if (a == null) {
      Log.w(LOG_TAG, "Can't find a element");
      return null;
    }
    String url = a.attr("href");
    Pair<Long, String> gidToken = EhUrl.getGidToken(url);
    if (gidToken == null) {
      Log.w(LOG_TAG, "Can't get gid and token from url");
      return null;
    }
    galleryInfo.gid = gidToken.first;
    galleryInfo.token = gidToken.second;

    Element img = a.getElementsByTag("img").first();
    if (img == null) {
      Log.w(LOG_TAG, "Can't find img element");
      return null;
    }
    galleryInfo.coverUrl = img.attr("src");
    galleryInfo.cover = EhUrl.getImageFingerprint(galleryInfo.coverUrl);
    galleryInfo.title = img.attr("title");
    galleryInfo.language = EhUtils.guessLang(galleryInfo.title);

    return galleryInfo;
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public WhatsHotResult error(Throwable t) {
    return WhatsHotResult.error(t);
  }
}
