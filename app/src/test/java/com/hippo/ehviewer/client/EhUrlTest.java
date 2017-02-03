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
 * Created by Hippo on 2/3/2017.
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EhUrlTest {

  @Test
  public void testGetFingerprint() {
    String url = "https://exhentai.org/t/e8/50/e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg_l.jpg";
    assertEquals("e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg", EhUrl.getImageFingerprint(url));
    assertEquals("e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg_l", EhUrl.getThumbnailFingerprint(url));
  }
}
