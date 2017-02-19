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

package com.hippo.ehviewer.network;

/*
 * Created by Hippo on 2/12/2017.
 */

import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import java.util.Map;

public class PresetDns extends ByteDns {

  private static final Map<String, byte[]> HOST_MAP;

  static {
    HOST_MAP = new ArrayMap<>();
    HOST_MAP.put("www.google.com", new byte[]{(byte) 61, (byte) 91, (byte) 161, (byte) 217});
    HOST_MAP.put("forums.e-hentai.org", new byte[]{(byte) 94, (byte) 100, (byte) 18, (byte) 243});
  }

  @Nullable
  @Override
  public byte[] address(String host) {
    return HOST_MAP.get(host);
  }
}
