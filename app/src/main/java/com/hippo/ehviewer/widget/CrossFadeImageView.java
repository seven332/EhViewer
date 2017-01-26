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
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import com.hippo.ehviewer.R;

/**
 * Implements {@link CrossFadeView} with {@link ImageView}.
 */
public class CrossFadeImageView extends CrossFadeView<ImageView, Drawable> {

  public CrossFadeImageView(Context context) {
    super(context);
    init(context);
  }

  public CrossFadeImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public CrossFadeImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  protected void init(Context context) {
    LayoutInflater.from(context).inflate(R.layout.widget_cross_fade_image_view, this);
    setViews((ImageView) getChildAt(0), (ImageView) getChildAt(1));
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
  }

  @Override
  protected void setData(ImageView view, @Nullable Drawable data) {
    view.setImageDrawable(data);
  }

  @Override
  protected Drawable cloneData(ImageView view) {
    Drawable drawable = view.getDrawable();
    if (drawable != null) {
      // TODO A better way to clone Drawable
      Drawable.ConstantState state = drawable.getConstantState();
      if (state != null) {
        return drawable.getConstantState().newDrawable();
      }
    }
    return null;
  }

  /**
   * Set image resource to the actual {@code ImageView}.
   */
  public void setImageResource(int resId) {
    getToView().setImageResource(resId);
  }
}
