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
 * Created by Hippo on 3/1/2017.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewParent;
import com.bluelinelabs.conductor.ChangeHandlerFrameLayout;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.drawerlayout.DrawerLayoutChild;
import java.util.ArrayList;
import java.util.List;

/**
 * Content for {@link com.hippo.drawerlayout.DrawerLayout}.
 */
public class EhvDrawerContent extends ChangeHandlerFrameLayout implements DrawerLayoutChild {

  private List<OnGetWindowPaddingTopListener> listeners = new ArrayList<>();
  private int windowPaddingTop;
  private int windowPaddingBottom;

  public EhvDrawerContent(Context context) {
    super(context);
  }

  public EhvDrawerContent(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public EhvDrawerContent(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /**
   * Gets it's parent, {@link DrawerLayout}.
   */
  public DrawerLayout getDrawerLayout() {
    ViewParent parent = getParent();
    if (parent instanceof DrawerLayout) {
      return (DrawerLayout) parent;
    } else {
      return null;
    }
  }

  @Override
  public void onGetWindowPadding(int top, int bottom) {
    windowPaddingTop = top;
    windowPaddingBottom = bottom;

    // Callback
    List<OnGetWindowPaddingTopListener> copy = new ArrayList<>(listeners);
    for (OnGetWindowPaddingTopListener l : copy) {
      l.onGetWindowPaddingTop(top);
    }
  }

  @Override
  public int getAdditionalTopMargin() {
    return 0;
  }

  @Override
  public int getAdditionalBottomMargin() {
    return windowPaddingBottom;
  }

  /**
   * Register a {@link OnGetWindowPaddingTopListener}.
   * The {@link OnGetWindowPaddingTopListener#onGetWindowPaddingTop(int)} will be called at once.
   */
  public void addOnGetWindowPaddingTopListener(OnGetWindowPaddingTopListener listener) {
    if (listener != null) {
      listener.onGetWindowPaddingTop(windowPaddingTop);
      listeners.add(listener);
    }
  }

  /**
   * Remove a {@link OnGetWindowPaddingTopListener}.
   */
  public void removeOnGetWindowPaddingTopListener(OnGetWindowPaddingTopListener listener) {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  /**
   * The callback to get window padding top.
   */
  public interface OnGetWindowPaddingTopListener {
    void onGetWindowPaddingTop(int top);
  }
}
