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

package com.hippo.ehviewer.widget;

/*
 * Created by Hippo on 2/11/2017.
 */

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.util.AttributeSet;

/**
 * {@code CuteTextInputLayout} hides error {@code TextView}
 * automatically if error text is null.
 */
public class CuteTextInputLayout extends TextInputLayout {

  public CuteTextInputLayout(Context context) {
    super(context);
  }

  public CuteTextInputLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CuteTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setError(@StringRes int resId) {
    setError(getResources().getString(resId));
  }

  @Override
  public void setError(@Nullable CharSequence error) {
    if (error != null) {
      setErrorEnabled(true);
      super.setError(error);
    } else {
      super.setError(null);
      setErrorEnabled(false);
    }
  }
}
