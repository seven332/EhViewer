/*
 * Copyright (C) 2014-2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.data;

import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.widget.AdvanceSearchTable;
import com.hippo.network.UrlBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ListUrlBuilder {

    // Mode
    public static final int MODE_NORMAL = 0x0;
    public static final int MODE_UPLOADER = 0x1;
    public static final int MODE_TAG = 0x2;
    public static final int MODE_POPULAR = 0x3;
    public static final int MODE_IMAGE_SEARCH = 0x4;

    public static final int ALL_CATEGORT = EhUtils.UNKNOWN - 1;
    //DOUJINSHI|MANGA|ARTIST_CG|GAME_CG|WESTERN|NON_H|IMAGE_SET|COSPLAY|ASIAN_PORN|MISC;

    public static final int DEFAULT_ADVANCE = AdvanceSearchTable.SNAME | AdvanceSearchTable.STAGS;
    public static final int DEFAULT_MIN_RATING = 2;

    private int mMode = MODE_NORMAL;

    private int mPageIndex = 0;

    private int mCategory = EhUtils.NONE;
    private String mSearchKey = null;

    private int mAdvanceSearch = -1;
    private int mMinRating = -1;

    private String mSearchTag = null;

    public int getMode() {
        return mMode;
    }

    public void setMode(int mode) {
        mMode = mode;
    }

    public int getPageIndex() {
        return mPageIndex;
    }

    public void setPageIndex(int pageIndex) {
        mPageIndex = pageIndex;
    }

    public int getCategory() {
        return mCategory;
    }

    public void setCategory(int category) {
        mCategory = category;
    }

    public String getSearchKey() {
        return mSearchKey;
    }

    public void setSearchKey(String searchKey) {
        mSearchKey = searchKey;
    }

    public int getAdvanceSearch() {
        return mAdvanceSearch;
    }

    public void setAdvanceSearch(int advanceSearch) {
        mAdvanceSearch = advanceSearch;
    }

    public int getMinRating() {
        return mMinRating;
    }

    public void setMinRating(int minRating) {
        mMinRating = minRating;
    }

    public String getSearchTag() {
        return mSearchTag;
    }

    public void setSearchTag(String searchTag) {
        mSearchTag = searchTag;
    }

    /**
     * Make them the same
     * @param glub The template
     */
    public void set(ListUrlBuilder glub) {
        mMode = glub.mMode;
        mPageIndex = glub.mPageIndex;
        mCategory = glub.mCategory;
        mSearchKey = glub.mSearchKey;
        mAdvanceSearch = glub.mAdvanceSearch;
        mMinRating = glub.mMinRating;
        mSearchTag = glub.mSearchTag;
    }

    public String build(int source) throws UnsupportedSearch {
        switch (mMode) {
            default:
            case MODE_NORMAL:
            case MODE_UPLOADER: {
                boolean filter = false;
                UrlBuilder ub = new UrlBuilder(EhClient.getUrlHeader(source));
                if (mCategory != EhUtils.NONE) {
                    ub.addQuery("f_doujinshi", ((mCategory & EhUtils.DOUJINSHI) == 0) ? "0" : "1");
                    ub.addQuery("f_manga", ((mCategory & EhUtils.MANGA) == 0) ? "0" : "1");
                    ub.addQuery("f_artistcg", ((mCategory & EhUtils.ARTIST_CG) == 0) ? "0" : "1");
                    ub.addQuery("f_gamecg", ((mCategory & EhUtils.GAME_CG) == 0) ? "0" : "1");
                    ub.addQuery("f_western", ((mCategory & EhUtils.WESTERN) == 0) ? "0" : "1");
                    ub.addQuery("f_non-h", ((mCategory & EhUtils.NON_H) == 0) ? "0" : "1");
                    ub.addQuery("f_imageset", ((mCategory & EhUtils.IMAGE_SET) == 0) ? "0" : "1");
                    ub.addQuery("f_cosplay", ((mCategory & EhUtils.COSPLAY) == 0) ? "0" : "1");
                    ub.addQuery("f_asianporn", ((mCategory & EhUtils.ASIAN_PORN) == 0) ? "0" : "1");
                    ub.addQuery("f_misc", ((mCategory & EhUtils.MISC) == 0) ? "0" : "1");
                    filter = true;
                }
                // Search key
                if (mSearchKey != null) {
                    try {
                        ub.addQuery("f_search", URLEncoder.encode(mSearchKey, "UTF-8"));
                        filter = true;
                    } catch (UnsupportedEncodingException e) {
                        // Empty
                    }
                }
                // Page index
                if (mPageIndex != 0) {
                    ub.addQuery("page", mPageIndex);
                }
                // Advance search
                if (mAdvanceSearch != -1) {
                    ub.addQuery("advsearch", "1");
                    if((mAdvanceSearch & AdvanceSearchTable.SNAME) != 0) ub.addQuery("f_sname", "on");
                    if((mAdvanceSearch & AdvanceSearchTable.STAGS) != 0) ub.addQuery("f_stags", "on");
                    if((mAdvanceSearch & AdvanceSearchTable.SDESC) != 0) ub.addQuery("f_sdesc", "on");
                    if((mAdvanceSearch & AdvanceSearchTable.STORR) != 0) ub.addQuery("f_storr", "on");
                    if((mAdvanceSearch & AdvanceSearchTable.STO) != 0) ub.addQuery("f_sto", "on");
                    if((mAdvanceSearch & AdvanceSearchTable.SDT1) != 0) ub.addQuery("f_sdt1", "on");
                    if((mAdvanceSearch & AdvanceSearchTable.SDT2) != 0) ub.addQuery("f_sdt2", "on");
                    if((mAdvanceSearch & AdvanceSearchTable.SH) != 0) ub.addQuery("f_sh", "on");
                    // Set min star
                    if (mMinRating != -1) {
                        ub.addQuery("f_sr", "on");
                        ub.addQuery("f_srdd", mMinRating);
                    }
                    filter = true;
                }
                // Add filter foot
                if (filter) {
                    ub.addQuery("f_apply", "Apply+Filter");
                }
                return ub.build();
            }
            case MODE_TAG: {
                if (source == EhClient.SOURCE_LOFI) {
                    throw new UnsupportedSearch("Lofi do not support tag search");
                }
                StringBuilder sb = new StringBuilder(EhClient.getUrlHeader(source));
                sb.append("tag/");
                try {
                    sb.append(URLEncoder.encode(mSearchTag, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // Empty
                }
                return sb.toString();
            }
        }
    }

}
