/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client.parser;

import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryTagGroup;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.ehviewer.client.exception.OffensiveException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.exception.PiningException;
import com.hippo.yorozuya.NumberUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryDetailParser {

    private static final DateFormat WEB_COMMENT_DATE_FORMAT = new SimpleDateFormat("dd MMMMM yyyy, HH:mm z", Locale.US);

    private static final Pattern ERROR_PATTERN = Pattern.compile("<div class=\"d\">\n<p>([^<]+)</p>");
    private static final Pattern DETAIL_PATTERN = Pattern.compile(
            "var gid = (\\d+)" // 1 gid
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
                    + "<div id=\"gdc\"><a[^<>]+><[^<>]*alt=\"([\\w\\-]+)\"[^<>]*></a></div>" // 8 category
                    + "<div id=\"gdn\"><a[^<>]+>([^<>]+)</a>" // 9 uploader
                    + ".+?"
                    + "<tr><td[^<>]*>Posted:</td><td[^<>]*>([\\w\\-\\s:]+)</td></tr>" // 10 posted
                    + "<tr><td[^<>]*>Parent:</td><td[^<>]*>(?:<a[^<>]*>)?([^<>]+)(?:</a>)?</td></tr>" // 11 parent
                    + "<tr><td[^<>]*>Visible:</td><td[^<>]*>([^<>]+)</td></tr>" // 12 visible
                    + "<tr><td[^<>]*>Language:</td><td[^<>]*>([^<>]+)(?:<span[^<>]*>[^<>]*</span>)?</td></tr>" // 13 language
                    + "<tr><td[^<>]*>File Size:</td><td[^<>]*>([^<>]+)(?:<span[^<>]*>([^<>]+)</span>)?</td></tr>" // 14 File size, 15 resize
                    + "<tr><td[^<>]*>Length:</td><td[^<>]*>([\\d,]+) pages</td></tr>" // 16 pageCount
                    + "<tr><td[^<>]*>Favorited:</td><[^<>]*>([^<>]+)</td></tr>" // 17 Favorite times "([\d,]+) times" or "Once" or "Never"
                    + ".+?"
                    + "<td id=\"grt3\"><span id=\"rating_count\">([\\d,]+)</span></td>" // 18 ratedTimes
                    + "</tr>"
                    + "<tr><td[^<>]*>([^<>]+)</td>" // 19 rating "Average: x.xx" or "Not Yet Rated"
                    + ".+?"
                    + "<a id=\"favoritelink\"[^<>]*>(.+?)</a>", Pattern.DOTALL); // 20 isFavored "Favorite Gallery" for favorite
    private static final Pattern TAG_GROUP_PATTERN = Pattern.compile("<tr><td[^<>]+>([\\w\\s]+):</td><td>(?:<div[^<>]+><a[^<>]+>[\\w\\s]+</a></div>)+</td></tr>");
    private static final Pattern TAG_PATTERN = Pattern.compile("<div[^<>]+><a[^<>]+>([\\w\\s]+)</a></div>");


    private static final Pattern COMMENT_PATTERN = Pattern.compile("<div class=\"c3\">Posted on ([^<>]+) by: &nbsp; <a[^<>]+>([^<>]+)</a>.+?<div class=\"c6\"[^>]*>(.+?)</div><div class=\"c[78]\"");

    public static final Pattern PAGES_PATTERN = Pattern.compile("<tr><td[^<>]*>Length:</td><td[^<>]*>([\\d,]+) pages</td></tr>");
    public static final Pattern PREVIEW_PAGES_PATTERN = Pattern.compile("<td[^>]+><a[^>]+>([\\d,]+)</a></td><td[^>]+>(?:<a[^>]+>)?&gt;(?:</a>)?</td>");
    private static final Pattern NORMAL_PREVIEW_PATTERN = Pattern.compile("<div[^<>]*class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*>");
    private static final Pattern LARGE_PREVIEW_PATTERN = Pattern.compile("<div class=\"gdtl\".+?<a href=\"(.+?)\"><img alt=\"([\\d,]+)\".+?src=\"(.+?)\"");

    static {
        WEB_COMMENT_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String OFFENSIVE_STRING =
            "<p>(And if you choose to ignore this warning, you lose all rights to complain about it in the future.)</p>";
    private static final String PINING_STRING =
            "<p>This gallery is pining for the fjords.</p>";

    public static GalleryDetail parse(String body) throws EhException {
        if (body.contains(OFFENSIVE_STRING)) {
            throw new OffensiveException();
        }

        if (body.contains(PINING_STRING)) {
            throw new PiningException();
        }

        // Error info
        Matcher m = ERROR_PATTERN.matcher(body);
        if (m.find()) {
            throw new EhException(m.group(1));
        }

        GalleryDetail galleryDetail = new GalleryDetail();
        parseDetail(body, galleryDetail);
        galleryDetail.tags = parseTagGroup(body);
        galleryDetail.comments = parseComment(body);
        galleryDetail.previewPages = parsePreviewPages(body);
        galleryDetail.previewSet = parsePreview(body);
        return galleryDetail;
    }

    public static void parseDetail(String body, GalleryDetail gd) throws EhException {
        Matcher m = DETAIL_PATTERN.matcher(body);
        if (!m.find()) {
            throw new ParseException("Parse gallery detail error", body);
        }

        gd.gid = ParserUtils.parseInt(m.group(1));
        gd.token = ParserUtils.trim(m.group(2));
        gd.thumb = ParserUtils.trim(m.group(3));
        gd.title = ParserUtils.trim(m.group(4));
        gd.titleJpn = ParserUtils.trim(m.group(5));
        gd.torrentUrl = ParserUtils.trim(m.group(6));
        gd.torrentCount = ParserUtils.parseInt(m.group(7));
        gd.category = EhUtils.getCategory(ParserUtils.trim(m.group(8)));
        gd.uploader = ParserUtils.trim(m.group(9));
        gd.posted = ParserUtils.trim(m.group(10));
        gd.parent = ParserUtils.trim(m.group(11));
        gd.visible = ParserUtils.trim(m.group(12));
        gd.language = ParserUtils.trim(m.group(13));
        gd.size = ParserUtils.trim(m.group(14));
        gd.resize = ParserUtils.trim(m.group(15));
        gd.pages = ParserUtils.parseInt(m.group(16));
        String favTimeStr = ParserUtils.trim(m.group(17));

        switch (favTimeStr) {
            case "Never":
                gd.favoredTimes = 0;
                break;
            case "Once":
                gd.favoredTimes = 1;
                break;
            default:
                int index = favTimeStr.indexOf(' ');
                if (index == -1) {
                    gd.favoredTimes = 0;
                } else {
                    gd.favoredTimes = ParserUtils.parseInt(favTimeStr.substring(0, index));
                }
                break;
        }

        gd.ratedTimes = ParserUtils.parseInt(m.group(18));

        String ratingStr = ParserUtils.trim(m.group(19));
        if ("Not Yet Rated".equals(ratingStr)) {
            gd.rating = Float.NaN;
        } else {
            int index = ratingStr.indexOf(' ');
            if (index == -1 || index >= ratingStr.length()) {
                gd.rating = 0f;
            } else {
                gd.rating = NumberUtils.parseFloatSafely(ratingStr.substring(index + 1), 0f);
            }
        }

        gd.isFavored = "Favorite Gallery".equals(ParserUtils.trim(m.group(20)));
    }


    public static GalleryTagGroup[] parseTagGroup(String body) throws EhException {
        List<GalleryTagGroup> list = new LinkedList<>();

        Matcher m = TAG_GROUP_PATTERN.matcher(body);
        while (m.find()) {
            GalleryTagGroup tagGroup = new GalleryTagGroup();
            tagGroup.groupName = ParserUtils.trim(m.group(1));
            parseGroup(tagGroup, m.group(0));
            list.add(tagGroup);
        }

        return list.toArray(new GalleryTagGroup[list.size()]);
    }

    private static void parseGroup(GalleryTagGroup tagGroup, String body) {
        Matcher m = TAG_PATTERN.matcher(body);
        while (m.find()) {
            tagGroup.addTag(ParserUtils.trim(m.group(1)));
        }
    }

    public static GalleryComment[] parseComment(String body) {
        List<GalleryComment> list = new LinkedList<>();

        Matcher m = COMMENT_PATTERN.matcher(body);
        while (m.find()) {
            String webDateString = ParserUtils.trim(m.group(1));
            Date date;
            try {
                date = WEB_COMMENT_DATE_FORMAT.parse(webDateString);
            } catch (java.text.ParseException e) {
                date = new Date(0L);
            }
            GalleryComment comment = new GalleryComment();
            comment.time = date.getTime();
            comment.user = ParserUtils.trim(m.group(2));
            comment.comment = m.group(3);
            list.add(comment);
        }

        return list.toArray(new GalleryComment[list.size()]);
    }

    public static int parsePreviewPages(String body) throws ParseException {
        Matcher m = PREVIEW_PAGES_PATTERN.matcher(body);
        int previewPages = -1;
        if (m.find()) {
            previewPages = ParserUtils.parseInt(m.group(1));
        }

        if (previewPages <= 0) {
            throw new ParseException("Parse preview page count error", body);
        }

        return previewPages;
    }

    public static int parsePages(String body) throws ParseException {
        Matcher m = PAGES_PATTERN.matcher(body);
        if (m.find()) {
            return ParserUtils.parseInt(m.group(1));
        } else {
            throw new ParseException("Parse pages error", body);
        }
    }

    public static LargePreviewSet parsePreview(String body) throws EhException {
        return parseLargePreview(body);
        /*
        if (body.contains("<div class=\"gdtm\"")) {
            throw new EhException("Not support normal preview now");
        } else {
            return parseLargePreview(body);
        }
        */
    }

    public static LargePreviewSet parseLargePreview(String body) {
        Matcher m = LARGE_PREVIEW_PATTERN.matcher(body);
        LargePreviewSet largePreviewSet = new LargePreviewSet();

        while (m.find()) {
            largePreviewSet.addItem(ParserUtils.parseInt(m.group(2)) - 1,
                    ParserUtils.trim(m.group(3)), ParserUtils.trim(m.group(1)));
        }
        return largePreviewSet;
    }
}
