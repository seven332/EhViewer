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
 * Created by Hippo on 2/1/2017.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.transition.Fade;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.hippo.easyrecyclerview.EasyAdapter;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.ehviewer.R;
import com.hippo.refreshlayout.RefreshLayout;
import com.jakewharton.rxbinding.view.RxView;
import java.util.concurrent.TimeUnit;

/**
 * A view to show data list, progress bar and empty state.
 * All data are stored in {@link ContentData}.
 */
public class ContentLayout extends FrameLayout implements ContentView {

  private RefreshLayout refreshLayout;
  private EasyRecyclerView recyclerView;
  private TextView tip;
  private ProgressBar progressBar;

  private RecyclerView.Adapter adapter;

  @Nullable
  private ContentData data;

  public ContentLayout(Context context) {
    super(context);
    init(context);
  }

  public ContentLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public ContentLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    LayoutInflater.from(context).inflate(R.layout.widget_content_layout, this);

    refreshLayout = (RefreshLayout) findViewById(R.id.refresh_layout);
    recyclerView = (EasyRecyclerView) refreshLayout.findViewById(R.id.recycler_view);
    tip = (TextView) findViewById(R.id.tip);
    progressBar = (ProgressBar) findViewById(R.id.progress_bar);

    refreshLayout.setHeaderColorSchemeResources(
        R.color.color_scheme_1,
        R.color.color_scheme_2,
        R.color.color_scheme_3,
        R.color.color_scheme_4,
        R.color.color_scheme_5,
        R.color.color_scheme_6
    );
    refreshLayout.setFooterColorSchemeResources(
        R.color.color_scheme_1,
        R.color.color_scheme_2,
        R.color.color_scheme_3,
        R.color.color_scheme_4,
        R.color.color_scheme_5,
        R.color.color_scheme_6
    );

    refreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
      @Override
      public void onHeaderRefresh() {
        if (data != null) {
          data.onRefreshHeader();
        }
      }

      @Override
      public void onFooterRefresh() {
        if (data != null) {
          data.onRefreshFooter();
        }
      }
    });
    RxView.clicks(tip)
        .throttleFirst(1, TimeUnit.SECONDS)
        .subscribe(a -> {
          if (data != null) {
            data.onClickTip();
          }
        });
  }

  /**
   * Sets {@code ContentData}.
   * Always call {@code setContentData(null)} when you
   * don't need this {@code ContentLayout} anymore
   * to avoid memory leak.
   */
  public void setContentData(ContentData data) {
    // Remove ContentLayout from old data
    if (this.data != null) {
      this.data.setContentView(null);
    }
    this.data = data;
    if (data != null) {
      data.setContentView(this);
    }
  }

  /**
   * Go to specialized page. It will discard all loaded data.
   */
  public void goTo(int page, boolean animation) {
    if (data != null) {
      showProgress(animation);
      data.goTo(page);
    }
  }

  /**
   * Switch to specialized page. It's different from goTo().
   * switchTo() will only scrollToPosition() if
   * the page is in range.
   */
  public void switchTo(int page, boolean animation) {
    if (data != null) {
      showProgress(animation);
      data.switchTo(page);
    }
  }

  /**
   * Set Adapter to the RecyclerView.
   */
  public void setAdapter(EasyAdapter adapter) {
    recyclerView.setAdapter(adapter);
    this.adapter = adapter;
  }

  /**
   * Set LayoutManager to the RecyclerView.
   */
  public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
    recyclerView.setLayoutManager(layoutManager);
  }

  private void prepareTransition() {
    Transition transition = new Fade(Fade.IN | Fade.OUT)
        .addTarget(refreshLayout)
        .addTarget(tip)
        .addTarget(progressBar);
    TransitionManager.beginDelayedTransition(this, transition);
  }

  void showContent(boolean animation) {
    if (animation) prepareTransition();
    refreshLayout.setVisibility(View.VISIBLE);
    tip.setVisibility(View.GONE);
    progressBar.setVisibility(View.GONE);
  }

  @Override
  public void showContent() {
    showContent(true);
  }

  void showTip(boolean animation) {
    if (animation) prepareTransition();
    refreshLayout.setVisibility(View.GONE);
    tip.setVisibility(View.VISIBLE);
    progressBar.setVisibility(View.GONE);
  }

  @Override
  public void showTip(TipInfo info) {
    // Set icon
    Drawable drawable = null;
    if (info != null && info.icon != 0) {
      drawable = AppCompatResources.getDrawable(getContext(), info.icon);
      if (drawable != null) {
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
      }
    }
    tip.setCompoundDrawables(null, drawable, null, null);
    // Set text
    String text = null;
    if (info != null) {
      text = info.text;
    }
    tip.setText(text);
    // Show tip
    showTip(true);
  }

  void showProgressBar(boolean animation) {
    if (animation) prepareTransition();
    refreshLayout.setVisibility(View.GONE);
    tip.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);
  }

  @Override
  public void showProgressBar() {
    showProgressBar(true);
  }

  void showProgress(boolean animation) {
    if (data == null || data.size() == 0) {
      // Show progress bar
      showProgressBar(animation);
    } else {
      // Refresh header
      showContent(animation);
      refreshLayout.setFooterRefreshing(false);
      refreshLayout.setHeaderRefreshing(true);
    }
  }

  @Override
  public void stopRefreshing() {
    refreshLayout.setHeaderRefreshing(false);
    refreshLayout.setFooterRefreshing(false);
  }

  @Override
  public void setHeaderRefreshing() {
    refreshLayout.setHeaderRefreshing(true);
  }

  @Override
  public void setFooterRefreshing() {
    refreshLayout.setFooterRefreshing(true);
  }

  @Override
  public void scrollToPosition(int position) {
    recyclerView.scrollToPosition(position);
  }

  @Override
  public void notifyItemRangeInserted(int positionStart, int itemCount) {
    adapter.notifyItemRangeInserted(positionStart, itemCount);
  }

  @Override
  public void notifyItemRangeRemoved(int positionStart, int itemCount) {
    adapter.notifyItemRangeRemoved(positionStart, itemCount);
  }

  /**
   * Stores tip icon and tip text.
   */
  public static class TipInfo {
    public int icon;
    public String text;
  }
}
