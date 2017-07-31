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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hippo.stage.Scene

/*
 * Created by Hippo on 2017/7/13.
 */

abstract class MvpScene : Scene() {

  companion object {
    private const val ERROR_PEN_NULL = "pen == null"
    private const val ERROR_PAPER_NULL = "paper == null"
  }

  private var pen: MvpPen<*>? = null
  private var paper: MvpPaper<*>? = null

  /**
   * Create a pen for this scene.
   */
  protected abstract fun createPen(): MvpPen<*>

  /**
   * Create a paper for the scene.
   */
  protected abstract fun createPaper(): MvpPaper<*>

  override fun onCreate(args: Bundle) {
    super.onCreate(args)
    pen = createPen()
    pen?.create() ?: error(ERROR_PEN_NULL)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    paper = createPaper()
    paper?.create(inflater, container) ?: error(ERROR_PAPER_NULL)
    return paper?.view ?: error(ERROR_PAPER_NULL)
  }

  override fun onAttachView(view: View) {
    super.onAttachView(view)
    paper?.attach() ?: error(ERROR_PAPER_NULL)
  }

  override fun onStart() {
    super.onStart()
    paper?.start() ?: error(ERROR_PAPER_NULL)
  }

  override fun onResume() {
    super.onResume()
    paper?.resume() ?: error(ERROR_PAPER_NULL)
  }

  override fun onPause() {
    super.onPause()
    paper?.pause() ?: error(ERROR_PAPER_NULL)
  }

  override fun onStop() {
    super.onStop()
    paper?.stop() ?: error(ERROR_PAPER_NULL)
  }

  override fun onDetachView(view: View) {
    super.onDetachView(view)
    paper?.detach() ?: error(ERROR_PAPER_NULL)
  }

  override fun onDestroyView(view: View) {
    super.onDestroyView(view)
    paper?.destroy() ?: error(ERROR_PAPER_NULL)
    paper = null
  }

  override fun onDestroy() {
    super.onDestroy()
    pen?.destroy() ?: error(ERROR_PEN_NULL)
    pen = null
  }

  override fun onSaveViewState(view: View, outViewState: Bundle) {
    super.onSaveViewState(view, outViewState)
    paper?.saveState(outViewState) ?: error(ERROR_PAPER_NULL)
  }

  override fun onRestoreViewState(view: View, savedViewState: Bundle) {
    super.onRestoreViewState(view, savedViewState)
    paper?.restoreState(savedViewState) ?: error(ERROR_PAPER_NULL)
  }
}
