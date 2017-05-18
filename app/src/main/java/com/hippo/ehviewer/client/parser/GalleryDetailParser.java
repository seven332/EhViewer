/*
 * Copyright 2017 Hippo Seven
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

/*
 * Created by Hippo on 5/17/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.CommentEntry;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.PreviewPage;
import com.hippo.ehviewer.client.data.TagSet;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.url.ArchiveUrlParser;
import com.hippo.ehviewer.client.parser.url.CategoryUrlParser;
import com.hippo.ehviewer.client.parser.url.GalleryDetailUrlParser;
import com.hippo.yorozuya.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class GalleryDetailParser {
  private GalleryDetailParser() {}

  private static final String LOG_TAG = GalleryDetailParser.class.getSimpleName();

  private static final Pattern PATTERN_GID = Pattern.compile("var gid = (\\d+);");
  private static final Pattern PATTERN_TOKEN = Pattern.compile("var token = \"(\\w+)\";");
  private static final Pattern PATTERN_COVER = Pattern.compile("width:(\\d+)px; height:(\\d+)px.+?url\\((.+?)\\)");
  private static final Pattern PATTERN_ARCHIVE = Pattern.compile("<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Archive Download</a>");
  private static final Pattern PATTERN_TORRENT = Pattern.compile("<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Torrent Download \\( (\\d+) \\)</a>");
  private static final Pattern PATTERN_NORMAL_PREVIEW = Pattern.compile("width:(\\d+).+?height:(\\d+).+?url\\((.+?)\\).+?-(\\d+)px");

  private static final String COMMENT_DATE_PREFIX = "Posted on ";
  private static final String COMMENT_DATE_SUFFIX = " by:";

  /**
   * Parses gallery detail page to a {@link GalleryInfo} object.
   * The {@code document} must be from the {@code body}.
   * <p>
   * Gid and token must be filled.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static GalleryInfo parseGalleryDetail(@NonNull String body, @NonNull Document document)
      throws ParseException {
    GalleryInfo info = new GalleryInfo();

    // Gid and token
    Matcher matcher = PATTERN_GID.matcher(body);
    if (matcher.find()) {
      info.gid = ParserUtils.parseLong(matcher.group(1), 0);
    }
    matcher = PATTERN_TOKEN.matcher(body);
    if (matcher.find()) {
      info.token = matcher.group(1);
    }
    if (info.gid == 0 || info.token == null) {
      throw new ParseException("Can't get gid or token", body);
    }

    // Title
    parseTitle(document, info);
    if (StringUtils.isEmpty(info.title) && StringUtils.isEmpty(info.titleJpn)) {
      throw new ParseException("Can't get title", body);
    }

    // Cover
    parseCover(document, info);
    // Archive and Torrent
    parseArchiveAndTorrent(document, info);
    // Info
    parseInfo(document, info);
    // Rating
    parseRating(document, info);
    // Favourite slot
    parseFavouriteSlot(document, info);
    // TagSet
    parseTagSet(document, info);
    // Child
    parseChild(document, info);

    return info;
  }

  private static void parseCover(Document document, GalleryInfo info) {
    Element gd1 = document.getElementById("gd1");
    if (gd1 == null) return;

    Element element = gd1.children().first();
    if (element == null) return;

    parseCoverStyle(element.attr("style"), info);
  }

  // width:250px; height:356px; background:transparent url(https://exhentai.org/t/fe/1f/fe1fcfa9bf8fba2f03982eda0aa347cc9d6a6372-145921-1050-1492-jpg_250.jpg) 0 0 no-repeat
  private static void parseCoverStyle(String str, GalleryInfo info) {
    Matcher matcher = PATTERN_COVER.matcher(str);
    if (!matcher.find()) return;

    int width = ParserUtils.parseInt(matcher.group(1), 0);
    int height = ParserUtils.parseInt(matcher.group(2), 0);
    if (width >= 0 && height >= 0) {
      info.coverRatio = (float) width / (float) height;
    }

    info.coverUrl = ParserUtils.unescape(matcher.group(3));
    info.cover = EhUrl.getImageFingerprint(info.coverUrl);
  }

  private static void parseTitle(Document document, GalleryInfo info) {
    Element gd2 = document.getElementById("gd2");
    if (gd2 == null) return;

    // Title
    Element gn = gd2.getElementById("gn");
    if (gn != null) {
      info.title = ParserUtils.unescape(gn.text());
    }

    // Japanese title
    Element gj = gd2.getElementById("gj");
    if (gj != null) {
      info.titleJpn = ParserUtils.unescape(gj.text());
    }
  }

  private static void parseArchiveAndTorrent(Document document, GalleryInfo info) {
    Element gd5 = document.getElementById("gd5");
    if (gd5 == null) return;

    String html = gd5.html();
    parseArchive(html, info);
    parseTorrent(html, info);
  }

  private static void parseArchive(String str, GalleryInfo info) {
    if (str == null) return;

    Matcher matcher = PATTERN_ARCHIVE.matcher(str);
    if (!matcher.find()) return;

    String url = ParserUtils.unescape(matcher.group(1));
    ArchiveUrlParser.Result result = ArchiveUrlParser.parser(url);
    if (result == null) return;

    if ((info.gid == 0 || info.gid == result.gid) &&
        (info.token == null || result.token.equals(info.token))) {
      info.gid = result.gid;
      info.token = result.token;
      info.archiverKey = result.archiverKey;
    } else {
      Log.w(LOG_TAG, "The gid and token in archive url do not match. "
          + "They are " + result.gid + ", " + result.token + ". "
          + "They should be " + result.gid + ", " + result.token + ".");
    }
  }

  private static void parseTorrent(String str, GalleryInfo info) {
    Matcher matcher = PATTERN_TORRENT.matcher(str);
    if (matcher.find()) {
      info.torrentCount = ParserUtils.parseInt(matcher.group(2), 0);
    }
  }


  private static void parseInfo(Document document, GalleryInfo info) {
    Element gd3 = document.getElementById("gd3");
    if (gd3 == null) return;

    // Category
    Element gdc = document.getElementById("gdc");
    if (gdc != null) {
      Element a = gdc.children().first();
      if (a != null) {
        String url = ParserUtils.unescape(a.attr("href"));
        info.category = CategoryUrlParser.parser(url);
      }
    }

    // Uploader
    Element gdn = document.getElementById("gdn");
    if (gdn != null) {
      info.uploader = ParserUtils.unescape(gdn.text());
    }

    // Info table
    Element gdd = document.getElementById("gdd");
    if (gdd != null) {
      Element tbody = gdd.getElementsByTag("tbody").first();
      if (tbody != null) {
        parseInfoTable(tbody, info);
      }
    }
  }

  private static void parseInfoTable(Element tbody, GalleryInfo info) {
    for (Element e : tbody.children()) {
      Elements es = e.children();
      if (es.size() < 2) {
        continue;
      }

      String key = ParserUtils.unescape(es.get(0).text());
      String value = ParserUtils.unescape(es.get(1).ownText());
      if (key.startsWith("Posted")) {
        // Date
        info.date = ParserUtils.parseDate(value, 0);
      } else if (key.startsWith("Parent")) {
        // Parent
        Element a = es.get(1).children().first();
        if (a != null) {
          String url = ParserUtils.unescape(a.attr("href"));
          GalleryDetailUrlParser.Result result = GalleryDetailUrlParser.parser(url);
          if (result != null) {
            info.parentGid = result.gid;
            info.parentToken = result.token;
          }
        }
      } else if (key.startsWith("Visible")) {
        // Invalid
        info.invalid = !value.equalsIgnoreCase("yes");
      } else if (key.startsWith("Language")) {
        // Language
        info.language = EhUtils.getLang(value);
      } else if (key.startsWith("File Size")) {
        // Size
        info.size = parseSize(value);
      } else if (key.startsWith("Length")) {
        // Pages
        int index = value.indexOf(' ');
        if (index >= 0) {
          info.pages = ParserUtils.parseInt(value.substring(0, index), 0);
        }
      } else if (key.startsWith("Favorited")) {
        // Favourited
        switch (value) {
          case "Never":
            info.favourited = 0;
            break;
          case "Once":
            info.favourited = 1;
            break;
          default:
            int index = value.indexOf(' ');
            if (index != -1) {
              info.favourited = ParserUtils.parseInt(value.substring(0, index), 0);
            }
            break;
        }
      }
    }
  }

  private static long parseSize(String str) {
    int index = str.indexOf(' ');
    if (index < 0) return 0;

    float num = ParserUtils.parseFloat(str.substring(0, index), 0.0f);
    if (num == 0.0f) return 0;

    str = str.substring(index + 1).toLowerCase();
    index = str.indexOf('b');
    if (index < 0) return 0;

    // XX.XX B
    if (index == 0) {
      return (long) num;
    }

    char ch = str.charAt(index - 1);
    switch (ch) {
      case 'k':
        // XX.XX KB
        return (long) (num * 1024);
      case 'm':
        // XX.XX MB
        return (long) (num * 1024 * 1024);
      case 'g':
        // XX.XX GB
        return (long) (num * 1024 * 1024 * 1024);
      default:
        return 0;
    }
  }

  private static void parseRating(Document document, GalleryInfo info) {
    Element gdr = document.getElementById("gdr");
    if (gdr == null) return;

    Element rating_label = gdr.getElementById("rating_label");
    if (rating_label != null) {
      String ratingStr = ParserUtils.unescape(rating_label.text());
      if (!"Not Yet Rated".equals(ratingStr)) {
        int index = ratingStr.indexOf(' ');
        if (index != -1) {
          info.rating = ParserUtils.parseFloat(ratingStr.substring(index + 1), 0.0f);
        }
      }
    }

    Element rating_count = gdr.getElementById("rating_count");
    if (rating_count != null) {
      info.rated = ParserUtils.parseInt(rating_count.text(), 0);
    }
  }

  private static void parseFavouriteSlot(Document document, GalleryInfo info) {
    Element fav = document.getElementById("fav");
    if (fav == null) return;

    Element div = fav.children().first();
    if (div == null) return;

    info.favouriteSlot = GalleryListParser.parseFavouriteSlot(div.attr("style"));
  }

  private static void parseTagSet(Document document, GalleryInfo info) {
    Element taglist = document.getElementById("taglist");
    if (taglist == null) return;

    Element tbody = taglist.getElementsByTag("tbody").first();
    if (tbody == null) return;

    TagSet tagSet = info.tagSet;
    for (Element tr : tbody.children()) {
      Elements children = tr.children();
      if (children.size() < 2) continue;

      String namespace = ParserUtils.unescape(children.get(0).text());
      if (StringUtils.isEmpty(namespace)) continue;

      if (namespace.endsWith(":")) {
        namespace = namespace.substring(0, namespace.length() - 1);
      }

      parseTags(children.get(1), namespace, tagSet);
    }
  }

  private static void parseTags(Element td, String namespace, TagSet tagSet) {
    for (Element div : td.children()) {
      String tag = ParserUtils.unescape(div.text());
      if (!StringUtils.isEmpty(tag)) {
        tagSet.add(namespace, tag);
      }
    }
  }

  private static void parseChild(Document document, GalleryInfo info) {
    Element gnd = document.getElementById("gnd");
    if (gnd == null) return;

    Element a = gnd.getElementsByTag("a").first();
    if (a == null) return;

    GalleryDetailUrlParser.Result result = GalleryDetailUrlParser.parser(a.attr("href"));
    if (result != null) {
      info.gid = result.gid;
      info.token = result.token;
    }
  }

  /**
   * Parses gallery detail page to preview pages.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static List<PreviewPage> parsePreviews(@NonNull String body,
      @NonNull Document document) throws ParseException {
    Element gdt = document.getElementById("gdt");
    if (gdt != null) {
      Elements gdts = gdt.children();
      Element first = gdts.first();
      if (first != null) {
        String className = first.className();
        if ("gdtl".equals(className)) {
          return parseLargePreviews(gdts, body);
        } else if ("gdtm".equals(className)) {
          return parseNormalPreviews(gdts, body);
        }
      }
    }
    throw new ParseException("Can't parse previews", body);
  }

  @NonNull
  private static List<PreviewPage> parseLargePreviews(Elements gdts, String body)
      throws ParseException {
    List<PreviewPage> list = new ArrayList<>();

    for (Element gdtl : gdts) {
      Element a = gdtl.children().first();
      if (a == null) continue;
      String url = ParserUtils.unescape(a.attr("href"));
      if (!ParserUtils.isUrl(url)) continue;

      Element img = a.children().first();
      if (img == null) continue;
      int index = ParserUtils.parseInt(img.attr("alt"), 0) - 1;
      if (index == -1) continue;
      String image = ParserUtils.unescape(img.attr("src"));
      if (!ParserUtils.isUrl(image)) continue;

      list.add(new PreviewPage(index, url, image));
    }

    if (!list.isEmpty()) {
      return list;
    } else {
      throw new ParseException("Can't parse large previews", body);
    }
  }

  @NonNull
  private static List<PreviewPage> parseNormalPreviews(Elements gdts, String body)
      throws ParseException {
    List<PreviewPage> list = new ArrayList<>();

    for (Element gdtm : gdts) {
      Element div = gdtm.children().first();
      if (div == null) continue;

      Matcher matcher = PATTERN_NORMAL_PREVIEW.matcher(div.attr("style"));
      if (!matcher.find()) continue;

      int width = ParserUtils.parseInt(matcher.group(1), -1);
      int height = ParserUtils.parseInt(matcher.group(2), -1);
      String image = ParserUtils.unescape(matcher.group(3));
      int clipLeft = ParserUtils.parseInt(matcher.group(4), -1);
      if (width == -1 || height == -1 || !ParserUtils.isUrl(image) || clipLeft == -1) continue;

      Element a = div.children().first();
      if (a == null) continue;
      String url = a.attr("href");
      if (!ParserUtils.isUrl(url)) continue;

      Element img = a.children().first();
      if (img == null) continue;
      int index = ParserUtils.parseInt(img.attr("alt"), 0) - 1;
      if (index == -1) continue;

      list.add(new PreviewPage(index, url, image, clipLeft, 0, width, height));
    }

    if (!list.isEmpty()) {
      return list;
    } else {
      throw new ParseException("Can't parse normal previews", body);
    }
  }

  /**
   * Parses gallery detail page to a list of comment.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static List<CommentEntry> parseComments(@NonNull String body,
      @NonNull Document document) throws ParseException {
    Element cdiv = document.getElementById("cdiv");
    if (cdiv != null) {
      List<CommentEntry> list = new ArrayList<>();

      for (Element c1 : cdiv.children()) {
        if (!"c1".equals(c1.className())) continue;
        CommentEntry entry = parseCommentEntry(c1);
        if (entry != null) {
          list.add(entry);
        }
      }

      if (!list.isEmpty()) {
        return list;
      }
    }

    throw new ParseException("Can't get comments", body);
  }

  @Nullable
  private static CommentEntry parseCommentEntry(Element c1) {
    // Id, required
    Element a = c1.previousElementSibling();
    if (a == null) return null;
    String text = ParserUtils.unescape(a.attr("name"));
    if (StringUtils.isEmpty(text)) return null;
    int id = ParserUtils.parseInt(text.substring(1), -1);
    if (id == -1) return null;

    // Comment, required
    Element c6 = c1.getElementsByClass("c6").first();
    if (c6 == null) return null;
    String comment = c6.html();
    if (StringUtils.isEmpty(comment)) return null;

    CommentEntry entry = new CommentEntry();
    entry.id = id;
    entry.comment = comment;

    // Date
    Element c3 = c1.getElementsByClass("c3").first();
    if (c3 != null) {
      text = ParserUtils.unescape(c3.ownText());
      if (text.length() > COMMENT_DATE_PREFIX.length() + COMMENT_DATE_SUFFIX.length()) {
        text = text.substring(COMMENT_DATE_PREFIX.length(),
            text.length() - COMMENT_DATE_SUFFIX.length());
        entry.date = ParserUtils.parseCommentDate(text, 0);
      }

      // User
      a = c3.children().first();
      if (a != null) {
        entry.user = ParserUtils.unescape(a.text());
      }
    }

    // Score
    Element c5 = c1.getElementsByClass("c5").first();
    if (c5 != null) {
      Element span = c5.children().first();
      if (span != null) {
        entry.score = ParserUtils.parseInt(span.text(), 0);
      }
    }

    // Vote up and vote down
    Element c4 = c1.getElementsByClass("c4").first();
    if (c4 != null) {
      Elements es = c4.children();
      if (es.size() == 2) {
        entry.votedUp = !TextUtils.isEmpty(StringUtils.trim(es.get(0).attr("style")));
        entry.votedDown = !TextUtils.isEmpty(StringUtils.trim(es.get(1).attr("style")));
      }
    }

    // Vote state
    Element c7 = c1.getElementsByClass("c7").first();
    if (c7 != null) {
      entry.voteState = ParserUtils.unescape(c7.text());
    }

    return entry;
  }
}
