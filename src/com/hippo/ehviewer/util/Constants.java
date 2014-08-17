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

package com.hippo.ehviewer.util;

public final class Constants {

    public static final int FALSE = 0;
    public static final int TRUE = 1;

    public static final int ANIMATE_TIME = 300;

    public static final int HTTP_TEMP_REDIRECT = 307;
    public static String defaultUserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.76 Safari/537.36";
    public static String userAgent = System.getProperty("http.agent", defaultUserAgent);
    public static final int DEFAULT_TIMEOUT = 5 * 1000;
    public static final int MAX_REDIRECTS = 5;

    public static final int BUFFER_SIZE = 4096;
}
