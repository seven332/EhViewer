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
 * Created by Hippo on 5/15/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.data.ApiUidKey;
import com.hippo.ehviewer.client.data.CommentEntry;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.PreviewPage;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.ApiUidKeyParser;
import com.hippo.ehviewer.client.parser.GalleryDetailParser;
import com.hippo.ehviewer.client.result.GalleryDetailResult;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * A {@link retrofit2.Converter} to parse gallery detail page.
 */
public class GalleryDetailConverter extends EhConverter<GalleryDetailResult> {

  public static final GalleryDetailConverter INSTANCE = new GalleryDetailConverter();

  private GalleryDetailConverter() {}

  @NonNull
  @Override
  public GalleryDetailResult convert(String body) throws ParseException {
    Document document = Jsoup.parse(body);

    GalleryInfo info = GalleryDetailParser.parseGalleryDetail(body, document);

    List<CommentEntry> comments;
    try {
      comments = GalleryDetailParser.parseComments(body, document);
    } catch (ParseException e) {
      comments = null;
    }

    List<PreviewPage> previews;
    try {
      previews = GalleryDetailParser.parsePreviews(body, document);
    } catch (ParseException e) {
      previews = null;
    }

    ApiUidKey apiUidKey;
    try {
      apiUidKey = ApiUidKeyParser.parseApiUidKey(body);
    } catch (ParseException e) {
      apiUidKey = null;
    }

    return new GalleryDetailResult(info, comments, previews, apiUidKey);
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public GalleryDetailResult error(Throwable t) {
    return GalleryDetailResult.error(t);
  }
}
