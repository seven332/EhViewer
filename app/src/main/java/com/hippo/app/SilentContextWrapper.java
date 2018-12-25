/*
 * Copyright 2018 Hippo Seven
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

package com.hippo.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import com.hippo.util.ExceptionUtils;
import java.lang.reflect.Field;

public class SilentContextWrapper extends ContextWrapper {

  public SilentContextWrapper(Context base) {
    super(base);
  }

  @Override
  public ComponentName startService(Intent service) {
    try {
      return super.startService(service);
    } catch (Throwable t) {
      ExceptionUtils.throwIfFatal(t);
      return null;
    }
  }

  public static void attach(ContextWrapper context) {
    // Get root ContextWrapper
    for (;;) {
      Context baseContext = context.getBaseContext();
      if (baseContext instanceof ContextWrapper) {
        context = (ContextWrapper) baseContext;
      } else {
        break;
      }
    }

    try {
      Field field = ContextWrapper.class.getDeclaredField("mBase");
      field.setAccessible(true);
      field.set(context, new SilentContextWrapper(context.getBaseContext()));
    } catch (Throwable t) {
      ExceptionUtils.throwIfFatal(t);
    }
  }
}
