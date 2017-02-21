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
 * Created by Hippo on 2/10/2017.
 */

import com.hippo.ehviewer.presenter.PresenterInterface;
import com.hippo.ehviewer.view.base.ViewInterface;

public interface ContentContract {

  interface Presenter extends PresenterInterface<View> {

    void onRefreshHeader();

    void onRefreshFooter();

    void onClickTip();

    void goTo(int page);

    void switchTo(int page);

    int size();
  }

  interface View extends ViewInterface {

    void showContent();

    void showTip(Throwable t);

    void showProgressBar();

    void showMessage(Throwable t);

    void stopRefreshing();

    void setHeaderRefreshing();

    void setFooterRefreshing();

    void scrollToPosition(int position);

    void notifyItemRangeInserted(int positionStart, int itemCount);

    void notifyItemRangeRemoved(int positionStart, int itemCount);
  }
}
