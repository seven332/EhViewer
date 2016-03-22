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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryTagGroup;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.client.data.NormalPreviewSet;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.ehviewer.client.exception.OffensiveException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.exception.PiningException;
import com.hippo.util.JsoupUtils;
import com.hippo.yorozuya.NumberUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
                    + "<tr><td[^<>]*>([^<>]+)</td>", // 19 rating "Average: x.xx" or "Not Yet Rated"
            Pattern.DOTALL);

    private static final Pattern IS_FAVORED_PATTERN = Pattern.compile("<a id=\"favoritelink\"[^<>]*>(.+?)</a>"); // isFavored "Favorite Gallery" for favorite

    private static final Pattern TAG_GROUP_PATTERN = Pattern.compile("<tr><td[^<>]+>([\\w\\s]+):</td><td>(?:<div[^<>]+><a[^<>]+>[\\w\\s]+</a></div>)+</td></tr>");
    private static final Pattern TAG_PATTERN = Pattern.compile("<div[^<>]+><a[^<>]+>([\\w\\s]+)</a></div>");


    private static final Pattern COMMENT_PATTERN = Pattern.compile("<div class=\"c3\">Posted on ([^<>]+) by: &nbsp; <a[^<>]+>([^<>]+)</a>.+?<div class=\"c6\"[^>]*>(.+?)</div><div class=\"c[78]\"");

    public static final Pattern PAGES_PATTERN = Pattern.compile("<tr><td[^<>]*>Length:</td><td[^<>]*>([\\d,]+) pages</td></tr>");
    public static final Pattern PREVIEW_PAGES_PATTERN = Pattern.compile("<td[^>]+><a[^>]+>([\\d,]+)</a></td><td[^>]+>(?:<a[^>]+>)?&gt;(?:</a>)?</td>");
    private static final Pattern NORMAL_PREVIEW_PATTERN = Pattern.compile("<div class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*><img alt=\"([\\d,]+)\"");
    private static final Pattern LARGE_PREVIEW_PATTERN = Pattern.compile("<div class=\"gdtl\".+?<a href=\"(.+?)\"><img alt=\"([\\d,]+)\".+?src=\"(.+?)\"");

    private static final GalleryTagGroup[] EMPTY_GALLERY_TAG_GROUP_ARRAY = new GalleryTagGroup[0];
    private static final GalleryComment[] EMPTY_GALLERY_COMMENT_ARRAY = new GalleryComment[0];

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
        Document document = Jsoup.parse(body);
        galleryDetail.tags = parseTagGroups(document);
        galleryDetail.comments = parseComments(document);
        galleryDetail.previewPages = parsePreviewPages(document, body);
        galleryDetail.previewSet = parsePreviewSet(body);
        return galleryDetail;
    }

    public static void parseDetail(String body, GalleryDetail gd) throws EhException {
        Matcher m = DETAIL_PATTERN.matcher(body);
        if (!m.find()) {
            throw new ParseException("Parse gallery detail error", body);
        }

        gd.gid = ParserUtils.parseLong(m.group(1));
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

        m = IS_FAVORED_PATTERN.matcher(body);
        gd.isFavored = m.find() && "Favorite Gallery".equals(ParserUtils.trim(m.group(1)));
    }

    @Nullable
    private static GalleryTagGroup parseTagGroup(Element element) {
        try {
            GalleryTagGroup group = new GalleryTagGroup();

            String nameSpace = element.child(0).text();
            // Remove last ':'
            nameSpace = nameSpace.substring(0, nameSpace.length() - 1);
            group.groupName = nameSpace;

            Elements tags = element.child(1).children();
            for (int i = 0, n = tags.size(); i < n; i++) {
                String tag = tags.get(i).text();
                // Sometimes parody tag is followed with '|' and english translate, just remove them
                int index = tag.indexOf('|');
                if (index >= 0) {
                    tag = tag.substring(0, index);
                }
                group.addTag(tag);
            }

            return group.size() > 0 ? group : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse tag groups with html parser
     */
    @NonNull
    public static GalleryTagGroup[] parseTagGroups(Document document) {
        try {
            Element taglist = document.getElementById("taglist");
            Elements tagGroups = taglist.child(0).child(0).children();

            List<GalleryTagGroup> list = new ArrayList<>(tagGroups.size());
            for (int i = 0, n = tagGroups.size(); i < n; i++) {
                GalleryTagGroup group = parseTagGroup(tagGroups.get(i));
                if (null != group) {
                    list.add(group);
                }
            }
            return list.toArray(new GalleryTagGroup[list.size()]);
        } catch (Exception e) {
            e.printStackTrace();
            return EMPTY_GALLERY_TAG_GROUP_ARRAY;
        }
    }

    /**
     * Parse tag groups with regular expressions
     */
    @NonNull
    private static GalleryTagGroup[] parseTagGroups(String body) throws EhException {
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

    @Nullable
    @SuppressWarnings("ConstantConditions")
    public static GalleryComment parseComment(Element element) {
        try {
            GalleryComment comment = new GalleryComment();
            Element c3 = JsoupUtils.getElementByClass(element, "c3");
            String temp = c3.ownText();
            temp = temp.substring("Posted on ".length(), temp.length() - " by:  ".length());
            comment.time = WEB_COMMENT_DATE_FORMAT.parse(temp).getTime();
            comment.user = c3.child(0).text();
            comment.comment = JsoupUtils.getElementByClass(element, "c6").html();
            return comment;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse comments with html parser
     */
    @NonNull
    public static GalleryComment[] parseComments(Document document) {
        try {
            Element cdiv = document.getElementById("cdiv");
            Elements c1s = cdiv.getElementsByClass("c1");

            List<GalleryComment> list = new ArrayList<>(c1s.size());
            for (int i = 0, n = c1s.size(); i < n; i++) {
                GalleryComment comment = parseComment(c1s.get(i));
                if (null != comment) {
                    list.add(comment);
                }
            }
            return list.toArray(new GalleryComment[list.size()]);
        } catch (Exception e) {
            e.printStackTrace();
            return EMPTY_GALLERY_COMMENT_ARRAY;
        }
    }

    /**
     * Parse comments with regular expressions
     */
    @NonNull
    public static GalleryComment[] parseComments(String body) {
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

    /**
     * Parse preview pages with html parser
     */
    public static int parsePreviewPages(Document document, String body) throws ParseException {
        try {
            Elements elements = document.getElementsByClass("ptt").first().child(0).child(0).children();
            return Integer.parseInt(elements.get(elements.size() - 2).text());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParseException("Can't parse preview pages", body);
        }
    }

    /**
     * Parse preview pages with regular expressions
     */
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

    /**
     * Parse pages with regular expressions
     */
    public static int parsePages(String body) throws ParseException {
        Matcher m = PAGES_PATTERN.matcher(body);
        if (m.find()) {
            return ParserUtils.parseInt(m.group(1));
        } else {
            throw new ParseException("Parse pages error", body);
        }
    }

    public static PreviewSet parsePreviewSet(String body) throws ParseException {
        try {
            return parseLargePreviewSet(body);
        } catch (ParseException e) {
            return parseNormalPreviewSet(body);
        }
    }

    /**
     * Parse large previews with regular expressions
     */
    private static LargePreviewSet parseLargePreviewSet(Document document, String body) throws ParseException {
        try {
            LargePreviewSet largePreviewSet = new LargePreviewSet();
            Element gdt = document.getElementById("gdt");
            Elements gdtls = gdt.getElementsByClass("gdtl");
            int n = gdtls.size();
            if (n <= 0) {
                throw new ParseException("Can't parse large preview", body);
            }
            for (int i = 0; i < n; i++) {
                Element element = gdtls.get(i).child(0);
                String pageUrl = element.attr("href");
                element = element.child(0);
                String imageUrl = element.attr("src");
                int index = Integer.parseInt(element.attr("alt")) - 1;
                largePreviewSet.addItem(index, imageUrl, pageUrl);
            }
            return largePreviewSet;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParseException("Can't parse large preview", body);
        }
    }

    /**
     * Parse large previews with regular expressions
     */
    private static LargePreviewSet parseLargePreviewSet(String body) throws ParseException {
        Matcher m = LARGE_PREVIEW_PATTERN.matcher(body);
        LargePreviewSet largePreviewSet = new LargePreviewSet();

        while (m.find()) {
            largePreviewSet.addItem(ParserUtils.parseInt(m.group(2)) - 1,
                    ParserUtils.trim(m.group(3)), ParserUtils.trim(m.group(1)));
        }

        if (largePreviewSet.size() == 0) {
            throw new ParseException("Can't parse large preview", body);
        }

        return largePreviewSet;
    }

    /**
     * Parse normal previews with regular expressions
     */
    private static NormalPreviewSet parseNormalPreviewSet(String body) throws ParseException {
        Matcher m = NORMAL_PREVIEW_PATTERN.matcher(body);
        NormalPreviewSet normalPreviewSet = new NormalPreviewSet();
        while (m.find()) {
            normalPreviewSet.addItem(ParserUtils.parseInt(m.group(6)) - 1,
                    ParserUtils.trim(m.group(3)), ParserUtils.parseInt((m.group(4))), 0,
                    ParserUtils.parseInt(m.group(1)), ParserUtils.parseInt(m.group(2)),
                    ParserUtils.trim(m.group(5)));
        }

        if (normalPreviewSet.size() == 0) {
            throw new ParseException("Can't parse large preview", body);
        }

        return normalPreviewSet;
    }
}
