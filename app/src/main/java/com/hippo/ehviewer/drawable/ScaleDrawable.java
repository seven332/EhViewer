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

package com.hippo.ehviewer.drawable;

/*
 * Created by Hippo on 2/28/2017.
 */

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.drawable.DrawableWrapper;
import android.view.Gravity;

public class ScaleDrawable extends DrawableWrapper {

  private static final float DO_NOT_SCALE = -1.0f;

  private float scale = DO_NOT_SCALE;
  private int gravity = Gravity.LEFT;

  private final Rect tmpRect = new Rect();

  public ScaleDrawable(Drawable drawable, int gravity, float scale) {
    super(drawable);
    this.scale = scale;
    this.gravity = gravity;
  }

  public float getScale() {
    return scale;
  }

  public void setScale(float scale) {
    this.scale = scale;
    onBoundsChange(getBounds());
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    final Drawable d = getWrappedDrawable();
    final Rect r = tmpRect;

    int w = bounds.width();
    int h = bounds.height();
    if (scale >= 0) {
      w *= scale;
      h *= scale;
    }

    Gravity.apply(gravity, w, h, bounds, r);

    if (w >= 0 && h >= 0) {
      d.setBounds(r.left, r.top, r.right, r.bottom);
    }
  }
}
