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

import com.hippo.network.UrlBuilder;

public class EhUrl {

    public static final String DOMAIN_EX = "exhentai.org";
    public static final String DOMAIN_E = "e-hentai.org";
    public static final String DOMAIN_G = "g.e-hentai.org";
    public static final String DOMAIN_LOFI = "lofi.e-hentai.org";

    public static final String HOST_EX = "http://" + DOMAIN_EX + "/";

    public static final String API_SIGN_IN = "http://forums.e-hentai.org/index.php?act=Login&CODE=01";

    public static final String API_EX = HOST_EX + "api.php";

    public static String getGalleryDetailUrl(int gid, String token) {
        return getGalleryDetailUrl(gid, token, 0, false);
    }

    public static String getGalleryDetailUrl(int gid, String token, int index, boolean allComment) {
        UrlBuilder builder = new UrlBuilder(HOST_EX + "g/" + gid + '/' + token + '/');
        if (index != 0) {
            builder.addQuery("p", index);
        }
        if (allComment) {
            builder.addQuery("hc", 1);
        }
        return builder.build();
    }
}
