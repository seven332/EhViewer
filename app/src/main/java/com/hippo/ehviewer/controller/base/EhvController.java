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

package com.hippo.ehviewer.controller.base;

/*
 * Created by Hippo on 2/19/2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bluelinelabs.conductor.Controller;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.presenter.base.ControllerPresenter;
import com.hippo.ehviewer.util.OpenSupplier;
import com.hippo.ehviewer.util.Supplier;
import com.hippo.ehviewer.view.base.EhvView;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;

/**
 * Base {@link com.bluelinelabs.conductor.Controller}
 * for {@link com.hippo.ehviewer.activity.EhvActivity}.
 */
public abstract class EhvController<P extends ControllerPresenter, V extends EhvView>
    extends MvpController<P, V> {

  private static Map<Class<? extends Controller>, Class<? extends EhvView>> CONTROLLER_VIEW_MAP =
      new HashMap<>();

  /**
   * Registers controller class and view class pair.
   * <p>
   * Must be called in every non-abstract {@code EhvController} static initialize part.
   */
  protected static void register(Class<? extends EhvController> c, Class<? extends EhvView> v) {
    CONTROLLER_VIEW_MAP.put(c, v);
  }

  /**
   * Gets view class for the controller class.
   */
  @Nullable
  public static Class<? extends EhvView> getViewClass(Class<? extends Controller> c) {
    return CONTROLLER_VIEW_MAP.get(c);
  }

  private OpenSupplier<EhvController> selfSupplier;

  public EhvController(Bundle args) {
    super(args);
  }

  @Override
  protected void onCreateView(@NonNull V view) {
    super.onCreateView(view);
    EhvApp app = (EhvApp) getApplicationContext();
    EhvActivity activity = (EhvActivity) getActivity();
    Assert.assertNotNull(app);
    Assert.assertNotNull(activity);
    view.setEhvApp(app);
    view.setEhvActivity(activity);
    view.setEhvController(this);
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
  public Supplier<EhvController> getSelfSupplier() {
    if (selfSupplier == null) {
      selfSupplier = new OpenSupplier<>();
      if (!isDestroyed()) {
        selfSupplier.set(this);
      }
    }
    return selfSupplier;
  }
}
