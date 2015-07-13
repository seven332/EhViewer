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
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.client.data.NormalPreviewSet;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.data.TagGroup;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.yorozuya.NumberUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
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

    public static GalleryDetail parse(String body, int source, int request) throws Exception {
        switch (source) {
            default:
            case EhUrl.SOURCE_G:
            case EhUrl.SOURCE_EX: {
                return parse(body, request);
            }
            case EhUrl.SOURCE_LOFI: {
                //parseLofi(body, request);
                //break;
                throw new EhException("Not support lofi now");
            }
        }
    }

    private static GalleryDetail parse(String body, int request) throws EhException {
        if (!body.startsWith("<")) {
            throw new EhException(body);
        }

        if (body.contains(OFFENSIVE_STRING)) {
            throw new OffensiveException();
        }

        if (body.contains(PINING_STRING)) {
            throw new PiningException();
        }

        Pattern p;
        Matcher m;

        // TODO I can't remember what's this for
        p = Pattern.compile("<div class=\"d\">\n<p>([^<]+)</p>");
        m = p.matcher(body);
        if (m.find()) {
            throw new EhException(m.group(1));
        }

        GalleryDetail galleryDetail = new GalleryDetail();

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
                galleryDetail.gid = ParserUtils.parseInt(m.group(1));
                galleryDetail.token = ParserUtils.trim(m.group(2));
                galleryDetail.thumb = ParserUtils.trim(m.group(3));
                galleryDetail.title = ParserUtils.trim(m.group(4));
                galleryDetail.titleJpn = ParserUtils.trim(m.group(5));
                galleryDetail.torrentUrl = ParserUtils.trim(m.group(6));
                galleryDetail.torrentCount = ParserUtils.parseInt(m.group(7));
                galleryDetail.category = EhUtils.getCategory(ParserUtils.trim(m.group(8)));
                galleryDetail.uploader = ParserUtils.trim(m.group(9));
                galleryDetail.posted = ParserUtils.trim(m.group(10));
                galleryDetail.parent = ParserUtils.trim(m.group(11));
                galleryDetail.visible = ParserUtils.trim(m.group(12));
                galleryDetail.language = ParserUtils.trim(m.group(13));
                galleryDetail.size = ParserUtils.trim(m.group(14));
                galleryDetail.resize = ParserUtils.trim(m.group(15));
                galleryDetail.pageCount = ParserUtils.parseInt(m.group(16));

                String favTimeStr = ParserUtils.trim(m.group(17));
                switch (favTimeStr) {
                    case "Never":
                        galleryDetail.favoredTimes = 0;
                        break;
                    case "Once":
                        galleryDetail.favoredTimes = 1;
                        break;
                    default:
                        int index = favTimeStr.indexOf(' ');
                        if (index == -1) {
                            galleryDetail.favoredTimes = 0;
                        } else {
                            galleryDetail.favoredTimes = ParserUtils.parseInt(favTimeStr.substring(0, index));
                        }
                        break;
                }

                galleryDetail.ratedTimes = ParserUtils.parseInt(m.group(18));

                String ratingStr = ParserUtils.trim(m.group(19));
                if ("Not Yet Rated".equals(ratingStr)) {
                    galleryDetail.rating = Float.NaN;
                } else {
                    int index = ratingStr.indexOf(' ');
                    if (index == -1 || index >= ratingStr.length()) {
                        galleryDetail.rating = 0f;
                    } else {
                        galleryDetail.rating = NumberUtils.parseFloatSafely(ratingStr.substring(index + 1), 0f);
                    }
                }

                galleryDetail.isFavored = "Favorite Gallery".equals(ParserUtils.trim(m.group(20)));
            } else {
                throw new ParseException("Parse gallery detail error", body);
            }
        }

        if ((request & REQUEST_TAG) != 0) {
            galleryDetail.tags = new LinkedList<>();
            p = Pattern.compile("<tr><td[^<>]+>([\\w\\s]+):</td><td>(?:<div[^<>]+><a[^<>]+>[\\w\\s]+</a></div>)+</td></tr>");
            m = p.matcher(body);
            while (m.find()) {
                TagGroup tagGroup = new TagGroup();
                tagGroup.groupName = ParserUtils.trim(m.group(1));
                parseTagGroup(tagGroup, m.group(0));
                galleryDetail.tags.add(tagGroup);
            }
        }

        // Get preview info
        if ((request & REQUEST_PREVIEW_INFO) != 0) {
            p = Pattern.compile("<td[^>]+><a[^>]+>([\\d,]+)</a></td><td[^>]+>(?:<a[^>]+>)?&gt;(?:</a>)?</td>");
            m = p.matcher(body);
            if (m.find()) {
                galleryDetail.previewPageCount = ParserUtils.parseInt(m.group(1));
            } else {
                galleryDetail.previewPageCount = 0;
            }
            if (galleryDetail.previewPageCount <= 0) {
                throw new ParseException("Parse preview page count error", body);
            }
        }

        // Get preview
        if ((request & REQUEST_PREVIEW) != 0) {
            PreviewSet previewSet = parsePreview(body);
            previewSet.setGid(galleryDetail.gid); // TODO What if not set REQUEST_DETAIL
            galleryDetail.previewSetArray = new PreviewSet[galleryDetail.previewPageCount];
            galleryDetail.previewSetArray[0] = previewSet;
        }

        // Get comment
        if ((request & REQUEST_COMMENT) != 0) {
            p = Pattern.compile("<div class=\"c3\">Posted on ([^<>]+) by: &nbsp; <a[^<>]+>([^<>]+)</a>.+?<div class=\"c6\"[^>]*>(.+?)</div><div class=\"c[78]\"");
            m = p.matcher(body);
            galleryDetail.comments = new LinkedList<>();
            while (m.find()) {
                String webDateString = ParserUtils.trim(m.group(1));
                Date date;
                try {
                    date = WEB_COMMENT_DATA_FORMAT.parse(webDateString);
                } catch (java.text.ParseException e) {
                    date = new Date(0l);
                }
                String outDateString = OUT_COMMENT_DATA_FORMAT.format(date);
                galleryDetail.comments.add(new Comment(outDateString, date, ParserUtils.trim(m.group(2)), m.group(3)));
            }
            Collections.sort(galleryDetail.comments, COMMENT_SORTER);
        }

        return galleryDetail;
    }

    private static void parseTagGroup(TagGroup tagGroup, String body) {
        Pattern p = Pattern.compile("<div[^<>]+><a[^<>]+>([\\w\\s]+)</a></div>");
        Matcher m = p.matcher(body);
        while (m.find()) {
            tagGroup.addTag(ParserUtils.trim(m.group(1)));
        }
    }

    public static PreviewSet parsePreview(String body, int source) throws EhException {
        switch (source) {
            default:
            case EhUrl.SOURCE_G:
            case EhUrl.SOURCE_EX: {
                return parsePreview(body);
            }
            case EhUrl.SOURCE_LOFI: {
                //parsePreviewLofi(body);
                //break;
                throw new EhException("Not support lofi now");
            }
        }
    }

    public static PreviewSet parsePreview(String body) {
        if (body.contains("<div class=\"gdtm\"")) {
            return parseNormalPreview(body);
        } else {
            return parseLargePreview(body);
        }
    }

    private static NormalPreviewSet parseNormalPreview(String body) {
        Pattern  p = Pattern.compile("<div[^<>]*class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*>");
        Matcher m = p.matcher(body);
        NormalPreviewSet normalPreviewSet = new NormalPreviewSet();
        while (m.find()) {
            normalPreviewSet.addItem(m.group(3), Integer.parseInt(m.group(4)), 0,
                    Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                    m.group(5));
        }
        return normalPreviewSet;
    }

    private static LargePreviewSet parseLargePreview(String body) {
        Pattern p = Pattern.compile("<div class=\"gdtl\".+?<a href=\"(.+?)\"><img.+?src=\"(.+?)\"");
        Matcher m = p.matcher(body);
        LargePreviewSet largePreviewSet = new LargePreviewSet();
        while (m.find()) {
            largePreviewSet.addItem(m.group(2), m.group(1));
        }
        return largePreviewSet;
    }

    static class CommentSort implements Comparator<Comment> {

        @Override
        public int compare(Comment c1, Comment c2) {
            return c1.date.compareTo(c2.date);
        }
    }
}
