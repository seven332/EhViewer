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

package com.hippo.ehviewer.client.param;

/*
 * Created by Hippo on 2/24/2017.
 */

import android.support.annotation.NonNull;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class GalleryMetadataParam {

  public static final MediaType MEDIA_TYPE_JSON =
      MediaType.parse("application/json; charset=utf-8");

  private long[] gids;
  private String[] tokens;

  public GalleryMetadataParam(@NonNull long[] gids, @NonNull String[] tokens) {
    this.gids = gids;
    this.tokens = tokens;
  }

  public String toJson() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"method\":\"gdata\",\"gidlist\":[");
    for (int i = 0, n = Math.min(gids.length, tokens.length); i < n; ++i) {
      if (i != 0) {
        sb.append(",");
      }
      sb.append("[").append(gids[i]).append(",\"").append(tokens[i]).append("\"]");
    }
    sb.append("],\"namespace\":1}");
    return sb.toString();
  }

  public static class Converter implements retrofit2.Converter<GalleryMetadataParam, RequestBody> {

    @Override
    public RequestBody convert(GalleryMetadataParam value) throws IOException {
      String json = value.toJson();
      return RequestBody.create(MEDIA_TYPE_JSON, json);
    }
  }
}
