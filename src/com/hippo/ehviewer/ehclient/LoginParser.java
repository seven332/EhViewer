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

public class LoginParser {
    public String displayname;
    
    public boolean parser(String body) {
        Pattern p = Pattern.compile("<p>You are now logged in as: (.+?)<br />");
        Matcher m = p.matcher(body);
        
        if (m.find()) {
            displayname = m.group(1);
            return true;
        } else {
            return false;
        }
    }
}
