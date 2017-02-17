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

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@code ComplexTask} is not suitable to run in UI thread.
 */
public abstract class ComplexTask<Param, Progress, Result> {

  @IntDef({STATE_NONE, STATE_RUNNING, STATE_SUCCESS, STATE_FAILURE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface State {}

  public static final int STATE_NONE = 0;
  public static final int STATE_RUNNING = 1;
  public static final int STATE_SUCCESS = 2;
  public static final int STATE_FAILURE = 3;

  @State
  private int state = STATE_NONE;

  private Progress progress;
  private Result result;
  private Throwable error;

  public final void start(Param param) {
    if (state == STATE_RUNNING) {
      throw new IllegalStateException("Can't start a running task");
    }

    state = STATE_RUNNING;
    result = null;
    error = null;
    onStart(param);
  }

  public final void progress(Progress progress) {
    if (state != STATE_RUNNING) {
      throw new IllegalStateException("Can only call progress() in running task");
    }

    this.progress = progress;
    onProgress(progress);
  }

  public final void success(Result result) {
    if (state != STATE_RUNNING) {
      throw new IllegalStateException("Can only call success() in running task");
    }

    this.state = STATE_SUCCESS;
    this.progress = null;
    this.result = result;
    onSuccess(result);
  }

  public final void failure(@NonNull Throwable e) {
    if (state != STATE_RUNNING) {
      throw new IllegalStateException("Can only call failure() in running task");
    }

    this.state = STATE_FAILURE;
    this.progress = null;
    this.error = e;
    onFailure(e);
  }

  public void onStart(Param param) {}

  public void onProgress(Progress progress) {}

  public void onSuccess(Result result) {}

  public void onFailure(Throwable e) {}

  @State
  public int getState() {
    return state;
  }

  public Progress getProgress() {
    return progress;
  }

  public Result getResult() {
    return result;
  }

  public Throwable getError() {
    return error;
  }
}
