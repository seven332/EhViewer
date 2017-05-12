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

package com.hippo.ehviewer.client;

/*
 * Created by Hippo on 2/25/2017.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.os.Build;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.EhvApp;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.N_MR1)
@RunWith(RobolectricTestRunner.class)
public class EhClientTest {

  private static final long[] GID_ARRAY = {
      1034131, 1034141, 1034139, 1034140, 1034138, 1034137, 1034125, 1034126, 1034122, 1034119, 1034118, 1034116, 1034112, 1034115, 1034111, 1034099, 1034107, 1034108, 1034110, 1034101, 1034103, 1034104, 1034102, 1034091, 1034085,
      1034086, 1034081, 1034079, 1034077, 1034076, 1034074, 1034071, 1034060, 1034070, 1033458, 1034067, 1025697, 1034062, 1034061, 1034050, 1034053, 1034055, 1034052, 1034054, 1034048, 1034043, 1034037, 1034051, 1034045, 1034047,
  };

  private static final String[] TOKEN_ARRAY = {
      "9ff2da0b6c", "ff3588b8e0", "4895e31c25", "04710adc6e", "b707c3e3c4", "63dfad82b0", "3841b50cd0", "bddf737d5a", "8b2a1d9809", "5b5aa3fd0d", "112e99817d", "ba5fc5f933", "ff3d389236", "7ff8b4998f", "8c37ba4c30", "d5e915d1ef", "c4af457127", "d29aadda97", "bc03adebf4", "7c64ef9fcf", "45517759f6", "e584c2b65f", "55e6a388c9", "89402ee520", "ffe1b06c8e",
      "a35b8014b7", "fbad23f7bf", "af84bd0997", "cf86bdc3ad", "56ed2b0ca6", "65ece3ff77", "9aa1ff6fd4", "e437e1e58a", "bca19ffda3", "5283b60061", "af0c6ac8c6", "4ae7f88e58", "081dc07086", "4e156e21b5", "fa56f4b4b8", "9b53e65985", "0bf77ca174", "2518db5d71", "ae2cdaf864", "000e986300", "caf8f64552", "5f0a7c76e0", "d35f2c4ee9", "8e97c266d9", "c12d033a47",
  };

  private EhClient client;

  @Before
  public void setUp() throws Exception {
    ShadowLog.stream = System.out;

    EhvApp app = (EhvApp) RuntimeEnvironment.application;
    client = app.getEhClient();
  }

  @Test
  public void testGetGalleryMetadata() {
    client.getGalleryMetadata(EhUrl.SITE_E, GID_ARRAY, TOKEN_ARRAY)
        .subscribe(EhSubscriber.from(
            result -> assertEquals(GID_ARRAY.length, result.galleryInfoList().size()),
            e -> fail()));

    long[] gids = Arrays.copyOfRange(GID_ARRAY, 0, 29);
    String[] tokens = Arrays.copyOfRange(TOKEN_ARRAY, 0, 29);
    client.getGalleryMetadata(EhUrl.SITE_E, gids, tokens)
        .subscribe(EhSubscriber.from(
            result -> assertEquals(gids.length, result.galleryInfoList().size()),
            e -> fail()));

    long[] gids2 = Arrays.copyOfRange(GID_ARRAY, 0, 6);
    String[] tokens2 = Arrays.copyOfRange(TOKEN_ARRAY, 0, 6);
    client.getGalleryMetadata(EhUrl.SITE_E, gids2, tokens2)
        .subscribe(EhSubscriber.from(
            result -> assertEquals(gids2.length, result.galleryInfoList().size()),
            e -> fail()));
  }

  @Test
  public void testGetGalleryMetadataDuplicate() {
    long[] gids = Arrays.copyOfRange(GID_ARRAY, 0, 10);
    String[] tokens = Arrays.copyOfRange(TOKEN_ARRAY, 0, 10);
    long[] duplicateGids = new long[gids.length * 3];
    String[] duplicateTokens = new String[tokens.length * 3];
    for (int i = 0; i < duplicateGids.length; i += gids.length) {
      System.arraycopy(gids, 0, duplicateGids, i, gids.length);
      System.arraycopy(tokens, 0, duplicateTokens, i, gids.length);
    }
    client.getGalleryMetadata(EhUrl.SITE_E, duplicateGids, duplicateTokens)
        .subscribe(EhSubscriber.from(
            result -> assertEquals(gids.length, result.galleryInfoList().size()),
            e -> fail()));
  }

  @Test
  public void testGetGalleryMetadataNull() {
    client.getGalleryMetadata(EhUrl.SITE_E, null, null)
        .subscribe(EhSubscriber.from(
            result -> assertEquals(0, result.galleryInfoList().size()),
            e -> fail()));
  }
}
