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
 * Created by Hippo on 2/12/2017.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.content.res.AppCompatResources;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.exception.GeneralException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.exception.SadPandaException;
import com.hippo.ehviewer.client.exception.StatusCodeException;
import com.hippo.ehviewer.widget.ContentData;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import org.apache.http.conn.ConnectTimeoutException;

/**
 * {@code ExceptionExplainer} makes {@code Throwable} human readable.
 */
public final class ExceptionExplainer {
  private ExceptionExplainer() {}

  /**
   * Explains the exception with a String.
   */
  public static String explain(Context context, Throwable e) {
    int textResId = 0;
    String text = null;

    // Network
    if (e instanceof MalformedURLException) {
      textResId = R.string.explanation_invalid_url;
    } else if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
      textResId = R.string.explanation_timeout;
    } else if (e instanceof UnknownHostException) {
      textResId = R.string.explanation_unknown_host;
    } else if (e instanceof StatusCodeException) {
      text = context.getString(R.string.explanation_status_code, ((StatusCodeException) e).code());
    } else if (e instanceof ProtocolException
        && e.getMessage().startsWith("Too many follow-up requests:")) {
      textResId = R.string.explanation_redirection;
    } else if (e instanceof ProtocolException || e instanceof SocketException
        || e instanceof SSLException) {
      textResId = R.string.explanation_socket;

    // EhClient
    } else if (e instanceof GeneralException) {
      text = e.getMessage();
    } else if (e instanceof ParseException) {
      textResId = R.string.explanation_parse;
    } else if (e instanceof SadPandaException) {
      textResId = R.string.explanation_sad_panda;

    // ContentLayout
    } else if (e instanceof ContentData.NotFoundException) {
      textResId = R.string.explanation_not_found;
    } else if (e instanceof ContentData.TapToLoadException) {
      textResId = R.string.explanation_tap_to_load;

    // Default
    } else {
      textResId = R.string.explanation_weird_error;
    }

    if (textResId != 0) {
      text = context.getString(textResId);
    }

    return text;
  }

  /**
   * Explains the exception with a Drawable.
   */
  public static Drawable explainVividly(Context context, Throwable e) {
    int drawableResId = 0;
    Drawable drawable = null;

    // ContentLayout
    if (e instanceof ContentData.NotFoundException) {
      drawableResId = R.drawable.v_emoticon_sad_x64;
    } else if (e instanceof ContentData.TapToLoadException) {
      drawableResId = R.drawable.v_gesture_tap_x64;

    // Default
    } else {
      drawableResId = R.drawable.v_emoticon_confused_x64;
    }

    if (drawableResId != 0) {
      drawable = AppCompatResources.getDrawable(context, drawableResId);
    }

    return drawable;
  }
}
