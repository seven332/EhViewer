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

import com.hippo.ehviewer.mvp.MvpUi
import com.hippo.viewstate.GenerateViewState
import com.hippo.viewstate.strategy.ClearByTag
import com.hippo.viewstate.strategy.SingleByTag
import com.hippo.viewstate.strategy.StrategyType

/*
 * Created by Hippo on 2017/7/14.
 */

@GenerateViewState
interface ContentUi : MvpUi {

  companion object {
    private const val TAG_DISPLAY = "ContentUi:display"
    private const val TAG_REFRESHING = "ContentUi:refreshing"
  }

  @StrategyType(value = SingleByTag::class, tag = TAG_DISPLAY)
  fun showContent()

  @StrategyType(value = SingleByTag::class, tag = TAG_DISPLAY)
  fun showTip(t: Throwable)

  @StrategyType(value = SingleByTag::class, tag = TAG_DISPLAY)
  fun showProgressBar()

  @StrategyType(value = SingleByTag::class, tag = TAG_DISPLAY)
  fun showMessage(t: Throwable)

  @StrategyType(value = ClearByTag::class, tag = TAG_REFRESHING)
  fun stopRefreshing()

  @StrategyType(value = SingleByTag::class, tag = TAG_REFRESHING)
  fun setHeaderRefreshing()

  @StrategyType(value = SingleByTag::class, tag = TAG_REFRESHING)
  fun setFooterRefreshing()

  fun scrollToPosition(position: Int)

  fun scrollUpALittle()

  fun scrollDownALittle()

  fun notifyDataSetChanged()

  fun notifyItemRangeInserted(positionStart: Int, itemCount: Int)

  fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int)

  fun notifyItemRangeChanged(positionStart: Int, itemCount: Int)
}
