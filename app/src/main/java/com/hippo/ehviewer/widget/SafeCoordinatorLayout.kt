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

/*
 * Created by Hippo on 2/7/2017.
 */

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.MotionEvent
import io.reactivex.exceptions.Exceptions

/**
 * `SafeCoordinatorLayout` ignores exceptions in
 * [onInterceptTouchEvent] and [onTouchEvent].
 */
class SafeCoordinatorLayout : CoordinatorLayout {

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    try {
      return super.onInterceptTouchEvent(ev)
    } catch (e: Throwable) {
      Exceptions.throwIfFatal(e)
      return false
    }
  }

  override fun onTouchEvent(ev: MotionEvent): Boolean {
    try {
      return super.onTouchEvent(ev)
    } catch (e: Throwable) {
      Exceptions.throwIfFatal(e)
      return false
    }
  }
}
