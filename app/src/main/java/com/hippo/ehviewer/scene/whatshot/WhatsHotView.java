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

package com.hippo.ehviewer.scene.whatshot;

/*
 * Created by Hippo on 2/23/2017.
 */

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.view.GalleryInfoView;

public class WhatsHotView extends GalleryInfoView<WhatsHotContract.Presenter>
    implements WhatsHotContract.View {

  private EhvPreferences preferences;

  @NonNull
  @Override
  protected View onCreateToolbarContent(LayoutInflater inflater, ViewGroup parent) {
    View view = super.onCreateToolbarContent(inflater, parent);

    preferences = getEhvApp().getPreferences();

    setTitle(R.string.nav_menu_whats_hot);
    setMenu(R.menu.view_whats_hot, item -> {
      switch (item.getItemId()) {
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
    return R.id.nav_whats_hot;
  }
}
