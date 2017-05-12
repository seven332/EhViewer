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
 * Created by Hippo on 1/18/2017.
 */

import retrofit2.adapter.rxjava.Result;
import rx.Subscriber;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * A base {@link Subscriber} for {@link EhClient}.
 */
public abstract class EhSubscriber<T extends EhResult> extends Subscriber<Result<T>> {

  // Use from() instead
  private EhSubscriber() {}

  @Override
  public void onCompleted() {}

  @Override
  public void onError(Throwable e) {
    EhReactiveX.handleError(e, this::onFailure);
  }

  @Override
  public void onNext(Result<T> result) {
    EhReactiveX.handleResult(result, t -> {
      onSuccess(t);
      return null;
    });
  }

  /**
   * Called if we get the result.
   */
  public abstract void onSuccess(T t);

  /**
   * Called if an error occurred.
   */
  public abstract void onFailure(Throwable e);

  /**
   * Creates a {@code EhSubscriber} from a {@code Action1<T>} and a {@code Action1<Throwable>}.
   */
  public static <T extends EhResult> EhSubscriber<T> from(Action1<T> onSuccess,
      Action1<Throwable> onFailure) {
    return new DelegateEhSubscriber<>(onSuccess, onFailure);
  }

  /**
   * Creates a {@code EhSubscriber} from a {@code Action1<T>} and a {@code Action1<Throwable>},
   * adds the subscriber to a {@link CompositeSubscription}.
   */
  public static <T extends EhResult> EhSubscriber<T> from(CompositeSubscription subscription,
      Action1<T> onSuccess, Action1<Throwable> onFailure) {
    return new AutoEhSubscriber<>(subscription, onSuccess, onFailure);
  }

  // TODO The document doesn't say that the Subscriber passed is the Subscription returned, this class may not work
  private static class AutoEhSubscriber<T extends EhResult> extends DelegateEhSubscriber<T> {

    private CompositeSubscription subscription;

    public AutoEhSubscriber(CompositeSubscription subscription, Action1<T> onSuccess,
        Action1<Throwable> onFailure) {
      super(onSuccess, onFailure);
      this.subscription = subscription;
      this.subscription.add(this);
    }

    @Override
    public void onCompleted() {
      subscription.remove(this);
    }
  }

  private static class DelegateEhSubscriber<T extends EhResult> extends EhSubscriber<T> {

    private final Action1<T> onSuccess;
    private final Action1<Throwable> onFailure;

    public DelegateEhSubscriber(Action1<T> onSuccess, Action1<Throwable> onFailure) {
      this.onSuccess = onSuccess;
      this.onFailure = onFailure;
    }

    @Override
    public void onSuccess(T t) {
      onSuccess.call(t);
    }

    @Override
    public void onFailure(Throwable e) {
      onFailure.call(e);
    }
  }
}
