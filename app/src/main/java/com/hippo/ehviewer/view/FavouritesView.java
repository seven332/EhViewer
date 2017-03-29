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
 * Created by Hippo on 3/2/2017.
 */

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.contract.FavouritesContract;
import com.hippo.ehviewer.view.base.GalleryInfoView;

public class FavouritesView extends GalleryInfoView<FavouritesContract.Presenter>
    implements FavouritesContract.View {

  private EhvPreferences preferences;

  @Override
  protected View createContentView(LayoutInflater inflater, ViewGroup parent) {
    View view = super.createContentView(inflater, parent);

    preferences = getEhvApp().getPreferences();

    setMenu(R.menu.view_favourites, item -> {
      switch (item.getItemId()) {
        case R.id.action_search:
          // TODO
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
    return R.id.nav_favourites;
  }

  @Override
  protected int getNavigationType() {
    return NAVIGATION_TYPE_RETURN;
  }
}
