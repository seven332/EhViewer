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

import android.graphics.Color
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.TagSet
import com.hippo.ehviewer.client.lang
import com.hippo.ehviewer.drawable.RoundRectDrawable
import com.hippo.ehviewer.mvp.EhvPaper
import com.hippo.ehviewer.mvp.MvpUi
import com.hippo.ehviewer.util.attrColor
import com.hippo.ehviewer.util.clamp
import com.hippo.ehviewer.util.dp2pix
import com.hippo.ehviewer.util.drawable
import com.hippo.ehviewer.util.explain
import com.hippo.ehviewer.util.explainVividly
import com.hippo.ehviewer.util.find
import com.hippo.ehviewer.util.prettyTime
import com.hippo.ehviewer.util.string
import com.hippo.ehviewer.widget.ViewTransition
import com.hippo.ehviewer.widget.ehv.EhvCover
import com.hippo.ehviewer.widget.ehv.EhvDetailPants
import com.hippo.viewstate.GenerateViewState
import com.hippo.viewstate.strategy.SingleByTag
import com.hippo.viewstate.strategy.StrategyType

/*
 * Created by Hippo on 2017/8/5.
 */

@GenerateViewState
interface GalleryDetailUi : MvpUi {

  companion object {
    private const val TAG_SHOW = "GalleryDetailUi:show"
    private const val TAG_SHOW_CONTENT = "GalleryDetailUi:show_content"
  }

  @StrategyType(value = SingleByTag::class, tag = TAG_SHOW)
  fun showContent(info: GalleryInfo, animation: Boolean)

  @StrategyType(value = SingleByTag::class, tag = TAG_SHOW)
  fun showTip(error: Throwable, animation: Boolean)

  @StrategyType(value = SingleByTag::class, tag = TAG_SHOW)
  fun showProgress(animation: Boolean)

  @StrategyType(value = SingleByTag::class, tag = TAG_SHOW_CONTENT)
  fun showContentBody(info: GalleryInfo, animation: Boolean)

  @StrategyType(value = SingleByTag::class, tag = TAG_SHOW_CONTENT)
  fun showContentTip(error: Throwable, animation: Boolean)

  @StrategyType(value = SingleByTag::class, tag = TAG_SHOW_CONTENT)
  fun showContentProgress(animation: Boolean)
}

class GalleryDetailPaper(
    private val logic: GalleryDetailLogic
) : EhvPaper<GalleryDetailPaper>(logic), GalleryDetailUi {

  lateinit var content: ViewGroup
  lateinit var tip: ViewGroup
  lateinit var progress: ViewGroup
  lateinit var transition: ViewTransition

  lateinit var contentBody: View
  lateinit var contentTip: TextView
  lateinit var contentProgress: View
  lateinit var contentTransition: ViewTransition

  override fun onCreate(inflater: LayoutInflater, container: ViewGroup) {
    super.onCreate(inflater, container)

    view = inflater.inflate(R.layout.paper_gallery_detail, container, false)
    content = view.find<ViewGroup>(R.id.content)
    tip = view.find<ViewGroup>(R.id.tip)
    progress = view.find<ViewGroup>(R.id.progress)
    transition = ViewTransition(content, tip, progress)

    contentBody = view.find(R.id.content_body)
    contentTip = view.find(R.id.content_tip)
    contentProgress = view.find(R.id.content_progress)
    contentTransition = ViewTransition(contentBody, contentTip, contentProgress)
  }

  override fun showTip(error: Throwable, animation: Boolean) {
    transition.show(tip, animation && !logic.isRestoring())

    tip.find<TextView>(R.id.tip_view).apply {
      text = explain(error, context)
      val drawable = explainVividly(error, context)
      drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
      setCompoundDrawables(null, drawable, null, null)
    }
  }

  override fun showProgress(animation: Boolean) {
    transition.show(progress, animation && !logic.isRestoring())
  }

  @Suppress("DEPRECATION")
  override fun showContent(info: GalleryInfo, animation: Boolean) {
    transition.show(content, animation && !logic.isRestoring())

    content.find<EhvDetailPants>(R.id.pants).info = info
    content.find<EhvCover>(R.id.cover).info = info
    content.find<TextView>(R.id.title).text = info.title
    content.find<TextView>(R.id.uploader).text = info.uploader
    content.find<View>(R.id.download).apply {
      setBackgroundDrawable(RoundRectDrawable(
          radius = 2.dp2pix(context).toFloat(),
          color = Color.WHITE,
          edgeWidth = 2.dp2pix(context).toFloat(),
          edgeColor = context.attrColor(R.attr.colorAccent)
      ))
    }
    content.find<View>(R.id.read).apply {
      setBackgroundDrawable(RoundRectDrawable(
          radius = 2.dp2pix(context).toFloat(),
          color = context.attrColor(R.attr.colorAccent)
      ))
    }
    content.find<Toolbar>(R.id.toolbar).apply {
      setNavigationIcon(R.drawable.arrow_left_dark_x24)
      inflateMenu(R.menu.gallery_detail)
    }
  }

  private fun Float.rating() : String =
      if (this != 0.0f) String.format("%.1f", this) else "|∀ﾟ"

  private fun Float.ratingText() : String {
    val id = if (this == 0.0f) R.string.rating_none
    else when (Math.ceil((this * 2).toDouble()).toInt().clamp(0, 10)) {
      10 -> R.string.rating_10
      9 -> R.string.rating_9
      8 -> R.string.rating_8
      7 -> R.string.rating_7
      6 -> R.string.rating_6
      5 -> R.string.rating_5
      4 -> R.string.rating_4
      3 -> R.string.rating_3
      2 -> R.string.rating_2
      1 -> R.string.rating_1
      0 -> R.string.rating_0
      else -> R.string.rating_none
    }
    return context.string(id)
  }

  private fun FlexboxLayout.inflateTags(tags: TagSet) {
    removeAllViews()

    for ((namespace, list) in tags) {
      val namespaceView = inflater.inflate(R.layout.gallery_detail_tags_namespace, this, false) as TextView
      namespaceView.text = namespace
      addView(namespaceView)

      for (tag in list) {
        val tagView = inflater.inflate(R.layout.gallery_detail_tags_tag, this, false) as TextView
        tagView.text = tag
        addView(tagView)
      }
    }
  }

  override fun showContentBody(info: GalleryInfo, animation: Boolean) {
    contentTransition.show(contentBody, animation && !logic.isRestoring())

    content.find<TextView>(R.id.language).text = info.language.lang().let { if (it != 0) it else R.string.language_unknown }.let { context.string(it) }
    content.find<TextView>(R.id.pages).text = context.string(R.string.gallery_detail_pages_value, info.pages)
    content.find<TextView>(R.id.favourited).text = context.string(R.string.gallery_detail_favourited_value, info.favourited)
    content.find<TextView>(R.id.date).text = info.date.prettyTime(context)
    content.find<TextView>(R.id.rating).text = info.rating.rating()
    content.find<TextView>(R.id.rating_text).text = info.rating.ratingText()
    content.find<TextView>(R.id.rated).let { rated ->
      rated.text = info.rated.toString()
      val drawable = context.drawable(R.drawable.account_primary_x16)
      drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
      rated.setCompoundDrawables(null, null, drawable, null)
    }
    content.find<ImageView>(R.id.rate).let { rate ->
      val id = if (info.rating >= 2.5f || info.rating == 0.0f) R.drawable.thumb_up_primary_x32  else R.drawable.thumb_down_primary_x32
      val drawable = context.drawable(id)
      rate.setImageDrawable(drawable)
    }
    if (info.tags.isEmpty()) {
      content.find<FlexboxLayout>(R.id.tags).visibility = View.GONE
      content.find<TextView>(R.id.no_tag).visibility = View.VISIBLE
    } else {
      content.find<FlexboxLayout>(R.id.tags).let { tags ->
        tags.visibility = View.VISIBLE
        tags.inflateTags(info.tags)
      }
      content.find<TextView>(R.id.no_tag).visibility = View.GONE
    }
  }

  override fun showContentTip(error: Throwable, animation: Boolean) {
    contentTransition.show(contentTip, animation && !logic.isRestoring())

    contentTip.apply {
      text = explain(error, context)
      val drawable = explainVividly(error, context)
      drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
      setCompoundDrawables(null, drawable, null, null)
    }
  }

  override fun showContentProgress(animation: Boolean) {
    contentTransition.show(contentProgress, animation && !logic.isRestoring())
  }
}
