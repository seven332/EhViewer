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

import android.net.Uri;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;

/**
 * A {@code EhCacheKeyFactory} detects ehentai thumbnail url,
 * create the same source url for the thumbnail urls with the same fingerprint.
 */
public class EhCacheKeyFactory extends DefaultCacheKeyFactory {

  private static final String EH_THUMBNAIL_SCHEME = "eh_thumbnail";

  @Override
  protected Uri getCacheKeySourceUri(Uri sourceUri) {
    String url = sourceUri.toString();
    String fingerprint = EhUrl.getThumbnailFingerprint(url);
    if (fingerprint != null) {
      return new Uri.Builder().scheme(EH_THUMBNAIL_SCHEME).path(fingerprint).build();
    } else {
      return sourceUri;
    }
  }
}
