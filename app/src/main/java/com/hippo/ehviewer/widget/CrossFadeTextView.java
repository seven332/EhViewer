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
 * Created by Hippo on 1/26/2017.
 */

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Implements {@link CrossFadeView} with {@link TextView}.
 */
public class CrossFadeTextView extends CrossFadeView<TextView, CharSequence> {

  public CrossFadeTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CrossFadeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void addView(View child, int index, LayoutParams params) {
    super.addView(child, index, params);

    if (child instanceof TextView) {
      TextView textView = (TextView) child;
      if (getFromView() == null) {
        setFromView(textView);
      } else if (getToView() == null) {
        setToView(textView);
      }
    }
  }

  @Override
  protected void setData(TextView view, @Nullable CharSequence data) {
    view.setText(data);
  }

  @Override
  protected CharSequence cloneData(TextView view) {
    return view.getText();
  }

  /**
   * Set text to the actual {@code TextView}.
   */
  public void setText(int resId) {
    getToView().setText(resId);
  }

  /**
   * Set text to the actual {@code TextView}.
   */
  public void setText(CharSequence text) {
    getToView().setText(text);
  }
}
