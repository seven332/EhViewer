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
import android.util.Log;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import okhttp3.Dns;

public abstract class ByteDns implements Dns {

  private static final String LOG_TAG = ByteDns.class.getSimpleName();

  @Nullable
  public abstract byte[] address(String host);

  @Override
  public List<InetAddress> lookup(String hostname) throws UnknownHostException {
    byte[] addr = address(hostname);
    if (addr != null) {
      try {
        InetAddress ia = InetAddress.getByAddress(hostname, addr);
        return Collections.singletonList(ia);
      } catch (UnknownHostException e) {
        Log.e(LOG_TAG, "Bad byte address: " + Arrays.toString(addr));
      }
    }
    return Arrays.asList(InetAddress.getAllByName(hostname));
  }
}
