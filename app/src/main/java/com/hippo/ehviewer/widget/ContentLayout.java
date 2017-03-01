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
import com.hippo.yorozuya.android.LayoutUtils;
import com.jakewharton.rxbinding.view.RxView;
import com.transitionseverywhere.Fade;
import com.transitionseverywhere.Transition;
import com.transitionseverywhere.TransitionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A view to show data list, progress bar and empty state.
 * All data are stored in {@link ContentData}.
 */
public class ContentLayout extends FrameLayout implements ContentContract.View {

  private RefreshLayout refreshLayout;
  private EasyRecyclerView recyclerView;
  private TextView tip;
  private ProgressBar progressBar;

  private RecyclerView.Adapter adapter;
  private List<RecyclerView.ItemDecoration> itemDecorations = new ArrayList<>();

  private int aLittleDistance;

  @Nullable
  private ContentContract.Presenter presenter;
  private Extension extension;

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
        if (presenter != null) {
          presenter.onRefreshHeader();
        }
      }

      @Override
      public void onFooterRefresh() {
        if (presenter != null) {
          presenter.onRefreshFooter();
        }
      }
    });

    recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        if (!refreshLayout.isRefreshing() && refreshLayout.isAlmostBottom()
            && presenter != null && !presenter.isMaxReached()) {
          refreshLayout.setFooterRefreshing(true);
          presenter.onRefreshFooter();
        }
      }
    });

    RxView.clicks(tip)
        .throttleFirst(1, TimeUnit.SECONDS)
        .subscribe(a -> {
          if (presenter != null) {
            presenter.onClickTip();
          }
        });

    aLittleDistance = LayoutUtils.dp2pix(context, 48);
  }

  public void setPresenter(@Nullable ContentContract.Presenter presenter) {
    this.presenter = presenter;
  }

  public void setExtension(Extension extension) {
    this.extension = extension;
  }

  /**
   * Go to specialized page. It will discard all loaded data.
   */
  public void goTo(int page, boolean animation) {
    if (presenter != null) {
      showProgress(animation);
      presenter.goTo(page);
    }
  }

  /**
   * Switch to specialized page. It's different from goTo().
   * switchTo() will only scrollToPosition() if
   * the page is in range.
   */
  public void switchTo(int page, boolean animation) {
    if (presenter != null) {
      showProgress(animation);
      presenter.switchTo(page);
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
   * Gets Adapter to the RecyclerView.
   */
  public RecyclerView.Adapter getAdapter() {
    return adapter;
  }

  /**
   * Set LayoutManager to the RecyclerView.
   */
  public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
    recyclerView.setLayoutManager(layoutManager);
  }

  /**
   * Add an {@link RecyclerView.ItemDecoration} to the RecyclerView.
   */
  public void addItemDecoration(RecyclerView.ItemDecoration decor) {
    recyclerView.addItemDecoration(decor);
    itemDecorations.add(decor);
  }

  /**
   * Remove all {@link RecyclerView.ItemDecoration} from the RecyclerView.
   */
  public void removeAllItemDecorations() {
    for (RecyclerView.ItemDecoration decor: itemDecorations) {
      recyclerView.removeItemDecoration(decor);
    }
    itemDecorations.clear();
  }

  public void setRecyclerViewPadding(int left, int top, int right, int bottom) {
    recyclerView.setPadding(left, top, right, bottom);
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

  public void showTip(TipInfo info) {
    // Set icon
    Drawable drawable = null;
    if (info != null) {
      drawable = info.icon;
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

  @Override
  public void showTip(Throwable t) {
    if (extension != null) {
      TipInfo info = extension.getTipFromThrowable(t);
      if (info != null) {
        showTip(info);
      }
    }
  }

  @Override
  public void showMessage(Throwable t) {
    if (extension != null) {
      TipInfo info = extension.getTipFromThrowable(t);
      if (info != null && info.text != null) {
        extension.showMessage(info.text);
      }
    }
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
    if (presenter == null || presenter.size() == 0) {
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
  public void scrollDownALittle() {
    // Only scroll down if catch bottom
    if (refreshLayout.isAlmostBottom()) {
      recyclerView.smoothScrollBy(0, aLittleDistance);
    }
  }

  @Override
  public void notifyDataSetChanged() {
    if (adapter != null) {
      adapter.notifyDataSetChanged();
    }
  }

  @Override
  public void notifyItemRangeInserted(int positionStart, int itemCount) {
    if (adapter != null) {
      adapter.notifyItemRangeInserted(positionStart, itemCount);
    }
  }

  @Override
  public void notifyItemRangeRemoved(int positionStart, int itemCount) {
    if (adapter != null) {
      adapter.notifyItemRangeRemoved(positionStart, itemCount);
    }
  }

  @Override
  public void notifyItemRangeChanged(int positionStart, int itemCount) {
    if (adapter != null) {
      adapter.notifyItemRangeChanged(positionStart, itemCount);
    }
  }

  /**
   * Stores tip icon and tip text.
   */
  public static class TipInfo {
    public Drawable icon;
    public String text;
  }

  /**
   * {@code ContentLayout} can't do all UI jobs. It needs a {@code Extension} to give a hand.
   */
  public interface Extension {

    /**
     * Gets tip to represent the {@code Throwable}.
     * <p>
     * {@link ContentData#NOT_FOUND_EXCEPTION} for no data.
     * <p>
     * {@link ContentData#TAP_TO_LOAD_EXCEPTION} for no data but can continue loading.
     */
    TipInfo getTipFromThrowable(Throwable e);

    /**
     * Show a non-interrupting message. Toast? SnackBar? OK.
     */
    void showMessage(String message);
  }
}
