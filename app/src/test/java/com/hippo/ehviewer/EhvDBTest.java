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

package com.hippo.ehviewer;

/*
 * Created by Hippo on 3/6/2017.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.os.Build;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.client.data.GalleryInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.N_MR1)
@RunWith(RobolectricTestRunner.class)
public class EhvDBTest {

  @Before
  public void before() {
    ShadowLog.stream = System.out;
  }

  @Test
  public void testPutGetFavouritesItem() {
    EhvApp app = (EhvApp) RuntimeEnvironment.application;
    EhvDB db = app.getDb();

    GalleryInfo info = new GalleryInfo();
    info.gid = 100;
    info.token = "abc";
    FavouritesItem item = new FavouritesItem();
    item.info = info;
    item.note = "ha";
    item.date = System.currentTimeMillis();

    db.putFavouritesItem(item);

    FavouritesItem fi = db.getFavouritesItem(100);
    assertNotNull(fi);
    assertNotNull(fi.info);
    assertEquals(info.gid, fi.info.gid);
    assertEquals(info.token, fi.info.token);
    assertEquals(item.note, fi.note);
    assertEquals(item.date, fi.date);
  }
}
