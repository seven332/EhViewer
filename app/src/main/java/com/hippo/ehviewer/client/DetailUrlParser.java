/*
 * Copyright (C) 2014-2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client;

import com.hippo.util.AssertUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DetailUrlParser {

    public int gid;
    public String token;

    public void parser(String url) throws Exception {
        AssertUtils.assertNotNull("Url is null when parse detail url", url);

        Pattern p = Pattern.compile("/(\\d+)/(\\w+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            gid = Integer.parseInt(m.group(1));
            token = m.group(2);
        } else {
            throw new EhException("Can not parse detail url");
        }
    }
}
