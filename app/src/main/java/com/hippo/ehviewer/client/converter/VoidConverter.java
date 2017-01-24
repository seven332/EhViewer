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
 * Created by Hippo on 1/24/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.result.VoidResult;

/**
 * A {@link retrofit2.Converter} to return {@link VoidResult}.
 */
public class VoidConverter extends EhConverter<VoidResult> {

  @NonNull
  @Override
  public VoidResult convert(String body) throws Exception {
    return VoidResult.getInstance();
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public VoidResult error(Throwable t) {
    return VoidResult.error(t);
  }
}
