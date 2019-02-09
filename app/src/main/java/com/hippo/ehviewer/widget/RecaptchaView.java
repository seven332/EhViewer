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
 * Created by Hippo on 2017/8/20.
 */

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import com.hippo.android.recaptcha.RecaptchaV1;
import com.hippo.drawable.TextDrawable;
import com.hippo.ehviewer.R;
import com.hippo.widget.LoadImageView;
import com.hippo.yorozuya.SimpleHandler;

public class RecaptchaView extends LoadImageView implements RecaptchaV1.RecaptchaCallback, View.OnClickListener {

  private static final String CHALLENGE = "6LdtfgYAAAAAALjIPPiCgPJJah8MhAUpnHcKF8u_";

  private boolean loading = false;
  private String challenge;
  private String image;

  private TextDrawable waitingDrawable;
  private TextDrawable loadingDrawable;
  private TextDrawable failureDrawable;

  public RecaptchaView(Context context) {
    super(context);
    init();
  }

  public RecaptchaView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    setOnClickListener(this);
    if (waitingDrawable == null) {
      waitingDrawable = new TextDrawable(getContext().getString(R.string.recaptcha_none), 0.6f);
      waitingDrawable.setBackgroundColor(Color.GRAY);
      waitingDrawable.setTextColor(Color.WHITE);
    }
    load(waitingDrawable);
  }

  public String getChallenge() {
    return challenge;
  }

  public void load() {
    if (loading) {
      return;
    }
    loading = false;
    challenge = null;
    image = null;

    if (loadingDrawable == null) {
      loadingDrawable = new TextDrawable(getContext().getString(R.string.recaptcha_loading), 0.6f);
      loadingDrawable.setBackgroundColor(Color.GRAY);
      loadingDrawable.setTextColor(Color.WHITE);
    }
    load(loadingDrawable);

    RecaptchaV1.recaptcha(getContext(), CHALLENGE, SimpleHandler.getInstance(), this);
  }

  @Override
  public void onClick(@NonNull View v) {
    load();
  }

  @Override
  public void onSuccess(@NonNull String challenge, @NonNull String image) {
    this.loading = false;
    this.challenge = challenge;
    this.image = image;

    load(image, image);
  }

  @Override
  public void onFailure() {
    this.loading = false;
    this.challenge = null;
    this.image = null;

    if (failureDrawable == null) {
      failureDrawable = new TextDrawable(getContext().getString(R.string.recaptcha_failure), 0.6f);
      failureDrawable.setBackgroundColor(Color.GRAY);
      failureDrawable.setTextColor(Color.WHITE);
    }
    load(failureDrawable);
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    SavedState ss = new SavedState(super.onSaveInstanceState());
    ss.loading = loading;
    ss.challenge = challenge;
    ss.image = image;
    return ss;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    if (!(state instanceof SavedState)) {
      super.onRestoreInstanceState(state);
      return;
    }

    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());

    if (ss.loading) {
      load();
    } else if (!TextUtils.isEmpty(ss.challenge) && !TextUtils.isEmpty(ss.image)) {
      onSuccess(ss.challenge, ss.image);
    }
  }

  private static class SavedState extends BaseSavedState {

    private boolean loading;
    private String challenge;
    private String image;

    public SavedState(Parcelable superState) {
      super(superState);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeByte(loading ? (byte) 1 : (byte) 0);
      out.writeString(challenge);
      out.writeString(image);
    }

    public static final Parcelable.Creator<SavedState> CREATOR
        = new Parcelable.Creator<SavedState>() {
      @Override
      public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      @Override
      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };

    public SavedState(Parcel source) {
      super(source);
      loading = source.readByte() != 0;
      challenge = source.readString();
      image = source.readString();
    }
  }
}
