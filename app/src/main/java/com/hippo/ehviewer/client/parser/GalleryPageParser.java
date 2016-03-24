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

import android.text.TextUtils;

import com.hippo.ehviewer.client.exception.ParseException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryPageParser {

    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile("<div id=\"i3\"><a[^>]+><img id=\"img\" src=\"([^\"]+)\"");
    private static final Pattern SKIP_HATH_KEY_PATTERN = Pattern.compile("onclick=\"return nl\\('([^\\)]+)'\\)");

    public static Result parse(String body) throws ParseException {
        Matcher m;
        Result result = new Result();
        m = IMAGE_URL_PATTERN.matcher(body);
        if (m.find()) {
            result.imageUrl = ParserUtils.trim(m.group(1));
        }
        m = SKIP_HATH_KEY_PATTERN.matcher(body);
        if (m.find()) {
            result.skipHathKey = ParserUtils.trim(m.group(1));
        }
        if (!TextUtils.isEmpty(result.imageUrl) && !TextUtils.isEmpty(result.skipHathKey)) {
            return result;
        } else {
            throw new ParseException("Parse image url and skip hath key error", body);
        }
    }

    public static class Result {
        public String imageUrl;
        public String skipHathKey;
    }
}
