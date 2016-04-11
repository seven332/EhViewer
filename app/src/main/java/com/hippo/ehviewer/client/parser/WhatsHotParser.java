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

package com.hippo.ehviewer.client.parser;

import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.util.JsoupUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class WhatsHotParser {

    @SuppressWarnings("ConstantConditions")
    public static List<GalleryInfo> parse(String body) throws ParseException {
        try {
            List<GalleryInfo> galleryInfoList = new ArrayList<>(15);
            Document d = Jsoup.parse(body);
            Element pp = d.getElementById("pp");
            Elements id1List = pp.getElementsByClass("id1");
            for (int i = 0, n = id1List.size(); i < n; i++) {
                GalleryInfo galleryInfo = new GalleryInfo();
                Element id1 = id1List.get(i);
                Element id3 = JsoupUtils.getElementByClass(id1, "id3");
                Element temp = JsoupUtils.getElementByTag(id3, "a");
                String url = temp.attr("href");
                GalleryDetailUrlParser.Result result = GalleryDetailUrlParser.parse(url);
                galleryInfo.gid = result.gid;
                galleryInfo.token = result.token;
                temp = JsoupUtils.getElementByTag(temp, "img");
                galleryInfo.thumb = EhUtils.handleThumbUrlResolution(temp.attr("src"));
                galleryInfo.title = temp.attr("title");
                galleryInfo.generateSLang();
                galleryInfoList.add(galleryInfo);
            }
            return galleryInfoList;
        } catch (Exception e) {
            throw new ParseException("Parse whats hot error", body);
        }
    }
}
