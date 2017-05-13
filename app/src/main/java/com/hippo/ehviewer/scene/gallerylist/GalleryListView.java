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

package com.hippo.ehviewer.scene.gallerylist;

/*
 * Created by Hippo on 2/10/2017.
 */

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.GLUrlBuilder;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.scene.gallerysearch.GallerySearchScene;
import com.hippo.ehviewer.view.GalleryInfoView;
import com.hippo.ehviewer.widget.ContentLayout;

public class GalleryListView extends GalleryInfoView<GalleryListContract.Presenter>
    implements GalleryListContract.View {

  private EhvPreferences preferences;

  @Override
  protected View onCreateToolbarContent(LayoutInflater inflater, ViewGroup parent) {
    View view = super.onCreateToolbarContent(inflater, parent);

    preferences = getEhvApp().getPreferences();

    ContentLayout layout = (ContentLayout) view;
    layout.setOnItemLongClickListener((recyclerView, holder) -> {
      int index = holder.getAdapterPosition();
      if (index == RecyclerView.NO_POSITION) {
        return false;
      }
      GalleryInfo info = getGalleryInfo(index);
      if (info == null) {
        return false;
      }
      // TODO getEhvActivity().showDialog(new GalleryListDialog(info));
      return true;
    });

    setMenu(R.menu.view_gallery_list, item -> {
      switch (item.getItemId()) {
        case R.id.action_search:
          getEhvActivity().pushScene(GallerySearchScene.create((GalleryListScene) getEhvScene()));
          return true;
        case R.id.action_go_to:
          // TODO
          return true;
        case R.id.action_detail:
          applyDetail();
          updateMenuLater();
          return true;
        case R.id.action_brief:
          applyBrief();
          updateMenuLater();
          return true;
        default:
          return false;
      }
    });
    updateMenu();

    return view;
  }

  // If we update menu immediately, the text in pop dialog is changed immediately
  // while pop dialog showing. That's odd.
  private void updateMenuLater() {
    // TODO 300ms seems fine, but we need accurate pop dialog fade animation duration.
    schedule(this::updateMenu, 300);
  }

  private void updateMenu() {
    Menu menu = getMenu();
    if (preferences.getListMode() == EhvPreferences.LIST_MODE_DETAIL) {
      menu.findItem(R.id.action_detail).setVisible(false);
      menu.findItem(R.id.action_brief).setVisible(true);
    } else {
      menu.findItem(R.id.action_detail).setVisible(true);
      menu.findItem(R.id.action_brief).setVisible(false);
    }
  }

  @Override
  protected int getLeftDrawerCheckedItem() {
    return R.id.nav_homepage;
  }

  @Override
  protected int getNavigationType() {
    return NAVIGATION_TYPE_MENU;
  }

  @Nullable
  private GalleryInfo getGalleryInfo(int index) {
    GalleryListContract.Presenter presenter = getPresenter();
    if (presenter != null) {
      return presenter.getGalleryInfo(index);
    } else {
      return null;
    }
  }

  private String getTitleForGLUrlBuilder(GLUrlBuilder builder) {
    String title = null;
    int titleResId = 0;

    if (builder.getCategory() == EhUtils.NONE) {
      titleResId = R.string.nav_menu_homepage;
    }

    if (titleResId != 0) {
      title = getString(titleResId);
    }

    return title;
  }

  @Override
  public void onUpdateGLUrlBuilder(GLUrlBuilder builder) {
    getEhvActivity().setLeftDrawerCheckedItem(builder.getCategory() == EhUtils.NONE
        ? R.id.nav_homepage
        : 0);
    setTitle(getTitleForGLUrlBuilder(builder));
  }
}
