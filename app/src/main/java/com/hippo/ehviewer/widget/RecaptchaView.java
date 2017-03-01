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
 * Created by Hippo on 2/12/2017.
 */

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.hippo.ehviewer.R;

/**
 * {@code RecaptchaView} shows recaptcha and tip text.
 */
public class RecaptchaView extends FrameLayout implements View.OnClickListener {

  private SimpleDraweeView image;
  private TextView text;

  private boolean loadingImage;

  private OnRequestListener listener;

  public RecaptchaView(Context context) {
    super(context);
    init(context);
  }

  public RecaptchaView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public RecaptchaView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    LayoutInflater.from(context).inflate(R.layout.widget_recaptcha_view, this);
    image = (SimpleDraweeView) findViewById(R.id.recaptcha_image);
    text = (TextView) findViewById(R.id.recaptcha_text);
    setOnClickListener(this);
  }

  public void none() {
    loadingImage = false;
    text.setText(R.string.recaptcha_click);
    text.setVisibility(VISIBLE);
    image.setController(null);
    image.setVisibility(GONE);
  }

  public void start() {
    loadingImage = false;
    text.setText(R.string.recaptcha_loading);
    text.setVisibility(VISIBLE);
    image.setController(null);
    image.setVisibility(GONE);
  }

  public void success(String url) {
    loadingImage = true;
    text.setText(R.string.recaptcha_loading);
    text.setVisibility(VISIBLE);
    DraweeController controller = Fresco.newDraweeControllerBuilder()
        .setCallerContext(null)
        .setUri(Uri.parse(url))
        .setOldController(image.getController())
        .setControllerListener(new BaseControllerListener<ImageInfo>() {
          @Override
          public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
            loadingImage = false;
            text.setVisibility(GONE);
            image.setVisibility(VISIBLE);
          }
          @Override
          public void onFailure(String id, Throwable throwable) {
            failure();
          }
        })
        .build();
    image.setController(controller);
    // Only visible DraweeView loads image
    image.setVisibility(VISIBLE);
  }

  public void failure() {
    loadingImage = false;
    text.setText(R.string.recaptcha_failure);
    text.setVisibility(VISIBLE);
    image.setController(null);
    image.setVisibility(GONE);
  }

  @Override
  public void onClick(View v) {
    if (!loadingImage && listener != null) {
      listener.onRequest();
    }
  }

  public void setOnRequestListener(OnRequestListener listener) {
    this.listener = listener;
  }

  public interface OnRequestListener {
    void onRequest();
  }
}
