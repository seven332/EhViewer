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

package com.hippo.ehviewer.noi

import io.reactivex.Single
import okio.Source
import java.io.InputStream

/*
 * Created by Hippo on 2017/8/1.
 */

@Suppress("UNCHECKED_CAST")
abstract class Request<out This> {

  var solid: Boolean = false
    private set

  var priority: Priority = Priority.MEDIUM
    private set

  protected fun checkSolid() = check(!solid) { "Can't modify a solid Request" }

  /** The priority of this request, [Priority.MEDIUM] in default. **/
  fun priority(priority: Priority): This {
    checkSolid()
    this.priority = priority
    return this as This
  }

  protected fun solidify() {
    checkSolid()
    solid = true
  }

  abstract fun asSource(): Single<Source>

  abstract fun asInputStream(): Single<InputStream>
}
