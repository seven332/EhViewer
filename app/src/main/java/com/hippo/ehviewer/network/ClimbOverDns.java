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
 * Created by Hippo on 1/19/2017.
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import okhttp3.Dns;

public class ClimbOverDns implements Dns {

  private static final String HOST_GOOGLE = "www.google.com";
  private static final byte[] IP_GOOGLE = new byte[]{(byte) 61, (byte) 91, (byte) 161, (byte) 217};
  private static final List<InetAddress> ADDRESSES_GOOGLE;

  static {
    List<InetAddress> list;
    try {
      list = Collections.singletonList(InetAddress.getByAddress(HOST_GOOGLE, IP_GOOGLE));
    } catch (UnknownHostException e) {
      list = null;
    }
    ADDRESSES_GOOGLE = list;
  }

  private boolean climb;

  /**
   * Use preset ip.
   */
  public void climbOver() {
    climb = true;
  }

  /**
   * Don't use preset ip.
   */
  public void climbBack() {
    climb = false;
  }

  @Override
  public List<InetAddress> lookup(String hostname) throws UnknownHostException {
    if (hostname == null) throw new UnknownHostException("hostname == null");
    if (climb && HOST_GOOGLE.equals(hostname) && ADDRESSES_GOOGLE != null) {
      return ADDRESSES_GOOGLE;
    } else {
      return Arrays.asList(InetAddress.getAllByName(hostname));
    }
  }
}
