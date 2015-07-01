/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.ehviewer.client;

import com.hippo.ehviewer.client.data.Comment;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.client.data.NormalPreviewSet;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.data.TagGroup;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.util.AssertUtils;
import com.hippo.util.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryDetailParser {

    private static final DateFormat WEB_COMMENT_DATA_FORMAT = new SimpleDateFormat("dd MMMMM yyyy, HH:mm z", Locale.US);
    private static final DateFormat OUT_COMMENT_DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    static {
        WEB_COMMENT_DATA_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        OUT_COMMENT_DATA_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String OFFENSIVE_STRING =
            "<p>(And if you choose to ignore this warning, you lose all rights to complain about it in the future.)</p>";
    private static final String PINING_STRING =
            "<p>This gallery is pining for the fjords.</p>";

    public static final int REQUEST_DETAIL = 0x1;
    public static final int REQUEST_TAG = 0x2;
    public static final int REQUEST_PREVIEW_INFO = 0x4;
    public static final int REQUEST_PREVIEW = 0x8;
    public static final int REQUEST_COMMENT = 0x10;

    private static final CommentSort COMMENT_SORTER = new CommentSort();

    public int gid;
    public String token;
    public String thumb;
    public String title;
    public String titleJpn;
    public int category;
    public String uploader;

    public int torrentCount;
    public String torrentUrl;

    public String posted;
    public String parent;
    public String visible;
    public String language;
    public String size;
    public String resize;
    public int pageCount;
    public int favoredTimes;
    public boolean isFavored;
    public float rating;
    public int ratedTimes;

    public List<TagGroup> tags;
    public List<Comment> comments;

    public int previewPageCount;

    public PreviewSet previewSet;

    private String sortOut(String str) {
        // Avoid null
        if (str == null) {
            str = "";
        }
        return Utils.unescapeXml(str).trim();
    }

    private int sortOutInteger(String str, int defaultValue) {
        return Utils.parseIntSafely(sortOut(str).replace(",", ""), defaultValue);
    }

    private void parseTagGroup(TagGroup tagGroup, String body) {
        Pattern p = Pattern.compile("<div[^<>]+><a[^<>]+>([\\w\\s]+)</a></div>");
        Matcher m = p.matcher(body);
        while (m.find()) {
            tagGroup.addTag(sortOut(m.group(1)));
        }
    }

    private void parseNormalPreview(String body) {
        Pattern  p = Pattern.compile("<div[^<>]*class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*>");
        Matcher m = p.matcher(body);
        NormalPreviewSet normalPreviewSet = new NormalPreviewSet();
        while (m.find()) {
            normalPreviewSet.addItem(m.group(3), Integer.parseInt(m.group(4)), 0,
                    Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                    m.group(5));
        }
        previewSet = normalPreviewSet;
    }

    private void parseLargePreview(String body) {
        Pattern p = Pattern.compile("<div class=\"gdtl\".+?<a href=\"(.+?)\"><img.+?src=\"(.+?)\"");
        Matcher m = p.matcher(body);
        LargePreviewSet largePreviewSet = new LargePreviewSet();
        while (m.find()) {
            largePreviewSet.addItem(m.group(2), m.group(1));
        }
        previewSet = largePreviewSet;
    }

    private void parsePreview(String body) {
        if (body.contains("<div class=\"gdtm\"")) {
            parseNormalPreview(body);
        } else {
            parseLargePreview(body);
        }
    }

    private void parse(String body, int request) throws EhException {
        Pattern p;
        Matcher m;

        if (!body.startsWith("<")) {
            throw new EhException(body);
        }

        if (body.contains(OFFENSIVE_STRING)) {
            throw new OffensiveException();
        }

        if (body.contains(PINING_STRING)) {
            throw new PiningException();
        }

        // TODO I can't remember what's this for
        p = Pattern.compile("<div class=\"d\">\n<p>([^<]+)</p>");
        m = p.matcher(body);
        if (m.find()) {
            throw new EhException(m.group(1));
        }

        if ((request & REQUEST_DETAIL) != 0) {
            p = Pattern.compile("var gid = (\\d+)" // 1 gid
                    + ".+?"
                    + "var token = \"([a-z0-9A]+)\"" // 2 token
                    + ".+?"
                    + "<div id=\"gd1\"><img src=\"([^\"]+)\"[^<>]+></div>" // 3 thumb
                    + "</div>"
                    + "<div id=\"gd2\">"
                    + "<h1 id=\"gn\">([^<>]+)</h1>" // 4 title
                    + "<h1 id=\"gj\">([^<>]*)</h1>" // 5 title_jpn might be empty string
                    + "</div>"
                    + ".+?"
                    + "<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Torrent Download \\( (\\d+) \\)</a>" // 6 torrentUrl, 7 torrentCount
                    + ".+?"
                    + "<div id=\"gdc\"><a[^<>]+><[^<>]*alt=\"([\\w|\\-]+)\"[^<>]*></a></div>" // 8 category
                    + "<div id=\"gdn\"><a[^<>]+>([^<>]+)</a>" // 9 uploader
                    + ".+?"
                    + "<tr><td[^<>]*>Posted:</td><td[^<>]*>([\\w|\\-|\\s|:]+)</td></tr>" // 10 posted
                    + "<tr><td[^<>]*>Parent:</td><td[^<>]*>(?:<a[^<>]*>)?([^<>]+)(?:</a>)?</td></tr>" // 11 parent
                    + "<tr><td[^<>]*>Visible:</td><td[^<>]*>([^<>]+)</td></tr>" // 12 visible
                    + "<tr><td[^<>]*>Language:</td><td[^<>]*>([^<>]+)(?:<span[^<>]*>[^<>]*</span>)?</td></tr>" // 13 language
                    + "<tr><td[^<>]*>File Size:</td><td[^<>]*>([^<>]+)(?:<span[^<>]*>([^<>]+)</span>)?</td></tr>" // 14 File size, 15 resize
                    + "<tr><td[^<>]*>Length:</td><td[^<>]*>([\\d|,]+) pages</td></tr>" // 16 pageCount
                    + "<tr><td[^<>]*>Favorited:</td><[^<>]*>([^<>]+)</td></tr>" // 17 Favorite times "([\d|,]+) times" or "Once" or "Never"
                    + ".+?"
                    + "<td id=\"grt3\"><span id=\"rating_count\">([\\d|,]+)</span></td>" // 18 ratedTimes
                    + "</tr>"
                    + "<tr><td[^<>]*>([^<>]+)</td>" // 19 rating "Average: x.xx" or "Not Yet Rated"
                    + ".+?"
                    + "<a id=\"favoritelink\"[^<>]*>(.+?)</a>", Pattern.DOTALL); // 20 isFavored "Favorite Gallery" for favorite
            m = p.matcher(body);
            if (m.find()) {
                gid = sortOutInteger(m.group(1), 0);
                token = sortOut(m.group(2));
                thumb = sortOut(m.group(3));
                title = sortOut(m.group(4));
                titleJpn = sortOut(m.group(5));
                torrentUrl = sortOut(m.group(6));
                torrentCount = sortOutInteger(m.group(7), 0);
                category = EhUtils.getCategory(sortOut(m.group(8)));
                uploader = sortOut(m.group(9));
                posted = sortOut(m.group(10));
                parent = sortOut(m.group(11));
                visible = sortOut(m.group(12));
                language = sortOut(m.group(13));
                size = sortOut(m.group(14));
                resize = sortOut(m.group(15));
                pageCount = sortOutInteger(m.group(16), 0);

                String favTimeStr = sortOut(m.group(17));
                switch (favTimeStr) {
                    case "Never":
                        favoredTimes = 0;
                        break;
                    case "Once":
                        favoredTimes = 1;
                        break;
                    default:
                        int index = favTimeStr.indexOf(' ');
                        if (index == -1) {
                            favoredTimes = 0;
                        } else {
                            favoredTimes = Utils.parseIntSafely(favTimeStr.substring(0, index), 0);
                        }
                        break;
                }

                ratedTimes = sortOutInteger(m.group(18), 0);

                String ratingStr = sortOut(m.group(19));
                if ("Not Yet Rated".equals(ratingStr)) {
                    rating = Float.NaN;
                } else {
                    int index = ratingStr.indexOf(' ');
                    if (index == -1 || index >= ratingStr.length()) {
                        rating = 0f;
                    } else {
                        rating = Utils.parseFloatSafely(ratingStr.substring(index + 1), 0f);
                    }
                }

                isFavored = "Favorite Gallery".equals(sortOut(m.group(20)));
            } else {
                throw new ParseException("Parse gallery body error", body);
            }
        }

        if ((request & REQUEST_TAG) != 0) {
            tags = new LinkedList<>();
            p = Pattern.compile("<tr><td[^<>]+>([\\w\\s]+):</td><td>(?:<div[^<>]+><a[^<>]+>[\\w\\s]+</a></div>)+</td></tr>");
            m = p.matcher(body);
            while (m.find()) {
                TagGroup tagGroup = new TagGroup();
                tagGroup.groupName = sortOut(m.group(1));
                parseTagGroup(tagGroup, m.group(0));
                tags.add(tagGroup);
            }
        }

        // Get preview info
        if ((request & REQUEST_PREVIEW_INFO) != 0) {
            p = Pattern.compile("<td[^>]+><a[^>]+>([\\d,]+)</a></td><td[^>]+>(?:<a[^>]+>)?&gt;(?:</a>)?</td>");
            m = p.matcher(body);
            if (m.find()) {
                previewPageCount = sortOutInteger(m.group(1), 0);
            } else {
                previewPageCount = 0;
            }
            if (previewPageCount <= 0) {
                throw new ParseException("Parse preview page count error", body);
            }
        }

        // Get preview
        if ((request & REQUEST_PREVIEW_INFO) != 0) {
            parsePreview(body);
        }

        // Get comment
        if ((request & REQUEST_COMMENT) != 0) {
            p = Pattern.compile("<div class=\"c3\">Posted on ([^<>]+) by: &nbsp; <a[^<>]+>([^<>]+)</a>.+?<div class=\"c6\"[^>]*>(.+?)</div><div class=\"c[78]\"");
            m = p.matcher(body);
            comments = new LinkedList<>();
            while (m.find()) {
                String webDateString = sortOut(m.group(1));
                Date date;
                try {
                    date = WEB_COMMENT_DATA_FORMAT.parse(webDateString);
                } catch (java.text.ParseException e) {
                    date = new Date(0l);
                }
                String outDateString = OUT_COMMENT_DATA_FORMAT.format(date);
                comments.add(new Comment(outDateString, date, sortOut(m.group(2)), m.group(3)));
            }
            Collections.sort(comments, COMMENT_SORTER);
        }

        if ((request & REQUEST_COMMENT) != 0) {

        }
    }

    public void parse(String body, int request, int source) throws Exception {
        AssertUtils.assertNotNull("Body is null when parse gallery list", body);

        switch (source) {
            default:
            case EhClient.SOURCE_G:
            case EhClient.SOURCE_EX: {
                parse(body, request);
                break;
            }
            case EhClient.SOURCE_LOFI: {
                //parseLofi(body, request);
                //break;
                throw new EhException("Not support lofi now");
            }
        }
    }

    static class CommentSort implements Comparator<Comment> {

        @Override
        public int compare(Comment c1, Comment c2) {
            return c1.date.compareTo(c2.date);
        }
    }
}
