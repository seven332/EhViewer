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

package com.hippo.ehviewer;

import com.hippo.okhttp.CookieDB;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CookieDBTest extends TestCase {

    private static final String[] TEST_DOMAIN = {
            null,
            "",
            "asdfghjkl",
            "asdfghjkl.sds",
            "asdfghjkl.sds.sddddd",
            "asdfghjkl.sds.sddddd.aaaaa",
            "asdfghjkl.sds.sddddd.",
            "asdfghjkl.sds..",
    };

    private static final String[] TEST_COOKIES_DOMAIN = {
            "",
            "",
            "asdfghjkl",
            "asdfghjkl.sds",
            "sds.sddddd",
            "sddddd.aaaaa",
            "sddddd.",
            ".",
    };

    public void testCookiesDomain() {
        for (int i = 0; i < TEST_DOMAIN.length; i++) {
            Assert.assertEquals(TEST_COOKIES_DOMAIN[i], CookieDB.cookiesDomain(TEST_DOMAIN[i]));
        }
    }
}
