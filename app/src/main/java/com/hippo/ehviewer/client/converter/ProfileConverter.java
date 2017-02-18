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
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.result.ProfileResult;
import com.hippo.yorozuya.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * A {@link retrofit2.Converter} to parse the html of profile
 * to get name and avatar.
 */
public class ProfileConverter extends EhConverter<ProfileResult> {

  @NonNull
  @Override
  public ProfileResult convert(String body) throws ParseException {
    String name;
    String avatar;

    Document d = Jsoup.parse(body);
    Element profileName = d.getElementById("profilename");
    try {
      name = StringUtils.strip(profileName.child(0).text());
    } catch (NullPointerException | IndexOutOfBoundsException e) {
      name = null;
    }
    try {
      avatar = profileName.nextElementSibling().nextElementSibling().child(0).attr("src");
      avatar = ConverterUtils.completeUrl(EhUrl.URL_FORUMS, avatar);
    } catch (NullPointerException | IndexOutOfBoundsException e) {
      avatar = null;
    }

    if (name == null) {
      // avatar could be null
      // name should not be null
      throw new ParseException("Can't parse profile", body);
    }

    return new ProfileResult(name, avatar);
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
