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

package com.hippo.ehviewer.data;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.network.UrlBuilder;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Utils;

public class ListUrls {

    public static final boolean NUM_CATEGORY = false;

    // Mode
    public static final int MODE_NORMAL = 0x0;
    public static final int MODE_UPLOADER = 0x1;
    public static final int MODE_TAG = 0x2;
    public static final int MODE_POPULAR = 0x3;
    public static final int MODE_IMAGE_SEARCH = 0x4;

    // Category
    public static final int NONE = -1; // Use it for homepage
    public static final int MISC = 0x1;
    public static final int DOUJINSHI = 0x2;
    public static final int MANGA = 0x4;
    public static final int ARTIST_CG = 0x8;
    public static final int GAME_CG = 0x10;
    public static final int IMAGE_SET = 0x20;
    public static final int COSPLAY = 0x40;
    public static final int ASIAN_PORN = 0x80;
    public static final int NON_H = 0x100;
    public static final int WESTERN = 0x200;
    public static final int UNKNOWN = 0x400;

    public static final int ALL_CATEGORT = UNKNOWN - 1;
    //DOUJINSHI|MANGA|ARTIST_CG|GAME_CG|WESTERN|NON_H|IMAGE_SET|COSPLAY|ASIAN_PORN|MISC;

    // advance search
    public static final int SNAME = 0x1;
    public static final int STAGS = 0x2;
    public static final int SDESC = 0x4;
    public static final int STORR = 0x8;
    public static final int STO = 0x10;
    public static final int STD1 = 0x20;
    public static final int STD2 = 0x40;
    public static final int SH = 0x80;

    public static final int DEFAULT_ADVANCE = SNAME | STAGS;
    public static final int DEFAULT_MIN_RATING = 2;

    private int mMode = MODE_NORMAL;

    private int page = 0;
    private int category = NONE;

    private int advsearchType = -1;
    private int minRating = -1;

    private String search = null;
    private String mTag = null;

    private int mImageSearchMode;
    private String mImageKey;
    private String mImageUrl;
    private File mSearchFile;
    private String mResultUrl = null;

    public ListUrls() {}

    public ListUrls(int category) {
        this.category = category;
    }

    public ListUrls(int category, int page) {
        this.category = category;
        this.page = page;
    }

    public ListUrls(int category, String search) {
        this.category = category;
        this.search = search;
    }

    public ListUrls(int category, String search, int page) {
        this.category = category;
        this.search = search;
        this.page = page;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public int getCategory() {
        return category;
    }

    public String getSearch() {
        return search;
    }

    public void setAdvance(boolean enabled) {
        if (enabled && advsearchType == -1)
            advsearchType = DEFAULT_ADVANCE;
        else if (!enabled)
            advsearchType = -1;
    }

    public void setAdvance(int advanceType) {
        advsearchType = advanceType;
    }

    public void setAdvance(int advanceType, int minRating) {
        advsearchType = advanceType;
        this.minRating = minRating;
    }

    public boolean isAdvance() {
        return advsearchType != -1;
    }

    public int getAdvanceType() {
        return advsearchType;
    }

    public boolean isMinRating() {
        return minRating != -1;
    }

    public int getMinRating() {
        return minRating;
    }

    public void setPage(int p) {
        page = p;
    }

    public int getPage() {
        return page;
    }

    public int getMode() {
        return mMode;
    }

    public void setMode(int mode) {
        mMode = mode;
    }

    // For tag mode
    public void setTag(String tag) {
        mMode = MODE_TAG;
        mTag = tag;
    }

    public String getTag() {
        return mTag;
    }

    // For image search mode
    public void setSearchImage(String key, String url, int imageSearchMode) {
        mMode = MODE_IMAGE_SEARCH;
        mImageKey = key;
        mImageUrl = url;
        mImageSearchMode = imageSearchMode;
    }

    public void setSearchFile(File searchFile, int imageSearchMode) {
        mMode = MODE_IMAGE_SEARCH;
        mSearchFile = searchFile;
        mImageSearchMode = imageSearchMode;
    }

    public void setSearchResult(String resultUrl) {
        mResultUrl = resultUrl;
    }

    public File getSearchFile() {
        return mSearchFile;
    }

    public String getSearchImageKey() {
        return mImageKey;
    }

    public String getSearchImageUrl() {
        return mImageUrl;
    }

    public int getImageSearchMode() {
        return mImageSearchMode;
    }


    public String getUrl() {
        return getUrl(Config.getMode());
    }

    public String getUrl(int mode) {

        switch (mMode) {
        case MODE_POPULAR:
            return null;

        case MODE_IMAGE_SEARCH:
            if (mResultUrl == null)
                return null;
            else
                return mResultUrl + "&page=" + page;

        case MODE_TAG:
            // Add tag
            StringBuilder sb = new StringBuilder(EhClient.getUrlHeader(mode));
            sb.append("tag/");
            if (mTag != null) {
                String[] tag = mTag.split("\\s+");
                try {
                    for (int i = 0; i < tag.length; i++)
                        tag[i] = URLEncoder.encode(tag[i], "UTF-8");
                } catch (UnsupportedEncodingException e) {}
                sb.append(Utils.join(tag, '+'));

                // Add page
                sb.append("/").append(page);
            } else {
                sb.append(page);
            }
            return sb.toString();

        case MODE_NORMAL:
        case MODE_UPLOADER:
        default:
            String header = EhClient.getUrlHeader(mode);
            boolean isNeedFooter = false;
            UrlBuilder ub;
            // Add category
            if (category != NONE) {
                isNeedFooter = true;
                if (NUM_CATEGORY) {
                    ub = new UrlBuilder(header + category);
                } else {
                    ub = new UrlBuilder(header);
                    ub.addQuery("f_doujinshi", ((category & DOUJINSHI) == 0) ? "0" : "1");
                    ub.addQuery("f_manga", ((category & MANGA) == 0) ? "0" : "1");
                    ub.addQuery("f_artistcg", ((category & ARTIST_CG) == 0) ? "0" : "1");
                    ub.addQuery("f_gamecg", ((category & GAME_CG) == 0) ? "0" : "1");
                    ub.addQuery("f_western", ((category & WESTERN) == 0) ? "0" : "1");
                    ub.addQuery("f_non-h", ((category & NON_H) == 0) ? "0" : "1");
                    ub.addQuery("f_imageset", ((category & IMAGE_SET) == 0) ? "0" : "1");
                    ub.addQuery("f_cosplay", ((category & COSPLAY) == 0) ? "0" : "1");
                    ub.addQuery("f_asianporn", ((category & ASIAN_PORN) == 0) ? "0" : "1");
                    ub.addQuery("f_misc", ((category & MISC) == 0) ? "0" : "1");
                }
            } else {
                ub = new UrlBuilder(header);
            }

            // Add search item
            if (search != null) {
                isNeedFooter = true;

                String[] tag = search.split("\\s+");
                try {
                    for (int i = 0; i < tag.length; i++)
                        tag[i] = URLEncoder.encode(tag[i], "UTF-8");
                } catch (UnsupportedEncodingException e) {}

                ub.addQuery("f_search", Utils.join(tag, '+'));
            }

            // Add page
            if (page != 0) {
                ub.addQuery("page", page);
            }

            // Add foot
            if (isNeedFooter) ub.addQuery("f_apply", "Apply+Filter");

            // Add advance search
            if (isAdvance()) {
                ub.addQuery("advsearch", "1");
                if((advsearchType & SNAME) != 0) ub.addQuery("f_sname", "on");
                if((advsearchType & STAGS) != 0) ub.addQuery("f_stags", "on");
                if((advsearchType & SDESC) != 0) ub.addQuery("f_sdesc", "on");
                if((advsearchType & STORR) != 0) ub.addQuery("f_storr", "on");
                if((advsearchType & STO) != 0) ub.addQuery("f_sto", "on");
                if((advsearchType & STD1) != 0) ub.addQuery("f_sdt1", "on");
                if((advsearchType & STD2) != 0) ub.addQuery("f_sdt2", "on");
                if((advsearchType & SH) != 0) ub.addQuery("f_sh", "on");

                // Set min star
                if (isMinRating()) {
                    ub.addQuery("f_sr", "on");
                    ub.addQuery("f_srdd", minRating);
                }
            }
            return ub.build();
        }
    }


    @Override
    public String toString() {
        switch (mMode) {
        case MODE_NORMAL:
        case MODE_UPLOADER:
            return search;
        case MODE_TAG:
            return mTag;
        case MODE_POPULAR:
        case MODE_IMAGE_SEARCH:
        default:
            return null;
        }
    }
}
