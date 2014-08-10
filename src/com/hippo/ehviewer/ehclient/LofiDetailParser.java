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

import com.hippo.ehviewer.data.LargePreviewList;


public class LofiDetailParser {

    public boolean isLastPage;
    public LargePreviewList preview;

    public boolean parser(String body) {
        Pattern p;
        Matcher m;

        // Get preview
        p = Pattern.compile("<div class=\"gi\"[^>]+><a[^>]+href=\"([^>\"]+)\"[^>]+><img[^>]+src=\"([^>\"]+)\"[^>]+>");
        m = p.matcher(body);
        while (m.find()) {
            if (preview == null)
                preview = new LargePreviewList();
            preview.addItem(m.group(2), m.group(1));
        }
        // Check is last page
        p = Pattern.compile("<a[^>]+>Next Page &gt;</a>");
        m = p.matcher(body);
        if (m.find())
            isLastPage = false;
        else
            isLastPage = true;

        return preview != null;
    }
}
