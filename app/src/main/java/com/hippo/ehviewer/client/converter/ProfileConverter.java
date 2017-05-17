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

package com.hippo.ehviewer.client.converter;

/*
 * Created by Hippo on 2/12/2017.
 */

import android.support.annotation.NonNull;
import android.util.Pair;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.ProfileParser;
import com.hippo.ehviewer.client.result.ProfileResult;

/**
 * A {@link retrofit2.Converter} to parse the html of profile
 * to get name and avatar.
 */
public class ProfileConverter extends EhConverter<ProfileResult> {

  @NonNull
  @Override
  public ProfileResult convert(String body) throws ParseException {
    Pair<String, String> pair = ProfileParser.parseProfile(body);
    return new ProfileResult(pair.first, pair.second);
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public ProfileResult error(Throwable t) {
    return ProfileResult.error(t);
  }
}
