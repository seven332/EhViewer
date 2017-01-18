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

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.exception.SadPandaException;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Base {@link Converter} of {@link com.hippo.ehviewer.client.EhClient}.
 */
public abstract class EhConverter<T extends EhResult> implements Converter<ResponseBody, T> {

  private static final MediaType SAD_PANDA_MEDIA_TYPE = MediaType.parse("image/gif");
  private static final long SAD_PANDA_CONTENT_LENGTH = 9615L;

  private void checkSadPanda(ResponseBody value) throws SadPandaException {
    if (SAD_PANDA_CONTENT_LENGTH == value.contentLength()
        && SAD_PANDA_MEDIA_TYPE.equals(value.contentType())) {
      throw new SadPandaException();
    }
  }

  @Override
  public final T convert(ResponseBody value) throws IOException {
    try {
      checkSadPanda(value);
      String body = value.string();
      return convert(body);
    } catch (Throwable e) {
      return error(e);
    } finally {
      value.close();
    }
  }

  /**
   * Converts body string to result object.
   */
  @NonNull
  public abstract T convert(String body) throws Exception;

  /**
   * Wraps the {@link Throwable} to a result object.
   */
  // TODO It's pain. I need a better way.
  @NonNull
  public abstract T error(Throwable t);
}
