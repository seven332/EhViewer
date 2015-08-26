/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.ehviewer.client.data;

import android.text.TextUtils;

import com.hippo.ehviewer.client.EhUrl;
import com.hippo.network.UrlBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FavoriteUrlBuilder implements Cloneable {

    public static final int FAV_CAT_LOCAL = -2;
    public static final int FAV_CAT_ALL = -1;

    private int mCat = -1;
    private String mKeyword = null;

    public int getCat() {
        return mCat;
    }

    public void setCat(int cat) {
        mCat = cat;
    }

    public String getKeyword() {
        return mKeyword;
    }

    public void setKeyword(String keyword) {
        mKeyword = keyword;
    }

    public boolean isLocal() {
        return mCat == FAV_CAT_LOCAL;
    }

    public String build(int source, int page) {
        UrlBuilder ub = new UrlBuilder(EhUrl.getUrlHeader(source) + "favorites.php");

        if (mCat != FAV_CAT_ALL) {
            ub.addQuery("favcat", mCat);
        }

        if (!TextUtils.isEmpty(mKeyword)) {
            try {
                ub.addQuery("f_search", URLEncoder.encode(mKeyword, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            ub.addQuery("f_apply", "Search+Favorites");
        }

        ub.addQuery("page", page);

        return ub.build();
    }

    @Override
    protected FavoriteUrlBuilder clone() {
        try {
            return (FavoriteUrlBuilder) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
