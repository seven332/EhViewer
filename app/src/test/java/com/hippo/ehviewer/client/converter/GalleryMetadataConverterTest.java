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

package com.hippo.ehviewer.client.converter;

/*
 * Created by Hippo on 2/25/2017.
 */

import static org.junit.Assert.assertEquals;

import android.os.Build;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.result.GalleryMetadataResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.N_MR1)
@RunWith(RobolectricTestRunner.class)
public class GalleryMetadataConverterTest {

  private static final String BODY = "{\n"
      + " \"gmetadata\": [\n"
      + "   {\n"
      + "     \"gid\": 618395,\n"
      + "     \"token\": \"0439fa3666\",\n"
      + "     \"archiver_key\": \"403565--d887c6dfe8aae79ed0071551aa1bafeb4a5ee361\",\n"
      + "     \"title\": \"(Kouroumu 8) [Handful☆Happiness! (Fuyuki Nanahara)] TOUHOU GUNMANIA A2 (Touhou Project)\",\n"
      + "     \"title_jpn\": \"(紅楼夢8) [Handful☆Happiness! (七原冬雪)] TOUHOU GUNMANIA A2 (東方Project)\",\n"
      + "     \"category\": \"Non-H\",\n"
      + "     \"thumb\": \"https://ehgt.org/14/63/1463dfbc16847c9ebef92c46a90e21ca881b2a12-1729712-4271-6032-jpg_l.jpg\",\n"
      + "     \"uploader\": \"avexotsukaai\",\n"
      + "     \"posted\": \"1376143500\",\n"
      + "     \"filecount\": \"20\",\n"
      + "     \"filesize\": 51210504,\n"
      + "     \"expunged\": false,\n"
      + "     \"rating\": \"4.43\",\n"
      + "     \"torrentcount\": \"0\",\n"
      + "     \"tags\": [\n"
      + "       \"parody:touhou project\",\n"
      + "       \"group:handful happiness\",\n"
      + "       \"artist:nanahara fuyuki\",\n"
      + "       \"full color\",\n"
      + "       \"artbook\"\n"
      + "     ]\n"
      + "   }\n"
      + " ]\n"
      + "}";

  @Test
  public void testConvert() throws Exception {
    GalleryMetadataResult result = new GalleryMetadataConverter().convert(BODY);
    List<GalleryInfo> list = result.galleryInfoList();
    assertEquals(1, list.size());
    GalleryInfo info = list.get(0);
    assertEquals(618395, info.gid);
    assertEquals("0439fa3666", info.token);
    assertEquals("403565--d887c6dfe8aae79ed0071551aa1bafeb4a5ee361", info.archiverKey);
    assertEquals("(Kouroumu 8) [Handful☆Happiness! (Fuyuki Nanahara)] TOUHOU GUNMANIA A2 (Touhou Project)", info.title);
    assertEquals("(紅楼夢8) [Handful☆Happiness! (七原冬雪)] TOUHOU GUNMANIA A2 (東方Project)", info.titleJpn);
    assertEquals(EhUtils.CATEGORY_NON_H, info.category);
    assertEquals("https://ehgt.org/14/63/1463dfbc16847c9ebef92c46a90e21ca881b2a12-1729712-4271-6032-jpg_l.jpg", info.coverUrl);
    assertEquals("1463dfbc16847c9ebef92c46a90e21ca881b2a12-1729712-4271-6032-jpg", info.cover);
    assertEquals("avexotsukaai", info.uploader);
    assertEquals(1376143500, info.date);
    assertEquals(20, info.pages);
    assertEquals(51210504, info.size);
    assertEquals(false, info.invalid);
    assertEquals(4.43f, info.rating, 0.0f);
    assertEquals(0, info.torrentCount);
    assertEquals(4, info.tags.size());
    assertEquals(Collections.singletonList("touhou project"), info.tags.get("parody"));
    assertEquals(Collections.singletonList("handful happiness"), info.tags.get("group"));
    assertEquals(Collections.singletonList("nanahara fuyuki"), info.tags.get("artist"));
    assertEquals(Arrays.asList("full color", "artbook"), info.tags.get("misc"));
  }
}
