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

import com.hippo.ehviewer.client.exception.GeneralException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.exception.SadPandaException;
import com.hippo.ehviewer.client.exception.StatusCodeException;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.Result;
import rx.Subscriber;

/**
 * A base {@link Subscriber} for {@link EhClient}.
 */
public abstract class EhSubscriber<T extends EhResult> extends Subscriber<Result<T>> {

  // TODO Is it suitable?
  private static final int PLAIN_NOTICE_MAX_LENGTH = 512;

  @Override
  public void onCompleted() {}

  @Override
  public void onError(Throwable e) {
    onFailure(e);
  }

  @Override
  public void onNext(Result<T> result) {
    if (result.isError()) {
      onFailure(result.error());
    } else {
      Response<T> response = result.response();
      if (response.isSuccessful()) {
        T body = response.body();
        if (body.isError()) {
          onFailure(fixError(body.error(), response.raw()));
        } else {
          onSuccess(body);
        }
      } else {
        onFailure(catchError(response.errorBody(), response.raw()));
      }
    }
  }

  private Throwable fixError(Throwable t, okhttp3.Response response) {
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

  private Throwable catchError(ResponseBody errorBody, okhttp3.Response response) {
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

  /**
   * Called if we get the result.
   */
  public abstract void onSuccess(T t);

  /**
   * Called if an error occurred.
   */
  public abstract void onFailure(Throwable e);
}
