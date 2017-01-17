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
 * Created by Hippo on 1/17/2017.
 */

import com.hippo.ehviewer.network.publicsuffix.PublicSuffixPatterns;

/**
 * {@code PublicSuffix} helps you to check whether a domain is a <i>public suffix</i>
 * using {@link PublicSuffixPatterns}.
 */
// https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/net/InternetDomainName.java
public class PublicSuffix {

  private static final String DOT_REGEX = "\\.";

  /**
   * Indicates whether this domain name represents a <i>public suffix</i>, as defined by the Mozilla
   * Foundation's <a href="http://publicsuffix.org/">Public Suffix List</a> (PSL). A public suffix
   * is one under which Internet users can directly register names, such as {@code com},
   * {@code co.uk} or {@code pvt.k12.wy.us}. Examples of domain names that are <i>not</i> public
   * suffixes include {@code ehviewer}, {@code ehviewer.com} and {@code foo.co.uk}.
   */
  public static boolean isPublicSuffix(String domain) {
    if (domain == null) return false;
    if (PublicSuffixPatterns.EXACT.containsKey(domain)) return true;
    if (PublicSuffixPatterns.EXCLUDED.containsKey(domain)) return false;
    if (matchesWildcardPublicSuffix(domain)) return true;
    return false;
  }

  /**
   * Does the domain name match one of the "wildcard" patterns (e.g. {@code "*.ar"})?
   */
  private static boolean matchesWildcardPublicSuffix(String domain) {
    final String[] pieces = domain.split(DOT_REGEX, 2);
    return pieces.length == 2 && PublicSuffixPatterns.UNDER.containsKey(pieces[1]);
  }
}
