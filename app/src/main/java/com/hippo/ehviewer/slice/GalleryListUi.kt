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

import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hippo.ehviewer.EHV_PREFERENCES
import com.hippo.ehviewer.R
import com.hippo.ehviewer.adapter.AlertAdapter
import com.hippo.ehviewer.adapter.AlertHolder
import com.hippo.ehviewer.client.categoryColor
import com.hippo.ehviewer.client.categoryStringNonNull
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.langAbbr
import com.hippo.ehviewer.mvp.EhvPaper
import com.hippo.ehviewer.mvp.MvpUi
import com.hippo.ehviewer.util.INVALID_INDEX
import com.hippo.ehviewer.util.dp2pix
import com.hippo.ehviewer.util.find
import com.hippo.ehviewer.util.prettyTime
import com.hippo.ehviewer.util.string
import com.hippo.ehviewer.widget.NumberRatingView
import com.hippo.ehviewer.widget.SmallRatingView
import com.hippo.ehviewer.widget.content.ContentLayout
import com.hippo.ehviewer.widget.ehv.EhvCover
import com.hippo.ehviewer.widget.recyclerview.AutoGridLayoutManager
import com.hippo.ehviewer.widget.recyclerview.MarginItemDecoration
import com.hippo.viewstate.GenerateViewState
import com.hippo.viewstate.strategy.SingleByTag
import com.hippo.viewstate.strategy.StrategyType

/*
 * Created by Hippo on 2017/7/24.
 */

@GenerateViewState
interface GalleryListUi : MvpUi {

  companion object {
    private const val TAG_MODE = "GalleryListUi:mode"
  }

  @StrategyType(value = SingleByTag::class, tag = TAG_MODE)
  fun asDetailList()

  @StrategyType(value = SingleByTag::class, tag = TAG_MODE)
  fun asBriefGrid()
}

class GalleryListPaper(
    private val logic: GalleryListLogic
) : EhvPaper<GalleryListPaper>(logic), GalleryListUi, ContentLayout.Extension {

  private lateinit var contentLayout: ContentLayout
  private lateinit var recyclerView: RecyclerView

  private lateinit var layoutManager: AutoGridLayoutManager

  private val detailAdapter by lazy { GalleryDetailAdapter() }
  private val briefAdapter by lazy { GalleryBriefAdapter() }
  private var hasInitAdapter = false
  private lateinit var adapter: AlertAdapter<GalleryInfo, *>

  private val detailDecoration by lazy { DividerItemDecoration(context, DividerItemDecoration.VERTICAL) }
  private val briefDecoration by lazy { MarginItemDecoration(1.dp2pix(context)) }
  private var hasInitDecoration = false
  private lateinit var decoration: RecyclerView.ItemDecoration

  override fun onCreate(inflater: LayoutInflater, container: ViewGroup) {
    super.onCreate(inflater, container)

    view = inflater.inflate(R.layout.paper_gallery_list, container, false)
    contentLayout = view.find(R.id.content_layout)
    recyclerView = view.find(R.id.recycler_view)

    contentLayout.extension = this
    logic.initContentLayout(contentLayout)

    layoutManager = AutoGridLayoutManager(context)
    recyclerView.layoutManager = layoutManager
  }

  override fun onDestroy() {
    super.onDestroy()
    logic.termAdapter(adapter)
    logic.termContentLayout(contentLayout)
    recyclerView.layoutManager = null
    recyclerView.adapter = null
  }

  private fun setRecyclerViewStyle(
      newAdapter: AlertAdapter<GalleryInfo, *>,
      newDecoration: RecyclerView.ItemDecoration,
      columnSize: Int,
      padding: Int
  ) {
    if (hasInitAdapter) {
      logic.termAdapter(adapter)
    }
    hasInitAdapter = true
    adapter = newAdapter
    logic.initAdapter(newAdapter)
    // RecyclerView will scroll to top after change adapter
    // Just save the first visible item position, and scroll to it
    val position = layoutManager.findFirstVisibleItemPosition()
    recyclerView.adapter = newAdapter
    if (position != INVALID_INDEX) {
      layoutManager.scrollToPosition(position)
    }

    if (hasInitDecoration) {
      recyclerView.removeItemDecoration(decoration)
    }
    hasInitDecoration = true
    decoration = newDecoration
    recyclerView.addItemDecoration(newDecoration)

    layoutManager.setColumnSize(columnSize)

    (recyclerView.layoutParams as ViewGroup.MarginLayoutParams).apply {
      leftMargin = padding
      topMargin = padding
      rightMargin = padding
      bottomMargin = padding
    }
    recyclerView.requestLayout()
  }

  override fun asDetailList() = setRecyclerViewStyle(detailAdapter, detailDecoration, EHV_PREFERENCES.detailWidth.value.dp2pix(context), 0)

  override fun asBriefGrid() = setRecyclerViewStyle(briefAdapter, briefDecoration, EHV_PREFERENCES.briefWidth.value.dp2pix(context), -(1.dp2pix(context)))

  override fun showMessage(message: String) {
    logic.showMessage(message)
  }

  private inner class GalleryDetailHolder(itemView: View) : AlertHolder(itemView) {

    val cover = itemView.find<EhvCover>(R.id.cover)
    val title = itemView.find<TextView>(R.id.title)
    val uploader = itemView.find<TextView>(R.id.uploader)
    val rating = itemView.find<SmallRatingView>(R.id.rating)
    val category = itemView.find<TextView>(R.id.category)
    val date = itemView.find<TextView>(R.id.date)
    val language = itemView.find<TextView>(R.id.language)

    val item: GalleryInfo? get() = adapterPosition.takeIf { it in 0 until adapter.size }?.let { adapter[it] }

    init {
      itemView.setOnClickListener { item?.let { logic.onClickGalleryInfo(it) } }
    }

    override fun onResume() {
      super.onResume()
      cover.start()
    }

    override fun onPause() {
      super.onPause()
      cover.stop()
    }
  }

  private inner class GalleryDetailAdapter : AlertAdapter<GalleryInfo, GalleryDetailHolder>(lifecycle) {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): GalleryDetailHolder =
        GalleryDetailHolder(inflater.inflate(R.layout.gallery_list_item_detail, parent, false))

    override fun onBindViewHolder(holder: GalleryDetailHolder, position: Int) {
      val info = get(position)

      holder.cover.load(info)
      holder.title.text = info.title
      holder.uploader.text = info.uploader
      holder.rating.rating = info.rating
      holder.category.text = info.category.categoryStringNonNull()
      holder.category.setBackgroundColor(info.category.categoryColor())
      holder.date.text = info.date.prettyTime(context)
      holder.language.text = info.language.langAbbr().let { if (it != 0) context.string(it) else null }

      super.onBindViewHolder(holder, position)
    }
  }

  private inner class GalleryBriefHolder(itemView: View) : AlertHolder(itemView) {

    val cover = itemView.find<EhvCover>(R.id.cover)
    val category = itemView.find<View>(R.id.category)
    val rating = itemView.find<NumberRatingView>(R.id.rating)
    val language = itemView.find<TextView>(R.id.language)

    val item: GalleryInfo? get() = adapterPosition.takeIf { it in 0 until adapter.size }?.let { adapter[it] }

    init {
      itemView.setOnClickListener { item?.let { logic.onClickGalleryInfo(it) } }
    }

    override fun onResume() {
      super.onResume()
      cover.start()
    }

    override fun onPause() {
      super.onPause()
      cover.stop()
    }
  }

  private inner class GalleryBriefAdapter : AlertAdapter<GalleryInfo, GalleryBriefHolder>(lifecycle) {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): GalleryBriefHolder =
        GalleryBriefHolder(inflater.inflate(R.layout.gallery_list_item_brief, parent, false))

    override fun onBindViewHolder(holder: GalleryBriefHolder, position: Int) {
      val info = get(position)

      holder.cover.load(info)
      holder.category.setBackgroundColor(info.category.categoryColor() and 0xffffff or 0xa2000000.toInt())
      holder.rating.rating = info.rating
      holder.language.text = info.language.langAbbr().let { if (it != 0) context.string(it) else null }

      super.onBindViewHolder(holder, position)
    }
  }
}
