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

package com.hippo.ehviewer.client;

import com.hippo.yorozuya.Say;
import com.hippo.yorozuya.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryPageParser {

    public static String parse(String body) throws ParseException {
        Pattern p = Pattern.compile("<div id=\"i3\"><a[^>]+><img id=\"img\" src=\"([^\"]+)\"");
        Matcher m = p.matcher(body);
        if (m.find()) {
            return StringUtils.unescapeXml(m.group(1));
        } else {
            Say.f("s", body);
            throw new ParseException("Parse gallery page error", body);
        }
    }
}
