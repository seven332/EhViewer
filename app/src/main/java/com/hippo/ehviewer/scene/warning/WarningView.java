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

package com.hippo.ehviewer.scene.warning;

/*
 * Created by Hippo on 2/8/2017.
 */

import android.support.v4.content.ContextCompat;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.view.MessageSheetView;

public class WarningView extends MessageSheetView<WarningContract.Presenter, WarningScene>
    implements WarningContract.View {

  @Override
  protected void bindView() {
    setHeader(R.color.red_500);
    setTitle(R.string.warning_title);
    setIcon(R.drawable.v_alert_white_x48);
    setMessage(R.string.warning_message);
    setNegativeButton(R.string.reject, v -> getEhvApp().finish());
    setPositiveButton(R.string.accept, v -> {
      WarningContract.Presenter presenter = getPresenter();
      if (presenter != null) {
        presenter.neverShowWarning();
      }
      getEhvActivity().nextScene();
    });
  }

  @Override
  public int getStatusBarColor() {
    return ContextCompat.getColor(getEhvActivity(), R.color.red_700);
  }

  @Override
  protected boolean whetherShowLeftDrawer() {
    return false;
  }
}
