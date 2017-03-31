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
 * Created by Hippo on 3/31/2017.
 */

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.hippo.yorozuya.StringUtils;

public class TextDrawable extends Drawable {

  private TextState state;

  private Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private float x;
  private float y;
  private boolean textSizeDirty = true;

  private boolean mutated;

  public TextDrawable(String text, @FloatRange(from = 0.0f, to = 1.0f) float contentPercent) {
    state = new TextState(text != null ? text : StringUtils.EMPTY, contentPercent);
  }

  /**
   * Gets the drawable's text color value.
   */
  public int getTextColor() {
    return state.textColor;
  }

  /**
   * Sets the drawable's text color value.
   */
  public void setTextColor(int color) {
    if (state.textColor != color) {
      state.textColor = color;
      invalidateSelf();
    }
  }

  /**
   * Gets the drawable's background color value.
   */
  public int getBackgroundColor() {
    return state.backgroundColor;
  }

  /**
   * Sets the drawable's background color value.
   */
  public void setBackgroundColor(int color) {
    if (state.backgroundColor != color) {
      state.backgroundColor = color;
      invalidateSelf();
    }
  }

  /**
   * Gets the drawable's alpha value.
   */
  @Override
  public int getAlpha() {
    return state.alpha;
  }

  /**
   * Sets the drawable's background color value.
   */
  @Override
  public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
    if (state.alpha != alpha) {
      state.alpha = alpha;
      invalidateSelf();
    }
  }

  /**
   * Sets the color filter applied to this color.
   * <p>
   * Only supported on version {@link android.os.Build.VERSION_CODES#LOLLIPOP} and
   * above. Calling this method has no effect on earlier versions.
   *
   * @see android.graphics.drawable.Drawable#setColorFilter(ColorFilter)
   */
  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    backgroundPaint.setColorFilter(colorFilter);
    textPaint.setColorFilter(colorFilter);
    invalidateSelf();
  }

  /**
   * Gets the drawable's content percent value.
   */
  public float getContentPercent() {
    return state.contentPercent;
  }

  /**
   * Gets the drawable's text value.
   */
  public String getText() {
    return state.text;
  }

  @Override
  public int getOpacity() {
    return getAlpha() != 0 ? PixelFormat.TRANSLUCENT : PixelFormat.TRANSPARENT;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    textSizeDirty = true;
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    if (!getBounds().isEmpty() && getAlpha() != 0) {
      // Draw background
      backgroundPaint.setColor(state.backgroundColor);
      backgroundPaint.setAlpha(state.alpha);
      canvas.drawRect(getBounds(), backgroundPaint);

      if (!TextUtils.isEmpty(state.text)) {
        // Draw text
        updateTextSizeIfDirty();
        textPaint.setColor(state.textColor);
        textPaint.setAlpha(state.alpha);
        canvas.drawText(state.text, x, y, textPaint);
      }
    }
  }

  private void updateTextSizeIfDirty() {
    if (!textSizeDirty) {
      return;
    }
    textSizeDirty = false;

    Rect bounds = getBounds();
    int contentWidth = (int) (bounds.width() * state.contentPercent);
    int contentHeight = (int) (bounds.height() * state.contentPercent);
    float widthRatio = (float) contentWidth / state.textBounds.width();
    float heightRatio = (float) contentHeight / state.textBounds.height();
    float ratio = Math.min(widthRatio, heightRatio);
    float textSize = TextState.STANDARD_TEXT_SIZE * ratio;
    textPaint.setTextSize(textSize);
    x = (bounds.width() - state.textBounds.width() * ratio) / 2 - state.textBounds.left * ratio;
    y = (bounds.height() - state.textBounds.height() * ratio) / 2 - state.textBounds.top * ratio;
  }

  @NonNull
  @Override
  public Drawable mutate() {
    if (!mutated && super.mutate() == this) {
      state = new TextState(state);
      mutated = true;
    }
    return this;
  }

  final static class TextState extends ConstantState {

    private static final float STANDARD_TEXT_SIZE = 1000.0f;
    private static final Paint STANDARD_PAINT;

    static {
      STANDARD_PAINT = new Paint();
      STANDARD_PAINT.setTextSize(STANDARD_TEXT_SIZE);
    }

    private int textColor;
    private int backgroundColor;
    private int alpha;
    private float contentPercent;
    private String text;
    /**
     * 100.0f text size bounds
     */
    private Rect textBounds;

    TextState(String text, float contentPercent) {
      this.text = text;
      this.contentPercent = contentPercent;
      this.textBounds = new Rect();
      STANDARD_PAINT.getTextBounds(text, 0, text.length(), this.textBounds);
    }

    TextState(TextState state) {
      textColor = state.textColor;
      backgroundColor = state.backgroundColor;
      alpha = state.alpha;
      contentPercent = state.contentPercent;
      text = state.text;
      textBounds = state.textBounds;
    }

    @NonNull
    @Override
    public Drawable newDrawable() {
      return new TextDrawable(this);
    }

    @Override
    public int getChangingConfigurations() {
      return 0;
    }
  }

  private TextDrawable(TextState state) {
    this.state = state;
  }
}
