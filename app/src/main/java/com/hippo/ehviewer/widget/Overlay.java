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
 * Created by Hippo on 2/28/2017.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import com.hippo.yorozuya.android.ViewUtils;
import java.util.LinkedList;
import java.util.List;

/**
 * {@code Overlay} for a view.
 * Call {@link #getRootOverlay(Activity)}, {@link #getRootOverlay(View)}
 * or {@link #getOverlay(View, int)} to get it.
 */
@SuppressLint("ViewConstructor")
@SuppressWarnings("deprecation")
public class Overlay extends AbsoluteLayout {

  private ViewGroup target;

  private List<Drawable> drawableList = new LinkedList<>();

  private Overlay(ViewGroup view) {
    super(view.getContext());
    target = view;
  }

  /**
   * Gets view location in the overlay target.
   */
  public boolean getLocationInTarget(View view, int[] location) {
    return ViewUtils.getLocationInAncestor(view, target, location);
  }

  /**
   * Sets view location in the overlay.
   */
  public static void setLocation(View view, int x, int y) {
    if (view == null) {
      return;
    }
    ViewGroup.LayoutParams lp = view.getLayoutParams();
    if (lp instanceof LayoutParams) {
      LayoutParams alp = (LayoutParams) lp;
      alp.x = x;
      alp.y = y;
      view.offsetLeftAndRight(x - view.getLeft());
      view.offsetTopAndBottom(y - view.getTop());
    }
  }

  /**
   * Add a view to the overlay.
   * <p>
   * Must call {@link #remove(View)} to remove it if you don't need it anymore.
   */
  public void add(View view) {
    if (view != null) {
      addView(view);
    }
  }

  /**
   * Remove a view from the overlay.
   */
  public void remove(View view) {
    if (view != null) {
      removeView(view);
    }
  }

  /**
   * Add a drawable to the overlay.
   * <p>
   * Must call {@link #remove(Drawable)} to remove it if you don't need it anymore.
   */
  public void add(Drawable drawable) {
    if (drawable != null) {
      if (!drawableList.contains(drawable)) {
        drawableList.add(drawable);
        invalidate(drawable.getBounds());
        drawable.setCallback(this);
      }
    }
  }

  /**
   * Remove a drawable from the overlay.
   */
  public void remove(Drawable drawable) {
    if (drawable != null) {
      drawableList.remove(drawable);
      invalidate(drawable.getBounds());
      drawable.setCallback(null);
    }
  }

  @Override
  protected boolean verifyDrawable(@NonNull Drawable who) {
    return super.verifyDrawable(who) || drawableList.contains(who);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    for (int i = 0, n = drawableList.size(); i < n; ++i) {
      drawableList.get(i).draw(canvas);
    }
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    return false;
  }

  @Override
  public boolean dispatchGenericMotionEvent(MotionEvent event) {
    return false;
  }

  /**
   * Creates or gets the overlay for {@code android.R.id.content}.
   */
  @Nullable
  public static Overlay getRootOverlay(Activity activity) {
    if (activity == null) {
      return null;
    }
    View view = activity.findViewById(android.R.id.content);
    if (view instanceof ViewGroup) {
      return getViewOverlay((ViewGroup) view);
    } else {
      return null;
    }
  }

  /**
   * Creates or gets the overlay for {@code android.R.id.content}.
   */
  @Nullable
  public static Overlay getRootOverlay(View view) {
    return getOverlay(view, android.R.id.content);
  }

  // TODO need a custom overlay inserting mechanism

  /**
   * Creates or gets the overlay for the view with the {@code id}
   * starting from the {@code view}.
   * <p>
   * A overlay will be added as a child of the view with the {@code id}.
   */
  @Nullable
  public static Overlay getOverlay(View view, int id) {
    if (view == null) {
      return null;
    }
    View root = ViewUtils.getAncestor(view, id);
    if (root instanceof ViewGroup) {
      return getViewOverlay((ViewGroup) root);
    } else {
      return null;
    }
  }

  @Nullable
  private static Overlay getViewOverlay(ViewGroup view) {
    if (view == null) {
      return null;
    }
    for (int i = 0, n = view.getChildCount(); i < n; ++i) {
      View child = view.getChildAt(i);
      if (child instanceof Overlay) {
        return (Overlay) child;
      }
    }

    Overlay overlay = new Overlay(view);
    view.addView(overlay, new LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 0, 0));
    return overlay;
  }
}
