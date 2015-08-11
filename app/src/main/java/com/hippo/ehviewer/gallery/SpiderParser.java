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

package com.hippo.ehviewer.gallery;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.GalleryDetailParser;
import com.hippo.ehviewer.client.ParseException;
import com.hippo.ehviewer.client.ParserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpiderParser {

    private static final Pattern NORMAL_PREVIEW_PATTERN = Pattern.compile("<div class=\"gdtm\"[^>]+><div[^>]+><a href=\"[^\"]+/(\\w+)/(\\d+)-(\\d+)\">");
    private static final Pattern LARGE_PREVIEW_PATTERN = Pattern.compile("<div class=\"gdtl\"[^>]+><a href=\"[^\"]+/(\\w+)/(\\d+)-(\\d+)\">");

    public static class Result {
        public int pages;
        public int previewPages;
        public String previewSize;
        public int startIndex;
        public List<String> tokens;
    }

    public static final int REQUEST_PREVIEW = 0;
    public static final int REQUEST_ALL = 1;

    public static Result parse(String body, int request) throws Exception {
        Result result = new Result();
        Pattern p;
        Matcher m;

        if (request == REQUEST_ALL) {
            m = GalleryDetailParser.PAGES_PATTERN.matcher(body);
            if (m.find()) {
                result.pages = ParserUtils.parseInt(m.group(1));
            } else {
                throw new ParseException("Parse pages error", body);
            }

            m = GalleryDetailParser.PREVIEW_PAGES_PATTERN.matcher(body);
            if (m.find()) {
                result.previewPages = ParserUtils.parseInt(m.group(1));
            } else {
                throw new ParseException("Parse preview page count error", body);
            }
        }

        List<String> list;
        if (body.contains("<div class=\"gdtm\"")) {
            result.previewSize = EhConfig.PREVIEW_SIZE_NORMAL;
            p = NORMAL_PREVIEW_PATTERN;
            list = new ArrayList<>(40);
        } else {
            result.previewSize = EhConfig.PREVIEW_SIZE_LARGE;
            p = LARGE_PREVIEW_PATTERN;
            list = new ArrayList<>(20);
        }
        result.tokens = list;
        m = p.matcher(body);

        boolean first = true;
        while (m.find()) {
            if (first) {
                first = false;
                result.startIndex = Integer.parseInt(m.group(3)) - 1;
            }
            list.add(m.group(1));
        }

        return result;
    }
}
