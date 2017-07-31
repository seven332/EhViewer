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

package com.hippo.ehviewer.widget.content

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.dp2pix
import com.hippo.ehviewer.util.explain
import com.hippo.ehviewer.util.explainVividly
import com.hippo.ehviewer.util.find
import com.hippo.ehviewer.util.onClick
import com.hippo.ehviewer.widget.ViewTransition
import com.hippo.refreshlayout.RefreshLayout

/*
 * Created by Hippo on 6/5/2017.
 */

class ContentLayout : FrameLayout, ContentUi {

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  lateinit var logic: ContentLogic
  lateinit var extension: Extension

  val refreshLayout: RefreshLayout
  val recyclerView: RecyclerView
  val tipView: TextView
  val progressView: View
  val transition: ViewTransition

  val aLittleDistance: Int

  init {
    LayoutInflater.from(context).inflate(R.layout.widget_content_layout, this)
    refreshLayout = find(R.id.refresh_layout)
    recyclerView = find(R.id.recycler_view)
    tipView = find(R.id.tip_view)
    progressView = find(R.id.progress_view)

    refreshLayout.setHeaderColorSchemeResources(
        R.color.color_scheme_1,
        R.color.color_scheme_2,
        R.color.color_scheme_3,
        R.color.color_scheme_4,
        R.color.color_scheme_5,
        R.color.color_scheme_6
    )
    refreshLayout.setFooterColorSchemeResources(
        R.color.color_scheme_1,
        R.color.color_scheme_2,
        R.color.color_scheme_3,
        R.color.color_scheme_4,
        R.color.color_scheme_5,
        R.color.color_scheme_6
    )
    refreshLayout.setOnRefreshListener(object : RefreshLayout.OnRefreshListener {
      override fun onHeaderRefresh() { logic.onRefreshHeader() }
      override fun onFooterRefresh() { logic.onRefreshFooter() }
    })

    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        if (!refreshLayout.isRefreshing && refreshLayout.isAlmostBottom) {
          logic.onReachBottom()
        }
      }
    })

    tipView.onClick { logic.onClickTip() }

    transition = ViewTransition(refreshLayout, tipView, progressView)

    aLittleDistance = 48.dp2pix(context)
  }

  override fun showContent() {
    transition.show(refreshLayout, !logic.isRestoring())
  }

  override fun showTip(t: Throwable) {
    transition.show(tipView, !logic.isRestoring())

    val drawable = explainVividly(t, context)
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    tipView.setCompoundDrawables(null, drawable, null, null)
    tipView.text = explain(t, context)
  }

  override fun showProgressBar() {
    transition.show(progressView, !logic.isRestoring())
  }

  override fun showMessage(t: Throwable) {
    extension.showMessage(explain(t, context))
  }

  override fun stopRefreshing() {
    refreshLayout.isHeaderRefreshing = false
    refreshLayout.isFooterRefreshing = false
  }

  override fun setHeaderRefreshing() {
    refreshLayout.isHeaderRefreshing = true
  }

  override fun setFooterRefreshing() {
    refreshLayout.isFooterRefreshing = true
  }

  override fun scrollToPosition(position: Int) {
    recyclerView.scrollToPosition(position)
  }

  override fun scrollUpALittle() {
    if (recyclerView.computeVerticalScrollOffset() == 0) {
      recyclerView.smoothScrollBy(0, -aLittleDistance)
    }
  }

  override fun scrollDownALittle() {
    if (refreshLayout.isAlmostBottom) {
      recyclerView.smoothScrollBy(0, aLittleDistance)
    }
  }

  override fun notifyDataSetChanged() {
    recyclerView.adapter?.notifyDataSetChanged()
  }

  override fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
    recyclerView.adapter?.notifyItemRangeInserted(positionStart, itemCount)
  }

  override fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int) {
    recyclerView.adapter?.notifyItemRangeRemoved(positionStart, itemCount)
  }

  override fun notifyItemRangeChanged(positionStart: Int, itemCount: Int) {
    recyclerView.adapter?.notifyItemRangeChanged(positionStart, itemCount)
  }

  /**
   * `ContentLayout` can't do all UI jobs. It needs a `Extension` to give a hand.
   */
  interface Extension {

    /**
     * Show a non-interrupting message. Toast? SnackBar? OK.
     */
    fun showMessage(message: String)
  }
}
