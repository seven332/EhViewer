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

package com.hippo.ehviewer.view;

/*
 * Created by Hippo on 5/14/2017.
 */

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.presenter.PresenterInterface;

/**
 * A view with a custom theme.
 * Override {@link #getThemeResId()} to change theme.
 */
public abstract class ThemedView<P extends PresenterInterface> extends SceneView<P> {

  private Context context;

  @NonNull
  @Override
  protected final View onCreate(LayoutInflater inflater, ViewGroup parent) {
    context = inflater.getContext();
    int themeResId = getThemeResId();
    if (themeResId != 0) {
      context = new ContextThemeWrapper(context, themeResId);
      inflater = inflater.cloneInContext(context);
    }
    return onCreate2(inflater, parent);
  }

  /**
   * Creates the actual view.
   */
  protected abstract View onCreate2(LayoutInflater inflater, ViewGroup parent);

  /**
   * Returns the theme for this view.
   * <p>
   * Override it to change the theme.
   * <p>
   * Default: {@code 0}, default theme.
   */
  protected int getThemeResId() {
    return 0;
  }

  /**
   * Returns the themed context.
   */
  @NonNull
  protected Context getContext() {
    return context;
  }

  /**
   * Gets {@code Resources} of the themed context.
   */
  @NonNull
  public Resources getResources() {
    return getContext().getResources();
  }

  /**
   * Gets a string from {@code Resources} of the themed context.
   */
  public String getString(@StringRes int resId) {
    return getResources().getString(resId);
  }
}
