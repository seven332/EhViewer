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

package com.hippo.ehviewer.scene.gallerydetail;

/*
 * Created by Hippo on 5/14/2017.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RatingBar;
import android.widget.TextView;
import com.google.android.flexbox.FlexboxLayout;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.CommentEntry;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.PreviewPage;
import com.hippo.ehviewer.client.data.TagSet;
import com.hippo.ehviewer.util.ExceptionExplainer;
import com.hippo.ehviewer.util.ReadableTime;
import com.hippo.ehviewer.view.StatusBarView;
import com.hippo.ehviewer.widget.CoverView;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.NumberUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GalleryDetailView
    extends StatusBarView<GalleryDetailContract.Presenter, GalleryDetailScene>
    implements GalleryDetailContract.View {

  private GalleryInfo info;

  private View progress;
  private View body;
  private TextView error;

  void setGalleryInfo(GalleryInfo info) {
    this.info = info;
  }

  @NonNull
  @Override
  protected View onCreateStatusBarContent(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.view_gallery_detail, parent, false);

    bindToolbarToScrollView(view);
    adjustHeaderBackgroundHeight(view);

    bindHeader(view, info);

    progress = view.findViewById(R.id.progress);
    body = view.findViewById(R.id.body);
    error = (TextView) view.findViewById(R.id.error);

    // Click error view to refresh
    error.setOnClickListener(v -> getPresenter().getGalleryDetail(info));

    return view;
  }

  @Override
  protected int getThemeResId() {
    return EhUtils.getCategoryTheme(info.category, false);
  }

  // Adjust toolbar alpha according to scroll view
  private void bindToolbarToScrollView(View view) {
    NestedScrollView scrollView = (NestedScrollView) view.findViewById(R.id.scroll_view);
    Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
    scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
      @Override
      public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX,
          int oldScrollY) {
        int height = toolbar.getHeight();
        float alpha = height > 0 ? MathUtils.clamp(scrollY / height, 0.0f, 1.0f) : 1.0f;
        toolbar.setAlpha(alpha);
      }
    });
    toolbar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
          int oldTop, int oldRight, int oldBottom) {
        toolbar.removeOnLayoutChangeListener(this);
        int height = toolbar.getHeight();
        float alpha = height > 0
            ? MathUtils.clamp(scrollView.getScrollY() / height, 0.0f, 1.0f)
            : 1.0f;
        toolbar.setAlpha(alpha);
      }
    });
  }

  private void adjustHeaderBackgroundHeight(View view) {
    View headerBackground = view.findViewById(R.id.header_background);
    View headerButtons = view.findViewById(R.id.header_buttons);
    headerBackground.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @SuppressWarnings("deprecation")
          @Override
          public void onGlobalLayout() {
            headerBackground.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            ViewGroup.LayoutParams lp = headerBackground.getLayoutParams();
            lp.height = headerButtons.getTop() + headerButtons.getHeight() / 2;
            headerBackground.requestLayout();
          }
        });
  }

  private void bindHeader(View view, GalleryInfo info) {
    CoverView cover = (CoverView) view.findViewById(R.id.cover);
    TextView title = (TextView) view.findViewById(R.id.title);
    TextView uploader = (TextView) view.findViewById(R.id.uploader);
    TextView category = (TextView) view.findViewById(R.id.category);
    cover.load(info);
    title.setText(info.title);
    uploader.setText(info.uploader);
    category.setText(EhUtils.getCategoryNotNull(info.category));
    category.setTextColor(EhUtils.getCategoryColor(info.category));
  }

  private void bindBody(View view, GalleryInfo info) {
    Context context = getContext();

    // Info
    TextView language = (TextView) view.findViewById(R.id.language);
    TextView pages = (TextView) view.findViewById(R.id.pages);
    TextView size = (TextView) view.findViewById(R.id.size);
    TextView date = (TextView) view.findViewById(R.id.date);

    language.setText(EhUtils.getLangNotNull(context, info.language));
    pages.setText(context.getString(R.string.gallery_detail_pages, info.pages));
    size.setText(context.getString(R.string.gallery_detail_size, NumberUtils.binaryPrefix(info.size)));
    date.setText(ReadableTime.getTimeAgo(context, info.date));

    // Actions
    TextView favourite = (TextView) view.findViewById(R.id.favourite);
    Drawable favouriteDrawable = AppCompatResources.getDrawable(context, R.drawable.v_heart_primary_x48);
    //noinspection ConstantConditions
    favouriteDrawable.setBounds(0, 0, favouriteDrawable.getIntrinsicWidth(), favouriteDrawable.getIntrinsicHeight());
    favourite.setCompoundDrawables(null, favouriteDrawable, null, null);

    TextView rate = (TextView) view.findViewById(R.id.rate);
    Drawable rateDrawable = AppCompatResources.getDrawable(context, R.drawable.v_thumb_up_primary_x48);
    //noinspection ConstantConditions
    rateDrawable.setBounds(0, 0, rateDrawable.getIntrinsicWidth(), rateDrawable.getIntrinsicHeight());
    rate.setCompoundDrawables(null, rateDrawable, null, null);

    // Rating
    RatingBar rating = (RatingBar) view.findViewById(R.id.rating);
    TextView ratingText = (TextView) view.findViewById(R.id.rating_text);
    rating.setRating(info.rating);
    ratingText.setText("" + info.rating);


    ViewGroup tags = (ViewGroup) view.findViewById(R.id.tags);
    bindTags(tags, info.tagSet);
  }

  private void bindTags(ViewGroup view, TagSet tagSet) {
    // Keep the no tag text
    if (tagSet.isEmpty()) {
      return;
    }

    // Hide no tags text
    view.findViewById(R.id.no_tags).setVisibility(View.GONE);

    Context context = getContext();
    LayoutInflater inflater = LayoutInflater.from(context);
    for (Map.Entry<String, Set<String>> entry : tagSet) {
      FlexboxLayout group = new FlexboxLayout(context);
      group.setFlexWrap(FlexboxLayout.FLEX_WRAP_WRAP);
      view.addView(group);

      inflateTag(inflater, R.layout.gallery_detail_tags_namespace, group, entry.getKey());
      for (String tag : entry.getValue()) {
        inflateTag(inflater, R.layout.gallery_detail_tags_tag, group, tag);
      }
    }
  }

  private void inflateTag(LayoutInflater inflater, int layoutResId, ViewGroup parent, String text) {
    inflater.inflate(layoutResId, parent);
    TextView textView = (TextView) parent.getChildAt(parent.getChildCount() - 1);
    textView.setText(text);
  }

  private void bindError(TextView error, Throwable e) {
    error.setText(ExceptionExplainer.explain(getContext(), e));
    Drawable drawable = ExceptionExplainer.explainVividly(getContext(), e);
    if (drawable != null) {
      drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getMinimumHeight());
    }
    error.setCompoundDrawables(null, drawable, null, null);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Get GalleryDetail
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public void onGetGalleryDetailNone() {
    // Get GalleryDetail at once
    getPresenter().getGalleryDetail(info);
  }

  @Override
  public void onGetGalleryDetailStart() {
    // Show progress
    progress.setVisibility(View.VISIBLE);
    body.setVisibility(View.GONE);
    error.setVisibility(View.GONE);
  }

  @Override
  public void onGetGalleryDetailSuccess(GalleryInfo info, List<CommentEntry> comments,
      List<PreviewPage> previews) {
    // Store this GalleryInfo
    this.info = info;

    // Show body
    progress.setVisibility(View.GONE);
    body.setVisibility(View.VISIBLE);
    error.setVisibility(View.GONE);

    // Bind body
    bindBody(body, info);
  }

  @Override
  public void onGetGalleryDetailFailure(Throwable e) {
    // Show error
    progress.setVisibility(View.GONE);
    body.setVisibility(View.GONE);
    error.setVisibility(View.VISIBLE);

    // Bind error
    bindError(error, e);
  }
}
