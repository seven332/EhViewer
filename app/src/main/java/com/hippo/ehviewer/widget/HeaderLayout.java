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
 * Created by Hippo on 2/21/2017.
 */

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.hippo.ehviewer.R;

/**
 * {@code HeaderLayout} shows a header and a content.
 * The header shows on the top, the content shows below the
 * header and fill remain space.
 * <p>
 * A shadow shows under header the header and floats over the content.
 * {@code HeaderLayout} wraps header and shadow to a LinearLayout.
 * {@link #getHeader()} returns the LinearLayout.
 */
public class HeaderLayout extends ViewGroup {

  private LinearLayout header;
  private View actualHeader;
  private View shadow;
  private View content;
  private int shadowHeight;

  public HeaderLayout(Context context) {
    super(context);
    init(context);
  }

  public HeaderLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public HeaderLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    shadowHeight = context.getResources().getDimensionPixelSize(R.dimen.strip_shadow_size);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void addView(View child, int index, LayoutParams params) {
    if (header == null) {
      Context context = getContext();

      actualHeader = child;

      header = new LinearLayout(context);
      header.setOrientation(LinearLayout.VERTICAL);

      header.addView(child,
          new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, params.height));

      shadow = new View(context);
      shadow.setBackgroundDrawable(
          ContextCompat.getDrawable(context, R.drawable.strip_shadow_bottom));
      header.addView(shadow,
          new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, shadowHeight));

      child = header;
    } else if (content == null) {
      content = child;
    }

    super.addView(child, index, params);
  }

  /**
   * Returns the LinearLayout which wraps the actual header and the shadow.
   */
  public View getHeader() {
    return header;
  }

  /**
   * Returns the actual header.
   */
  public View getActualHeader() {
    return actualHeader;
  }

  /**
   * Returns the shadow.
   */
  public View getShadow() {
    return shadow;
  }

  /**
   * Returns the content.
   */
  public View getContent() {
    return content;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (header == null || content == null) {
      throw new IllegalArgumentException("HeaderLayout must only contain a header and a content.");
    }

    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);

    if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
      throw new IllegalArgumentException("HeaderLayout must be measured with MeasureSpec.EXACTLY.");
    }

    header.measure(widthMeasureSpec,
        MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST));
    int headerHeight = header.getMeasuredHeight();
    int contentHeight = Math.max(heightSize - headerHeight + shadowHeight, 1);
    content.measure(widthMeasureSpec,
        MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY));

    setMeasuredDimension(widthSize, heightSize);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int headerHeight = header.getMeasuredHeight();
    header.layout(0, 0,
        header.getMeasuredWidth(), headerHeight);
    content.layout(0, headerHeight - shadowHeight,
        content.getMeasuredWidth(), headerHeight - shadowHeight + content.getMeasuredHeight());
  }
}
