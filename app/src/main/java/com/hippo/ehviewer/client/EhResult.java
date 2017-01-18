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

package com.hippo.ehviewer.client;

/*
 * Created by Hippo on 1/18/2017.
 */

/**
 * A base result for {@link com.hippo.ehviewer.client.EhClient}.
 * <p>
 * {@link retrofit2.adapter.rxjava.Result} in {@link retrofit2.adapter.rxjava.RxJavaCallAdapterFactory}
 * doesn't support error and response at the same time.
 * But I need response to fix error, so wrap error in result.
 */
public abstract class EhResult {

  private final Throwable error;

  public EhResult(Throwable error) {
    this.error = error;
  }

  /**
   * Only present when {@link #isError()} is true, null otherwise.
   */
  public Throwable error() {
    return error;
  }

  /**
   * Returns {@code true} if the result is an error.
   */
  public boolean isError() {
    return error != null;
  }
}
