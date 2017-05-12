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

package com.hippo.ehviewer.activity;

/*
 * Created by Hippo on 5/12/2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.stage.Director;
import com.hippo.stage.Scene;
import com.hippo.stage.Stage;

public abstract class StageActivity extends AppCompatActivity {

  private static final int STAGE_ID = 0;

  private Director director;
  private Stage stage;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    onSetContentView();

    director = Director.hire(this, savedInstanceState);
    boolean needRoot = !director.contains(STAGE_ID);
    stage = director.direct(onGetStageLayout(), STAGE_ID);
    if (needRoot) {
      stage.pushScene(onCreateRootScene());
    }
  }

  /**
   * Sets content view for this {@code Activity}.
   * <p>
   * Please call {@link #setContentView(int)}, {@link #setContentView(View)} or
   * {@link #setContentView(View, ViewGroup.LayoutParams)} here.
   * <p>
   * Called in {@link #onCreate(Bundle)}.
   */
  protected abstract void onSetContentView();

  /**
   * Returns a container for scenes.
   */
  @NonNull
  protected abstract ViewGroup onGetStageLayout();

  /**
   * Returns root scene for this {@code StageActivity}.
   */
  @NonNull
  protected abstract Scene onCreateRootScene();

  @Override
  protected void onDestroy() {
    super.onDestroy();
    director = null;
    stage = null;
  }

  @Override
  public void onBackPressed() {
    if (director == null || !director.handleBack()) {
      super.onBackPressed();
    }
  }

  /**
   * Pushes a scene to the stage.
   * It's a no-op if the activity is destroyed.
   */
  protected void pushScene(@NonNull Scene scene) {
    if (stage != null) {
      stage.pushScene(scene);
    }
  }

  /**
   * Replace the top scene.
   * It's a no-op if the activity is destroyed.
   */
  protected void replaceTopScene(@NonNull Scene scene) {
    if (stage != null) {
      stage.replaceTopScene(scene);
    }
  }

  /**
   * Returns the top scene.
   * Always returns {@code null} if the activity is destroyed.
   */
  @Nullable
  protected Scene getTopScene() {
    if (stage != null) {
      return stage.getTopScene();
    } else {
      return null;
    }
  }
}
