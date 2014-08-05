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

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.LargePreviewList;
import com.hippo.ehviewer.data.NormalPreviewList;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.util.Utils;

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
    public static final int ERROR = 0x80;

    private static final CommentSort cs = new CommentSort();

    public String eMesg;
    public String thumb;
    public String title;
    public String title_jpn;
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
    /**
     * If no comment, just an empty list
     **/
    public LinkedList<Comment> comments;

    public void reset() {
        eMesg = null;
        thumb = null;
        title = null;
        title_jpn = null;
        category = 0;
        uploader = null;
        posted = null;
        pages = 0;
        size = null;
        resized = null;
        parent = null;
        visible = null;
        language = null;
        people = 0;
        rating = 0;
        firstPage = null;
        previewPerPage = 0;
        previewSum = 0;
        tags = null;
        previewList = null;
        comments = null;
    }

    public int parser(String body, int mode) {
        int re = 0;
        Pattern p;
        Matcher m;

        if (!body.contains("<")) {
            eMesg = body;
            return ERROR;
        }

        if (body.contains(OFFENSIVE_STRING)) {
            return OFFENSIVE;
        }

        if (body.contains(PINING_STRING)) {
            return PINING;
        }

        // Get detail
        if ((mode & DETAIL) != 0) {
            p = Pattern
                    .compile("<div id=\"gd1\"><img src=\"([^\"]+)\"[^<>]+/></div>" //  thumb
                            + "</div>"
                            + "<div id=\"gd2\">"
                            + "<h1 id=\"gn\">([^<>]+)</h1>" // title
                            + "<h1 id=\"gj\">([^<>]*)</h1>" // title_jpn might be empty string
                            + "</div>"
                            + ".+"
                            + "<div id=\"gdc\"><a[^<>]+><[^<>]*alt=\"([\\w|\\-]+)\"[^<>]*/></a></div>" // category
                            + "<div id=\"gdn\"><a[^<>]+>([^<>]+)</a>" // uploader
                            + ".+"
                            + "<tr><td[^<>]*>Posted:</td><td[^<>]*>([\\w|\\-|\\s|:]+)</td></tr>" // posted
                            + "<tr><td[^<>]*>Images:</td><td[^<>]*>([\\d]+) @ ([\\w|\\.|\\s]+)</td></tr>" // pages and size
                            + "<tr><td[^<>]*>Resized:</td><td[^<>]*>([^<>]+)</td></tr>" // resized
                            + "<tr><td[^<>]*>Parent:</td><td[^<>]*>(?:<a[^<>]*>)?([^<>]+)(?:</a>)?</td></tr>" // parent
                            + "<tr><td[^<>]*>Visible:</td><td[^<>]*>([^<>]+)</td></tr>" // visible
                            + "<tr><td[^<>]*>Language:</td><td[^<>]*>([^<>]+)</td></tr>" // language
                            + ".+"
                            + "<td id=\"grt3\">\\(<span id=\"rating_count\">([\\d|,]+)</span>\\)</td>" // people
                            + "</tr>"
                            + "<tr><td[^<>]*>([^<>]+)</td>" // rating
                            + ".+"
                            + "<div id=\"gdt\"><div[^<>]*>(?:<div[^<>]*>)?<a[^<>]*href=\"([^<>\"]+)\"[^<>]*>"); // get firstPage
            m = p.matcher(body);
            if (m.find()) {
                re |= DETAIL;

                thumb = Utils.htmlUnsescape(m.group(1));
                title = Utils.htmlUnsescape(m.group(2));
                title_jpn = Utils.htmlUnsescape(m.group(3));
                category = EhClient.getType(m.group(4));
                uploader = m.group(5);
                posted = m.group(6);
                pages = Integer.parseInt(m.group(7));
                size = m.group(8);
                resized = m.group(9);
                parent = m.group(10);
                visible = m.group(11);
                language = m.group(12);
                people = Integer.parseInt(m.group(13).replace(",", ""));

                Pattern pattern = Pattern.compile("([\\d|\\.]+)");
                Matcher matcher = pattern.matcher(m.group(14));
                if (matcher.find())
                    rating = Float.parseFloat(matcher.group(1));
                else
                    rating = Float.NaN;

                firstPage = m.group(15);
            }
        }
        // Get tag
        if ((mode & TAG) != 0) {
            tags = new LinkedHashMap<String, LinkedList<SimpleEntry<String, Integer>>>();
            p = Pattern
                    .compile("<tr><td[^<>]*>([^<>]+):</td><td>(?:<div[^<>]*><a[^<>]*>[^<>]*</a>[^<>]*<span[^<>]*>\\d+</span>[^<>]*</div>)+</td></tr>");
            m = p.matcher(body);
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
        if ((mode & PREVIEW_INFO) != 0) {
            p = Pattern.compile("<p class=\"ip\">Showing ([\\d|,]+) - ([\\d|,]+) of ([\\d|,]+) images</p>");
            m = p.matcher(body);
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
        if ((mode & PREVIEW) != 0) {
            boolean isLargePreview = false;
            if (body.contains("<div class=\"gdtl\""))
                isLargePreview = true;

            if (isLargePreview) {
                p = Pattern.compile("<div class=\"gdtl\".+?<a href=\"(.+?)\"><img.+?src=\"(.+?)\"");
                m = p.matcher(body);
                while (m.find()) {
                    if (previewList == null) {
                        re |= PREVIEW;
                        previewList = new LargePreviewList();
                    }
                    ((LargePreviewList)previewList).addItem(m.group(2), m.group(1));
                }
            } else {
                p = Pattern.compile("<div[^<>]*class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*>");
                m = p.matcher(body);
                while (m.find()) {
                    if (previewList == null) {
                        re |= PREVIEW;
                        previewList = new NormalPreviewList();
                    }
                    ((NormalPreviewList)previewList).addItem(m.group(3), m.group(4), "0", m.group(1),
                            m.group(2), m.group(5));
                }
            }
        }
        // Get comment
        if ((mode & COMMENT) != 0) {
            p = Pattern
                    .compile("<div class=\"c3\">Posted on ([^<>]+) by <a[^<>]+>([^<>]+)</a>.*?<div class=\"c6\">(.*?)</div>");
            m = p.matcher(body);
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
