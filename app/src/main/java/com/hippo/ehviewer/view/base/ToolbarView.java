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

package com.hippo.ehviewer.view.base;

/*
 * Created by Hippo on 2/8/2017.
 */

import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.presenter.PresenterInterface;

/**
 * {@code ToolbarView} shows a Toolbar at top.
 */
public abstract class ToolbarView<P extends PresenterInterface> extends EhvView<P> {

  private Toolbar toolbar;

  @NonNull
  @Override
  protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.controller_toolbar, parent, false);

    toolbar = (Toolbar) view.findViewById(R.id.toolbar);
    ViewGroup container = (ViewGroup) view.findViewById(R.id.content_container);

    View contentView = createContentView(inflater, container);
    container.addView(contentView);

    return view;
  }

  /**
   * Creates content view for this {@code ToolbarView}.
   */
  protected abstract View createContentView(LayoutInflater inflater, ViewGroup parent);

  /**
   * Return the Toolbar.
   */
  public Toolbar getToolbar() {
    return toolbar;
  }

  /**
   * Set title for the {@code Toolbar}.
   */
  public void setTitle(int resId) {
    toolbar.setTitle(resId);
  }

  /**
   * Set title for the {@code Toolbar}.
   */
  public void setTitle(CharSequence title) {
    toolbar.setTitle(title);
  }

  /**
   * Sets navigation icon for the {@code Toolbar}.
   */
  public void setNavigationIcon(@DrawableRes int resId) {
    toolbar.setNavigationIcon(resId);
  }

  /**
   * Sets navigation on click listener for the {@code Toolbar}.
   */
  public void setNavigationOnClickListener(View.OnClickListener l) {
    toolbar.setNavigationOnClickListener(l);
  }

  /**
   * Sets menu for the {@code Toolbar}.
   */
  public void setMenu(@MenuRes int resId, Toolbar.OnMenuItemClickListener listener) {
    toolbar.inflateMenu(resId);
    toolbar.setOnMenuItemClickListener(listener);
  }
}
