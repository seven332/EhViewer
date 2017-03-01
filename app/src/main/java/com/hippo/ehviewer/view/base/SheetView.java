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

package com.hippo.ehviewer.view.base;

/*
 * Created by Hippo on 2/8/2017.
 */

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.presenter.base.PresenterInterface;
import com.hippo.ehviewer.widget.crossfade.CrossFadeImageView;
import com.hippo.ehviewer.widget.crossfade.CrossFadeTextView;
import com.hippo.ehviewer.widget.IndicatingScrollView;

/**
 * {@code SheetView} shows a header, a message and two buttons.
 */
public abstract class SheetView<P extends PresenterInterface> extends EhvView<P> {

  private View header;
  private CrossFadeTextView title;
  private CrossFadeImageView icon;
  private Button positive;
  private Button negative;
  private IndicatingScrollView scroll;
  private View contentView;

  @NonNull
  @Override
  protected final View onCreateView(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.controller_sheet, parent, false);

    header = view.findViewById(R.id.header);
    title = (CrossFadeTextView) header.findViewById(R.id.title);
    icon = (CrossFadeImageView) header.findViewById(R.id.icon);
    positive = (Button) view.findViewById(R.id.positive);
    negative = (Button) view.findViewById(R.id.negative);
    scroll = (IndicatingScrollView) view.findViewById(R.id.scroll_view);

    contentView = createContentView(inflater, scroll);
    scroll.addView(contentView);

    return view;
  }

  /**
   * Creates content view for this {@code SheetView}.
   */
  @NonNull
  protected abstract View createContentView(LayoutInflater inflater, ViewGroup parent);

  /**
   * Sets header background.
   */
  protected void setHeader(@ColorRes int bgId) {
    header.setBackgroundResource(bgId);
  }

  /**
   * Sets header background.
   */
  @SuppressWarnings("deprecation")
  protected void setHeader(Drawable bg) {
    header.setBackgroundDrawable(bg);
  }

  /**
   * Sets header title.
   */
  protected void setTitle(@StringRes int titleId) {
    title.setText(titleId);
  }

  /**
   * Sets header icon.
   */
  protected void setIcon(@DrawableRes int iconId) {
    icon.setImageResource(iconId);
  }

  /**
   * Sets positive button.
   */
  protected void setPositiveButton(@StringRes int textId, View.OnClickListener listener) {
    positive.setText(textId);
    positive.setOnClickListener(listener);
  }

  /**
   * Sets negative button.
   */
  protected void setNegativeButton(@StringRes int textId, View.OnClickListener listener) {
    negative.setText(textId);
    negative.setOnClickListener(listener);
  }

  /**
   * Returns the content view created by {@link #createView(LayoutInflater, ViewGroup)}.
   */
  protected View getContentView() {
    return contentView;
  }

  /**
   * Returns the {@code ScrollView} which content view is added to.
   */
  protected IndicatingScrollView getScrollView() {
    return scroll;
  }

  /**
   * Returns positive button
   */
  protected Button getPositiveButton() {
    return positive;
  }

  /**
   * Returns negative button
   */
  protected Button getNegativeButton() {
    return negative;
  }
}
