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

package com.hippo.ehviewer.view;

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
import com.hippo.ehviewer.scene.EhvScene;
import com.hippo.ehviewer.util.TextUtils2;

public abstract class MessageSheetView<P extends PresenterInterface, S extends EhvScene>
    extends SheetView<P, S> {

  private TextView message;

  @NonNull
  @Override
  protected View onCreateSheetContent(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.view_message_sheet, parent, false);
    message = (TextView) view.findViewById(R.id.message);
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
    return message;
  }

  /**
   * Sets message.
   */
  protected void setMessage(@StringRes int messageId) {
    setMessage(getString(messageId));
  }

  /**
   * Sets message.
   */
  protected void setMessage(CharSequence messageText) {
    if (getResources().getBoolean(R.bool.paragraph_leading_margin)) {
      int margin = (int) (message.getTextSize() * 2);
      messageText = TextUtils2.addLeadingMarginSpan(messageText, margin);
    }
    message.setText(messageText);
  }
}
