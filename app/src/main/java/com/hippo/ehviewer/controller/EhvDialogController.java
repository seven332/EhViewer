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
 * Created by Hippo on 3/18/2017.
 */

import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import com.hippo.android.dialog.conductor.AnDialogController;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.util.OpenSupplier;
import com.hippo.ehviewer.util.Supplier;

/**
 * {@link AnDialogController} for {@link com.hippo.ehviewer.activity.EhvActivity}.
 */
public abstract class EhvDialogController extends AnDialogController {

  private OpenSupplier<EhvDialogController> selfSupplier;

  protected EhvDialogController() {
    super();
  }

  @Keep
  protected EhvDialogController(Bundle args) {
    super(args);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Clear self supplier
    if (selfSupplier != null) {
      selfSupplier.set(null);
    }
  }

  /**
   * Returns a {@link Supplier} to get itself.
   * <p>
   * The {@link Supplier} returns {@code null} after itself destroyed.
   * <p>
   * Use it to avoid memory leak.
   */
  public Supplier<EhvDialogController> getSelfSupplier() {
    if (selfSupplier == null) {
      selfSupplier = new OpenSupplier<>();
      if (!isDestroyed()) {
        selfSupplier.set(this);
      }
    }
    return selfSupplier;
  }

  /**
   * Gets the {@link EhvApp}.
   */
  @Nullable
  public EhvApp getEhvApp() {
    return (EhvApp) getApplicationContext();
  }

  /**
   * Gets the {@link EhvActivity}.
   */
  @Nullable
  public EhvActivity getEhvActivity() {
    return (EhvActivity) getActivity();
  }
}
