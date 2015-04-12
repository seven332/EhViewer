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

import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.LargePreviewList;
import com.hippo.ehviewer.data.NormalPreviewList;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public LinkedHashMap<String, LinkedList<String>> tags;
    public PreviewList previewList;
    public LinkedList<Comment> comments;
    public int torrentNumber;
    public String torrentUrl;

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
        torrentNumber = 0;
        torrentUrl = null;
    }

    public int parser(String body, int mode) {
        int re = 0;
        Pattern p;
        Matcher m;

        if (body == null)
            return 0;

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

        p = Pattern.compile("<div class=\"d\">\n<p>([^<]+)</p>");
        m = p.matcher(body);
        if (m.find()) {
            eMesg = m.group(1);
            return ERROR;
        }

        // Get detail
        if ((mode & DETAIL) != 0) {
            p = Pattern
                    .compile("<div id=\"gd1\"><img src=\"([^\"]+)\"[^<>]+></div>" //  thumb
                            + "</div>"
                            + "<div id=\"gd2\">"
                            + "<h1 id=\"gn\">([^<>]+)</h1>" // title
                            + "<h1 id=\"gj\">([^<>]*)</h1>" // title_jpn might be empty string
                            + "</div>"
                            + ".+"
                            + "<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Torrent Download \\( (\\d+) \\)</a>"
                            + ".+"
                            + "<div id=\"gdc\"><a[^<>]+><[^<>]*alt=\"([\\w|\\-]+)\"[^<>]*></a></div>" // category
                            + "<div id=\"gdn\"><a[^<>]+>([^<>]+)</a>" // uploader
                            + ".+"
                            + "<tr><td[^<>]*>Posted:</td><td[^<>]*>([\\w|\\-|\\s|:]+)</td></tr>" // posted
                            //+ "<tr><td[^<>]*>Images:</td><td[^<>]*>([\\d]+) @ ([\\w|\\.|\\s]+)</td></tr>" // pages and size
                            //+ "<tr><td[^<>]*>Resized:</td><td[^<>]*>([^<>]+)</td></tr>" // resized
                            + "<tr><td[^<>]*>Parent:</td><td[^<>]*>(?:<a[^<>]*>)?([^<>]+)(?:</a>)?</td></tr>" // parent
                            + "<tr><td[^<>]*>Visible:</td><td[^<>]*>([^<>]+)</td></tr>" // visible
                            + "<tr><td[^<>]*>Language:</td><td[^<>]*>([^<>]+)(?:<span[^<>]*>[^<>]*</span>)?</td></tr>" // language
                            + "<tr><td[^<>]*>File Size:</td><td[^<>]*>([^<>]+)(?:<span[^<>]*>([^<>]+)</span>)?</td></tr>" // File size and resize
                            + "<tr><td[^<>]*>Length:</td><td[^<>]*>([\\d|,]+) pages</td></tr>" // pages
                            + "<tr><td[^<>]*>Favorited:</td><[^<>]*>([^<>]+)</td></tr>" // Favorite times  ([\d|,]+) times or Never
                            + ".+"
                            + "<td id=\"grt3\"><span id=\"rating_count\">([\\d|,]+)</span></td>" // people
                            + "</tr>"
                            + "<tr><td[^<>]*>([^<>]+)</td>" // rating
                            + ".+"
                            + "<div id=\"gdt\"><div[^<>]*>(?:<div[^<>]*>)?<a[^<>]*href=\"([^<>\"]+)\"[^<>]*>"); // get firstPage
            m = p.matcher(body);
            if (m.find()) {
                re |= DETAIL;

                thumb = Utils.unescapeXml(m.group(1));
                title = Utils.unescapeXml(m.group(2));
                title_jpn = Utils.unescapeXml(m.group(3));
                torrentUrl = Utils.unescapeXml(m.group(4));
                torrentNumber = Utils.parseIntSafely(m.group(5), 0);
                category = EhUtils.getCategory(m.group(6));
                uploader = m.group(7);
                posted = m.group(8);
                parent = m.group(9);
                visible = m.group(10);
                language = Utils.unescapeXml(m.group(11)).trim();
                size = Utils.unescapeXml(m.group(12)).trim();
                resized = m.group(13);
                pages = Integer.parseInt(m.group(14).replace(",", ""));
                // favoriteTimes = m.group(15)
                people = Integer.parseInt(m.group(16).replace(",", ""));

                Pattern pattern = Pattern.compile("([\\d|\\.]+)");
                Matcher matcher = pattern.matcher(m.group(17));
                if (matcher.find())
                    rating = Float.parseFloat(matcher.group(1));
                else
                    rating = Float.NaN;

                firstPage = m.group(18);
            }
        }
        // Get tag
        if ((mode & TAG) != 0) {
            tags = new LinkedHashMap<>();
            p = Pattern
                    .compile("<tr><td[^<>]+>([\\w\\s]+):</td><td>(?:<div[^<>]+><a[^<>]+>[\\w\\s]+</a></div>)+</td></tr>");
            m = p.matcher(body);
            while (m.find()) {
                re |= TAG;
                String groupName = m.group(1);
                LinkedList<String> group = getTagGroup(m.group(0));
                if (group != null) {
                    tags.put(groupName, group);
                }
            }
        }

        // Get preview info
        if ((mode & PREVIEW_INFO) != 0) {
            p = Pattern.compile("<td[^>]+><a[^>]+>([\\d,]+)</a></td><td[^>]+>(?:<a[^>]+>)?&gt;(?:</a>)?</td>");
            m = p.matcher(body);
            if (m.find()) {
                re |= PREVIEW_INFO;
                previewSum = Integer.valueOf(m.group(1).replace(",", ""));
            }
        }

        // Get preview
        if ((mode & PREVIEW) != 0) {
            boolean isLargePreview = false;
            if (body.contains("<div class=\"gdtl\""))
                isLargePreview = true;

            if (isLargePreview) {
                previewList = new LargePreviewList();
                p = Pattern.compile("<div class=\"gdtl\".+?<a href=\"(.+?)\"><img.+?src=\"(.+?)\"");
                m = p.matcher(body);
                while (m.find()) {
                    re |= PREVIEW;
                    ((LargePreviewList)previewList).addItem(m.group(2), m.group(1));
                }
            } else {
                previewList = new NormalPreviewList();
                p = Pattern.compile("<div[^<>]*class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*>");
                m = p.matcher(body);
                while (m.find()) {
                    re |= PREVIEW;
                    ((NormalPreviewList)previewList).addItem(m.group(3), m.group(4), "0", m.group(1),
                            m.group(2), m.group(5));
                }
            }
            // Set previewPerPage
            if (previewList != null)
                previewPerPage = previewList.size();
        }
        // Get comment
        if ((mode & COMMENT) != 0) {
            p = Pattern
                    .compile("<div class=\"c3\">Posted on ([^<>]+) by: &nbsp; <a[^<>]+>([^<>]+)</a>.+?<div class=\"c6\"[^>]*>(.+?)</div><div class=\"c[78]\"");
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

    private LinkedList<String> getTagGroup(String pageContent) {
        LinkedList<String> list = new LinkedList<String>();
        Pattern p = Pattern.compile("<div[^<>]+><a[^<>]+>([\\w\\s]+)</a></div>");
        Matcher m = p.matcher(pageContent);
        while (m.find())
            list.add(m.group(1));
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
