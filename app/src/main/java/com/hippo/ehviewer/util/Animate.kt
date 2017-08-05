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

package com.hippo.ehviewer.util

import android.view.animation.Interpolator

/*
 * Created by Hippo on 6/7/2017.
 */

/**
 * User-driving animation.
 */
abstract class Animate {

  companion object {
    const private val ANIMATION_START = -1L
    const private val NO_ANIMATION = -2L

    const val INFINITE = -1L
  }

  /** Animation duration, must be greater than 0 **/
  var duration: Long = 300L
    set(value) { check(value > 0L, { "Duration must be greater than 0" }).let { field = value } }
  /** Animation interpolator **/
  var interpolator: Interpolator? = null
  /** Animation repeat times, must be greater than 0, or INFINITE **/
  var repeat: Long = 1
    set(value) { check(value > 0L || value == INFINITE, { "Repeat must be greater than 0, or INFINITE" }).let { field = value } }

  private var startTime: Long = NO_ANIMATION
  private var runCount: Long = 0L

  /**
   * Starts the animation. If the animation is started, ignore it.
   */
  fun start() {
    if (startTime == NO_ANIMATION) {
      startTime = ANIMATION_START
      runCount = 0L
    }
  }

  /**
   * Cancels the animation. If the animation isn't started, ignore it.
   */
  fun cancel() {
    if (startTime != NO_ANIMATION) {
      startTime = NO_ANIMATION
      onEnd()
    }
  }

  /**
   * Drives the animation. If the animation isn't started, ignore it.
   */
  fun calculate(current: Long): Boolean {
    check(current >= 0L, { "`current` must be greater than or equal 0" })

    if (startTime == NO_ANIMATION) {
      return false
    }

    if (startTime == ANIMATION_START) {
      startTime = current
    } else {
      check(current >= startTime, { "`current` must be greater than or equal `startTime`" })
    }

    val elapse = current - startTime
    runCount += elapse / duration
    val end = repeat != INFINITE && runCount >= repeat
    val factor = if (end) 1.0f else (elapse % duration) / duration.toFloat()

    onCalculate(interpolator?.getInterpolation(factor) ?: factor)

    if (end) {
      onEnd()
      startTime = NO_ANIMATION
    }

    return startTime != NO_ANIMATION
  }

  /** Called when calculated. **/
  protected abstract fun onCalculate(progress: Float)

  /** Called when end **/
  open fun onEnd() {}
}
