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
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * {@code ObservedScrollView} has {@link #setOnScrollChangeListener(OnScrollChangeListener)}.
 */
public class ObservedScrollView extends ScrollView {

  private OnScrollChangeListener onScrollChangeListener;

  public ObservedScrollView(Context context) {
    super(context);
  }

  public ObservedScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ObservedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);
    if (onScrollChangeListener != null) {
      onScrollChangeListener.onScrollChange(this, l, t, oldl, oldt);
    }
  }

  /**
   * Register a callback to be invoked when the scroll X or Y positions of
   * this view change.
   */
  public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
    this.onScrollChangeListener = onScrollChangeListener;
  }

  /**
   * Interface definition for a callback to be invoked when the scroll
   * X or Y positions of a view change.
   */
  public interface OnScrollChangeListener {

    /**
     * Called when the scroll position of a view changes.
     *
     * @param v The view whose scroll position has changed.
     * @param scrollX Current horizontal scroll origin.
     * @param scrollY Current vertical scroll origin.
     * @param oldScrollX Previous horizontal scroll origin.
     * @param oldScrollY Previous vertical scroll origin.
     */
    void onScrollChange(ObservedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY);
  }
}
