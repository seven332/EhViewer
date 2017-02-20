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

package com.hippo.ehviewer.changehandler;

/*
 * Created by Hippo on 2/20/2017.
 */

import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler;

/**
 * {@code DialogChangeHandler} is for {@link com.hippo.ehviewer.controller.base.DialogController}.
 * It keeps from view on push and show no animations or transitions.
 */
public class DialogChangeHandler extends SimpleSwapChangeHandler {

  public DialogChangeHandler() {
    super(false);
  }
}
