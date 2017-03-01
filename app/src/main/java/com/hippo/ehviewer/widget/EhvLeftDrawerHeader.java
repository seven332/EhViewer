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

package com.hippo.ehviewer.widget;

/*
 * Created by Hippo on 2/14/2017.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvBusTags;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.R;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;

/**
 * {@code EhvLeftDrawerHeader} shows username, avatar and sad panda background.
 */
public class EhvLeftDrawerHeader extends FrameLayout {

  private TextView name;
  private SimpleDraweeView avatar;

  private boolean hasSetDefaultAvatar;
  private boolean hasSetFailureAvatar;

  public EhvLeftDrawerHeader(Context context) {
    super(context);
    init(context);
  }

  public EhvLeftDrawerHeader(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public EhvLeftDrawerHeader(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    LayoutInflater.from(context).inflate(R.layout.widget_ehv_left_drawer_header, this);
    name = (TextView) findViewById(R.id.name);
    avatar = (SimpleDraweeView) findViewById(R.id.avatar);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    // Update profile
    EhvPreferences p = ((EhvApp) getContext().getApplicationContext()).getPreferences();
    setProfile(p.getDisplayName(), p.getAvatar());

    RxBus.get().register(this);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    RxBus.get().unregister(this);
  }

  private void showDefaultAvatar() {
    if (!hasSetDefaultAvatar) {
      hasSetDefaultAvatar = true;
      Drawable defaultAvatar =
          AppCompatResources.getDrawable(getContext(), R.drawable.ehv_default_avatar);
      avatar.getHierarchy().setPlaceholderImage(defaultAvatar);
    }
    avatar.setController(null);
  }

  private void showAvatar(String url) {
    setFailureAvatar();
    avatar.setImageURI(url);
  }

  private void setFailureAvatar() {
    if (!hasSetFailureAvatar) {
      hasSetFailureAvatar = true;
      Drawable defaultAvatar =
          AppCompatResources.getDrawable(getContext(), R.drawable.ehv_default_avatar);
      avatar.getHierarchy().setFailureImage(defaultAvatar);
    }
  }

  private void setProfile(String name, String image) {
    if (name == null) {
      this.name.setText(R.string.ehv_default_name);
    } else {
      this.name.setText(name);
    }

    if (image == null) {
      showDefaultAvatar();
    } else {
      showAvatar(image);
    }
  }

  @Keep
  @Subscribe(
      thread = EventThread.MAIN_THREAD,
      tags = {
          @Tag(EhvBusTags.TAG_SIGN_IN),
      }
  )
  public void onSignIn(Pair<String, String> profile) {
    if (profile != null) {
      setProfile(profile.first, profile.second);
    } else {
      setProfile(null, null);
    }
  }

  @Keep
  @Subscribe(
      thread = EventThread.MAIN_THREAD,
      tags = {
          @Tag(EhvBusTags.TAG_SIGN_OUT),
      }
  )
  public void onSignOut(Void v) {
    setProfile(null, null);
  }
}
