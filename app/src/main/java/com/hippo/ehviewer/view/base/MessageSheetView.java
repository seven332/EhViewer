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
 * Created by Hippo on 2/11/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.presenter.PresenterInterface;
import com.hippo.ehviewer.util.TextUtils2;
import com.hippo.ehviewer.widget.crossfade.CrossFadeTextView;

public abstract class MessageSheetView<P extends PresenterInterface> extends SheetView<P> {

  private CrossFadeTextView message;

  @NonNull
  @Override
  protected View createContentView(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.controller_message_sheet, parent, false);
    message = (CrossFadeTextView) view.findViewById(R.id.message);
    bindView();
    return view;
  }

  /**
   * Call {@code setXXX()} here.
   */
  protected abstract void bindView();

  /**
   * Returns message TextView.
   */
  protected TextView getMessageView() {
    return message.getToView();
  }

  /**
   * Sets message.
   */
  protected void setMessage(@StringRes int messageId) {
    setMessage(getResources().getString(messageId));
  }

  /**
   * Sets message.
   */
  protected void setMessage(CharSequence messageText) {
    if (getResources().getBoolean(R.bool.paragraph_leading_margin)) {
      int margin = (int) (message.getToView().getTextSize() * 2);
      messageText = TextUtils2.addLeadingMarginSpan(messageText, margin);
    }
    message.setText(messageText);
  }
}
