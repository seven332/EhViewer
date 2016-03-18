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

import android.support.annotation.NonNull;

import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.yorozuya.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryListParser {

    static final Pattern PAGES_PATTERN = Pattern.compile("<a[^<>]+>([\\d]+)</a></td><td[^<>]+>(?:<a[^<>]+>)?&");
    static final Pattern PATTERN = Pattern.compile(
            "<td class=\"itdc\">(?:<a.+?>)?<img.+?alt=\"(.+?)\".+?/>(?:</a>)?</td>" // category
                    + "<td.+?>(.+?)</td>" // posted
                    + "<td.+?><div.+?><div.+?height:(\\d+)px; width:(\\d+)px\">"
                    + "(?:<img.+?src=\"(.+?)\".+?alt=\"(.+?)\" style.+?/>"
                    + "|init~([^<>\"~]+~[^<>\"~]+)~([^<>]+))" // thumb and title
                    + "</div>"
                    + ".+?"
                    + "<div class=\"it5\"><a href=\"([^<>\"]+)\"[^<>]+>(.+?)</a></div>" // url and title
                    + ".+?"
                    + "<div class=\"ir it4r\" style=\"([^<>\"]+)\">" // rating
                    + ".+?"
                    + "<td class=\"itu\"><div><a.+?>(.+?)</a>"); // uploader
    static final Pattern RATING_PATTERN = Pattern.compile("\\d+px");

    public static class Result {
        public int pages;
        public List<GalleryInfo> galleryInfos;
    }

    @SuppressWarnings("unchecked")
    public static Result parse(@NonNull String body) throws Exception {
        Result result = new Result();
        Matcher m;

        // pages
        m = PAGES_PATTERN.matcher(body);
        if (m.find()) {
            result.pages = ParserUtils.parseInt(m.group(1));
        } else if (body.contains("No hits found</p>")) {
            result.pages = 0;
            result.galleryInfos = Collections.EMPTY_LIST;
        } else {
            // Can not get page number
            throw new ParseException("Can't parse gallery list", body);
        }

        if (result.pages > 0) {
            List<GalleryInfo> list = new ArrayList<>(25);
            result.galleryInfos = list;

            m = PATTERN.matcher(body);
            while (m.find()) {
                GalleryInfo gi = new GalleryInfo();

                gi.category = EhUtils.getCategory(ParserUtils.trim(m.group(1)));
                gi.posted = ParserUtils.trim(m.group(2));
                //gi.thumbHeight = ParserUtils.parseInt(m.group(3));
                //gi.thumbWidth = ParserUtils.parseInt(m.group(4));

                if (m.group(5) == null) {
                    gi.thumb = ParserUtils.trim("http://" + m.group(7).replace('~', '/'));
                    gi.title = ParserUtils.trim(m.group(8));
                } else {
                    gi.thumb = ParserUtils.trim(m.group(5));
                    gi.title = ParserUtils.trim(m.group(6));
                }

                Matcher matcher = GalleryDetailUrlParser.URL_PATTERN.matcher(m.group(9));
                if (matcher.find()) {
                    gi.gid = ParserUtils.parseLong(matcher.group(1));
                    gi.token = ParserUtils.trim(matcher.group(2));
                } else {
                    continue;
                }

                gi.rating = NumberUtils.parseFloatSafely(getRating(m.group(11)), Float.NaN);
                gi.uploader = ParserUtils.trim(m.group(12));
                gi.generateSLang();

                list.add(gi);
            }

            if (list.size() == 0) {
                throw new ParseException("Can't parse gallery list", body);
            }
        }

        return result;
    }

    private static String getRating(String rawRate) {
        Matcher m = RATING_PATTERN.matcher(rawRate);
        int num1;
        int num2;
        int rate = 5;
        String re;
        if (m.find()) {
            num1 = ParserUtils.parseInt(m.group().replace("px", ""));
        } else {
            return null;
        }
        if (m.find()) {
            num2 = ParserUtils.parseInt(m.group().replace("px", ""));
        } else {
            return null;
        }
        rate = rate - num1 / 16;
        if (num2 == 21) {
            rate--;
            re = Integer.toString(rate);
            re = re + ".5";
        } else
            re = Integer.toString(rate);
        return re;
    }
}
