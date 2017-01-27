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

package com.hippo.ehviewer.app;

/*
 * Created by Hippo on 1/26/2017.
 */

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Records all activities.
 */
public class RecordingApplication extends Application {

  private List<WeakReference<Activity>> list = new LinkedList<>();

  @Override
  public void onCreate() {
    super.onCreate();

    registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
      @Override
      public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        list.add(new java.lang.ref.WeakReference<>(activity));
      }

      @Override
      public void onActivityDestroyed(Activity activity) {
        Iterator<WeakReference<Activity>> iterator = list.iterator();
        while (iterator.hasNext()) {
          WeakReference<Activity> reference = iterator.next();
          Activity a = reference.get();
          // Remove current activity and null
          if (a == null || a == activity) {
            iterator.remove();
          }
        }
      }

      @Override
      public void onActivityStarted(Activity activity) {}

      @Override
      public void onActivityResumed(Activity activity) {}

      @Override
      public void onActivityPaused(Activity activity) {}

      @Override
      public void onActivityStopped(Activity activity) {}

      @Override
      public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    });
  }

  /**
   * Finish all activities.
   */
  public void finish() {
    // Copy list
    ArrayList<Activity> listCopy = new ArrayList<>(list.size());
    for (WeakReference<Activity> reference: list) {
      Activity activity = reference.get();
      if (activity == null) continue;
      listCopy.add(activity);
    }

    // Finish all activities
    for (int i = listCopy.size() - 1; i >= 0; --i) {
      listCopy.get(i).finish();
    }
  }
}
