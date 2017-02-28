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

package com.hippo.ehviewer.changehandler;

/*
 * Created by Hippo on 2/26/2017.
 */

import static com.hippo.yorozuya.android.ViewUtils.screenshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.ActionMenuView;
import android.util.Property;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.drawable.ScaleDrawable;
import com.hippo.ehviewer.widget.ControllerContainer;
import com.hippo.ehviewer.widget.Overlay;
import com.hippo.yorozuya.android.AnimationUtils;
import com.transitionseverywhere.ArcMotion;
import com.transitionseverywhere.utils.AnimatorUtils;
import com.transitionseverywhere.utils.FloatProperty;
import com.transitionseverywhere.utils.IntProperty;
import com.transitionseverywhere.utils.PointFProperty;
import com.transitionseverywhere.utils.ViewUtils;
import java.util.ArrayList;
import java.util.List;

public final class ChangeHandlerAnimators {
  private ChangeHandlerAnimators() {}

  ///////////////////////////////////////////////////////////////////////////
  // Recolor status bar
  ///////////////////////////////////////////////////////////////////////////

  private static final boolean SUPPORT_COLOR_BAR =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

  private static final Property<DrawerLayout, Integer> STATUS_BAR_COLOR;

  static {
    STATUS_BAR_COLOR = new IntProperty<DrawerLayout>() {
      @Override
      public void setValue(DrawerLayout object, int value) {
        object.setStatusBarColor(value);
      }

      @Override
      public Integer get(DrawerLayout object) {
        return object.getStatusBarColor();
      }
    }.optimize();
  }

  /**
   * Creates recolor status bar animator for ControllerChangeHandler.
   * <p>
   * Return {@code null} if not supported.
   * <p>
   * Only works for {@link com.hippo.ehviewer.view.base.EhvView}
   * in {@link com.hippo.ehviewer.activity.EhvActivity}.
   */
  @Nullable
  public static Animator recolorStatusBar(
      @NonNull ViewGroup container, @Nullable View from, @Nullable View to) {
    if (!SUPPORT_COLOR_BAR
        || !(container instanceof ControllerContainer)
        || from == null || to == null) {
      return null;
    }
    ControllerContainer controllerContainer = (ControllerContainer) container;
    DrawerLayout drawerLayout = controllerContainer.getDrawLayout();
    if (drawerLayout == null) {
      return null;
    }

    Object startObj = from.getTag(R.id.controller_status_bar_color);
    Object endObj = to.getTag(R.id.controller_status_bar_color);
    if (!(startObj instanceof Integer) || !(endObj instanceof Integer) || startObj.equals(endObj)) {
      return null;
    }

    ObjectAnimator statusBarAnimator = ObjectAnimator.ofInt(
        drawerLayout, STATUS_BAR_COLOR,
        (Integer) startObj, (Integer) endObj
    );
    statusBarAnimator.setEvaluator(new ArgbEvaluator());

    return statusBarAnimator;
  }


  ///////////////////////////////////////////////////////////////////////////
  // Exchange content
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Remember to call {@code from.setTranslationY(0)} and {@code from.setAlpha(1.0f)}
   * in {@link com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler#resetFromView(View)}.
   *
   * @param offsetY the y distance to transfer content view
   * @param duration the duration for single content animator
   * @param delay the delay for to content animator
   */
  @Nullable
  public static Animator exchangeContent(@Nullable View from, @Nullable View to, float offsetY,
      long duration, long delay) {
    if (from == null || to == null) {
      return null;
    }

    Animator fromYAnimator = ObjectAnimator.ofFloat(from, View.TRANSLATION_Y, 0, offsetY);
    fromYAnimator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
    Animator fromAlphaAnimator = ObjectAnimator.ofFloat(from, View.ALPHA, 1.0f, 0.0f);
    AnimatorSet fromAnimator = new AnimatorSet();
    fromAnimator.playTogether(fromYAnimator, fromAlphaAnimator);
    fromAnimator.setDuration(duration);

    to.setTranslationY(offsetY);
    to.setAlpha(0.0f);
    Animator toYAnimator = ObjectAnimator.ofFloat(to, View.TRANSLATION_Y, offsetY, 0);
    toYAnimator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
    Animator toAlphaAnimator = ObjectAnimator.ofFloat(to, View.ALPHA, 0.0f, 1.0f);
    AnimatorSet toAnimator = new AnimatorSet();
    toAnimator.playTogether(toYAnimator, toAlphaAnimator);
    toAnimator.setDuration(duration);
    toAnimator.setStartDelay(delay);

    AnimatorSet set = new AnimatorSet();
    set.playTogether(fromAnimator, toAnimator);
    return set;
  }


  ///////////////////////////////////////////////////////////////////////////
  // Change DrawerArrowDrawable
  ///////////////////////////////////////////////////////////////////////////

  private static final Property<DrawerArrowDrawable, Float> DRAWER_ARROW_DRAWABLE_PROGRESS;

  static {
    DRAWER_ARROW_DRAWABLE_PROGRESS = new FloatProperty<DrawerArrowDrawable>() {
      @Override
      public void setValue(DrawerArrowDrawable object, float value) {
        object.setProgress(value);
      }

      @Override
      public Float get(DrawerArrowDrawable object) {
        return object.getProgress();
      }
    };
  }

  @Nullable
  public static Animator changeDrawerArrowDrawable(@Nullable DrawerArrowDrawable from,
      @Nullable DrawerArrowDrawable to) {
    if (from == null || to == null || from.getProgress() == to.getProgress()) {
      return null;
    }

    return ObjectAnimator.ofFloat(
        to, DRAWER_ARROW_DRAWABLE_PROGRESS,
        from.getProgress(), to.getProgress()
    );
  }


  ///////////////////////////////////////////////////////////////////////////
  // Change bounds
  ///////////////////////////////////////////////////////////////////////////

  private static final PointFProperty<ViewBounds> TOP_LEFT_PROPERTY;
  private static final PointFProperty<ViewBounds> BOTTOM_RIGHT_PROPERTY;
  private static final PointFProperty<View> BOTTOM_RIGHT_ONLY_PROPERTY;
  private static final PointFProperty<View> TOP_LEFT_ONLY_PROPERTY;
  private static final PointFProperty<View> POSITION_PROPERTY;
  private static final ArcMotion ACR_PATH_MOTION;

  static {
    TOP_LEFT_PROPERTY = new PointFProperty<ViewBounds>() {
      @Override
      public void set(ViewBounds viewBounds, PointF topLeft) {
        viewBounds.setTopLeft(topLeft);
      }
    };
    BOTTOM_RIGHT_PROPERTY = new PointFProperty<ViewBounds>() {
      @Override
      public void set(ViewBounds viewBounds, PointF bottomRight) {
        viewBounds.setBottomRight(bottomRight);
      }
    };
    BOTTOM_RIGHT_ONLY_PROPERTY = new PointFProperty<View>() {
      @Override
      public void set(View view, PointF bottomRight) {
        int left = view.getLeft();
        int top = view.getTop();
        int right = Math.round(bottomRight.x);
        int bottom = Math.round(bottomRight.y);
        ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
      }
    };
    TOP_LEFT_ONLY_PROPERTY = new PointFProperty<View>() {
      @Override
      public void set(View view, PointF topLeft) {
        int left = Math.round(topLeft.x);
        int top = Math.round(topLeft.y);
        int right = view.getRight();
        int bottom = view.getBottom();
        ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
      }
    };
    POSITION_PROPERTY = new PointFProperty<View>() {
      @Override
      public void set(View view, PointF topLeft) {
        int left = Math.round(topLeft.x);
        int top = Math.round(topLeft.y);
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
      }
    };
    ACR_PATH_MOTION = new ArcMotion();
    ACR_PATH_MOTION.setMaximumAngle(90);
    ACR_PATH_MOTION.setMinimumHorizontalAngle(15);
    ACR_PATH_MOTION.setMinimumVerticalAngle(0);
  }

  @Nullable
  public static Animator changeBounds(@Nullable View from, @Nullable View to) {
    if (from == null || !ViewCompat.isLaidOut(from) || from.getWidth() == 0 || from.getHeight() == 0
        || to == null || !ViewCompat.isLaidOut(to) || to.getWidth() == 0 || to.getHeight() == 0) {
      return null;
    }
    ViewGroup fromParent = (ViewGroup) from.getParent();
    ViewGroup toParent = (ViewGroup) to.getParent();
    if (fromParent == null || toParent == null) {
      return null;
    }
    final int startLeft = from.getLeft();
    final int endLeft = to.getLeft();
    final int startTop = from.getTop();
    final int endTop = to.getTop();
    final int startRight = from.getRight();
    final int endRight = to.getRight();
    final int startBottom = from.getBottom();
    final int endBottom = to.getBottom();
    final int startWidth = startRight - startLeft;
    final int startHeight = startBottom - startTop;
    final int endWidth = endRight - endLeft;
    final int endHeight = endBottom - endTop;
    int numChanges = 0;
    if ((startWidth != 0 && startHeight != 0) || (endWidth != 0 && endHeight != 0)) {
      if (startLeft != endLeft || startTop != endTop) ++numChanges;
      if (startRight != endRight || startBottom != endBottom) ++numChanges;
    }
    if (numChanges > 0) {
      Animator anim;
      ViewUtils.setLeftTopRightBottom(to, startLeft, startTop, startRight, startBottom);
      if (numChanges == 2) {
        if (startWidth == endWidth && startHeight == endHeight) {
          anim = AnimatorUtils.ofPointF(to, POSITION_PROPERTY, ACR_PATH_MOTION,
              startLeft, startTop, endLeft, endTop);
        } else {
          ViewBounds viewBounds = new ViewBounds(to);
          Animator topLeftAnimator = AnimatorUtils.ofPointF(viewBounds,
              TOP_LEFT_PROPERTY, ACR_PATH_MOTION,
              startLeft, startTop, endLeft, endTop);
          Animator bottomRightAnimator = AnimatorUtils.ofPointF(viewBounds,
              BOTTOM_RIGHT_PROPERTY, ACR_PATH_MOTION,
              startRight, startBottom, endRight, endBottom);
          AnimatorSet set = new AnimatorSet();
          set.playTogether(topLeftAnimator, bottomRightAnimator);
          set.addListener(viewBounds);
          anim = set;
        }
      } else if (startLeft != endLeft || startTop != endTop) {
        anim = AnimatorUtils.ofPointF(to, TOP_LEFT_ONLY_PROPERTY, null,
            startLeft, startTop, endLeft, endTop);
      } else {
        anim = AnimatorUtils.ofPointF(to, BOTTOM_RIGHT_ONLY_PROPERTY, null,
            startRight, startBottom, endRight, endBottom);
      }
      return anim;
    }
    return null;
  }

  private static class ViewBounds extends AnimatorListenerAdapter {
    private int left;
    private int top;
    private int right;
    private int bottom;
    private boolean isTopLeftSet;
    private boolean isBottomRightSet;
    private View view;

    public ViewBounds(View view) {
      this.view = view;
    }

    public void setTopLeft(PointF topLeft) {
      left = Math.round(topLeft.x);
      top = Math.round(topLeft.y);
      isTopLeftSet = true;
      if (isBottomRightSet) {
        setLeftTopRightBottom();
      }
    }

    public void setBottomRight(PointF bottomRight) {
      right = Math.round(bottomRight.x);
      bottom = Math.round(bottomRight.y);
      isBottomRightSet = true;
      if (isTopLeftSet) {
        setLeftTopRightBottom();
      }
    }

    private void setLeftTopRightBottom() {
      ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
      isTopLeftSet = false;
      isBottomRightSet = false;
    }
  }


  ///////////////////////////////////////////////////////////////////////////
  // Cross fade
  ///////////////////////////////////////////////////////////////////////////

  private static final int[] TEMP_LOCATION = new int[2];

  private static final IntProperty<Drawable> DRAWABLE_ALPHA_PROPERTY;
  private static final PointFProperty<Drawable> DRAWABLE_POSITION_PROPERTY;

  static {
    DRAWABLE_ALPHA_PROPERTY = new IntProperty<Drawable>() {
      @Override
      public void setValue(Drawable object, int value) {
        object.setAlpha(value);
      }
    };

    DRAWABLE_POSITION_PROPERTY = new PointFProperty<Drawable>() {
      private Rect mBounds = new Rect();
      @Override
      public void set(Drawable object, PointF value) {
        object.copyBounds(mBounds);
        mBounds.offsetTo(Math.round(value.x), Math.round(value.y));
        object.setBounds(mBounds);
      }
      @Override
      public PointF get(Drawable object) {
        object.copyBounds(mBounds);
        return new PointF(mBounds.left, mBounds.top);
      }
    };
  }

  @Nullable
  public static Animator crossFade(@Nullable View from, @Nullable View to, boolean changePosition,
      boolean toIsTop) {
    if (from == null || !ViewCompat.isLaidOut(from)
        || to == null || !ViewCompat.isLaidOut(to)) {
      return null;
    }

    // Create overlay
    final Overlay overlay = Overlay.getOverlay(from, EhvActivity.OVERLAY_CONTENT_ID);
    if (overlay == null) {
      return null;
    }

    // Get location
    if (!overlay.getLocationInTarget(from, TEMP_LOCATION)) {
      return null;
    }
    int startX = TEMP_LOCATION[0];
    int startY = TEMP_LOCATION[1];

    // Create bitmap
    Bitmap startBitmap = screenshot(from);
    if (startBitmap == null) {
      return null;
    }
    Bitmap endBitmap = screenshot(to);
    if (endBitmap == null) {
      startBitmap.recycle();
      return null;
    }
    final Drawable startDrawable = new BitmapDrawable(from.getContext().getResources(), startBitmap);
    startDrawable.setBounds(startX, startY,
        startX + startBitmap.getWidth(), startY + startBitmap.getHeight());
    final Drawable endDrawable = new BitmapDrawable(to.getContext().getResources(), endBitmap);
    endDrawable.setBounds(startX, startY,
        startX + endBitmap.getWidth(), startY + endBitmap.getHeight());

    // Hide from and to view
    from.setAlpha(0.0f);
    to.setAlpha(0.0f);
    // Add drawables to overlay
    if (toIsTop) {
      overlay.add(startDrawable);
      overlay.add(endDrawable);
    } else {
      overlay.add(endDrawable);
      overlay.add(startDrawable);
    }

    List<Animator> animators = new ArrayList<>(3);

    // Create alpha animator
    Animator startAlpha = ObjectAnimator.ofInt(startDrawable, DRAWABLE_ALPHA_PROPERTY, 255, 0);
    Animator endAlpha = ObjectAnimator.ofInt(endDrawable, DRAWABLE_ALPHA_PROPERTY, 0, 255);
    animators.add(startAlpha);
    animators.add(endAlpha);

    // Create position animator
    if (changePosition && overlay.getLocationInTarget(to, TEMP_LOCATION)) {
      int endX = TEMP_LOCATION[0];
      int endY = TEMP_LOCATION[1];
      if (startX != endX || startY != endY) {
        Animator startPosition = AnimatorUtils.ofPointF(startDrawable, DRAWABLE_POSITION_PROPERTY,
            ACR_PATH_MOTION, startX, startY, endX, endY);
        startPosition.setInterpolator(AnimationUtils.SLOW_FAST_SLOW_INTERPOLATOR);
        Animator endPosition = AnimatorUtils.ofPointF(endDrawable, DRAWABLE_POSITION_PROPERTY,
            ACR_PATH_MOTION, startX, startY, endX, endY);
        endPosition.setInterpolator(AnimationUtils.SLOW_FAST_SLOW_INTERPOLATOR);
        animators.add(startPosition);
        animators.add(endPosition);
      }
    }

    // Combine animator
    AnimatorSet set = new AnimatorSet();
    set.playTogether(animators);
    set.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        // Remove drawables from overlay
        overlay.remove(startDrawable);
        overlay.remove(endDrawable);
        // Show from and to view
        from.setAlpha(1.0f);
        to.setAlpha(1.0f);
        // Recycle bitmaps
        startBitmap.recycle();
        endBitmap.recycle();
      }
    });

    return set;
  }


  ///////////////////////////////////////////////////////////////////////////
  // ChangeActionMenu
  ///////////////////////////////////////////////////////////////////////////

  private static final FloatProperty<ScaleDrawable> DRAWABLE_SCALE_PROPERTY;

  static {
    DRAWABLE_SCALE_PROPERTY = new FloatProperty<ScaleDrawable>() {
      @Override
      public void setValue(ScaleDrawable object, float value) {
        object.setScale(value);
      }
      @Override
      public Float get(ScaleDrawable object) {
        return object.getScale();
      }
    };
  }

  @Nullable
  private static List<View> getMenuItemViews(@Nullable ActionMenuView menuView) {
    if (menuView == null || !ViewCompat.isLaidOut(menuView)) {
      return null;
    }

    List<View> views = new ArrayList<>();
    for (int i = 0, n = menuView.getChildCount(); i < n; i++) {
      View child = menuView.getChildAt(i);
      if (child.getVisibility() != View.GONE) {
        views.add(child);
      }
    }
    return views;
  }

  @Nullable
  private static Animator createMenuViewAnimator(Overlay overlay,
      ActionMenuView menu, List<View> views, long duration, boolean isFrom) {
    if (menu == null || views == null || views.isEmpty()) {
      return null;
    }

    int size = views.size();
    List<Animator> animators = new ArrayList<>(size);
    List<Bitmap> bitmaps = new ArrayList<>(size);
    List<Drawable> drawables = new ArrayList<>(size);
    for (View v: views) {
      if (!ViewCompat.isLaidOut(v)) {
        continue;
      }
      if (!overlay.getLocationInTarget(v, TEMP_LOCATION)) {
        continue;
      }

      // Add bitmap
      Bitmap bitmap = screenshot(v);
      if (bitmap == null) {
        continue;
      }
      bitmaps.add(bitmap);

      // Add drawable
      float startScale = isFrom ? 1.0f : 0.0f;
      float endScale = isFrom ? 0.0f : 1.0f;
      ScaleDrawable drawable = new ScaleDrawable(
          new BitmapDrawable(v.getResources(), bitmap), Gravity.CENTER, startScale);
      drawable.setBounds(TEMP_LOCATION[0], TEMP_LOCATION[1],
          TEMP_LOCATION[0] + bitmap.getWidth(), TEMP_LOCATION[1] + bitmap.getHeight());
      overlay.add(drawable);
      drawables.add(drawable);

      // Add animator
      Animator animator = ObjectAnimator.ofFloat(
          drawable, DRAWABLE_SCALE_PROPERTY, startScale, endScale);
      animator.setDuration(duration);
      animators.add(animator);
    }

    if (!animators.isEmpty()) {
      AnimatorSet set = new AnimatorSet();
      set.playSequentially(animators);
      set.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          for (Drawable drawable: drawables) {
            overlay.remove(drawable);
          }
          for (Bitmap bitmap: bitmaps) {
            bitmap.recycle();
          }
        }
      });
      return set;
    } else {
      for (Drawable drawable: drawables) {
        overlay.remove(drawable);
      }
      for (Bitmap bitmap: bitmaps) {
        bitmap.recycle();
      }
      return null;
    }
  }

  public static Animator createActionMenuAnimator(Overlay overlay,
      ActionMenuView from, List<View> fromViews,
      ActionMenuView to, List<View> toViews,
      int duration, boolean toIsTop) {
    Animator fromAnimator;
    Animator toAnimator;
    if (toIsTop) {
      fromAnimator = createMenuViewAnimator(overlay, from, fromViews, duration, true);
      toAnimator = createMenuViewAnimator(overlay, to, toViews, duration, false);
    } else {
      toAnimator = createMenuViewAnimator(overlay, to, toViews, duration, false);
      fromAnimator = createMenuViewAnimator(overlay, from, fromViews, duration, true);
    }

    if (fromAnimator == null) {
      return toAnimator;
    }
    if (toAnimator == null) {
      return fromAnimator;
    }

    // Adjust delay according to from views count and to views count
    int fromCount = fromViews.size();
    int toCount = toViews.size();
    int delay = (1 + fromCount - toCount) * duration;
    if (delay > 0) {
      toAnimator.setStartDelay(delay);
    } else {
      fromAnimator.setStartDelay(-delay);
    }
    AnimatorSet set = new AnimatorSet();
    set.playTogether(fromAnimator, toAnimator);
    return set;
  }

  private static void addOverflow(Overlay overlay, Animator animator, View overflow) {
    if (!overlay.getLocationInTarget(overflow, TEMP_LOCATION)) {
      return;
    }
    Bitmap bitmap = screenshot(overflow);
    if (bitmap == null) {
      return;
    }
    Drawable drawable = new BitmapDrawable(overflow.getResources(), bitmap);
    drawable.setBounds(TEMP_LOCATION[0], TEMP_LOCATION[1],
        TEMP_LOCATION[0] + bitmap.getWidth(), TEMP_LOCATION[1] + bitmap.getHeight());
    overlay.add(drawable);
    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        overlay.remove(drawable);
        bitmap.recycle();
      }
    });
  }

  @Nullable
  private static Animator changeActionMenuInternal(ActionMenuView from,
      ActionMenuView to, int duration, boolean toIsTop) {
    Overlay overlay = Overlay.getOverlay(to, EhvActivity.OVERLAY_CONTENT_ID);
    if (overlay == null) {
      return null;
    }

    // Get all visible views
    List<View> fromViews = getMenuItemViews(from);
    List<View> toViews = getMenuItemViews(to);

    // Remove overflow view if both contain it
    View overflow = null;
    if (fromViews != null && !fromViews.isEmpty()
        && fromViews.get(fromViews.size() - 1) instanceof ImageView
        && toViews != null && !toViews.isEmpty()
        && toViews.get(toViews.size() - 1) instanceof ImageView) {
      overflow = toViews.get(toViews.size() - 1);
      fromViews = fromViews.subList(0, fromViews.size() - 1);
      toViews = toViews.subList(0, toViews.size() - 1);
    }

    Animator animator = createActionMenuAnimator(
        overlay, from, fromViews, to, toViews, duration, toIsTop);

    // Keep overflow
    if (animator != null && overflow != null) {
      addOverflow(overlay, animator, overflow);
    }

    return animator;
  }

  /**
   * Change action menu animator.
   *
   * @param duration the duration for each menu item animation
   */
  public static Animator changeActionMenu(@Nullable ActionMenuView from,
      @Nullable ActionMenuView to, int duration, boolean toIsTop) {
    Animator animator = changeActionMenuInternal(from, to, duration, toIsTop);
    if (animator != null) {
      // Hide menu
      if (from != null) {
        from.setAlpha(0.0f);
      }
      if (to != null) {
        to.setAlpha(0.0f);
      }
      animator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          // Show menu
          if (from != null) {
            from.setAlpha(1.0f);
          }
          if (to != null) {
            to.setAlpha(1.0f);
          }
        }
      });
    }
    return animator;
  }
}
