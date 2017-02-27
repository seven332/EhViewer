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

import android.support.annotation.NonNull;
import rx.functions.Func1;

/**
 * {@code Thrower1} can throw Throwable.
 * <p>
 * Must let {@link Catcher} to catch the error.
 */
public abstract class Thrower1<T, R> implements Func1<T, R> {

  public abstract R onCall(T t) throws Throwable;

  @Override
  public final R call(T t) {
    try {
      return onCall(t);
    } catch (Throwable e) {
      throw ThrowableWrapper.wrap(e);
    }
  }

  /**
   * Creates a {@code Thrower1} from a {@code ThrowableFunc1}.
   */
  public static <T, R> Thrower1<T, R> from(@NonNull ThrowableFunc1<T, R> func) {
    return new DelegateThrower1<>(func);
  }

  private static class DelegateThrower1<T, R> extends Thrower1<T,R> {

    private ThrowableFunc1<T, R> func;

    public DelegateThrower1(ThrowableFunc1<T, R> func) {
      this.func = func;
    }

    @Override
    public R onCall(T t) throws Throwable {
      return func.call(t);
    }
  }
}
