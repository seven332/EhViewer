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

package com.hippo.ehviewer.widget.crossfade;

/*
 * Created by Hippo on 2/25/2017.
 */

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.StyleRes;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;

/**
 * {@code IntegratedCrossFadeTextView} add views by itself.
 * No need to add children in xml.
 */
public class IntegratedCrossFadeTextView extends CrossFadeTextView {

  public IntegratedCrossFadeTextView(Context context) {
    super(context);
    init(context);
  }

  public IntegratedCrossFadeTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public IntegratedCrossFadeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    addView(new AppCompatTextView(context));
    addView(new AppCompatTextView(context));
  }

  public void setSingleLine() {
    getFromView().setSingleLine();
    getToView().setSingleLine();
  }

  public void setEllipsize(TextUtils.TruncateAt where) {
    getFromView().setEllipsize(where);
    getToView().setEllipsize(where);
  }

  @SuppressWarnings("deprecation")
  public void setTextAppearance(Context context, @StyleRes int resId) {
    getFromView().setTextAppearance(context, resId);
    getToView().setTextAppearance(context, resId);
  }

  public void setTextColor(@ColorInt int color) {
    getFromView().setTextColor(color);
    getToView().setTextColor(color);
  }
}
