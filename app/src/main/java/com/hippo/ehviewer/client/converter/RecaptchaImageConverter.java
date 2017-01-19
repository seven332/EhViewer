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
 * Created by Hippo on 1/19/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.result.RecaptchaImageResult;

public class RecaptchaImageConverter extends EhConverter<RecaptchaImageResult> {

  @NonNull
  @Override
  public RecaptchaImageResult convert(String body) throws Exception {
    int start = body.indexOf('\'');
    if (start == -1) throwParseException(body);
    int end = body.indexOf('\'', start + 1);
    if (end == -1) throwParseException(body);
    String token = body.substring(start + 1, end);
    String image = "https://www.google.com/recaptcha/api/image?c=" + token;
    return new RecaptchaImageResult(image);
  }

  private void throwParseException(String body) throws ParseException {
    throw new ParseException("Can't parse the body of recaptcha token", body);
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public RecaptchaImageResult error(Throwable t) {
    return RecaptchaImageResult.error(t);
  }
}
