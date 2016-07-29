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

import com.hippo.ehviewer.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EdDetailParser {

    int previewPageNum;
    int previewStartIndex;
    int imageNum;
    boolean isLastPage;
    List<String> pageTokenArray;
    String emsg;

    public boolean parser(String body, int mode, boolean needPreviewInfo) {
        Pattern p;
        Matcher m;

        if (body == null)
            return false;

        if (!body.contains("<"))
            emsg = body;

        if (mode == EhClient.MODE_LOFI) {
            // Check is last page
            p = Pattern.compile("<a[^>]+>Next Page &gt;</a>");
            m = p.matcher(body);
            if (m.find())
                isLastPage = false;
            else
                isLastPage = true;

            // Parser preview
            p = Pattern.compile("<div class=\"gi\"[^>]*><a href=\"https?://lofi.e-hentai.org/s/(\\w+)/\\d+-(\\d+)\"[^>]*>");
            m = p.matcher(body);
            boolean first = true;
            while(m.find()) {
                if (first) {
                    pageTokenArray = new LinkedList<String>();
                    previewStartIndex = Integer.parseInt(m.group(2)) - 1;
                    first = false;
                }
                pageTokenArray.add(m.group(1));
            }
            if (first)
                return false;
            else
                return true;

        } else {
            // Parser preview info
            if (needPreviewInfo) {
                p = Pattern.compile("<td[^>]+><a[^>]+>([\\d,]+)</a></td><td[^>]+>(?:<a[^>]+>)?&gt;(?:</a>)?</td>");
                m = p.matcher(body);
                if (m.find())
                    previewPageNum = Integer.valueOf(m.group(1).replace(",", ""));
                else {
                    Log.e("TAG", "get needPreviewInfo failed");
                    return false;
                }

                p = Pattern.compile("<tr><td[^<>]*>Length:</td><td[^<>]*>([\\d|,]+) pages</td></tr>");
                m = p.matcher(body);
                if (m.find())
                    imageNum = Integer.valueOf(m.group(1).replace(",", ""));
                else {
                    Log.e("TAG", "get needPreviewInfo failed");
                    return false;
                }
            }

            // Parser preview

            boolean first = true;
            if (body.contains("<div class=\"gdtl\"")) {
                p = Pattern.compile("<div class=\"gdtl\"[^>]*><a href=\"https?://[^/]+/s/(\\w+)/\\d+-(\\d+)\">");
                m = p.matcher(body);
                while(m.find()) {
                    if (first) {
                        pageTokenArray = new LinkedList<String>();
                        previewStartIndex = Integer.parseInt(m.group(2)) - 1;
                        first = false;
                    }
                    pageTokenArray.add(m.group(1));
                }

            } else {
                p = Pattern.compile("<div class=\"gdtm\"[^>]*><div[^>]*><a href=\"https?://[^/]+/s/(\\w+)/\\d+-(\\d+)\">");
                m = p.matcher(body);
                while(m.find()) {
                    if (first) {
                        pageTokenArray = new LinkedList<String>();
                        previewStartIndex = Integer.parseInt(m.group(2)) - 1;
                        first = false;
                    }
                    pageTokenArray.add(m.group(1));
                }
            }



            if (first) {
                Log.e("TAG", "get gdtm");
                return false;
            } else
                return true;
        }
    }

}
