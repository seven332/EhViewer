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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Like http://exhentai.org/s/91ea4b6d89/901103-12
 */
public final class GalleryPageUrlParser {

    public static final Pattern URL_PATTERN = Pattern.compile("https?://(?:" +
            EhUrl.DOMAIN_EX + "|" + EhUrl.DOMAIN_E + "|" + EhUrl.DOMAIN_LOFI + ")/s/(\\w+)/(\\d+)-(\\d+)");

    public static Result parse(String url) {
        if (url == null) {
            return null;
        }

        Matcher m = URL_PATTERN.matcher(url);
        if (m.find()) {
            Result result = new Result();
            result.gid = Long.parseLong(m.group(2));
            result.pToken = m.group(1);
            result.page = Integer.parseInt(m.group(3)) - 1;
            return result;
        } else {
            return null;
        }
    }

    public static class Result {
        public long gid;
        public String pToken;
        public int page;
    }
}
