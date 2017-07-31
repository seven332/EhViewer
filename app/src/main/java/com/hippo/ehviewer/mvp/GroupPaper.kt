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

package com.hippo.ehviewer.mvp

import android.os.Bundle
import android.view.ViewGroup

/*
 * Created by Hippo on 6/19/2017.
 */

abstract class GroupPaper<This : Any>(
    logic: MvpLogic<This>
) : EhvPaper<This>(logic) {

  private val children = mutableListOf<MvpPaper<*>>()

  fun addChild(paper: MvpPaper<*>) {
    children.add(paper)
  }

  /**
   * Inflates and adds child.
   */
  fun <T : MvpPaper<*>> inflateChild(
      containerId: Int,
      creator: (ViewGroup) -> T
  ): T {
    val container = view.findViewById(containerId) as ViewGroup
    val child = creator(container)
    container.addView(child.view, 0)
    children.add(child)
    return child
  }

  // Don't call children's create() in onCreate(), let them call it by themselves

  override fun onAttach() {
    super.onAttach()
    children.forEach { it.attach() }
  }

  override fun onStart() {
    super.onStart()
    children.forEach { it.start() }
  }

  override fun onResume() {
    super.onResume()
    children.forEach { it.resume() }
  }

  override fun onPause() {
    super.onPause()
    children.forEach { it.pause() }
  }

  override fun onStop() {
    super.onStop()
    children.forEach { it.stop() }
  }

  override fun onDetach() {
    super.onDetach()
    children.forEach { it.detach() }
  }

  override fun onDestroy() {
    super.onDestroy()
    children.forEach { it.destroy() }
  }

  override fun onSaveState(outState: Bundle) {
    super.onSaveState(outState)
    children.forEach { it.saveState(outState) }
  }

  override fun onRestoreState(savedState: Bundle) {
    super.onRestoreState(savedState)
    children.forEach { it.restoreState(savedState) }
  }
}
