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

import android.support.v7.widget.RecyclerView

/*
 * Created by Hippo on 6/8/2017.
 */

abstract class ContentDataAdapter<T : Any, VH: RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

  lateinit var data: ContentData<T>

  val size get() = data.size()

  override fun getItemCount(): Int = data.size()

  operator fun get(index: Int): T = data.get(index)
}
