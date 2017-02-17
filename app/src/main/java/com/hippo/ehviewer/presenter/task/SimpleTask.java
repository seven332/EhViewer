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

package com.hippo.ehviewer.presenter.task;

/*
 * Created by Hippo on 2/16/2017.
 */

import android.support.annotation.UiThread;

/**
 * {@code SimpleTask} is simple enough to run in UI thread.
 */
public abstract class SimpleTask<Result> {

  private boolean hasCalled;
  private Result result;

  @UiThread
  public final void start() {
    hasCalled = true;
    result = onStart();
  }

  public abstract Result onStart();

  public boolean hasCalled() {
    return hasCalled;
  }

  public Result getResult() {
    return result;
  }
}
