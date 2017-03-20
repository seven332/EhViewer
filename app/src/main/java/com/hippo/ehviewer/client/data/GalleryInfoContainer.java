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
 * Created by Hippo on 3/5/2017.
 */

import android.support.annotation.Nullable;

/**
 * {@code GalleryInfoContainer} contains a {@link GalleryInfo}.
 */
public interface GalleryInfoContainer {

  /**
   * Returns the {@link GalleryInfo}.
   */
  @Nullable
  GalleryInfo getGalleryInfo();

  /**
   * Sets {@link GalleryInfo}.
   */
  void setGalleryInfo(GalleryInfo info);
}
