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
 * Created by Hippo on 2/8/2017.
 */

import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.contract.AnalyticsContract;
import com.hippo.ehviewer.view.base.MessageSheetView;
import com.hippo.html.Html;

public class AnalyticsView extends MessageSheetView<AnalyticsContract.Presenter>
    implements AnalyticsContract.View {

  @Override
  protected void bindView() {
    setHeader(R.color.indigo_500);
    setTitle(R.string.analytics);
    setIcon(R.drawable.v_chart_white_x48);
    setMessage(Html.fromHtml(getString(R.string.analytics_message), Html.FROM_HTML_MODE_LEGACY));
    getMessageView().setMovementMethod(LinkMovementMethod.getInstance());
    setNegativeButton(R.string.reject, a -> {
      AnalyticsContract.Presenter presenter = getPresenter();
      if (presenter != null) {
        presenter.rejectAnalytics();
        presenter.neverAskAnalytics();
      }
      getActivity().nextController();
    });
    setPositiveButton(R.string.accept, a -> {
      AnalyticsContract.Presenter presenter = getPresenter();
      if (presenter != null) {
        presenter.acceptAnalytics();
        presenter.neverAskAnalytics();
      }
      getActivity().nextController();
    });
  }

  @Override
  protected int getStatusBarColor() {
    return ContextCompat.getColor(getActivity(), R.color.indigo_700);
  }

  @Override
  protected boolean whetherShowLeftDrawer() {
    return false;
  }
}
