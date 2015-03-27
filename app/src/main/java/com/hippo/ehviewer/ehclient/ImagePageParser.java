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

import com.hippo.ehviewer.util.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImagePageParser {

    public String imageUrl;
    public String originalImageUrl;
    public int gid;
    public String token;

    public void reset() {
        imageUrl = null;
        originalImageUrl = null;
        gid = 0;
        token = null;
    }

    public boolean parser(String body, int mode) {
        Pattern p;
        Matcher m;

        if (body == null)
            return false;

        if (mode ==EhClient.MODE_LOFI) {
            p = Pattern.compile("<img id=\"sm\" src=\"([^\"]+)\"[^>]+>");
            m = p.matcher(body);
            if (m.find())
                imageUrl = Utils.unescapeXml(m.group(1));
            else
                return false;

            p = Pattern.compile("<a href=\"(?:http|https)://.+?/g/(\\d+)/(\\w+)/?\">Back</a>");
            m = p.matcher(body);
            if (m.find()) {
                gid = Integer.parseInt(m.group(1));
                token = m.group(2);
                return true;
            } else {
                return false;
            }

        } else {
            p = Pattern.compile(
                    "<(?:/iframe|div id=\"i3\")[^>]*><a[^>]*><img[^>]*src=\"(.+?)\"" +
                    ".+?" +
                    "<div class=\"sb\"><a href=\"(?:http|https)://.+?/g/(\\d+)/(\\w+)");
            m = p.matcher(body);
            if (m.find()) {
                imageUrl = Utils.unescapeXml(m.group(1));
                gid = Integer.parseInt(m.group(2));
                token = m.group(3);

                p = Pattern.compile("<a href=\"([^\"]+)fullimg.php([^\"]+)\">");
                m = p.matcher(body);
                if (m.find()) {
                    originalImageUrl = Utils.unescapeXml(
                            new StringBuilder(m.group(1)).append("fullimg.php").append(m.group(2)).toString());
                }
                return true;
            } else {
                return false;
            }
        }
    }
}
