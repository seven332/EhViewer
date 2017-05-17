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
 * Created by Hippo on 1/18/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.exception.GeneralException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.SignInParser;
import com.hippo.ehviewer.client.result.SignInResult;

/**
 * A {@link retrofit2.Converter} to parse the html of signing in.
 */
public class SignInConverter extends EhConverter<SignInResult> {

  @NonNull
  @Override
  public SignInResult convert(String body) throws ParseException, GeneralException {
    return new SignInResult(SignInParser.parseSignIn(body));
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public SignInResult error(Throwable t) {
    return SignInResult.error(t);
  }
}
