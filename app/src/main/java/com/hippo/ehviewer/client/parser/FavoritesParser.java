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

import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.util.JsoupUtils;
import com.hippo.yorozuya.AssertUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FavoritesParser {

    private static final Pattern LIMIT_PATTERN = Pattern.compile("Currently Used Favorite Slots: ([\\d,]+) / ([\\d,]+)");

    public static class Result {
        public String[] catArray; // Size 10
        public int[] countArray; // Size 10
        public int current; // -1 for error
        public int limit; // -1 for error
        public int pages;
        public List<GalleryInfo> galleryInfoList;
    }

    @SuppressWarnings("ConstantConditions")
    public static Result parse(String body) throws Exception {
        String[] catArray = new String[10];
        int[] countArray = new int[10];
        int current;
        int limit;

        try {
            Document d = Jsoup.parse(body, EhUrl.HOST_EX);
            Element ido = JsoupUtils.getElementByClass(d, "ido");
            Elements fps = ido.getElementsByClass("fp");
            // Last one is "fp fps"
            AssertUtils.assertEqualsEx(11, fps.size());

            for (int i = 0; i < 10; i++) {
                Element fp = fps.get(i);
                countArray[i] = ParserUtils.parseInt(fp.child(0).text());
                catArray[i] = ParserUtils.trim(fp.child(2).text());
            }

            Matcher m = LIMIT_PATTERN.matcher(body);
            if (m.find()) {
                current = ParserUtils.parseInt(m.group(1));
                limit = ParserUtils.parseInt(m.group(2));
            } else {
                current = -1;
                limit = -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParseException("Parse favorites error", body);
        }

        GalleryListParser.Result result = GalleryListParser.parse(body);

        Result re = new Result();
        re.catArray = catArray;
        re.countArray = countArray;
        re.current = current;
        re.limit = limit;
        re.pages = result.pages;
        re.galleryInfoList = result.galleryInfos;

        return re;
    }
}
