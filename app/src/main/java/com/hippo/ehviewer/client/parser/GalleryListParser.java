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
import android.support.annotation.Nullable;
import android.util.Log;

import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.util.JsoupUtils;
import com.hippo.yorozuya.NumberUtils;
import com.hippo.yorozuya.StringUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryListParser {

    private static final String TAG = GalleryListParser.class.getSimpleName();

    private static final Pattern PATTERN_RATING = Pattern.compile("\\d+px");
    private static final Pattern PATTERN_THUMB_SIZE = Pattern.compile("height:(\\d+)px; width:(\\d+)px");

    public static class Result {
        public int pages;
        public List<GalleryInfo> galleryInfoList;
    }

    private static int parsePages(Document d, String body) throws ParseException {
        try {
            Elements es = d.getElementsByClass("ptt").first().child(0).child(0).children();
            return Integer.parseInt(es.get(es.size() - 2).text().trim());
        } catch (Exception e) {
            throw new ParseException("Can't parse gallery list pages", body);
        }
    }

    private static String parseRating(String ratingStyle) {
        Matcher m = PATTERN_RATING.matcher(ratingStyle);
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

    @Nullable
    private static GalleryInfo parseGalleryInfo(Element e) {
        GalleryInfo gi = new GalleryInfo();
        // Get category
        Element ic = JsoupUtils.getElementByClass(e, "ic");
        if (null != ic) {
            gi.category = EhUtils.getCategory(ic.attr("alt").trim());
        } else {
            Log.w(TAG, "Can't parse gallery info category");
            gi.category = EhUtils.UNKNOWN;
        }
        // Posted
        Element itd = JsoupUtils.getElementByClass(e, "itd");
        if (null != itd) {
            gi.posted = itd.text().trim();
        } else {
            Log.w(TAG, "Can't parse gallery info posted");
            gi.posted = "";
        }
        // Thumb
        Element it2 = JsoupUtils.getElementByClass(e, "it2");
        if (null != it2) {
            // Thumb size
            Matcher m = PATTERN_THUMB_SIZE.matcher(it2.attr("style"));
            if (m.find()) {
                gi.thumbWidth = NumberUtils.parseIntSafely(m.group(2), 0);
                gi.thumbHeight = NumberUtils.parseIntSafely(m.group(1), 0);
            } else {
                Log.w(TAG, "Can't parse gallery info thumb size");
                gi.thumbWidth = 0;
                gi.thumbHeight = 0;
            }

            // Thumb url
            Elements es = it2.children();
            if (null != es && es.size() >= 1) {
                gi.thumb = EhUtils.handleThumbUrlResolution(es.get(0).attr("src"));
            } else {
                String html = it2.html();
                int index1 = html.indexOf('~');
                int index2 = StringUtils.ordinalIndexOf(html, '~', 2);
                if (index1 < index2) {
                    gi.thumb = EhUtils.handleThumbUrlResolution(
                            "http://" +StringUtils.replace(html.substring(index1 + 1, index2), "~", "/"));
                } else {
                    Log.w(TAG, "Can't parse gallery info thumb url");
                    gi.thumb = "";
                }
            }
        } else {
            Log.w(TAG, "Can't parse gallery info thumb");
            gi.thumbWidth = 0;
            gi.thumbHeight = 0;
            gi.thumb = "";
        }
        // Title (required)
        Element it5 = JsoupUtils.getElementByClass(e, "it5");
        if (null == it5) {
            Log.e(TAG, "Can't parse gallery info title");
            return null;
        }
        Elements es = it5.children();
        if (null == es || es.size() <= 0) {
            Log.e(TAG, "Can't parse gallery info title");
            return null;
        }
        Element a = es.get(0);
        GalleryDetailUrlParser.Result result = GalleryDetailUrlParser.parse(a.attr("href"));
        if (null == result) {
            Log.e(TAG, "Can't parse gallery info title");
            return null;
        }
        gi.gid = result.gid;
        gi.token = result.token;
        gi.title = a.text().trim();
        // Rating
        Element it4r = JsoupUtils.getElementByClass(e, "it4r");
        if (null != it4r) {
            gi.rating = NumberUtils.parseFloatSafely(parseRating(it4r.attr("style")), -1.0f);
        } else {
            Log.w(TAG, "Can't parse gallery info rating");
            gi.rating = -1.0f;
        }
        // Uploader
        Element itu = JsoupUtils.getElementByClass(e, "itu");
        if (null != itu) {
            gi.uploader = itu.text().trim();
        } else {
            Log.w(TAG, "Can't parse gallery info uploader");
            gi.uploader = "";
        }

        gi.generateSLang();

        return gi;
    }

    public static Result parse(@NonNull String body) throws Exception {
        Result result = new Result();
        Document d = Jsoup.parse(body);

        try {
            result.pages = parsePages(d, body);
        } catch (ParseException e) {
            if (body.contains("No hits found</p>")) {
                result.pages = 0;
                //noinspection unchecked
                result.galleryInfoList = Collections.EMPTY_LIST;
                return result;
            } else {
                throw e;
            }
        }

        try {
            Elements es = d.getElementsByClass("itg").first().child(0).children();
            List<GalleryInfo> list = new ArrayList<>(es.size() - 1);
            for (int i = 1; i < es.size(); i++) { // First one is table header, skip it
                GalleryInfo gi = parseGalleryInfo(es.get(i));
                if (null != gi) {
                    list.add(gi);
                }
            }
            result.galleryInfoList = list;
        } catch (Exception e) {
            throw new ParseException("Can't parse gallery list", body);
        }

        return result;
    }
}
