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

package com.hippo.ehviewer.client.data;

/*
 * Created by Hippo on 5/16/2017.
 */

/**
 * Api uid and api key can be get from gallery detail page.
 * They are used to vote comment.
 */
public final class ApiUidKey {

  public final long uid;
  public final String key;

  public ApiUidKey(long uid, String key) {
    this.uid = uid;
    this.key = key;
  }
}
