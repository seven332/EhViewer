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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.PreviewList;

public class DetailParser {
    
    private static final String OFFENSIVE_STRING =
            "<p>(And if you choose to ignore this warning, you lose all rights to complain about it in the future.)</p>";
    private static final String PINING_STRING =
            "<p>This gallery is pining for the fjords.</p>";
    
    public static final int DETAIL = 0x1;
    public static final int TAG = 0x2;
    public static final int PREVIEW_INFO = 0x4;
    public static final int PREVIEW = 0x8;
    public static final int COMMENT = 0x10;
    public static final int OFFENSIVE = 0x20;
    public static final int PINING = 0x40;
    
    private static final CommentSort cs = new CommentSort();
    
    private int mMode;
    
    public int category;
    public String uploader;
    public String posted;
    public int pages;
    public String size;
    public String resized;
    public String parent;
    public String visible;
    public String language;
    public int people;
    public float rating;
    public String firstPage;
    public int previewPerPage;
    public int previewSum;
    public LinkedHashMap<String, LinkedList<SimpleEntry<String, Integer>>> tags;
    public PreviewList previewList;
    public LinkedList<Comment> comments;
    
    public void setMode(int mode) {
        mMode = mode;
    }
    
    public int parser(String pageContent) {
        int re = 0;
        Pattern p;
        Matcher m;
        
        if (pageContent.contains(OFFENSIVE_STRING)) {
            return OFFENSIVE;
        }
        
        if (pageContent.contains(PINING_STRING)) {
            return PINING;
        }
        
        // Get detail
        if ((mMode & DETAIL) != 0) {
            p = Pattern
                    .compile("<div id=\"gdc\"><a href=\"[^<>\"]+\"><img[^<>]*alt=\"([\\w|\\-]+)\"[^<>]*/></a></div><div id=\"gdn\"><a href=\"[^<>\"]+\">([^<>]+)</a>.+Posted:</td><td[^<>]*>([\\w|\\-|\\s|:]+)</td></tr><tr><td[^<>]*>Images:</td><td[^<>]*>([\\d]+) @ ([\\w|\\.|\\s]+)</td></tr><tr><td[^<>]*>Resized:</td><td[^<>]*>([^<>]+)</td></tr><tr><td[^<>]*>Parent:</td><td[^<>]*>(?:<a[^<>]*>)?([^<>]+)(?:</a>)?</td></tr><tr><td[^<>]*>Visible:</td><td[^<>]*>([^<>]+)</td></tr><tr><td[^<>]*>Language:</td><td[^<>]*>([^<>]+)</td></tr>(?:</tbody>)?</table></div><div[^<>]*><table>(?:<tbody>)?<tr><td[^<>]*>Rating:</td><td[^<>]*><div[^<>]*style=\"([^<>]+)\"[^<>]*><img[^<>]*></div></td><td[^<>]*>\\(<span[^<>]*>([\\d]+)</span>\\)</td></tr><tr><td[^<>]*>([^<>]+)</td>.+<p class=\"ip\">Showing ([\\d|,]+) - ([\\d|,]+) of ([\\d|,]+) images</p>.+<div id=\"gdt\"><div[^<>]*>(?:<div[^<>]*>)?<a[^<>]*href=\"([^<>\"]+)\"[^<>]*>");
            m = p.matcher(pageContent);
            if (m.find()) {
                re |= DETAIL;
                category = getType(m.group(1));
                uploader = m.group(2);
                posted = m.group(3);
                pages = Integer.parseInt(m.group(4));
                size = m.group(5);
                resized = m.group(6);
                parent = m.group(7);
                visible = m.group(8);
                language = m.group(9);
                people = Integer.parseInt(m.group(11));
                
                Pattern pattern = Pattern.compile("([\\d|\\.]+)");
                Matcher matcher = pattern.matcher(m.group(12));
                if (matcher.find())
                    rating = Float.parseFloat(matcher.group(1));
                else
                    rating = Float.NaN;
                
                firstPage = m.group(16);
            }
        }
        // Get tag
        if ((mMode & TAG) != 0) {
            tags = new LinkedHashMap<String, LinkedList<SimpleEntry<String, Integer>>>();
            p = Pattern
                    .compile("<tr><td[^<>]*>([^<>]+):</td><td>(?:<div[^<>]*><a[^<>]*>[^<>]*</a>[^<>]*<span[^<>]*>\\d+</span>[^<>]*</div>)+</td></tr>");
            m = p.matcher(pageContent);
            while (m.find()) {
                re |= TAG;
                String groupName = m.group(1);
                LinkedList<SimpleEntry<String, Integer>> group = getTagGroup(m.group(0));
                if (group != null) {
                    tags.put(groupName, group);
                }
            }
        }
        
        // Get preview info
        if ((mMode & PREVIEW_INFO) != 0) {
            p = Pattern.compile("<p class=\"ip\">Showing ([\\d|,]+) - ([\\d|,]+) of ([\\d|,]+) images</p>");
            m = p.matcher(pageContent);
            if (m.find()) {
                re |= PREVIEW_INFO;
                previewPerPage = Integer.parseInt(m.group(2).replace(",",
                        ""))
                        - Integer.parseInt(m.group(1).replace(",", ""))
                        + 1;
                int total = Integer.parseInt(m.group(3).replace(",", ""));
                previewSum = (total + previewPerPage - 1) / previewPerPage;
            }
        }
        // Get preview
        if ((mMode & PREVIEW) != 0) {
            p = Pattern
                    .compile("<div[^<>]*class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*>");
            m = p.matcher(pageContent);
            while (m.find()) {
                if (previewList == null) {
                    re |= PREVIEW;
                    previewList = new PreviewList();
                }
                previewList.addItem(m.group(3), m.group(4), "0", m.group(1),
                        m.group(2), m.group(5));
            }
        }
        // Get comment
        if ((mMode & COMMENT) != 0) {
            p = Pattern
                    .compile("<div class=\"c3\">Posted on ([^<>]+) by <a[^<>]+>([^<>]+)</a>.*?<div class=\"c6\">(.*?)</div>");
            m = p.matcher(pageContent);
            comments = new LinkedList<Comment>();
            while (m.find()) {
                re |= COMMENT;
                comments.add(new Comment(m.group(1), m.group(2), m.group(3)));
            }
            Collections.sort(comments, cs);
        }
        return re;
    }
    
    private LinkedList<SimpleEntry<String, Integer>> getTagGroup(String pageContent) {
        LinkedList<SimpleEntry<String, Integer>> list =
                new LinkedList<SimpleEntry<String, Integer>>();
        Pattern p = Pattern.compile("<a[^<>]*>([^<>]+)</a> \\(<span[^<>]*>([\\d|,]+)</span>\\)");
        Matcher m = p.matcher(pageContent);
        while (m.find())
            list.add(new SimpleEntry<String, Integer>(m.group(1),
                    Integer.parseInt(m.group(2).replace(",", ""))));
        
        if (list.size() == 0)
            return null;
        else
            return list;
    }
    
    private int getType(String rawType) {
        int type;
        if (rawType.equalsIgnoreCase("doujinshi"))
            type = ListUrls.DOUJINSHI;
        else if (rawType.equalsIgnoreCase("manga"))
            type = ListUrls.MANGA;
        else if (rawType.equalsIgnoreCase("artistcg"))
            type = ListUrls.ARTIST_CG;
        else if (rawType.equalsIgnoreCase("gamecg"))
            type = ListUrls.GAME_CG;
        else if (rawType.equalsIgnoreCase("western"))
            type = ListUrls.WESTERN;
        else if (rawType.equalsIgnoreCase("non-h"))
            type = ListUrls.NON_H;
        else if (rawType.equalsIgnoreCase("imageset"))
            type = ListUrls.IMAGE_SET;
        else if (rawType.equalsIgnoreCase("cosplay"))
            type = ListUrls.COSPLAY;
        else if (rawType.equalsIgnoreCase("asianporn"))
            type = ListUrls.ASIAN_PORN;
        else if (rawType.equalsIgnoreCase("misc"))
            type = ListUrls.MISC;
        else
            type = ListUrls.UNKNOWN;
        return type;
    }
    
    static class CommentSort implements Comparator<Comment> {
        private int compareNum(String n1, String n2, int median) {
            int re = 0;
            for (int i = 0; i < median; i++) {
                re *= 10;
                re += n1.charAt(i) - n2.charAt(i);
            }
            return re;
        }
        private int getMonth(String m) {
            if (m.equals("January"))
                return 1;
            else if (m.equals("February"))
                return 2;
            else if (m.equals("March"))
                return 3;
            else if (m.equals("April"))
                return 4;
            else if (m.equals("May"))
                return 5;
            else if (m.equals("June"))
                return 6;
            else if (m.equals("July"))
                return 7;
            else if (m.equals("August"))
                return 8;
            else if (m.equals("September"))
                return 9;
            else if (m.equals("October"))
                return 10;
            else if (m.equals("November"))
                return 11;
            else if (m.equals("December"))
                return 12;
            else
                return 0;
        }
        
        @Override
        public int compare(Comment c1, Comment c2) {
            int re = 0;
            Pattern p = Pattern.compile("(\\d{2}) (\\w+) (\\d{4}), (\\d{2}):(\\d{2})");
            Matcher m1 = p.matcher(c1.time);
            Matcher m2 = p.matcher(c2.time);
            if (!m1.find() || !m2.find())
                return 0;
            // year
            re += compareNum(m1.group(3), m2.group(3), 4);
            re <<= 4;
            // Month
            re += getMonth(m1.group(2)) - getMonth(m2.group(2));
            re <<= 5;
            // Day
            re += compareNum(m1.group(1), m2.group(1), 2);
            re <<= 5;
            // Hour
            re += compareNum(m1.group(4), m2.group(4), 2);
            re <<= 6;
            // Minute
            re += compareNum(m1.group(5), m2.group(5), 2);
            return re;
        }
    }
}
