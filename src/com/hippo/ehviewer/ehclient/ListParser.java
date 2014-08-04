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

import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.LofiGalleryInfo;
import com.hippo.ehviewer.util.Utils;

public class ListParser {

    public static final int TARGET_PAGE_IS_LAST = -1;

    public static final int ALL = 0x1;
    public static final int NOT_FOUND = 0x2;
    public static final int PARSER_ERROR = -1;
    public static final int INDEX_ERROR = -2;

    public int pageNum;

    public ArrayList<GalleryInfo> giList;

    /**
     * If NOT_FOUND maxPage is 0,
     * if can not get maxPage, set it Integer.MAX_VALUE
     *
     * @param pageContext
     * @param mode
     * @return
     */
    public int parser(String body, int mode) {

        Pattern p;
        Matcher m;

        giList = new ArrayList<GalleryInfo>(25);

        switch (mode) {
        case EhClient.MODE_LOFI:
            p = Pattern.compile("<td class=\"ii\"><a href=\"(.+?)\">" // detail url
                    + "<img src=\"(.+?)\".+?/>" // thumb url
                    + ".+?<a class=\"b\" href=\".+?\">(.+?)</a>" // title
                    + ".+?<td class=\"ik ip\">Posted:</td><td class=\"ip\">(.+?)</td>" // Posted and uploader
                    + "</tr><tr><td class=\"ik\">Category:</td><td>(.+?)</td>" // Category
                    + "</tr><tr><td class=\"ik\">Tags:</td><td>(.+?)</td>" // Tags
                    + "</tr><tr><td class=\"ik\">Rating:</td><td class=\"ir\">(.+?)</td>"); // rating
            m = p.matcher(body);
            DetailUrlParser dup = new DetailUrlParser();
            while (m.find()) {
                LofiGalleryInfo lgi = new LofiGalleryInfo();
                String[] pau = new String[2];

                if (dup.parser(m.group(1))) {
                    lgi.gid = dup.gid;
                    lgi.token = dup.token;
                } else {
                    continue;
                }
                lgi.thumb = Utils.htmlUnsescape(m.group(2));
                lgi.title = Utils.htmlUnsescape(m.group(3));
                if (getPostedAndUploader(m.group(4), pau)) {
                    lgi.posted = pau[0];
                    lgi.uploader = pau[1];
                } else {
                    continue;
                }
                lgi.category = EhClient.getType(m.group(5));
                String tags = m.group(6);
                if (tags.equals("-"))
                    lgi.lofiTags = null;
                else
                    lgi.lofiTags = tags.split(", ");
                String rating = m.group(7);
                if (rating.equals("-"))
                    lgi.rating = Float.NaN;
                else
                    lgi.rating = getStartNum(rating);
                lgi.generateSLang();

                giList.add(lgi);
            }

            if (giList.size() == 0) {
                if (body.contains("No hits found</div>")) {
                    pageNum = 0;
                    return NOT_FOUND;
                } else if (body.contains("No more hits found</div>")) {
                    return INDEX_ERROR;
                } else {
                    return PARSER_ERROR;
                }
            } else {
                if (!body.contains("Next Page &gt;</a>"))
                    pageNum = TARGET_PAGE_IS_LAST;
                else
                    pageNum = Integer.MAX_VALUE;
                return ALL;
            }

        case EhClient.MODE_G:
        case EhClient.MODE_EX:
        default:
            p = Pattern.compile("<a[^<>]+>([\\d]+)</a></td><td[^<>]+>(?:<a[^<>]+>)?&");
            m = p.matcher(body);
            if (m.find()) {
                pageNum = Integer.parseInt(m.group(1));
            } else if (body.contains("No hits found</p>")) {
                pageNum = 0;
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
            m = p.matcher(body);
            while (m.find()) {
                GalleryInfo gi = new GalleryInfo();

                gi.category = EhClient.getType(m.group(1));
                gi.posted = m.group(2);
                if (m.group(3) == null) {
                    gi.thumb = Utils.htmlUnsescape("http://"
                            + m.group(5).replace('~', '/'));
                    gi.title = Utils.htmlUnsescape(m.group(6));
                } else {
                    gi.thumb = Utils.htmlUnsescape(m.group(3));
                    gi.title = Utils.htmlUnsescape(m.group(4));
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
                gi.generateSLang();

                giList.add(gi);
            }
            return ALL;
        }
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

    private static final String PAU_SPACER = " by ";

    private boolean getPostedAndUploader(String raw, String[] pau) {
        int index = raw.indexOf(PAU_SPACER);
        if (index == -1) {
            return false;
        } else {
            pau[0] = raw.substring(0, index);
            pau[1] = raw.substring(index + PAU_SPACER.length());
            return true;
        }
    }

    private int getStartNum(String str) {
        int startNum = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '*')
                startNum++;
        }
        return startNum;
    }
}
