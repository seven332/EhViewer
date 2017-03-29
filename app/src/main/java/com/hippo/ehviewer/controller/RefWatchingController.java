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

package com.hippo.ehviewer.controller;

/*
 * Created by Hippo on 2/20/2017.
 */

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.ControllerChangeHandler;
import com.bluelinelabs.conductor.ControllerChangeType;
import com.hippo.ehviewer.EhvApp;

/**
 * {@code RefWatchingController} watches itself after destroyed.
 */
public abstract class RefWatchingController extends Controller {

  private boolean hasExited;

  protected RefWatchingController() {
    super();
  }

  protected RefWatchingController(@Nullable Bundle args) {
    super(args);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (hasExited) {
      watchSelf();
    }
  }

  @Override
  protected void onChangeEnded(@NonNull ControllerChangeHandler changeHandler,
      @NonNull ControllerChangeType changeType) {
    super.onChangeEnded(changeHandler, changeType);
    hasExited = !changeType.isEnter;
    if (isDestroyed()) {
      watchSelf();
    }
  }

  private void watchSelf() {
    Context context = getApplicationContext();
    if (context instanceof EhvApp) {
      ((EhvApp) context).getRefWatcher().watch(this);
    }
  }
}
