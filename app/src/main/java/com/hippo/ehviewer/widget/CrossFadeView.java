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

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.yorozuya.android.AnimationUtils;

/**
 * The view to help {@link com.hippo.ehviewer.transition.CrossFade}.
 * <p>
 * Uses a duplicate view to pretend the start-view.
 *
 * @param <V> the actual view type
 * @param <D> the data type
 */
public abstract class CrossFadeView<V extends View, D> extends ViewGroup {

  private V from;
  private V to;
  private int fromIndex;
  private int toIndex;

  private boolean showFrom;

  public CrossFadeView(Context context) {
    super(context);
    init();
  }

  public CrossFadeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public CrossFadeView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setChildrenDrawingOrderEnabled(true);
  }

  /**
   * Set from-view.
   * <p>
   * Must call it before layout.
   */
  protected void setFromView(@NonNull V from) {
    this.from = from;
    this.fromIndex = indexOfChild(from);
    if (this.fromIndex == -1) {
      throw new IllegalStateException("From-view is not a child");
    }
  }

  /**
   * Set to-view.
   * <p>
   * Must call it before layout.
   */
  protected void setToView(@NonNull V to) {
    this.to = to;
    this.toIndex = indexOfChild(to);
    if (this.toIndex == -1) {
      throw new IllegalStateException("To-view is not a child");
    }
  }

  /**
   * The view to pretend to be another view.
   */
  public V getFromView() {
    return from;
  }

  /**
   * The actual view.
   */
  public V getToView() {
    return to;
  }

  /**
   * Set data to the view.
   * <p>
   * {@code data == null} means clearing.
   * <p>
   * Example:<pre>{@code
   *   @Override
   *   protected void setData(ImageView view, Drawable data) {
   *     view.setImageDrawable(data);
   *   }
   * }</pre>
   */
  protected abstract void setData(V view, @Nullable D data);

  /**
   * Clone data from view.
   * <p>
   * Example:<pre>{@code
   *   @Override
   *   protected Drawable cloneData(ImageView view) {
   *     Drawable drawable = view.getDrawable();
   *     if (drawable != null) {
   *       Drawable.ConstantState state = drawable.getConstantState();
   *       if (state != null) {
   *         return drawable.getConstantState().newDrawable();
   *       }
   *     }
   *     return null;
   *   }
   * }</pre>
   */
  protected abstract D cloneData(V view);

  /**
   * Clone the data of to-view
   */
  public D cloneData() {
    return cloneData(to);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    measureChild(to, widthMeasureSpec, heightMeasureSpec);
    setMeasuredDimension(to.getMeasuredWidth(), to.getMeasuredHeight());
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    to.layout(0, 0, to.getMeasuredWidth(), to.getMeasuredHeight());
  }

  @Override
  protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    if (child != from || showFrom) {
      return super.drawChild(canvas, child, drawingTime);
    } else {
      return true;
    }
  }

  @Override
  protected int getChildDrawingOrder(int childCount, int i) {
    // Always draw from-view before to-view
    if (toIndex < fromIndex) {
      if (i == toIndex) return fromIndex;
      if (i == fromIndex) return toIndex;
    }
    return i;
  }

  public Animator crossFade(int fromWidth, int fromHeight, D fromData, D toData) {
    showFrom = true;
    // onLayout will be blocked during transition, so layout here
    from.measure(makeMeasureSpec(fromWidth, EXACTLY), makeMeasureSpec(fromHeight, EXACTLY));
    from.layout(0, 0, from.getMeasuredWidth(), from.getMeasuredHeight());

    setData(from, fromData);
    setData(to, toData);

    final ObjectAnimator fromAnim = ObjectAnimator.ofFloat(from, View.ALPHA, 1.0f, 0.0f);
    fromAnim.addListener(new FromAnimatorListener(from));
    final ObjectAnimator toAnim = ObjectAnimator.ofFloat(to, View.ALPHA, 0.0f, 1.0f);
    toAnim.addListener(new ToAnimatorListener(to));
    AnimatorSet set = new AnimatorSet();
    set.playTogether(fromAnim, toAnim);
    set.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
    return set;
  }

  private class FromAnimatorListener extends AnimatorListenerAdapter {
    private final V mView;

    public FromAnimatorListener(V view) {
      mView = view;
    }

    @Override
    public void onAnimationEnd(Animator animator) {
      mView.setAlpha(1.0f);
      setData(mView, null);
      showFrom = false;
    }
  }

  private class ToAnimatorListener extends AnimatorListenerAdapter {
    private final V mView;

    public ToAnimatorListener(V view) {
      mView = view;
    }

    @Override
    public void onAnimationEnd(Animator animator) {
      mView.setAlpha(1.0f);
    }
  }
}
