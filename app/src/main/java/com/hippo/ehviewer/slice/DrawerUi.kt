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

import android.support.annotation.AttrRes
import android.support.annotation.DimenRes
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hippo.drawerlayout.DrawerLayout
import com.hippo.ehviewer.R
import com.hippo.ehviewer.mvp.GroupPaper
import com.hippo.ehviewer.mvp.MvpUi
import com.hippo.ehviewer.util.attrColor
import com.hippo.ehviewer.util.color
import com.hippo.ehviewer.util.dimensionPixelSize
import com.hippo.ehviewer.util.find
import com.hippo.ehviewer.widget.Drawer
import com.hippo.viewstate.GenerateViewState
import com.hippo.viewstate.strategy.SingleByMethod
import com.hippo.viewstate.strategy.SingleByTag
import com.hippo.viewstate.strategy.StrategyType

/*
 * Created by Hippo on 2017/7/24.
 */

@GenerateViewState
interface DrawerUi : MvpUi {

  companion object {
    private const val TAG_STATUS_BAR_COLOR = "DrawerUi:status_bar_color"
  }

  @StrategyType(value = SingleByMethod::class)
  fun setLeftDrawerWidth(@DimenRes resId: Int)

  @StrategyType(value = SingleByMethod::class)
  fun setRightDrawerWidth(@DimenRes resId: Int)

  @StrategyType(value = SingleByMethod::class)
  fun setDrawerContentMode(mode: Int)

  @StrategyType(value = SingleByMethod::class)
  fun setLeftDrawerMode(mode: Int)

  @StrategyType(value = SingleByMethod::class)
  fun setRightDrawerMode(mode: Int)

  @StrategyType(value = SingleByMethod::class)
  fun setLeftDrawerShadow(resId: Int)

  @StrategyType(value = SingleByMethod::class)
  fun setRightDrawerShadow(resId: Int)

  fun closeDrawers()

  fun closeLeftDrawer()

  fun closeRightDrawer()

  fun openLeftDrawer()

  fun openRightDrawer()

  fun toggleLeftDrawer()

  fun toggleRightDrawer()

  @StrategyType(value = SingleByTag::class, tag = TAG_STATUS_BAR_COLOR)
  fun setStatusBarColor(@AttrRes id: Int)

  @StrategyType(value = SingleByTag::class, tag = TAG_STATUS_BAR_COLOR)
  fun setStatusBarAttrColor(@AttrRes id: Int)
}


class DrawerPaper(
    private val logic: DrawerLogic
) : GroupPaper<DrawerUi>(logic), DrawerUi {

  companion object {
    const val CONTAINER_ID_CONTENT = R.id.drawer_content
    const val CONTAINER_ID_LEFT = R.id.left_drawer
    const val CONTAINER_ID_RIGHT = R.id.right_drawer
  }

  private lateinit var drawerLayout: DrawerLayout
  private lateinit var drawerContent: Drawer
  private lateinit var leftDrawer: Drawer
  private lateinit var rightDrawer: Drawer

  override fun onCreate(inflater: LayoutInflater, container: ViewGroup) {
    super.onCreate(inflater, container)

    view = inflater.inflate(R.layout.paper_drawer, container, false)
    drawerLayout = view.find(R.id.drawer_layout)
    drawerContent = view.find(R.id.drawer_content)
    leftDrawer = view.find(R.id.left_drawer)
    rightDrawer = view.find(R.id.right_drawer)

    drawerLayout.setDrawerListener(object : DrawerLayout.DrawerListener {
      override fun onDrawerStateChanged(view: View?, state: Int) {}
      override fun onDrawerSlide(view: View?, percent: Float) {}
      override fun onDrawerClosed(view: View?) {
        when (view) {
          leftDrawer -> logic.onCloseLeftDrawer()
          rightDrawer -> logic.onCloseRightDrawer()
        }
      }
      override fun onDrawerOpened(view: View?) {
        when (view) {
          leftDrawer -> logic.onOpenLeftDrawer()
          rightDrawer -> logic.onOpenRightDrawer()
        }
      }
    })
  }

  override fun setLeftDrawerWidth(resId: Int) {
    leftDrawer.layoutParams.width = context.dimensionPixelSize(resId)
    leftDrawer.requestLayout()
  }

  override fun setRightDrawerWidth(resId: Int) {
    rightDrawer.layoutParams.width = context.dimensionPixelSize(resId)
    rightDrawer.requestLayout()
  }

  override fun setDrawerContentMode(mode: Int) {
    drawerContent.mode = mode
  }

  override fun setLeftDrawerMode(mode: Int) {
    leftDrawer.mode = mode
  }

  override fun setRightDrawerMode(mode: Int) {
    rightDrawer.mode = mode
  }

  override fun setLeftDrawerShadow(resId: Int) {
    drawerLayout.setDrawerShadow(resId, Gravity.LEFT)
  }

  override fun setRightDrawerShadow(resId: Int) {
    drawerLayout.setDrawerShadow(resId, Gravity.RIGHT)
  }

  override fun closeDrawers() {
    drawerLayout.closeDrawers()
  }

  override fun closeLeftDrawer() {
    drawerLayout.closeDrawer(Gravity.LEFT)
  }

  override fun closeRightDrawer() {
    drawerLayout.closeDrawer(Gravity.RIGHT)
  }

  override fun openLeftDrawer() {
    drawerLayout.openDrawer(Gravity.LEFT)
  }

  override fun openRightDrawer() {
    drawerLayout.openDrawer(Gravity.RIGHT)
  }

  override fun toggleLeftDrawer() {
    if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
      drawerLayout.closeDrawer(Gravity.LEFT)
    } else {
      drawerLayout.openDrawer(Gravity.LEFT)
    }
  }

  override fun toggleRightDrawer() {
    if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
      drawerLayout.closeDrawer(Gravity.RIGHT)
    } else {
      drawerLayout.openDrawer(Gravity.RIGHT)
    }
  }

  override fun setStatusBarColor(id: Int) {
    drawerLayout.statusBarColor = context.color(id)
  }

  override fun setStatusBarAttrColor(id: Int) {
    drawerLayout.statusBarColor = context.attrColor(id)
  }
}
