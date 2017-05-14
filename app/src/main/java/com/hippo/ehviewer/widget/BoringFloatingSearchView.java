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
 * Created by Hippo on 5/14/2017.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.hippo.ehviewer.R;

/**
 * A {@link FloatingSearchView} without suggestions and input.
 */
public class BoringFloatingSearchView extends FloatingSearchView {

  private OnClickEditTextListener listener;

  public BoringFloatingSearchView(Context context) {
    super(context);
    init();
  }

  public BoringFloatingSearchView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    findViewById(R.id.search_suggestions_section).setVisibility(View.GONE);
    View view = findViewById(R.id.search_bar_text);
    view.setFocusable(false);
    view.setOnClickListener(v -> {
      if (listener != null) {
        listener.onClickEditText();
      }
    });
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    // Skip FloatingSearchView.onLayout(), avoid memory leak
    for (int i = 0, n = getChildCount(); i < n; ++i) {
      View child = getChildAt(i);
      if (child.getVisibility() != View.GONE) {
        child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
      }
    }
  }

  /**
   * Register a callback to be invoked when this EditText is clicked.
   */
  public void setOnClickEditTextListener(OnClickEditTextListener listener) {
    this.listener = listener;
  }

  /**
   * Interface definition for a callback to be invoked when a EditText is clicked.
   */
  public interface OnClickEditTextListener {
    /**
     * Called when a EditText has been clicked.
     */
    void onClickEditText();
  }
}
