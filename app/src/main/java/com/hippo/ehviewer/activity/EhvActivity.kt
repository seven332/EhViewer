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

package com.hippo.ehviewer.activity

import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.view.ViewGroup
import com.hippo.ehviewer.R
import com.hippo.ehviewer.scene.MainScene
import com.hippo.ehviewer.util.find
import com.hippo.stage.Scene

/*
 * Created by Hippo on 2017/7/23.
 */

class EhvActivity : StageActivity() {

  private lateinit var coordinatorLayout: CoordinatorLayout
  private lateinit var stageLayout: ViewGroup

  override fun onSetContentView() {
    setContentView(R.layout.activity_ehv)
    coordinatorLayout = find(R.id.coordinator_layout)
    stageLayout = find(R.id.stage_layout)
  }

  override fun onGetStageLayout(): ViewGroup = stageLayout

  override fun onCreateRootScene(): Scene = MainScene()

  fun snack(resId: Int) {
    Snackbar.make(coordinatorLayout, resId, Snackbar.LENGTH_SHORT).show()
  }

  fun snack(message: CharSequence?) {
    if (message != null && message.isNotBlank()) {
      Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show()
    }
  }
}
