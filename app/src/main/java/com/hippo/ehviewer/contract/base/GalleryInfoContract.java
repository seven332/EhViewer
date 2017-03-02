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

package com.hippo.ehviewer.contract.base;

/*
 * Created by Hippo on 2/24/2017.
 */

import android.content.Context;
import android.support.annotation.NonNull;
import com.hippo.ehviewer.component.base.GalleryInfoAdapter;
import com.hippo.ehviewer.presenter.base.PresenterInterface;
import com.hippo.ehviewer.view.base.ViewInterface;
import com.hippo.ehviewer.widget.ContentLayout;

public interface GalleryInfoContract {

  interface Presenter<V extends View> extends PresenterInterface<V> {

    /**
     * Attaches Presenter to the {@code ContentLayout}.
     * Returns the {@code RecyclerView.Adapter}.
     */
    @NonNull
    GalleryInfoAdapter attachContentLayout(Context context, ContentLayout layout);

    /**
     * Detaches Presenter from the {@code ContentLayout}.
     */
    void detachContentLayout(ContentLayout layout);
  }

  interface View extends ViewInterface {}
}
