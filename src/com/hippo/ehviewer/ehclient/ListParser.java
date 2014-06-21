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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import com.hippo.ehviewer.data.GalleryInfo;

public class ListParser {
    
    public static final int ALL = 0x1;
    public static final int NOT_FOUND = 0x2;
    public static final int PARSER_ERROR = -1;
    
    public int indexPerPage = 25;
    public int maxPage;
    
    public ArrayList<GalleryInfo> giList;
    
    public int parser(String pageContext) {
        
        Pattern p;
        Matcher m;
        
        giList = new ArrayList<GalleryInfo>(indexPerPage);
        
        p = Pattern.compile("<a[^<>]+>([\\d]+)</a></td><td[^<>]+>(?:<a[^<>]+>)?&");
        m = p.matcher(pageContext);
        if (m.find()) {
            maxPage = Integer.parseInt(m.group(1));
        } else if (pageContext.contains("No hits found</p>")) {
            maxPage = 0;
            return NOT_FOUND;
        } else {
            return PARSER_ERROR;
        }
        
        p = Pattern.compile("<td class=\"itdc\">(?:<a.+?>)?<img.+?alt=\"(.+?)\".+?/>(?:</a>)?</td>" // category
                + "<td.+?>(.+?)</td>" // posted
                + "<td.+?><div.+?><div.+?>"
                + "(?:<img.+?src=\"(.+?)\".+?alt=\"(.+?)\" style.+?/>"
                + "|init~([^<>\"~]+~[^<>\"~]+)~([^<>]+))" // thumb and title
                + "</div>"
                + ".+?"
                + "<div class=\"it5\"><a href=\"([^<>\"]+)\"[^<>]+>(.+?)</a></div>" // url and title
                + ".+?"
                + "<div class=\"ir it4r\" style=\"([^<>\"]+)\">" // rating
                + ".+?"
                + "<td class=\"itu\"><div><a.+?>(.+?)</a>"); // uploader
        m = p.matcher(pageContext);
        while (m.find()) {
            GalleryInfo gi = new GalleryInfo();
            
            gi.category = EhClient.getType(m.group(1));
            gi.posted = m.group(2);
            if (m.group(3) == null) {
                gi.thumb = "http://"
                        + m.group(5).replace('~', '/');
                gi.title = m.group(6);
            } else {
                gi.thumb = m.group(3);
                gi.title = m.group(4);
            }
            
            Pattern pattern = Pattern
                    .compile("/(\\d+)/(\\w+)");
            Matcher matcher = pattern.matcher(m.group(7));
            if (matcher.find()) {
                gi.gid = Integer.parseInt(matcher.group(1));
                gi.token = matcher.group(2);
            } else
                continue;
            
            gi.rating = Float.parseFloat(getRate(m.group(9)));
            gi.uploader = m.group(10);
            giList.add(gi);
        }
        indexPerPage = giList.size();
        return ALL;
    }
    
    private String getRate(String rawRate) {
        Pattern p = Pattern.compile("\\d+px");
        Matcher m = p.matcher(rawRate);
        int num1;
        int num2;
        int rate = 5;
        String re;
        if (m.find())
            num1 = Integer.parseInt(m.group().replace("px", ""));
        else
            return null;
        if (m.find())
            num2 = Integer.parseInt(m.group().replace("px", ""));
        else
            return null;
        rate = rate - num1 / 16;
        if (num2 == 21) {
            rate--;
            re = Integer.toString(rate);
            re = re + ".5";
        } else
            re = Integer.toString(rate);
        return re;
    }
}
