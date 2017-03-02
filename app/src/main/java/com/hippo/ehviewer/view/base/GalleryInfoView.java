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
 * Created by Hippo on 2/24/2017.
 */

import android.support.annotation.CallSuper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.component.base.GalleryInfoAdapter;
import com.hippo.ehviewer.contract.base.GalleryInfoContract;
import com.hippo.ehviewer.util.ExceptionExplainer;
import com.hippo.ehviewer.widget.AutoGridLayoutManager;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.ehviewer.widget.DividerItemDecoration;
import com.hippo.yorozuya.android.LayoutUtils;
import com.hippo.yorozuya.android.ResourcesUtils;

public abstract class GalleryInfoView<P extends GalleryInfoContract.Presenter>
    extends ToolbarView<P> {

  private ContentLayout layout;
  private GalleryInfoAdapter adapter;
  private AutoGridLayoutManager layoutManager;
  private EhvPreferences preferences;

  @CallSuper
  @Override
  protected View createContentView(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.controller_gallery_info_list, parent, false);
    layout = (ContentLayout) view;

    preferences = getApplication().getPreferences();

    adapter = getPresenter().attachContentLayout(getActivity(), layout);
    layout.setExtension(new ContentLayoutExtension());
    layoutManager = new AutoGridLayoutManager(getActivity(), -1);
    layout.setLayoutManager(layoutManager);
    if (preferences.getListMode() == EhvPreferences.LIST_MODE_DETAIL) {
      applyDetailInternal();
    } else {
      applyBriefInternal();
    }

    return view;
  }

  /**
   * Switches to detail list mode, and save it to preferences.
   */
  protected void applyDetail() {
    preferences.putListMode(EhvPreferences.LIST_MODE_DETAIL);
    applyDetailInternal();
  }

  private void applyDetailInternal() {
    adapter.showDetail();

    int columnSize = LayoutUtils.dp2pix(getActivity(), preferences.getListDetailSize());
    layoutManager.setColumnSize(columnSize);

    layout.removeAllItemDecorations();
    DividerItemDecoration decoration = new DividerItemDecoration(
        ResourcesUtils.getAttrColor(getActivity(), R.attr.dividerColor),
        getResources().getDimensionPixelSize(R.dimen.divider_thickness)
    );
    layout.addItemDecoration(decoration);
    layout.setRecyclerViewPadding(0, 0, 0, 0);
  }

  /**
   * Switches to brief list mode, and save it to preferences.
   */
  protected void applyBrief() {
    preferences.putListMode(EhvPreferences.LIST_MODE_BRIEF);
    applyBriefInternal();
  }

  private void applyBriefInternal() {
    adapter.showBrief();

    int columnSize = LayoutUtils.dp2pix(getActivity(), preferences.getListBriefSize());
    layoutManager.setColumnSize(columnSize);

    layout.removeAllItemDecorations();
    int padding = LayoutUtils.dp2pix(getActivity(), 1);
    layout.setRecyclerViewPadding(padding, padding, padding, padding);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    getPresenter().detachContentLayout(layout);
  }

  private class ContentLayoutExtension implements ContentLayout.Extension {

    @Override
    public ContentLayout.TipInfo getTipFromThrowable(Throwable e) {
      // TODO
      e.printStackTrace();

      ContentLayout.TipInfo info = new ContentLayout.TipInfo();
      info.icon = ExceptionExplainer.explainVividly(getActivity(), e);
      info.text = ExceptionExplainer.explain(getActivity(), e);
      return info;
    }

    @Override
    public void showMessage(String message) {
      getActivity().showMessage(message);
    }
  }
}
