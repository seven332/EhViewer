/*
 * Copyright 2017 Hippo Seven
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

package com.hippo.ehviewer.network;

/*
 * Created by Hippo on 1/16/2017.
 */

import static org.junit.Assert.assertNull;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import org.junit.Test;

public class CookieTest {


  @Test
  public void test() {
    Cookie cookie = Cookie.parse(
        HttpUrl.parse("https://www.baidu.com/"),
        "BIDUPSID=0F0DD1F30091D35600F9EAC0E6968242; expires=Thu, 31-Dec-37 23:55:55 GMT; max-age=2147483647; path=/; domain=.com");


    assertNull(cookie);




  }

}
