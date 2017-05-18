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

package com.hippo.ehviewer.client.parser;

/*
 * Created by Hippo on 5/18/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.data.ApiUidKey;
import com.hippo.ehviewer.client.exception.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ApiUidKeyParser {
  private ApiUidKeyParser() {}

  private static final Pattern PATTERN_UID = Pattern.compile("var apiuid = (\\d+);");
  private static final Pattern PATTERN_KEY = Pattern.compile("var apikey = \"(\\w+)\";");

  /**
   * Parses page to get api uid and api key.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static ApiUidKey parseApiUidKey(@NonNull String body) throws ParseException {
    Matcher matcher1 = PATTERN_UID.matcher(body);
    Matcher matcher2 = PATTERN_KEY.matcher(body);
    if (matcher1.find() && matcher2.find()) {
      int uid = ParserUtils.parseInt(matcher1.group(1), 0);
      String key = matcher2.group(2);
      return new ApiUidKey(uid, key);
    }
    throw new ParseException("Can't get uid and key", body);
  }
}
