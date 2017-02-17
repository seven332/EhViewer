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

package com.hippo.ehviewer.util;

/*
 * Created by Hippo on 2/17/2017.
 */

import com.hippo.yorozuya.HashCodeUtils;
import com.hippo.yorozuya.ObjectUtils;

public class Triple<L,M,R> {

  public final L left;
  public final M middle;
  public final R right;

  public Triple(L left, M middle, R right) {
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Triple)) {
      return false;
    }
    Triple<?, ?, ?> p = (Triple<?, ?, ?>) o;
    return ObjectUtils.equals(p.left, left)
        && ObjectUtils.equals(p.middle, middle)
        && ObjectUtils.equals(p.right, right);
  }

  @Override
  public int hashCode() {
    return HashCodeUtils.hashCode(left, middle, right);
  }

  @Override
  public String toString() {
    return "Triple{"
        + String.valueOf(left) + " "
        + String.valueOf(middle) + " "
        + String.valueOf(right) + "}";
  }

  /**
   * Convenience method for creating an appropriately typed triple.
   * @param a the left object in the Triple
   * @param b the middle object in the Triple
   * @param c the right object in the Triple
   * @return a Triple that is templatized with the types of a, b, c
   */
  public static <A, B, C> Triple<A, B, C> create(A a, B b, C c) {
    return new Triple<>(a, b, c);
  }
}
