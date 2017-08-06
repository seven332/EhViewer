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

package com.hippo.ehviewer.util

import android.content.Context
import android.graphics.drawable.Drawable
import com.hippo.ehviewer.R
import com.hippo.ehviewer.exception.GeneralException
import com.hippo.ehviewer.exception.ParseException
import com.hippo.ehviewer.exception.PresetException
import com.hippo.ehviewer.exception.ResponseCodeException
import com.hippo.ehviewer.exception.SadPandaException
import java.io.InterruptedIOException
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/*
 * Created by Hippo on 2017/7/24.
 */

/**
 * Explains the exception with a String.
 */
fun explain(error: Throwable, context: Context): String = when {
  error is GeneralException -> error.message
  error is PresetException -> context.string(error.text)
  error is ParseException -> context.string(R.string.error_parse)
  error is SadPandaException -> context.string(R.string.error_sad_panda)
  error is ResponseCodeException -> context.string(R.string.error_response_code, error.code)
  error is MalformedURLException -> context.string(R.string.error_invalid_url)
  error is InterruptedIOException -> context.string(R.string.error_timeout)
  error is UnknownHostException -> context.string(R.string.error_unknown_host)
  (error is ProtocolException) && (error.message?.startsWith("Too many follow-up requests:") ?: false) ->
    context.string(R.string.error_redirection)
  error is ProtocolException || error is SocketException || error is SSLException -> context.string(R.string.error_socket)
  else -> context.string(R.string.error_unknown).apply { error.printStackTrace() }
}

/**
 * Explains the exception with a Drawable.
 */
fun explainVividly(error: Throwable, context: Context): Drawable = when {
  (error is PresetException) && (error.icon != 0) -> context.drawable(error.icon)
  else -> context.drawable(R.drawable.emoticon_confused_primary_x64)
}
