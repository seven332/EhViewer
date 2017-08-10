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

package com.hippo.ehviewer.widget.ehv

import android.content.Context
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import com.hippo.ehviewer.R
import com.hippo.ehviewer.drawable.RoundRectDrawable
import com.hippo.ehviewer.util.attrColor
import com.hippo.ehviewer.util.dp2pix

/*
 * Created by Hippo on 2017/8/10.
 */

class EhvTag : AppCompatTextView {

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  init {
    @Suppress("DEPRECATION")
    setBackgroundDrawable(RoundRectDrawable(
        radius = 2.dp2pix(context).toFloat(),
        color = context.attrColor(R.attr.colorAccent)
    ))
  }
}
