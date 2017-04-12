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
 * Created by Hippo on 4/12/2017.
 */

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.presenter.PresenterInterface;
import com.hippo.ehviewer.widget.EhvDrawerContent;
import com.hippo.yorozuya.android.ResourcesUtils;

/**
 * A view with a status bar on the top.
 */
public abstract class StatusBarView<P extends PresenterInterface> extends EhvView<P>
    implements StatusBarViewImpl, EhvDrawerContent.OnGetWindowPaddingTopListener {

  private View statusBar;
  private EhvDrawerContent drawerContent;

  @CallSuper
  @NonNull
  @Override
  protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.controller_status_bar, parent, false);

    statusBar = view.findViewById(R.id.status_bar);
    drawerContent = (EhvDrawerContent) parent;
    // Sets status bar height
    drawerContent.addOnGetWindowPaddingTopListener(this);
    // Sets status bar color
    statusBar.setBackgroundColor(getStatusBarColor());

    // Sets content
    ViewGroup container = (ViewGroup) view.findViewById(R.id.status_bar_content_container);
    View content = onCreateStatusBarContent(inflater, container);
    container.addView(content);

    return view;
  }

  @Override
  public void onGetWindowPaddingTop(int top) {
    ViewGroup.LayoutParams lp = statusBar.getLayoutParams();
    lp.height = top;
    statusBar.setLayoutParams(lp);
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    drawerContent.removeOnGetWindowPaddingTopListener(this);
  }

  /**
   * Creates the content for {@link StatusBarView}.
   */
  @NonNull
  protected abstract View onCreateStatusBarContent(LayoutInflater inflater, ViewGroup parent);

  @Override
  public void setStatusBarColor(int color) {
    if (statusBar != null) {
      statusBar.setBackgroundColor(color);
    }
  }

  /**
   * Returns the default status bar color for this view.
   */
  @Override
  public int getStatusBarColor() {
    return ResourcesUtils.getAttrColor(getEhvActivity(), R.attr.colorPrimaryDark);
  }
}
