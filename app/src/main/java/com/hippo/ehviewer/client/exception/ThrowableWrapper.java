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
 * Created by Hippo on 1/19/2017.
 */

/**
 * A {@link RuntimeException} wrapper of {@link Throwable}.
 */
public class ThrowableWrapper extends RuntimeException {

  private final Throwable origin;

  public ThrowableWrapper(Throwable origin) {
    this.origin = origin;
  }

  public Throwable unwrap() {
    return origin;
  }

  /**
   * Wraps a {@link Throwable}.
   */
  public static ThrowableWrapper wrap(Throwable origin) {
    return new ThrowableWrapper(origin);
  }
}
