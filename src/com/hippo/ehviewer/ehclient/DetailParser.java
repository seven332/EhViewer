package com.hippo.ehviewer.ehclient;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hippo.ehviewer.ListUrls;
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
    public static final int OFFENSIVE = 0x10;
    public static final int PINING = 0x20;
    
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
    public String[][] tags;
    public PreviewList previewList;
    
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
            ArrayList<String[]> list = new ArrayList<String[]>();
            p = Pattern
                    .compile("<tr><td[^<>]*>([^<>]+):</td><td>(?:<div[^<>]*><a[^<>]*>[^<>]*</a>[^<>]*<span[^<>]*>\\d+</span>[^<>]*</div>)+</td></tr>");
            m = p.matcher(pageContent);
            while (m.find()) {
                re |= TAG;
                String groupName = m.group(1);
                String[] group = getTagGroup(m.group(0));
                if (groupName != null && group != null) {
                    group[0] = groupName;
                    list.add(group);
                }
            }
            tags = new String[list.size()][];
            int i = 0;
            for (String[] item : list) {
                tags[i] = item;
                i++;
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
        
        return re;
    }
    
    private String[] getTagGroup(String pageContent) {
        ArrayList<String> list = new ArrayList<String>();
        Pattern p = Pattern.compile("<a[^<>]*>([^<>]+)</a>");
        Matcher m = p.matcher(pageContent);
        while (m.find()) {
            String str = m.group(1);
            if (str != null)
                list.add(str);
        }
        if (list.size() == 0)
            return null;
        String[] strs = new String[list.size() + 1];
        int i = 1;
        for (String str : list) {
            strs[i] = str;
            i++;
        }
        return strs;
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
}
