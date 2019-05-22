package com.hippo.ehviewer.client.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class SuggestionParser {

    public static class Result {
        public String keyword;
        public String url;

        Result(String keyword, String url) {
            this.keyword = keyword;
            this.url = url;
        }
    }

    public static Result[] parse(String body) throws JSONException {

        List<Result> result = new ArrayList<>();

        JSONArray jo = new JSONArray(body);
        JSONArray words = jo.getJSONArray(1);
        JSONArray links = jo.getJSONArray(3);

        for (int i = 0; i < words.length(); i++) {
            String keyword = words.getString(i);
            String link = links.getString(i);
            result.add(new Result(keyword, link));
        }

        return result.toArray(new Result[0]);
    }

    public static String[] parseDetail(String body) {
        Document doc = Jsoup.parse(body);
        Element e = doc.selectFirst("tr:contains(姓名)");
        // e.select("span[lang=ja]");
        // e.select("span[lang=ja]");
        Element nameObject = e.select("span[itemprop=name]").first();
        nameObject.select("ruby").remove();
        String name = nameObject.text();
        String nameEn = name.replaceAll("[()（）]", "").replace((char) 12288, ' ').trim();

        return new String[]{nameEn};
    }

}
