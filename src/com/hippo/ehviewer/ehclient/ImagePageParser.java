/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ehclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImagePageParser {

    public String imageUrl;
    public String fullImageUrl;
    public int gid;
    public String token;

    public void reset() {
        imageUrl = null;
        fullImageUrl = null;
        gid = 0;
        token = null;
    }

    public boolean parser(String body) {
        Pattern p = Pattern.compile("<div id=\"i3\">.+?<img id=\"img\" src=\"(.+?)\""
                + ".+?"
                + "<div id=\"i5\"><div class=\"sb\"><a href=\"(?:http|https)://.+?/g/(\\d+)/(\\w+)"
                + "(?:.+?<div id=\"i7\" class=\"if\">.+?<a href=\"(.+?)\">)?");
        Matcher m = p.matcher(body);
        if (m.matches()) {
            imageUrl = m.group(1);
            gid = Integer.parseInt(m.group(2));
            token = m.group(3);
            fullImageUrl = m.group(4);
            return true;
        } else {
            return false;
        }
    }
}
