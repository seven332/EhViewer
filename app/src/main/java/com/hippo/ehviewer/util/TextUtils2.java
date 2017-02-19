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
 * Created by Hippo on 1/26/2017.
 */

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;

public final class TextUtils2 {
  private TextUtils2() {}

  /**
   * Add {@code LeadingMarginSpan} to the text.
   * <p>
   * If the text is null, return null.
   * <p>
   * If the text is a {@link Spannable}, add spans, return itself.
   * <p>
   * If the text isn't a {@link Spannable},
   * create a {@link SpannableString} on the text,
   * add spans, return the {@link SpannableString}.
   */
  public static CharSequence addLeadingMarginSpan(CharSequence text, int margin) {
    if (text == null) return null;

    Spannable spannable;
    if (text instanceof Spannable) {
      spannable = (Spannable) text;
    } else {
      spannable = new SpannableString(text);
    }

    boolean recording = false;
    int start = 0;
    int end = 0;
    for (int i = 0, n = text.length(); i < n; i++) {
      char ch = text.charAt(i);
      if (recording) {
        if (ch == '\n') {
          recording = false;
          end = i + 1;
          spannable.setSpan(new LeadingMarginSpan.Standard(margin, 0),
              start, end, Spanned.SPAN_PARAGRAPH);
        }
      } else {
        if (ch != '\n') {
          recording = true;
          start = i;
        }
      }
    }
    if (recording) {
      end = text.length();
      spannable.setSpan(new LeadingMarginSpan.Standard(margin, 0),
          start, end, Spanned.SPAN_PARAGRAPH);
    }

    return spannable;
  }
}
