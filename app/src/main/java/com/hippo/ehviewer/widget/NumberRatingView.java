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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.View;
import com.hippo.ehviewer.R;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.StringUtils;

/**
 * {@code NumberRatingView} shows a rating number and a star.
 */
public class NumberRatingView extends View {

  private float rating = -1.0f;
  private String ratingStr;
  private Paint paint;
  private Rect bounds;
  private int starSize;
  private int starInterval;
  private Drawable star;

  public NumberRatingView(Context context) {
    super(context);
    init(context);
  }

  public NumberRatingView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public NumberRatingView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    Resources resources = context.getResources();
    starSize = resources.getDimensionPixelOffset(R.dimen.rating_size);
    starInterval = resources.getDimensionPixelOffset(R.dimen.rating_interval);
    star = AppCompatResources.getDrawable(context, R.drawable.v_star_white_x16);
    star.setBounds(0, 0, starSize, starSize);

    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextSize(starSize);
    paint.setColor(Color.WHITE);
    bounds = new Rect();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (StringUtils.isEmpty(ratingStr)) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else {
      paint.getTextBounds(ratingStr, 0, ratingStr.length(), bounds);
      setMeasuredDimension(bounds.width() + starInterval + starSize,
          Math.max(bounds.height(), starSize));
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (StringUtils.isEmpty(ratingStr)) {
      return;
    }

    int height = getMeasuredHeight();
    int x = -bounds.left;
    int y = -bounds.top + (height - bounds.height()) / 2;
    canvas.drawText(ratingStr, x, y, paint);

    int saved = canvas.save(Canvas.MATRIX_SAVE_FLAG);
    canvas.translate(bounds.width() + starInterval, (height - starSize) / 2);
    star.draw(canvas);
    canvas.restoreToCount(saved);
  }

  /**
   * Sets rating to change displayed stars.
   * Range: {@code [0.0f, 5.0f]}.
   * <p>
   * If the {@code rating} isn't in range, clamp it to the range.
   */
  public void setRating(float rating) {
    // Clamp rating to range
    rating = MathUtils.clamp(rating, 0.0f, 5.0f);
    if (this.rating != rating) {
      this.rating = rating;
      String ratingStr = getRatingStr(rating);
      if (!ratingStr.equals(this.ratingStr)) {
        this.ratingStr = ratingStr;
        requestLayout();
      }
    }
  }

  /**
   * Get rating.
   */
  public float getRating() {
    return rating;
  }

  public static String getRatingStr(float rating) {
    if (Float.isNaN(rating)) {
      return "N/A";
    }

    int n = Math.round(rating * 10);
    int high = n / 10;
    int low = n % 10;
    return high + "." + low;
  }
}
