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

package com.hippo.ehviewer

import android.content.Context
import com.hippo.ehviewer.client.EH_MODE_E
import com.hippo.ehviewer.preference.Preferences

/*
 * Created by Hippo on 2017/7/28.
 */

const val LIST_MODE_DETAIL = 0
const val LIST_MODE_BRIEF = 1

class EhvPreferences(context: Context) : Preferences(context.getSharedPreferences("ehv2", 0)) {

  val listMode = IntPreference("list_mode", LIST_MODE_DETAIL)
  val detailWidth = IntPreference("detail_width", 300)
  val briefWidth = IntPreference("brief_width", 120)

  val ehMode = IntPreference("eh_mode", EH_MODE_E)
}
