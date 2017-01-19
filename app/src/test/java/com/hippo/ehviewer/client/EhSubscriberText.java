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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.hippo.ehviewer.client.result.SignInResult;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.adapter.rxjava.Result;
import rx.Observable;
import rx.exceptions.OnErrorFailedException;
import rx.functions.Func1;

public class EhSubscriberText {

  @Test
  public void testThrowUncheckedException() {
    Observable.just("dump")
        .map(new Func1<String, Result<SignInResult>>() {
          @Override
          public Result<SignInResult> call(String s) {
            throw new RuntimeException("HAHA");
          }
        })
        .subscribe(new EhSubscriber<SignInResult>() {
          @Override
          public void onSuccess(SignInResult result) {
            fail();
          }

          @Override
          public void onFailure(Throwable e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertEquals("HAHA", e.getMessage());
          }
        });
  }

  @Test
  public void testEmitErrorResult() {
    Observable.just("dump")
        .map(new Func1<String, Result<SignInResult>>() {
          @Override
          public Result<SignInResult> call(String s) {
            return Result.error(new RuntimeException("HAHA"));
          }
        })
        .subscribe(new EhSubscriber<SignInResult>() {
          @Override
          public void onSuccess(SignInResult result) {
            fail();
          }

          @Override
          public void onFailure(Throwable e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertEquals("HAHA", e.getMessage());
          }
        });
  }

  @Test
  public void testThrowInOnSuccess() {
    try {
      Observable.just(Result.response(Response.success(new SignInResult("HAHA"))))
          .subscribe(new EhSubscriber<SignInResult>() {
            @Override
            public void onSuccess(SignInResult result) {
              assertEquals("HAHA", result.profileName());
              throw new RuntimeException("HAHA");
            }

            @Override
            public void onFailure(Throwable e) {
              fail();
            }
          });
    } catch (OnErrorFailedException e) {
    }
  }

  @Test
  public void testThrowBefore() {
    Observable.just("dump")
        .map(new Func1<String, String>() {
          @Override
          public String call(String s) {
            throw new RuntimeException("HAHA");
          }
        })
        .map(new Func1<String, Result<SignInResult>>() {
          @Override
          public Result<SignInResult> call(String s) {
            return Result.response(Response.success(new SignInResult("HAHA")));
          }
        })
        .subscribe(new EhSubscriber<SignInResult>() {
          @Override
          public void onSuccess(SignInResult result) {
            fail();
          }

          @Override
          public void onFailure(Throwable e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertEquals("HAHA", e.getMessage());
          }
        });
  }
}
