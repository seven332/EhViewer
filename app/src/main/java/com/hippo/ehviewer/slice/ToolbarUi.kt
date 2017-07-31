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

import android.graphics.drawable.Drawable
import android.support.v7.widget.Toolbar
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import com.hippo.ehviewer.R
import com.hippo.ehviewer.mvp.GroupPaper
import com.hippo.ehviewer.mvp.MvpUi
import com.hippo.ehviewer.util.find
import com.hippo.viewstate.GenerateViewState
import com.hippo.viewstate.strategy.SingleByMethod
import com.hippo.viewstate.strategy.SingleByTag
import com.hippo.viewstate.strategy.StrategyType

/*
 * Created by Hippo on 2017/7/24.
 */

@GenerateViewState
interface ToolbarUi : MvpUi {

  companion object {
    private const val TAG_SET_TITLE = "ToolbarUi:set_title"
    private const val TAG_SET_SUBTITLE = "ToolbarUi:set_subtitle"
    private const val TAG_SET_NAVIGATION_ICON = "ToolbarUi:set_navigation_icon"
  }

  @StrategyType(value = SingleByTag::class, tag = TAG_SET_TITLE)
  fun setTitle(title: CharSequence?)

  @StrategyType(value = SingleByTag::class, tag = TAG_SET_TITLE)
  fun setTitle(resId: Int)

  @StrategyType(value = SingleByTag::class, tag = TAG_SET_SUBTITLE)
  fun setSubtitle(subtitle: CharSequence)

  @StrategyType(value = SingleByTag::class, tag = TAG_SET_SUBTITLE)
  fun setSubtitle(resId: Int)

  @StrategyType(value = SingleByTag::class, tag = TAG_SET_NAVIGATION_ICON)
  fun setNavigationIcon(icon: Drawable)

  @StrategyType(value = SingleByTag::class, tag = TAG_SET_NAVIGATION_ICON)
  fun setNavigationIcon(resId: Int)

  @StrategyType(value = SingleByMethod::class)
  fun setDoubleClickEnabled()

  @StrategyType(value = SingleByMethod::class)
  fun inflateMenu(resId: Int)
}


class ToolbarPaper(
    private val logic: ToolbarLogic
) : GroupPaper<ToolbarPaper>(logic), ToolbarUi {

  companion object {
    const val CONTAINER_ID = R.id.toolbar_container
  }

  private lateinit var toolbar: Toolbar

  override fun onCreate(inflater: LayoutInflater, container: ViewGroup) {
    super.onCreate(inflater, container)

    view = inflater.inflate(R.layout.paper_toolbar, container, false)
    toolbar = view.find(R.id.toolbar)
    toolbar.setNavigationOnClickListener { logic.onClickNavigationIcon() }
    toolbar.setOnMenuItemClickListener { logic.onClickMenuItem(it) }
  }

  override fun setTitle(title: CharSequence?) {
    toolbar.title = title
  }

  override fun setTitle(resId: Int) {
    toolbar.setTitle(resId)
  }

  override fun setSubtitle(subtitle: CharSequence) {
    toolbar.subtitle = subtitle
  }

  override fun setSubtitle(resId: Int) {
    toolbar.setSubtitle(resId)
  }

  override fun setNavigationIcon(icon: Drawable) {
    toolbar.navigationIcon = icon
  }

  override fun setNavigationIcon(resId: Int) {
    toolbar.setNavigationIcon(resId)
  }

  override fun setDoubleClickEnabled() {
    val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
      override fun onDoubleTap(e: MotionEvent?): Boolean {
        logic.onDoubleClick()
        return true
      }
    })
    toolbar.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
  }

  override fun inflateMenu(resId: Int) {
    toolbar.menu.clear()
    toolbar.inflateMenu(resId)
  }
}
