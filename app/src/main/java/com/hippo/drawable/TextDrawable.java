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

package com.hippo.drawable;

/*
 * Created by Hippo on 2017/9/6.
 */

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TextDrawable extends Drawable {

  private static final float STANDARD_TEXT_SIZE = 1000.0f;
  private static final Paint STANDARD_PAINT = new Paint();

  static {
    STANDARD_PAINT.setTextSize(STANDARD_TEXT_SIZE);
  }

  private String text;
  private float contentPercent;

  private Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private int textColor = Color.BLACK;
  private int backgroundColor = Color.BLACK;

  private float x = 0.0f;
  private float y = 0.0f;
  private boolean textSizeDirty = false;
  private Rect textBounds = new Rect();

  public TextDrawable(String text, float contentPercent) {
    this.text = text;
    this.contentPercent = contentPercent;
    STANDARD_PAINT.getTextBounds(text, 0, text.length(), textBounds);
  }

  public int getTextColor() {
    return textColor;
  }

  public void setTextColor(int textColor) {
    if (this.textColor != textColor) {
      this.textColor = textColor;
      invalidateSelf();
    }
  }

  public int getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(int backgroundColor) {
    if (this.backgroundColor != backgroundColor) {
      this.backgroundColor = backgroundColor;
      invalidateSelf();
    }
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    textSizeDirty = true;
  }

  private void updateTextSizeIfDirty(Rect bounds) {
    if (!textSizeDirty) {
      return;
    }
    textSizeDirty = false;

    int contentWidth = (int) (bounds.width() * contentPercent);
    int contentHeight = (int) (bounds.height() * contentPercent);
    float widthRatio = (float) contentWidth / (float) textBounds.width();
    float heightRatio = (float) contentHeight / (float) textBounds.height();
    float ratio = Math.min(widthRatio, heightRatio);
    float textSize = STANDARD_TEXT_SIZE * ratio;
    textPaint.setTextSize(textSize);
    x = (bounds.width() - textBounds.width() * ratio) / 2 - textBounds.left * ratio;
    y = (bounds.height() - textBounds.height() * ratio) / 2 - textBounds.top * ratio;
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    Rect bounds = getBounds();
    if (!bounds.isEmpty()) {
      // Draw background
      backgroundPaint.setColor(backgroundColor);
      canvas.drawRect(bounds, backgroundPaint);

      if (!TextUtils.isEmpty(text)) {
        // Draw text
        updateTextSizeIfDirty(bounds);
        textPaint.setColor(textColor);
        canvas.drawText(text, x, y, textPaint);
      }
    }
  }

  @Override
  public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {}

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    backgroundPaint.setColorFilter(colorFilter);
    textPaint.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
