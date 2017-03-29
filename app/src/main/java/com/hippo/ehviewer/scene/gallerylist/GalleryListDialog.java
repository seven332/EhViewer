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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.afollestad.materialdialogs.MaterialDialog;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.controller.EhvDialogController;
import com.hippo.ehviewer.util.Supplier;
import junit.framework.Assert;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/*
 * Created by Hippo on 3/7/2017.
 */

public class GalleryListDialog extends EhvDialogController {

  private static final String KEY_GALLERY_INFO = "gallery_list_dialog:info";

  private GalleryInfo info;

  /**
   * Only for restore. Call {@link #create(GalleryInfo)} please.
   */
  @Deprecated
  public GalleryListDialog(@NonNull Bundle bundle) {
    super(bundle);
    info = bundle.getParcelable(KEY_GALLERY_INFO);
  }

  /**
   * Creates a {@code GalleryListDialog}.
   */
  @SuppressWarnings("deprecation")
  public static GalleryListDialog create(@NonNull GalleryInfo info) {
    Bundle bundle = new Bundle();
    bundle.putParcelable(KEY_GALLERY_INFO, info);
    return new GalleryListDialog(bundle);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog() {
    Context context = getActivity();
    Assert.assertNotNull(context);
    return new MaterialDialog.Builder(context)
        .items(R.array.gallery_list_entries)
        .itemsCallback((dialog, view, i, charSequence) -> {
          // Prepare context data
          EhvApp app = getEhvApp();
          EhvActivity activity = getEhvActivity();
          if (app == null || activity == null) {
            return;
          }
          Supplier<EhvActivity> supplier = activity.getSelfSupplier();

          switch (i) {
            case 0:
              addToFavourites(app, supplier, info);
              break;
          }
        })
        .build();
  }

  private static void addToFavourites(
      EhvApp app, Supplier<EhvActivity> supplier, GalleryInfo info) {
    EhvDB db = app.getDb();

    FavouritesItem item = new FavouritesItem();
    item.info = info;
    item.date = System.currentTimeMillis();

    db.putFavouritesItem(item)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(putResult -> {
          EhvActivity activity = supplier.get();
          if (activity != null) {
            activity.showMessage(R.string.add_to_favourites_success);
          }
        }, e -> {
          // TODO
          e.printStackTrace();

          EhvActivity activity = supplier.get();
          if (activity != null) {
            activity.showMessage(R.string.add_to_favourites_failure);
          }
        });
  }
}
