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

package com.hippo.ehviewer.client.data;

/*
 * Created by Hippo on 5/17/2017.
 */

import static org.junit.Assert.assertEquals;

import android.os.Parcel;
import com.hippo.ehviewer.client.EhUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class GalleryInfoTest {

  @Test
  public void testParcel() {
    Parcel parcel = Parcel.obtain();

    GalleryInfo info = new GalleryInfo();
    info.gid = 100;
    info.token = "0123456789";
    info.title = "HaHa";
    info.titleJpn = "XiXi";
    info.cover = "aaa";
    info.coverUrl = "https://aaa.jpg";
    info.coverRatio = 1.5f;
    info.category = EhUtils.CATEGORY_DOUJINSHI;
    info.date = 324;
    info.uploader = "XiHa";
    info.rating = 9.9f;
    info.rated = 2345324;
    info.language = EhUtils.LANG_DE;
    info.favourited = 22;
    info.favouriteSlot = 324;
    info.invalid = true;
    info.archiverKey = "BB";
    info.pages = 99;
    info.size = 4324325634L;
    info.torrentCount = 42;
    info.tagSet.add("xi", "ha");
    info.parentGid = 12121;
    info.parentToken = "21312";
    info.childGid = 12121;
    info.childToken = "21312";

    info.writeToParcel(parcel, 0);
    parcel.setDataPosition(0);

    GalleryInfo copy = GalleryInfo.CREATOR.createFromParcel(parcel);
    assertEquals(info, copy);

    parcel.recycle();
  }
}
