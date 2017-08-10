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

import com.hippo.ehviewer.EHV_PREFERENCES
import com.hippo.ehviewer.EH_CLIENT
import com.hippo.ehviewer.EH_URL
import com.hippo.ehviewer.LIST_MODE_BRIEF
import com.hippo.ehviewer.LIST_MODE_DETAIL
import com.hippo.ehviewer.client.GLUrlBuilder
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.mvp.EhvPen
import com.hippo.ehviewer.mvp.MvpLogic
import com.hippo.ehviewer.widget.content.ContentData
import com.hippo.ehviewer.widget.content.ContentDataAdapter
import com.hippo.ehviewer.widget.content.ContentLayout
import com.hippo.viewstate.ViewState
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

/*
 * Created by Hippo on 2017/7/24.
 */

interface GalleryListLogic : MvpLogic<GalleryListUi> {

  fun initAdapter(adapter: ContentDataAdapter<GalleryInfo, *>)

  fun termAdapter(adapter: ContentDataAdapter<GalleryInfo, *>)

  fun initContentLayout(layout: ContentLayout)

  fun termContentLayout(layout: ContentLayout)

  fun showMessage(message: String)

  fun onClickGalleryInfo(info: GalleryInfo)
}

open class GalleryListPen private constructor(
    val state: GalleryListUiState
) : EhvPen<GalleryListUi>(), GalleryListLogic, GalleryListUi by state {
  constructor() : this (GalleryListUiState())

  private val data = GalleryData()
  private val builder = GLUrlBuilder()

  val builderObservable: Observable<GLUrlBuilder> = BehaviorRelay.createDefault(builder)

  override fun onCreate() {
    super.onCreate()

    EHV_PREFERENCES.listMode.observable.register {
      when (it) {
        LIST_MODE_DETAIL -> view.asDetailList()
        LIST_MODE_BRIEF -> view.asBriefGrid()
      }
    }

    // `observeOn(AndroidSchedulers.mainThread())` to make sure
    // it works after `EH_CLIENT` and `EH_URL` changes
    EHV_PREFERENCES.ehMode.observable.observeOn(AndroidSchedulers.mainThread()).register {
      data.goTo(0)
    }
  }

  override fun createViewState(): ViewState<GalleryListUi> = state

  override fun initAdapter(adapter: ContentDataAdapter<GalleryInfo, *>) {
    adapter.data = data
    adapter.notifyDataSetChanged()
  }

  override fun termAdapter(adapter: ContentDataAdapter<GalleryInfo, *>) {}

  override fun initContentLayout(layout: ContentLayout) {
    layout.logic = data
    data.attach(layout)
  }

  override fun termContentLayout(layout: ContentLayout) {
    data.detach()
  }

  override fun showMessage(message: String) {}

  override fun onClickGalleryInfo(info: GalleryInfo) {}

  fun refresh() {
    data.goTo(0)
  }

  private inner class GalleryData : ContentData<GalleryInfo>() {

    override fun onRequireData(id: Int, page: Int) {
      builder.page = page
      EH_CLIENT.galleryList(EH_URL.galleryListUrl(builder.build()))
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ (data, pages) ->
            setData(id, data, pages)
          }, {
            setError(id, it)
          })
    }

    override fun onRestoreData(id: Int) {
      // TODO("not implemented")
    }

    override fun onBackupData(data: List<GalleryInfo>) {
      // TODO("not implemented")
    }

    override fun isDuplicate(t1: GalleryInfo, t2: GalleryInfo): Boolean = t1.gid == t2.gid
  }
}
