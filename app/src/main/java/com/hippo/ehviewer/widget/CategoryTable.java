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
 * Created by Hippo on 5/13/2017.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import com.google.android.flexbox.FlexboxLayout;
import com.hippo.android.animator.Animators;
import com.hippo.android.animator.util.FloatProperty;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.yorozuya.MathUtils;

/**
 * {@code CategoryTable} shows all categories as a table.
 */
public class CategoryTable extends FlexboxLayout {

  private static final int[] CATEGORY_ARRAY = {
      EhUtils.CATEGORY_DOUJINSHI,
      EhUtils.CATEGORY_MANGA,
      EhUtils.CATEGORY_ARTIST_CG,
      EhUtils.CATEGORY_GAME_CG,
      EhUtils.CATEGORY_WESTERN,
      EhUtils.CATEGORY_NON_H,
      EhUtils.CATEGORY_IMAGE_SET,
      EhUtils.CATEGORY_COSPLAY,
      EhUtils.CATEGORY_ASIAN_PORN,
      EhUtils.CATEGORY_MISC,
  };

  public CategoryTable(Context context) {
    super(context);
    init(context);
  }

  public CategoryTable(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public CategoryTable(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    setFlexWrap(FLEX_WRAP_WRAP);

    LayoutInflater inflater = LayoutInflater.from(context);
    for (int category : CATEGORY_ARRAY) {
      addCategoryView(inflater, category);
    }
  }

  private void addCategoryView(LayoutInflater inflater, int category) {
    inflater.inflate(R.layout.category_table_item, this);
    CategoryView view = (CategoryView) getChildAt(getChildCount() - 1);
    view.setText(EhUtils.getCategoryNotNull(category).toUpperCase());
    view.setBackgroundColor(EhUtils.getCategoryColor(category));
  }

  /**
   * Return the unchecked category.
   */
  public int getCategory() {
    int result = 0;
    for (int i = 0, n = CATEGORY_ARRAY.length; i < n; ++i) {
      if (!((CategoryView) getChildAt(i)).isChecked()) {
        result |= CATEGORY_ARRAY[i];
      }
    }
    return result;
  }

  @Override
  public Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState savedState = new SavedState(superState);
    for (int i = 0, n = savedState.checkedArray.length; i < n; ++i) {
      savedState.checkedArray[i] = ((CategoryView) getChildAt(i)).isChecked();
    }
    return savedState;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());
    for (int i = 0, n = ss.checkedArray.length; i < n; ++i) {
      ((CategoryView) getChildAt(i)).setChecked(ss.checkedArray[i]);
    }
  }

  public static class SavedState extends BaseSavedState {

    boolean[] checkedArray = new boolean[CATEGORY_ARRAY.length];

    SavedState(Parcelable superState) {
      super(superState);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      for (boolean checked : checkedArray) {
        out.writeInt(checked ? 1 : 0);
      }
    }

    public static final Parcelable.Creator<SavedState> CREATOR
        = new Parcelable.Creator<SavedState>() {
      @Override
      public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      @Override
      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };

    private SavedState(Parcel in) {
      super(in);
      for (int i = 0, n = checkedArray.length; i < n; ++i) {
        checkedArray[i] = in.readInt() != 0;
      }
    }
  }

  /**
   * One {@code CategoryView} represents one category.
   */
  public static class CategoryView extends AppCompatTextView implements OnClickListener {

    private static final FloatProperty<CategoryView> CATEGORY_VIEW_RADIUS_PROPERTY;

    private static final long ANIMATOR_DURATION = 150L;

    static {
      CATEGORY_VIEW_RADIUS_PROPERTY = new FloatProperty<CategoryView>() {
        @Override
        public Float get(CategoryView object) {
          return object.getRadius();
        }
        @Override
        public void setValue(CategoryView object, float v) {
          object.setRadius(v);
        }
      };
    }

    private float radius;
    private float x;
    private float y;
    private boolean checked;
    private int maskColor;
    private Paint paint;
    private Animator animator;

    public CategoryView(Context context) {
      super(context);
      init();
    }

    public CategoryView(Context context, AttributeSet attrs) {
      super(context, attrs);
      init();
    }

    public CategoryView(Context context, AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
      init();
    }

    private void init() {
      maskColor = 0x8a000000;
      paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setColor(maskColor);
      setOnClickListener(this);
    }

    private void setRadius(float radius) {
      float bigger = Math.max(this.radius, radius);
      this.radius = radius;
      invalidate((int) (x - bigger), (int) (y - bigger),
          (int) (x + bigger), (int) (y + bigger));
    }

    private float getRadius() {
      return radius;
    }

    @Override
    public void onClick(View v) {
      setChecked(!checked, true);
    }

    /**
     * Sets checked value for this {@code CategoryView}.
     */
    public void setChecked(boolean checked) {
      setChecked(checked, false);
    }

    /**
     * Returns {@code true} if this {@code CategoryView} is checked.
     */
    public boolean isChecked() {
      return checked;
    }

    private void endAnimator() {
      if (animator != null) {
        animator.end();
        animator = null;
      }
    }

    private void startAnimator() {
      endAnimator();

      float startRadius;
      float endRadius;
      float maxRadius = MathUtils.coverRadius(getWidth(), getHeight(), x, y);
      if (checked) {
        startRadius = 0;
        endRadius = maxRadius;
      } else {
        startRadius = maxRadius;
        endRadius = 0;
      }

      animator = ObjectAnimator.ofFloat(this, CATEGORY_VIEW_RADIUS_PROPERTY, startRadius, endRadius);
      animator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          animator = null;
        }
      });
      animator.setInterpolator(Animators.SLOW_FAST);
      animator.setDuration(ANIMATOR_DURATION);
      animator.start();
    }

    private void setChecked(boolean checked, boolean animation) {
      if (this.checked != checked) {
        this.checked = checked;
        if (animation) {
          startAnimator();
        }
      }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        x = event.getX();
        y = event.getY();
        endAnimator();
      }
      return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
      super.onDraw(canvas);

      if (animator != null) {
        canvas.drawCircle(x, y, radius, paint);
      } else if (checked) {
        canvas.drawColor(maskColor);
      }
    }

    @Override
    public Parcelable onSaveInstanceState() {
      Parcelable superState = super.onSaveInstanceState();
      SavedState savedState = new SavedState(superState);
      savedState.checked = checked;
      return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
      SavedState ss = (SavedState) state;
      super.onRestoreInstanceState(ss.getSuperState());
      setChecked(ss.checked);
    }

    public static class SavedState extends BaseSavedState {

      boolean checked;

      SavedState(Parcelable superState) {
        super(superState);
      }

      @Override
      public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(checked ? 1 : 0);
      }

      public static final Parcelable.Creator<SavedState> CREATOR
          = new Parcelable.Creator<SavedState>() {
        @Override
        public SavedState createFromParcel(Parcel in) {
          return new SavedState(in);
        }

        @Override
        public SavedState[] newArray(int size) {
          return new SavedState[size];
        }
      };

      private SavedState(Parcel in) {
        super(in);
        checked = in.readInt() != 0;
      }
    }
  }
}
