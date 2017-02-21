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
 * Created by Hippo on 2/12/2017.
 */

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;

/**
 * {@code AutoHideTextView} is only visible when text is not empty.
 */
public class AutoHideTextView extends AppCompatTextView {

  public AutoHideTextView(Context context) {
    super(context);
  }

  public AutoHideTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AutoHideTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void setText(CharSequence text, BufferType type) {
    super.setText(text, type);
    setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
  }
}
