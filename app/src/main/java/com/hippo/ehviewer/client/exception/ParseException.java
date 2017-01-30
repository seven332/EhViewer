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

package com.hippo.ehviewer.client.exception;

/*
 * Created by Hippo on 1/18/2017.
 */

import com.hippo.ehviewer.client.EhConverter;
import okhttp3.HttpUrl;

/**
 * Signals that html/json/others parsing failed
 * in {@link com.hippo.ehviewer.client.EhClient}.
 */
public class ParseException extends Exception {

  private HttpUrl url;
  private String body;

  public ParseException(String message, String body) {
    super(message);
    this.body = body;
  }

  public ParseException(String message, String body, Throwable cause) {
    super(message, cause);
    this.body = body;
  }

  /**
   * We can't get url in {@link EhConverter}.
   * So set it in {@link com.hippo.ehviewer.client.EhSubscriber}.
   */
  public void url(HttpUrl url) {
    this.url = url;
  }

  public HttpUrl url() {
    return url;
  }

  public String body() {
    return body;
  }
}
