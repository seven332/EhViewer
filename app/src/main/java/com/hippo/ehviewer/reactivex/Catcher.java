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

import rx.functions.Action1;

/**
 * {@code Catcher} can catch the error thrown by {@link Thrower1}.
 */
public abstract class Catcher implements Action1<Throwable> {

  public abstract void onCall(Throwable e);

  @Override
  public final void call(Throwable e) {
    if (e instanceof ThrowableWrapper) {
      e = ((ThrowableWrapper) e).unwrap();
    }
    onCall(e);
  }

  /**
   * Creates a {@code Catcher} from a {@code Action1<Throwable>}.
   */
  public static Catcher from(Action1<Throwable> action) {
    return new DelegateCatcher(action);
  }

  private static class DelegateCatcher extends Catcher {

    private Action1<Throwable> action;

    public DelegateCatcher(Action1<Throwable> action) {
      this.action = action;
    }

    @Override
    public void onCall(Throwable e) {
      action.call(e);
    }
  }
}
