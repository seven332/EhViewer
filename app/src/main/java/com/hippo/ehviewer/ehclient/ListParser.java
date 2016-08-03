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
import com.hippo.ehviewer.util.EhUtils;
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

        if (body == null)
            return 0;

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
                lgi.thumb = Utils.unescapeXml(m.group(2));
                lgi.title = Utils.unescapeXml(m.group(3));
                if (getPostedAndUploader(m.group(4), pau)) {
                    lgi.posted = pau[0];
                    lgi.uploader = pau[1];
                } else {
                    continue;
                }
                lgi.category = EhUtils.getCategory(m.group(5));
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

            //Fixed RegExpr , Delete thumb matcher
            p = Pattern.compile("<td class=\"itdc\">(?:<a.+?>)?<img.+?alt=\"(.+?)\".+?/>(?:</a>)?</td>" // category
                    + "<td.+?>(.+?)</td>" // posted
                    + "<td.+?><div.+?><div.+?height:(\\d+)px; width:(\\d+)px\">"
                    + ".+?"
                    + "<div class=\"it5\"><a href=\"([^<>\"]+)\"[^<>]+>(.+?)</a></div>" // url and title
                    + ".+?"
                    + "<div class=\"ir it4r\" style=\"([^<>\"]+)\">" // rating
                    + ".+?"
                    + "<td class=\"itu\"><div><a.+?>(.+?)</a>"// uploader
                    );


            m = p.matcher(body);
            while (m.find()) {
                GalleryInfo gi = new GalleryInfo();
                gi.category = EhUtils.getCategory(m.group(1));
                gi.posted = m.group(2);
                gi.thumbHeight = Integer.parseInt(m.group(3));
                gi.thumbWidth = Integer.parseInt(m.group(4));

                /*
                if (m.group(5) == null) {
                    gi.thumb = Utils.unescapeXml("http://"
                            + m.group(7).replace('~', '/'));
                    gi.title = Utils.unescapeXml(m.group(8));
                } else {
                    gi.thumb = Utils.unescapeXml(m.group(5));
                    gi.title = Utils.unescapeXml(m.group(6));
                }
                */

                /*
                Pattern pattern = Pattern
                        .compile("/(\\d+)/(\\w+)");
                Matcher matcher = pattern.matcher(m.group(9));
                if (matcher.find()) {
                    gi.gid = Integer.parseInt(matcher.group(1));
                    gi.token = matcher.group(2);
                } else
                    continue;
                */
				
                String[] tempStr=m.group(5).split("/");
                gi.gid = Integer.parseInt(tempStr[4]);
                gi.token = tempStr[5];

                gi.title = m.group(6);
                gi.rating = Float.parseFloat(getRate(m.group(7)));
                gi.uploader = m.group(8);
                gi.generateSLang();

                giList.add(gi);
            }
            //thumbFinding
            ArrayList<String> thumbList = getThumbList(body);
            for(int i=0;i<giList.size();++i){
                giList.get(i).thumb=thumbList.get(i+1);
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

    //thumbFinder
    private ArrayList<String> getThumbList(String body){
        ArrayList<String> thumbList = new ArrayList<String>(26);
        Pattern qq = Pattern.compile("exhentai.org/t(.+?)\" alt=\"(.+?)\"");
        Matcher nn=qq.matcher(body);
        Pattern pp = Pattern.compile("inits~exhentai.org~t(.+?)~(.+?)</div>");
        Matcher mm=pp.matcher(body);

        while(nn.find()){
            thumbList.add("https://exhentai.org/t"+nn.group(1));
            System.out.println(nn.group(2));
        }
        while(mm.find()){
            thumbList.add("https://exhentai.org/t"+mm.group(1));
            System.out.println(mm.group(2));
        }
        return thumbList;
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
