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

package com.hippo.ehviewer.client;

import android.support.annotation.NonNull;

import com.hippo.ehviewer.Settings;
import com.hippo.network.UrlBuilder;

public class EhUrl {

    public static final int SITE_G = 0;
    public static final int SITE_EX = 1;

    public static final String DOMAIN_EX = "exhentai.org";
    public static final String DOMAIN_E = "e-hentai.org";
    public static final String DOMAIN_G = "g.e-hentai.org";
    public static final String DOMAIN_LOFI = "lofi.e-hentai.org";

    public static final String HOST_E = "http://" + DOMAIN_E + "/";
    public static final String HOST_EX = "http://" + DOMAIN_EX + "/";
    public static final String HOST_G = "http://" + DOMAIN_G + "/";

    public static final String API_SIGN_IN = "http://forums.e-hentai.org/index.php?act=Login&CODE=01";

    public static final String API_G = HOST_G + "api.php";
    public static final String API_EX = HOST_EX + "api.php";

    public static final String URL_SIGN_IN = "http://forums.e-hentai.org/index.php?act=Login";
    public static final String URL_REGISTER = "http://forums.e-hentai.org/index.php?act=Reg&CODE=00";
    public static final String URL_FAVORITES_G = HOST_G + "favorites.php";
    public static final String URL_FAVORITES_EX = HOST_EX + "favorites.php";
    public static final String URL_FORUMS = "https://forums.e-hentai.org/";

    public static String getGalleryDetailUrl(long gid, String token) {
        return getGalleryDetailUrl(gid, token, 0, false);
    }

    public static String getHost() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_G:
                return HOST_G;
            case SITE_EX:
                return HOST_EX;
        }
    }

    public static String getFavoritesUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_G:
                return URL_FAVORITES_G;
            case SITE_EX:
                return URL_FAVORITES_EX;
        }
    }

    public static String getApiUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_G:
                return API_G;
            case SITE_EX:
                return API_EX;
        }
    }

    public static String getGalleryDetailUrl(long gid, String token, int index, boolean allComment) {
        UrlBuilder builder = new UrlBuilder(getHost() + "g/" + gid + '/' + token + '/');
        if (index != 0) {
            builder.addQuery("p", index);
        }
        if (allComment) {
            builder.addQuery("hc", 1);
        }
        return builder.build();
    }

    public static String getPageUrl(long gid, int index, String pToken) {
        return getHost() + "s/" + pToken + '/' + gid + '-' + (index + 1);
    }

    public static String getAddFavorites(long gid, String token) {
        return getHost() + "gallerypopups.php?gid=" + gid + "&t=" + token + "&act=addfav";
    }

    public static String getTagDefinitionUrl(String tag) {
        return "https://ehwiki.org/wiki/" + tag.replace(' ', '_');
    }

    @NonNull
    public static String getImageSearchUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_G:
                return "http://ul.e-hentai.org/image_lookup.php";
            case SITE_EX:
                return "http://ul.exhentai.org/image_lookup.php";
        }
    }
}
