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

import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.LofiGalleryInfo;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.util.AssertException;
import com.hippo.util.AssertUtils;
import com.hippo.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListParser {
    private static final String PARSE_MESSAGE = "Gallery list parser error";

    private static final int DEFAULT_LIST_SIZE = 25;

    public static final int CURRENT_PAGE_IS_LAST = -1;

    /**
     * If NOT FOUND, pageNum is 0.<br>
     * For lofi, we can not get pages number,
     * so pageNum is {@link java.lang.Integer#MAX_VALUE} when it is not the last page,
     * pageNum is {@link #CURRENT_PAGE_IS_LAST} when it is the last page
     */
    public int pageNum;

    public GalleryInfo[] giArray;

    private void parse(String body) throws EhException {
        Pattern p;
        Matcher m;

        // Get page number
        p = Pattern.compile("<a[^<>]+>([\\d]+)</a></td><td[^<>]+>(?:<a[^<>]+>)?&");
        m = p.matcher(body);
        if (m.find()) {
            pageNum = Integer.parseInt(m.group(1));
        } else if (body.contains("No hits found</p>")) {
            pageNum = 0;
        } else {
            // Can not get page number
            throw new EhException(PARSE_MESSAGE);
        }

        // Get gallery
        List<GalleryInfo> giList = new ArrayList<>(DEFAULT_LIST_SIZE);

        p = Pattern.compile("<td class=\"itdc\">(?:<a.+?>)?<img.+?alt=\"(.+?)\".+?/>(?:</a>)?</td>" // category
                + "<td.+?>(.+?)</td>" // posted
                + "<td.+?><div.+?><div.+?height:(\\d+)px; width:(\\d+)px\">"
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

            gi.category = EhUtils.getCategory(m.group(1));
            gi.posted = m.group(2);
            gi.thumbHeight = Integer.parseInt(m.group(3));
            gi.thumbWidth = Integer.parseInt(m.group(4));

            if (m.group(5) == null) {
                gi.thumb = Utils.unescapeXml("http://"
                        + m.group(7).replace('~', '/'));
                gi.title = Utils.unescapeXml(m.group(8));
            } else {
                gi.thumb = Utils.unescapeXml(m.group(5));
                gi.title = Utils.unescapeXml(m.group(6));
            }

            Pattern pattern = Pattern
                    .compile("/(\\d+)/(\\w+)");
            Matcher matcher = pattern.matcher(m.group(9));
            if (matcher.find()) {
                gi.gid = Integer.parseInt(matcher.group(1));
                gi.token = matcher.group(2);
            } else {
                continue;
            }

            gi.rating = Utils.parseFloatSafely(getRate(m.group(11)), Float.NaN);
            gi.uploader = m.group(12);
            gi.generateSLang();

            giList.add(gi);
        }

        if (giList.size() == 0) {
            throw new EhException(PARSE_MESSAGE);
        }

        giArray = giList.toArray(new GalleryInfo[giList.size()]);
    }

    private void parseLofi(String body) throws Exception {
        Pattern p;
        Matcher m;

        // Get gallery
        List<LofiGalleryInfo> lgiList = new ArrayList<>(DEFAULT_LIST_SIZE);
        p = Pattern.compile("<td class=\"ii\"><a href=\"(.+?)\">" // detail url
                + "<img src=\"(.+?)\".+?/>" // thumb url
                + ".+?<a class=\"b\" href=\".+?\">(.+?)</a>" // title
                + ".+?<td class=\"ik ip\">Posted:</td><td class=\"ip\">(.+?)</td>" // Posted and uploader
                + "</tr><tr><td class=\"ik\">Category:</td><td>(.+?)</td>" // Category
                + "</tr><tr><td class=\"ik\">Tags:</td><td>(.+?)</td>" // Tags
                + "</tr><tr><td class=\"ik\">Rating:</td><td class=\"ir\">(.+?)</td>"); // rating
        m = p.matcher(body);
        DetailUrlParser dup = new DetailUrlParser();
        String[] pau = new String[2];
        while (m.find()) {
            LofiGalleryInfo lgi = new LofiGalleryInfo();

            dup.parser(m.group(1));
            lgi.gid = dup.gid;
            lgi.token = dup.token;

            lgi.thumb = Utils.unescapeXml(m.group(2));
            lgi.title = Utils.unescapeXml(m.group(3));

            getPostedAndUploader(m.group(4), pau);
            lgi.posted = pau[0];
            lgi.uploader = pau[1];

            lgi.category = EhUtils.getCategory(m.group(5));
            String tags = m.group(6);
            if (tags.equals("-"))
                lgi.lofiTags = new String[0];
            else
                lgi.lofiTags = tags.split(", ");
            String rating = m.group(7);
            if (rating.equals("-"))
                lgi.rating = Float.NaN;
            else
                lgi.rating = getStartNum(rating);
            lgi.generateSLang();

            lgiList.add(lgi);
        }

        if (lgiList.size() == 0) {
            if (body.contains("No hits found</div>")) {
                pageNum = 0;
            } else if (body.contains("No more hits found</div>")) {
                throw new EhException("Index is out of range");
            } else {
                throw new EhException(PARSE_MESSAGE);
            }
        } else {
            if (!body.contains("Next Page &gt;</a>")) {
                pageNum = CURRENT_PAGE_IS_LAST;
            } else {
                pageNum = Integer.MAX_VALUE;
            }
        }
    }

    public void parse(String body, int source) throws Exception {
        AssertUtils.assertNotNull("Body is null when parse gallery list", body);

        switch (source) {
            default:
            case EhClient.SOURCE_G:
            case EhClient.SOURCE_EX: {
                parse(body);
                break;
            }
            case EhClient.SOURCE_LOFI: {
                parseLofi(body);
                break;
            }
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

    private void getPostedAndUploader(String raw, String[] pau) throws AssertException {
        int index = raw.indexOf(PAU_SPACER);
        AssertUtils.assertEqualsEx("Can not parse posted and uploader", index, -1);
        pau[0] = raw.substring(0, index);
        pau[1] = raw.substring(index + PAU_SPACER.length());
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
