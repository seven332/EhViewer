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

package com.hippo.ehviewer.widget

import android.animation.Animator
import android.view.View
import com.hippo.ehviewer.util.DURATION_IN
import com.hippo.ehviewer.util.DURATION_OUT

/*
 * Created by Hippo on 2017/7/24.
 */

class ViewTransition(vararg private val views: View) {

  private var shownView: View? = null

  fun show(view: View, animation: Boolean) {
    check(views.contains(view))

    if (view == shownView) {
      return
    }

    if (animation) {
      views.forEach {
        if (it == view) {
          showWithAnimation(it)
        } else {
          hideWithAnimation(it)
        }
      }
    } else {
      views.forEach {
        it.animate().cancel()
        it.visibility = if (it == view) View.VISIBLE else View.GONE
      }
    }
  }

  private fun showWithAnimation(view: View) {
    view.animate().cancel()

    if (view.visibility == View.GONE) {
      view.alpha = 0.0f
      view.visibility = View.VISIBLE
    }

    view.animate().alpha(1.0f).setDuration(DURATION_IN).setListener(null).start()
  }

  private fun hideWithAnimation(view: View) {
    view.animate().cancel()

    if (view.visibility == View.GONE) {
      return
    }

    val listener = object : Animator.AnimatorListener {
      private var cancelled: Boolean = false
      override fun onAnimationRepeat(animation: Animator?) {}
      override fun onAnimationEnd(animation: Animator?) {
        if (!cancelled) {
          view.visibility = View.GONE
        }
      }
      override fun onAnimationCancel(animation: Animator?) {
        cancelled = true
      }
      override fun onAnimationStart(animation: Animator?) {}
    }

    view.animate().alpha(0.0f).setDuration(DURATION_OUT).setListener(listener).start()
  }
}
