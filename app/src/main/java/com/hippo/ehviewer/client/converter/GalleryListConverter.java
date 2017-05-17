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
 * Created by Hippo on 1/29/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.client.result.GalleryListResult;
import java.util.Collections;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * A {@link retrofit2.Converter} to parse the html of gallery list.
 */
public class GalleryListConverter extends EhConverter<GalleryListResult> {

  @NonNull
  @Override
  public GalleryListResult convert(String body) throws ParseException {
    Document document = Jsoup.parse(body);

    int pages = GalleryListParser.parsePages(body, document);

    List<GalleryInfo> galleryInfoList;
    if (pages > 0) {
      galleryInfoList = GalleryListParser.parseGalleryList(body, document);
    } else {
      galleryInfoList = Collections.emptyList();
    }

    return new GalleryListResult(pages, galleryInfoList);
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public GalleryListResult error(Throwable t) {
    return GalleryListResult.error(t);
  }
}
