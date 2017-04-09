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

package com.hippo.ehviewer.controller.dialog;

/*
 * Created by Hippo on 3/24/2017.
 */

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.bluelinelabs.conductor.Controller;
import com.hippo.android.dialog.base.DialogView;
import com.hippo.ehviewer.controller.EhvDialogController;

public class GoToDialog extends EhvDialogController {

  private static final String KEY_START = "GoToDialog:start";
  private static final String KEY_END = "GoToDialog:end";
  private static final String KEY_CURRENT = "GoToDialog:current";

  private int start;
  private int end;
  private int current;

  /**
   * Only for restore. Call {@link #create(Controller, int, int, int)} please.
   */
  public GoToDialog(@NonNull Bundle bundle) {
    super(bundle);
    start = bundle.getInt(KEY_START);
    end = bundle.getInt(KEY_END);
    current = bundle.getInt(KEY_CURRENT);
  }

  @NonNull
  @Override
  protected DialogView onCreateDialogView(@NonNull LayoutInflater layoutInflater,
      @NonNull ViewGroup viewGroup) {
    return null;
  }

  /**
   * Creates a {@code GoToDialog}.
   *
   * @param start the start page, included
   * @param end the end page, included
   * @param current current page, might not in range
   */
  //public static <T extends Controller & Supplier<Listener>> GoToDialog create(
  public static <T extends Controller> GoToDialog create(
      T t, int start, int end, int current) {
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_START, start);
    bundle.putInt(KEY_END, end);
    bundle.putInt(KEY_CURRENT, current);
    GoToDialog dialog = new GoToDialog(bundle);
    dialog.setTargetController(t);
    return dialog;
  }


  private static class GoToView extends View {

    public GoToView(Context context) {
      super(context);
    }

    public GoToView(Context context,
        @Nullable AttributeSet attrs) {
      super(context, attrs);
    }

    public GoToView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
    }

    private void init(Context context) {

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

      Log.d("TAG", "width mode = " + MeasureSpec.getMode(widthMeasureSpec));
      Log.d("TAG", "width size = " + MeasureSpec.getSize(widthMeasureSpec));
      Log.d("TAG", "height mode = " + MeasureSpec.getMode(heightMeasureSpec));
      Log.d("TAG", "height size = " + MeasureSpec.getSize(heightMeasureSpec));


      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
  }




  public interface Listener {
    void onGoTo(int page);
  }
}
