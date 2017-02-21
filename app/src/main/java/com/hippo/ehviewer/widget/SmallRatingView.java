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
 * Created by Hippo on 2/6/2017.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.View;
import com.hippo.ehviewer.R;
import com.hippo.yorozuya.MathUtils;

/**
 * 5 stars, from 0 to 10.
 */
public class SmallRatingView extends View {

  private Drawable starDrawable;
  private Drawable starHalfDrawable;
  private Drawable starOutlineDrawable;
  private int starSize;
  private int starInterval;

  private float rating;
  private int ratingInt;

  public SmallRatingView(Context context) {
    super(context);
    init(context);
  }

  public SmallRatingView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public SmallRatingView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    Resources resources = context.getResources();
    starDrawable = AppCompatResources.getDrawable(context, R.drawable.v_star_x16);
    starHalfDrawable = AppCompatResources.getDrawable(context, R.drawable.v_star_half_x16);
    starOutlineDrawable = AppCompatResources.getDrawable(context, R.drawable.v_star_outline_x16);
    starSize = resources.getDimensionPixelOffset(R.dimen.rating_size);
    starInterval = resources.getDimensionPixelOffset(R.dimen.rating_interval);

    starDrawable.setBounds(0, 0, starSize, starSize);
    starHalfDrawable.setBounds(0, 0, starSize, starSize);
    starOutlineDrawable.setBounds(0, 0, starSize, starSize);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(starSize * 5 + starInterval * 4 + getPaddingLeft() + getPaddingRight(),
        starSize + getPaddingTop() + getPaddingBottom());
  }

  @Override
  protected void onDraw(Canvas canvas) {
    int step = starSize + starInterval;
    int numStar = ratingInt / 2;
    int numStarHalf = ratingInt % 2;
    int saved = canvas.save();
    canvas.translate(getPaddingLeft(), getPaddingTop());
    while (numStar-- > 0) {
      starDrawable.draw(canvas);
      canvas.translate(step, 0);
    }
    if (numStarHalf == 1) {
      starHalfDrawable.draw(canvas);
      canvas.translate(step, 0);
    }
    int numOutline = 5 - numStar - numStarHalf;
    while (numOutline-- > 0) {
      starOutlineDrawable.draw(canvas);
      canvas.translate(step, 0);
    }
    canvas.restoreToCount(saved);
  }

  /**
   * Sets rating to change displayed stars.
   * Range: {@code [0.0f, 5.0f]}.
   */
  public void setRating(float rating) {
    if (this.rating != rating) {
      this.rating = rating;
      int ratingInt = Float.isNaN(rating) ? 0 : MathUtils.clamp((int) Math.ceil(rating * 2), 0, 10);
      if (this.ratingInt != ratingInt) {
        this.ratingInt = ratingInt;
        invalidate();
      }
    }
  }

  /**
   * Get rating.
   */
  public float getRating() {
    return rating;
  }
}
