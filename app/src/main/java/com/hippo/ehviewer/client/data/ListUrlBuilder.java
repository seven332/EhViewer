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

package com.hippo.ehviewer.client.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.dao.QuickSearch;
import com.hippo.ehviewer.widget.AdvanceSearchTable;
import com.hippo.network.UrlBuilder;
import com.hippo.yorozuya.StringUtils;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class ListUrlBuilder implements Cloneable, Parcelable {

    @IntDef({MODE_NORMAL, MODE_UPLOADER, MODE_TAG, MODE_WHATS_HOT, MODE_IMAGE_SEARCH})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Mode {}

    // Mode
    public static final int MODE_NORMAL = 0x0;
    public static final int MODE_UPLOADER = 0x1;
    public static final int MODE_TAG = 0x2;
    public static final int MODE_WHATS_HOT = 0x3;
    public static final int MODE_IMAGE_SEARCH = 0x4;

    public static final int DEFAULT_ADVANCE = AdvanceSearchTable.SNAME | AdvanceSearchTable.STAGS;
    public static final int DEFAULT_MIN_RATING = 2;

    @Mode
    private int mMode = MODE_NORMAL;

    private int mPageIndex = 0;

    private int mCategory = EhUtils.NONE;
    private String mKeyword = null;

    private int mAdvanceSearch = -1;
    private int mMinRating = -1;

    private String mImagePath;
    private boolean mUseSimilarityScan;
    private boolean mOnlySearchCovers;
    private boolean mShowExpunged;

    /**
     * Make this ListUrlBuilder point to homepage
     */
    public void reset() {
        mMode = MODE_NORMAL;
        mPageIndex = 0;
        mCategory = EhUtils.NONE;
        mKeyword = null;
        mAdvanceSearch = -1;
        mMinRating = -1;
        mImagePath = null;
        mUseSimilarityScan = false;
        mOnlySearchCovers = false;
        mShowExpunged = false;
    }

    @Override
    public ListUrlBuilder clone() {
        try {
            return (ListUrlBuilder) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Mode
    public int getMode() {
        return mMode;
    }

    public void setMode(@Mode int mode) {
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

    public String getKeyword() {
        return mKeyword;
    }

    public void setKeyword(String keyword) {
        mKeyword = keyword;
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

    @Nullable
    public String getImagePath() {
        return mImagePath;
    }

    public void setImagePath(String imagePath) {
        mImagePath = imagePath;
    }

    public boolean isUseSimilarityScan() {
        return mUseSimilarityScan;
    }

    public void setUseSimilarityScan(boolean useSimilarityScan) {
        mUseSimilarityScan = useSimilarityScan;
    }

    public boolean isOnlySearchCovers() {
        return mOnlySearchCovers;
    }

    public void setOnlySearchCovers(boolean onlySearchCovers) {
        mOnlySearchCovers = onlySearchCovers;
    }

    public boolean isShowExpunged() {
        return mShowExpunged;
    }

    public void setShowExpunged(boolean showExpunged) {
        mShowExpunged = showExpunged;
    }

    /**
     * Make them the same
     * @param lub The template
     */
    public void set(ListUrlBuilder lub) {
        mMode = lub.mMode;
        mPageIndex = lub.mPageIndex;
        mCategory = lub.mCategory;
        mKeyword = lub.mKeyword;
        mAdvanceSearch = lub.mAdvanceSearch;
        mMinRating = lub.mMinRating;
        mImagePath = lub.mImagePath;
        mUseSimilarityScan = lub.mUseSimilarityScan;
        mOnlySearchCovers = lub.mOnlySearchCovers;
        mShowExpunged = lub.mShowExpunged;
    }

    public void set(QuickSearch q) {
        mMode = q.mode;
        mCategory = q.category;
        mKeyword = q.keyword;
        mAdvanceSearch = q.advanceSearch;
        mMinRating = q.minRating;
        mImagePath = null;
        mUseSimilarityScan = false;
        mOnlySearchCovers = false;
        mShowExpunged = false;
    }

    public QuickSearch toQuickSearch() {
        QuickSearch q = new QuickSearch();
        q.mode = mMode;
        q.category = mCategory;
        q.keyword = mKeyword;
        q.advanceSearch = mAdvanceSearch;
        q.minRating = mMinRating;
        return q;
    }

    public boolean equalsQuickSearch(QuickSearch q) {
        if (null == q) {
            return false;
        }

        if (q.mode != mMode) {
            return false;
        }
        if (q.category != mCategory) {
            return false;
        }
        if (!StringUtils.equals(q.keyword, mKeyword)) {
            return false;
        }
        if (q.advanceSearch != mAdvanceSearch) {
            return false;
        }
        if (q.minRating != mMinRating) {
            return false;
        }

        return true;
    }

    /**
     * @param query xxx=yyy&mmm=nnn
     */
    // TODO page
    public void setQuery(String query) {
        reset();

        if (TextUtils.isEmpty(query)) {
            return;
        }

        String[] querys = StringUtils.split(query, '&');
        boolean apply = false;
        int category = 0;
        String keyword = null;
        boolean enableAdvanceSearch = false;
        int advanceSearch = 0;
        boolean enableMinRating = false;
        int minRating = 0;
        for (int i = 0, size = querys.length; i < size; i++) {
            String str = querys[i];
            int index = str.indexOf('=');
            if (index < 0) {
                continue;
            }
            String key = str.substring(0, index);
            String value = str.substring(index + 1);
            if ("f_doujinshi".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.DOUJINSHI;
                }
            } else if ("f_manga".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.MANGA;
                }
            } else if ("f_artistcg".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.ARTIST_CG;
                }
            } else if ("f_gamecg".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.GAME_CG;
                }
            } else if ("f_western".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.WESTERN;
                }
            } else if ("f_non-h".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.NON_H;
                }
            } else if ("f_imageset".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.IMAGE_SET;
                }
            } else if ("f_cosplay".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.COSPLAY;
                }
            } else if ("f_asianporn".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.ASIAN_PORN;
                }
            } else if ("f_misc".equals(key)) {
                if ("1".equals(value)) {
                    category |= EhConfig.MISC;
                }
            } else if ("f_search".equals(key)) {
                try {
                    keyword = URLDecoder.decode(value, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    // Ignore
                }
            } else if ("advsearch".equals(key)) {
                if ("1".equals(value)) {
                    enableAdvanceSearch = true;
                }
            } else if ("f_sname".equals(key)) {
                if ("on".equals(value)) {
                    advanceSearch |= AdvanceSearchTable.SNAME;
                }
            } else if ("f_stags".equals(key)) {
                if ("on".equals(value)) {
                    advanceSearch |= AdvanceSearchTable.STAGS;
                }
            } else if ("f_sdesc".equals(key)) {
                if ("on".equals(value)) {
                    advanceSearch |= AdvanceSearchTable.SDESC;
                }
            } else if ("f_storr".equals(key)) {
                if ("on".equals(value)) {
                    advanceSearch |= AdvanceSearchTable.STORR;
                }
            } else if ("f_sto".equals(key)) {
                if ("on".equals(value)) {
                    advanceSearch |= AdvanceSearchTable.STO;
                }
            } else if ("f_sdt1".equals(key)) {
                if ("on".equals(value)) {
                    advanceSearch |= AdvanceSearchTable.SDT1;
                }
            } else if ("f_sdt2".equals(key)) {
                if ("on".equals(value)) {
                    advanceSearch |= AdvanceSearchTable.SDT2;
                }
            } else if ("f_sh".equals(key)) {
                if ("on".equals(value)) {
                    advanceSearch |= AdvanceSearchTable.SH;
                }
            } else if ("f_sr".equals(key)) {
                if ("on".equals(value)) {
                    enableMinRating = true;
                }
            } else if ("f_srdd".equals(key)) {
                try {
                    minRating = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            } else if ("f_apply".equals(key)) {
                if ("Apply+Filter".equals(value)) {
                    apply = true;
                }
            }
        }

        if (!apply) {
            return;
        }

        mCategory = category;
        mKeyword = keyword;
        if (enableAdvanceSearch) {
            mAdvanceSearch = advanceSearch;
            if (enableMinRating) {
                mMinRating = minRating;
            } else {
                mMinRating = -1;
            }
        } else {
            mAdvanceSearch = -1;
        }
    }

    public String build() {
        switch (mMode) {
            default:
            case MODE_NORMAL: {
                boolean filter = false;
                UrlBuilder ub = new UrlBuilder(EhUrl.getHost());
                if (mCategory != EhUtils.NONE) {
                    ub.addQuery("f_doujinshi", ((mCategory & EhConfig.DOUJINSHI) == 0) ? "0" : "1");
                    ub.addQuery("f_manga", ((mCategory & EhConfig.MANGA) == 0) ? "0" : "1");
                    ub.addQuery("f_artistcg", ((mCategory & EhConfig.ARTIST_CG) == 0) ? "0" : "1");
                    ub.addQuery("f_gamecg", ((mCategory & EhConfig.GAME_CG) == 0) ? "0" : "1");
                    ub.addQuery("f_western", ((mCategory & EhConfig.WESTERN) == 0) ? "0" : "1");
                    ub.addQuery("f_non-h", ((mCategory & EhConfig.NON_H) == 0) ? "0" : "1");
                    ub.addQuery("f_imageset", ((mCategory & EhConfig.IMAGE_SET) == 0) ? "0" : "1");
                    ub.addQuery("f_cosplay", ((mCategory & EhConfig.COSPLAY) == 0) ? "0" : "1");
                    ub.addQuery("f_asianporn", ((mCategory & EhConfig.ASIAN_PORN) == 0) ? "0" : "1");
                    ub.addQuery("f_misc", ((mCategory & EhConfig.MISC) == 0) ? "0" : "1");
                    filter = true;
                }
                // Search key
                if (mKeyword != null) {
                    try {
                        ub.addQuery("f_search", URLEncoder.encode(mKeyword, "UTF-8"));
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
            case MODE_UPLOADER: {
                StringBuilder sb = new StringBuilder(EhUrl.getHost());
                sb.append("uploader/");
                try {
                    sb.append(URLEncoder.encode(mKeyword, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // Empty
                }
                if (mPageIndex != 0) {
                    sb.append('/').append(mPageIndex);
                }
                return sb.toString();
            }
            case MODE_TAG: {
                StringBuilder sb = new StringBuilder(EhUrl.getHost());
                sb.append("tag/");
                try {
                    sb.append(URLEncoder.encode(mKeyword, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // Empty
                }
                if (mPageIndex != 0) {
                    sb.append('/').append(mPageIndex);
                }
                return sb.toString();
            }
            case MODE_IMAGE_SEARCH:
                return EhUrl.getImageSearchUrl();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mMode);
        dest.writeInt(this.mPageIndex);
        dest.writeInt(this.mCategory);
        dest.writeString(this.mKeyword);
        dest.writeInt(this.mAdvanceSearch);
        dest.writeInt(this.mMinRating);
        dest.writeString(this.mImagePath);
        dest.writeByte(mUseSimilarityScan ? (byte) 1 : (byte) 0);
        dest.writeByte(mOnlySearchCovers ? (byte) 1 : (byte) 0);
        dest.writeByte(mShowExpunged ? (byte) 1 : (byte) 0);
    }

    public ListUrlBuilder() {
    }

    @SuppressWarnings("WrongConstant")
    protected ListUrlBuilder(Parcel in) {
        this.mMode = in.readInt();
        this.mPageIndex = in.readInt();
        this.mCategory = in.readInt();
        this.mKeyword = in.readString();
        this.mAdvanceSearch = in.readInt();
        this.mMinRating = in.readInt();
        this.mImagePath = in.readString();
        this.mUseSimilarityScan = in.readByte() != 0;
        this.mOnlySearchCovers = in.readByte() != 0;
        this.mShowExpunged = in.readByte() != 0;
    }

    public static final Creator<ListUrlBuilder> CREATOR = new Creator<ListUrlBuilder>() {
        @Override
        public ListUrlBuilder createFromParcel(Parcel source) {
            return new ListUrlBuilder(source);
        }

        @Override
        public ListUrlBuilder[] newArray(int size) {
            return new ListUrlBuilder[size];
        }
    };
}
