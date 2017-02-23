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

package com.hippo.ehviewer.client.result;

/*
 * Created by Hippo on 2/12/2017.
 */

import android.support.annotation.Nullable;
import com.hippo.ehviewer.client.EhResult;

public class ProfileResult extends EhResult {

  private String name;
  private String avatar;

  public ProfileResult(String name, String avatar) {
    super(null);
    this.name = name;
    this.avatar = avatar;
  }

  public String name() {
    return name;
  }

  @Nullable
  public String avatar() {
    return avatar;
  }


  ////////////////
  // Pain part
  ////////////////

  private ProfileResult(Throwable t) {
    super(t);
  }

  public static ProfileResult error(Throwable t) {
    return new ProfileResult(t);
  }
}
