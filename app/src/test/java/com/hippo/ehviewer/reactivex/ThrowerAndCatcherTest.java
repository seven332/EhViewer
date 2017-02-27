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

package com.hippo.ehviewer.reactivex;

/*
 * Created by Hippo on 2/27/2017.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import rx.Observable;

public class ThrowerAndCatcherTest {

  @Test
  public void testThrowAndCatch() {
    final Cover cover = new Cover();
    Observable.just(null)
        .map(Thrower1.from(o -> {
          throw new Exception("Error from Thrower1");
        }))
        .subscribe(o -> {
          fail();
        }, Catcher.from(e -> {
          cover.cover();
          assertEquals("Error from Thrower1", e.getMessage());
        }));
    assertEquals(1, cover.count());
  }

  @Test
  public void testThrowAndCatchComplete() {
    final Cover nextCover = new Cover();
    final Cover completeCover = new Cover();
    Observable.just("1", "2", "3")
        .map(Thrower1.from(str -> str))
        .subscribe(str -> nextCover.cover(),
            Catcher.from(e -> fail()),
            completeCover::cover);
    assertEquals(3, nextCover.count());
    assertEquals(1, completeCover.count());
  }

  private static class Cover {
    private int count = 0;

    public void cover() {
      ++count;
    }

    public int count() {
      return count;
    }
  }
}
