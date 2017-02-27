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
 * Created by Hippo on 1/19/2017.
 */

import com.hippo.ehviewer.client.exception.ErrorWrapper;
import com.hippo.ehviewer.client.exception.GeneralException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.exception.RuntimeExceptionWrapper;
import com.hippo.ehviewer.client.exception.SadPandaException;
import com.hippo.ehviewer.client.exception.StatusCodeException;
import com.hippo.ehviewer.reactivex.ThrowableWrapper;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.Result;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Helps RxJava.
 */
final class EhReactiveX {
  private EhReactiveX() {}

  // TODO Is it suitable?
  private static final int PLAIN_NOTICE_MAX_LENGTH = 512;

  /**
   * Handles {@link Throwable}.
   * {@code handler} do the actual job.
   */
  public static void handleError(Throwable e, Action1<Throwable> handler) {
    // Unwraps RuntimeExceptionWrapper and ErrorWrapper,
    // and throws them. They are thrown by onSuccess().
    if (e instanceof RuntimeExceptionWrapper) {
      throw ((RuntimeExceptionWrapper) e).unwrap();
    }
    if (e instanceof ErrorWrapper) {
      throw ((ErrorWrapper) e).unwrap();
    }

    // Unwrap ThrowableWrapper which wrapped in onNext()
    if (e instanceof ThrowableWrapper) {
      e = ((ThrowableWrapper) e).unwrap();
    }

    handler.call(e);
  }

  /**
   * Handles {@link Result}, get error from it.
   * {@code handler} do the actual job.
   * <p>
   * Call {@link #handleError(Throwable, Action1)} to handle error.
   */
  public static <T extends EhResult, R> R handleResult(Result<T> result, Func1<T, R> handler) {
    // Wraps all kinds of throwable in ThrowableWrapper,
    // let onError() handle it.
    if (result.isError()) {
      Throwable error = result.error();
      Exceptions.throwIfFatal(error);
      throw ThrowableWrapper.wrap(error);
    } else {
      Response<T> response = result.response();
      if (response.isSuccessful()) {
        T body = response.body();
        if (body.isError()) {
          Throwable error = body.error();
          Exceptions.throwIfFatal(error);
          throw ThrowableWrapper.wrap(fixError(error, response.raw()));
        } else {
          // Only one of onSuccess() and onFailure() can be called.
          // We must catch unchecked exceptions thrown by onSuccess()
          // to avoid onFailure() calling it.
          try {
            return handler.call(body);
          } catch (RuntimeException e) {
            throw RuntimeExceptionWrapper.wrap(e);
          } catch (Error e) {
            throw ErrorWrapper.wrap(e);
          }
        }
      } else {
        throw ThrowableWrapper.wrap(catchError(response.errorBody(), response.raw()));
      }
    }
  }

  private static Throwable fixError(Throwable t, okhttp3.Response response) {
    // Always throw SadPandaException first
    if (t instanceof SadPandaException) return t;

    if (t instanceof ParseException) {
      ParseException pe = (ParseException) t;
      pe.url(response.request().url());

      String body = pe.body();
      // If a body don't contains '<' and not too long, it should be a plain txt,
      // and it might be a notice.
      // TODO What if it's a JSON?
      if (!body.contains("<") && body.length() <= PLAIN_NOTICE_MAX_LENGTH) {
        t = new GeneralException(body, pe);
      }
    }

    // Don't let bad status code covers EhException
    if (t instanceof GeneralException) return t;

    if (!response.isSuccessful()) {
      t = new StatusCodeException(response.code(), t);
    }

    return t;
  }

  private static Throwable catchError(ResponseBody errorBody, okhttp3.Response response) {
    try {
      String body = errorBody.string();

      // If a body don't contains '<' and not too long, it should be a plain txt,
      // and it might be a notice.
      // TODO What if it's a JSON?
      if (!body.contains("<") && body.length() <= PLAIN_NOTICE_MAX_LENGTH) {
        return new GeneralException(body);
      }

      // Only bad status code cause this
      return new StatusCodeException(response.code());
    } catch (IOException e) {
      return e;
    } finally {
      errorBody.close();
    }
  }
}
