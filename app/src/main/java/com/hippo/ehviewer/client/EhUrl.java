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

package com.hippo.ehviewer.client;

public class EhUrl {

    public static final int SOURCE_G = 0x0;
    public static final int SOURCE_EX = 0x1;
    public static final int SOURCE_LOFI = 0x2;

    public static final String HOST_G = "G.E-Hentai";
    public static final String HOST_EX = "ExHentai";
    public static final String HOST_LOFI = "Lofi.E-Hentai";

    public static final String HEADER_G = "http://g.e-hentai.org/";
    public static final String HEADER_EX = "http://exhentai.org/";
    public static final String HEADER_LOFI = "http://lofi.e-hentai.org/";

    public static final String API_G = "http://g.e-hentai.org/api.php";
    public static final String API_EX = "http://exhentai.org/api.php";

    public static String getReadableHost(int source) {
        switch (source) {
            default:
            case SOURCE_G:
                return HOST_G;
            case SOURCE_EX:
                return HOST_EX;
            case SOURCE_LOFI:
                return HOST_LOFI;
        }
    }

    public static String getApiUrl(int source) {
        switch (source) {
            default:
            case EhUrl.SOURCE_G:
                return API_G;
            case EhUrl.SOURCE_EX:
                return API_EX;
        }
    }

    public static String getUrlHeader(int source) {
        switch (source) {
            default:
            case SOURCE_G:
                return HEADER_G;
            case SOURCE_EX:
                return HEADER_EX;
            case SOURCE_LOFI:
                return HEADER_LOFI;
        }
    }

    /**
     * Get gellary detail url in target source
     */
    public static String getDetailUrl(int source, int gid, String token, int pageIndex) {
        StringBuilder sb = new StringBuilder(getUrlHeader(source));
        sb.append("g/").append(gid).append('/').append(token).append('/');
        if (pageIndex > 0) {
            sb.append("?p=").append(pageIndex);
        }

        return sb.toString();
    }

    public static String getPageUrl(int source, int gid, String token, int pageIndex) {
        return getUrlHeader(source) + "s/" + token + '/' + gid + '-' + (pageIndex + 1);
    }

    /**
     * @param cat -1 for all
     */
    public static String getFavoriteUrl(int source, int cat, int pageIndex) {
        if (cat == -1) {
            return getUrlHeader(source) + "favorites.php?page=" + pageIndex;
        } else {
            return getUrlHeader(source) + "favorites.php?favcat=" + cat + "&page=" + pageIndex;
        }
    }
}
