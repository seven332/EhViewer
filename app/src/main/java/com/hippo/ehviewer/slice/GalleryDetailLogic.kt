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

package com.hippo.ehviewer.slice

import com.hippo.ehviewer.EH_CLIENT
import com.hippo.ehviewer.EH_URL
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.mvp.EhvPen
import com.hippo.ehviewer.mvp.MvpLogic
import com.hippo.viewstate.ViewState
import io.reactivex.android.schedulers.AndroidSchedulers

/*
 * Created by Hippo on 2017/8/5.
 */

interface GalleryDetailLogic : MvpLogic<GalleryDetailUi> {

  fun onClickTag(namespace: String, tag: String)
}

open class GalleryDetailPen private constructor(
    val state: GalleryDetailUiState
) : EhvPen<GalleryDetailUi>(), GalleryDetailLogic, GalleryDetailUi by state {
  constructor() : this(GalleryDetailUiState())

  override fun createViewState(): ViewState<GalleryDetailUi> = state

  override fun onClickTag(namespace: String, tag: String) {}

  fun init(info: GalleryInfo) {
    showContent(info, false)
    showContentProgress(false)

    EH_CLIENT.galleryDetail(EH_URL.galleryDetailUrl(info.gid, info.token))
        .observeOn(AndroidSchedulers.mainThread())
        .register({ (info, comments) ->
          showContentBody(info, comments, true)
        }, {
          showContentTip(it, true)
        })
  }

  fun init(gid: Long, token: String) {

  }
}
