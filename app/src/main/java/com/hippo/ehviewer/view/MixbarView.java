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
 * Created by Hippo on 2/10/2017.
 */

import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.presenter.PresenterInterface;
import com.hippo.ehviewer.widget.Mixbar;

/**
 * {@code MixbarView} shows a Mixbar at top.
 */
public abstract class MixbarView<P extends PresenterInterface> extends EhvView<P> {

  private Mixbar mixbar;

  @NonNull
  @Override
  protected final View onCreateView(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.controller_mixbar, parent, false);

    mixbar = (Mixbar) view.findViewById(R.id.mixbar);
    ViewGroup container = (ViewGroup) view.findViewById(R.id.content_container);

    View contentView = createContentView(inflater, container);
    container.addView(contentView);

    return view;
  }

  /**
   * Creates content view for this {@code MixbarView}.
   */
  protected abstract View createContentView(LayoutInflater inflater, ViewGroup parent);

  /**
   * Return the Mixbar.
   */
  public Mixbar getMixbar() {
    return mixbar;
  }

  /**
   * Set title for the {@code Mixbar}.
   */
  public void setTitle(int resId) {
    mixbar.getToolbar().setTitle(resId);
  }

  /**
   * Set title for the {@code Mixbar}.
   */
  public void setTitle(CharSequence title) {
    mixbar.getToolbar().setTitle(title);
  }

  /**
   * Starts action mode.
   *
   * @return the action bar
   */
  public Toolbar startActionMode() {
    return mixbar.startActionMode();
  }

  /**
   * Ends action mode.
   */
  public void endActionMode() {
    mixbar.endActionMode();
  }
}
