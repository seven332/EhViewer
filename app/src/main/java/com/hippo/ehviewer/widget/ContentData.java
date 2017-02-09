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
 * Created by Hippo on 2/5/2017.
 */

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.hippo.yorozuya.MathUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

// TODO Remove duplicate data

/**
 * Data container for {@link ContentLayout}.
 * It can be keep even after the Activity recreated
 * as long as the data is not relative to the Activity.
 *
 * @param <T> data type
 */
public abstract class ContentData<T> {

  private static final String LOG_TAG = ContentData.class.getSimpleName();

  @SuppressWarnings("ThrowableInstanceNeverThrown")
  private static final Exception NOT_FOUND_EXCEPTION = new NotFoundException();
  @SuppressWarnings("ThrowableInstanceNeverThrown")
  private static final Exception TAP_TO_LOAD_EXCEPTION = new TapToLoadException();

  @IntDef({
      TYPE_GOTO,
      TYPE_PREV_PAGE,
      TYPE_PREV_PAGE_ADJUST_POSITION,
      TYPE_NEXT_PAGE,
      TYPE_NEXT_PAGE_ADJUST_POSITION,
      TYPE_REFRESH_PAGE,
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface Type {}

  static final int TYPE_GOTO = 0;
  static final int TYPE_PREV_PAGE = 1;
  static final int TYPE_PREV_PAGE_ADJUST_POSITION = 2;
  static final int TYPE_NEXT_PAGE = 3;
  static final int TYPE_NEXT_PAGE_ADJUST_POSITION = 4;
  static final int TYPE_REFRESH_PAGE = 6;

  private static final long INVALID_ID = -1;

  private long idGenerator = INVALID_ID;

  private int requirePage;
  @Type
  private int requireType;
  private long requireId = INVALID_ID;

  /** The min page index **/
  @VisibleForTesting
  int minPage;
  /** The max page index + 1 **/
  @VisibleForTesting
  int maxPage;
  /** 0-base **/
  @VisibleForTesting
  int beginPage;
  /** The last loaded page index + 1 **/
  @VisibleForTesting
  int endPage;
  @VisibleForTesting
  List<T> data = new ArrayList<>();
  /**
   * Store the data divider index
   * <p>
   * For example, the data contain page 3, page 4, page 5,
   * page 3 size is 7, page 4 size is 8, page 5 size is 9,
   * so <code>dataDivider</code> contain 7, 15, 24.
   */
  @VisibleForTesting
  List<Integer> dataDivider = new ArrayList<>();

  @Nullable
  private ContentView view;
  private RecordView record = new RecordView();

  void setContentView(@Nullable ContentView view) {
    this.view = view;
    if (view != null) {
      record.restore(view);
    }
  }

  /**
   * Return the number of data.
   */
  public int size() {
    return data.size();
  }

  /**
   * Return the datum at specified position.
   */
  public T get(int index) {
    return data.get(index);
  }

  // Return true if min page reached.
  private boolean isMinReached() {
    return beginPage <= minPage;
  }

  // Return true if max page reached.
  private boolean isMaxReached() {
    return endPage >= maxPage;
  }

  // Will discard all data
  void goTo(int page) {
    requireData(page, TYPE_GOTO);
  }

  // It's different from goTo()
  // switchTo() will only scrollToPosition() if
  // the page is in range.
  void switchTo(int page) {
    if (page < endPage && page >= beginPage) {
      int beginIndex = (page == beginPage ? 0 : dataDivider.get(page - beginPage - 1));
      scrollToPosition(beginIndex);
    } else if (page == endPage) {
      nextPage();
    } else if (page == beginPage - 1) {
      prevPage();
    } else {
      goTo(page);
    }
  }

  void prevPage() {
    requireData(beginPage - 1, TYPE_PREV_PAGE);
  }

  void nextPage() {
    requireData(endPage, TYPE_NEXT_PAGE);
  }

  void refreshPage(int page) {
    if (page < endPage && page >= beginPage) {
      requireData(page, TYPE_REFRESH_PAGE);
    }
  }

  void onRefreshHeader() {
    if (isMinReached()) {
      requireData(minPage, TYPE_GOTO);
    } else {
      requireData(beginPage - 1, TYPE_PREV_PAGE);
    }
    record.setHeaderRefreshing();
  }

  void onRefreshFooter() {
    if (isMaxReached()) {
      if (endPage > beginPage) {
        // onRefreshPage() will check requirePage,
        // make sure it is in range.
        requireData(endPage - 1, TYPE_REFRESH_PAGE);
      } else {
        Log.e(LOG_TAG, "Invalid page state: beginPage=" + beginPage + ", endPage=" + endPage);
      }
    } else {
      requireData(endPage, TYPE_NEXT_PAGE);
    }
    record.setFooterRefreshing();
  }

  void onClickTip() {
    showProgressBar();
    if (!isMaxReached()) {
      requireData(endPage, TYPE_NEXT_PAGE_ADJUST_POSITION);
    } else if (!isMinReached()) {
      requireData(beginPage - 1, TYPE_PREV_PAGE_ADJUST_POSITION);
    } else {
      requireData(minPage, TYPE_GOTO);
    }
  }

  private long nextId() {
    long id;
    do {
      id = idGenerator++;
    } while (id == INVALID_ID);
    return id;
  }

  void requireData(int page, @Type int type) {
    requirePage = page;
    requireType = type;
    requireId = nextId();
    onRequireData(requireId, page);
  }

  /**
   * Requires data. This method is not blocking.
   * When you get data, call {@link #setData(long, List, int)}
   * or {@link #setError(long, Throwable)}.
   * If you get data right now, post it.
   */
  public abstract void onRequireData(long id, int page);

  /**
   * Gets tip to represent the {@code Throwable}.
   * <p>
   * {@link #NOT_FOUND_EXCEPTION} for no data.
   * <p>
   * {@link #TAP_TO_LOAD_EXCEPTION} for no data but can continue loading.
   */
  @NonNull
  public abstract ContentLayout.TipInfo getTipFromThrowable(Throwable e);

  /**
   * Show a non-interrupting message. Toast? SnackBar? OK.
   */
  public abstract void showMessage(String text);

  /**
   * {@code setData(id, d, 0, p)}.
   */
  public void setData(long id, List<T> d, int p) {
    setData(id, d, 0, p);
  }

  /**
   * Got data.
   */
  public void setData(long id, List<T> d, int min, int max) {
    if (requireId == INVALID_ID || id != requireId) {
      // Invalid id
      return;
    }
    // Reset require id
    requireId = INVALID_ID;

    // Reset refresh UI
    stopRefreshing();

    switch (requireType) {
      case TYPE_GOTO:
        onGoTo(d, min, max);
        break;
      case TYPE_PREV_PAGE:
      case TYPE_PREV_PAGE_ADJUST_POSITION:
        onPrevPage(d, min, max, requireType == TYPE_PREV_PAGE_ADJUST_POSITION);
        break;
      case TYPE_NEXT_PAGE:
      case TYPE_NEXT_PAGE_ADJUST_POSITION:
        onNextPage(d, min, max, requireType == TYPE_NEXT_PAGE_ADJUST_POSITION);
        break;
      case TYPE_REFRESH_PAGE:
        onRefreshPage(d, min, max);
        break;
      default:
        throw new IllegalStateException("Unknown type: " + requireType);
    }
  }

  private void onGoTo(List<T> d, int min, int max) {
    // Update data
    if (!data.isEmpty()) {
      int size = data.size();
      data.clear();
      notifyItemRangeRemoved(0, size);
    }
    if (!d.isEmpty()) {
      data.addAll(d);
      notifyItemRangeInserted(0, d.size());
    }

    // Update dataDivider
    dataDivider.clear();
    dataDivider.add(d.size());

    // Update pages, beginPage, endPage
    minPage = min;
    maxPage = max;
    beginPage = requirePage;
    endPage = beginPage + 1;

    // Update UI
    if (data.isEmpty()) {
      if (isMinReached() && isMaxReached()) {
        showTip(getTipFromThrowable(NOT_FOUND_EXCEPTION));
      } else {
        showTip(getTipFromThrowable(TAP_TO_LOAD_EXCEPTION));
      }
    } else {
      showContent();
      // Scroll to top
      scrollToPosition(0);
    }
  }

  private void onPrevPage(List<T> d, int min, int max, boolean adjustPosition) {
    if (requirePage != beginPage - 1) {
      throw new IllegalStateException("TYPE_PREV_PAGE or TYPE_PREV_PAGE_ADJUST_POSITION"
          + " always require the page before begin page, beginPage=" + beginPage + ", requirePage=" + requirePage);
    }

    // Update data
    int size = d.size();
    if (size != 0) {
      data.addAll(0, d);
      notifyItemRangeInserted(0, size);
    }

    // Update dataDivider
    if (size != 0) {
      for (int i = 0, n = dataDivider.size(); i < n; i++) {
        dataDivider.set(i, dataDivider.get(i) + size);
      }
    }
    dataDivider.add(0, size);

    // Update pages, beginPage, endPage
    minPage = min;
    maxPage = max;
    --beginPage;

    // Update UI
    if (data.isEmpty()) {
      if (isMinReached() && isMaxReached()) {
        showTip(getTipFromThrowable(NOT_FOUND_EXCEPTION));
      } else {
        showTip(getTipFromThrowable(TAP_TO_LOAD_EXCEPTION));
      }
    } else {
      showContent();
      if (adjustPosition) {
        // Scroll to the first position of require page
        scrollToPosition(0);
      }
    }
  }

  private void onNextPage(List<T> d, int min, int max, boolean adjustPosition) {
    if (requirePage != endPage) {
      throw new IllegalStateException("TYPE_NEXT_PAGE or TYPE_NEXT_PAGE_ADJUST_POSITION"
          + " always require end page, endPage=" + endPage + ", requirePage=" + requirePage);
    }

    // Update data
    int oldSize = data.size();
    if (!d.isEmpty()) {
      data.addAll(d);
      notifyItemRangeInserted(oldSize, d.size());
    }

    // Update dataDivider
    dataDivider.add(data.size());

    // Update pages, beginPage, endPage
    minPage = min;
    maxPage = max;
    ++endPage;

    // Update UI
    if (data.isEmpty()) {
      if (isMinReached() && isMaxReached()) {
        showTip(getTipFromThrowable(NOT_FOUND_EXCEPTION));
      } else {
        showTip(getTipFromThrowable(TAP_TO_LOAD_EXCEPTION));
      }
    } else {
      showContent();
      if (adjustPosition) {
        // Scroll to the first position of require page
        scrollToPosition(MathUtils.clamp(oldSize, 0, data.size() - 1));
      }
    }
  }

  private void onRefreshPage(List<T> d, int min, int max) {
    if (requirePage >= endPage || requirePage < beginPage) {
      throw new IllegalStateException("TYPE_REFRESH_PAGE requires requirePage in range, "
          + "beginPage=" + beginPage + ", endPage=" + endPage + ", requirePage=" + requirePage);
    }

    // Update data
    int oldBeginIndex = (requirePage == beginPage ? 0 : dataDivider.get(requirePage - beginPage - 1));
    int oldEndIndex = dataDivider.get(requirePage - beginPage);
    if (oldBeginIndex != oldEndIndex) {
      data.subList(oldBeginIndex, oldEndIndex).clear();
      notifyItemRangeRemoved(oldBeginIndex, oldEndIndex - oldBeginIndex);
    }
    @SuppressWarnings("UnnecessaryLocalVariable")
    int newBeginIndex = oldBeginIndex;
    int newEndIndex = newBeginIndex + d.size();
    if (newBeginIndex != newEndIndex) {
      data.addAll(newBeginIndex, d);
      notifyItemRangeInserted(newBeginIndex, newEndIndex - newBeginIndex);
    }

    // Update dataDivider
    if (newEndIndex != oldEndIndex) {
      for (int i = requirePage - beginPage, n = dataDivider.size(); i < n; i++) {
        dataDivider.set(i, dataDivider.get(i) - oldEndIndex + newEndIndex);
      }
    }

    // Update pages, beginPage, endPage
    minPage = min;
    maxPage = max;

    // Update UI
    if (data.isEmpty()) {
      if (isMinReached() && isMaxReached()) {
        showTip(getTipFromThrowable(NOT_FOUND_EXCEPTION));
      } else {
        showTip(getTipFromThrowable(TAP_TO_LOAD_EXCEPTION));
      }
    } else {
      showContent();
    }
  }

  /**
   * Got exception.
   */
  public void setError(long id, Throwable e) {
    if (requireId == INVALID_ID || id != requireId) {
      // Invalid id
      return;
    }
    // Reset require id
    requireId = INVALID_ID;

    // Reset refresh UI
    stopRefreshing();

    if (data.isEmpty()) {
      // No data at all
      // Reset all and show tip
      data.clear();
      dataDivider.clear();
      minPage = 0;
      maxPage = 0;
      beginPage = 0;
      endPage = 0;
      showTip(getTipFromThrowable(e));
    } else {
      // Has some data
      // Only non-interrupting message
      showMessage(getTipFromThrowable(e).text);
    }
  }

  public void showContent() {
    if (view != null) {
      view.showContent();
    }
    record.showContent();
  }

  public void showTip(ContentLayout.TipInfo info) {
    if (view != null) {
      view.showTip(info);
    }
    record.showTip(info);
  }

  public void showProgressBar() {
    if (view != null) {
      view.showProgressBar();
    }
    record.showProgressBar();
  }

  public void stopRefreshing() {
    if (view != null) {
      view.stopRefreshing();
    }
    record.stopRefreshing();
  }

  public void scrollToPosition(int position) {
    if (view != null) {
      view.scrollToPosition(position);
    }
    record.scrollToPosition(position);
  }

  public void notifyItemRangeInserted(int positionStart, int itemCount) {
    if (view != null) {
      view.notifyItemRangeInserted(positionStart, itemCount);
    }
    record.notifyItemRangeInserted(positionStart, itemCount);
  }

  public void notifyItemRangeRemoved(int positionStart, int itemCount) {
    if (view != null) {
      view.notifyItemRangeRemoved(positionStart, itemCount);
    }
    record.notifyItemRangeRemoved(positionStart, itemCount);
  }

  public static class RecordView implements ContentView {

    private boolean showContent;
    private boolean showProgressBar;
    private ContentLayout.TipInfo tip;
    private boolean headerRefreshing;
    private boolean footerRefreshing;

    public void restore(ContentView view) {
      if (showContent) {
        view.showContent();
      } else if (tip != null) {
        view.showTip(tip);
      } else if (showProgressBar) {
        view.showProgressBar();
      }
      if (headerRefreshing) {
        view.setHeaderRefreshing();
      } else if (footerRefreshing) {
        view.setFooterRefreshing();
      }
    }

    @Override
    public void showContent() {
      showContent = true;
      tip = null;
      showProgressBar = false;
    }

    @Override
    public void showTip(ContentLayout.TipInfo info) {
      showContent = false;
      tip = info;
      showProgressBar = false;
    }

    @Override
    public void showProgressBar() {
      showContent = false;
      tip = null;
      showProgressBar = true;
    }

    @Override
    public void stopRefreshing() {
      headerRefreshing = false;
      footerRefreshing = false;
    }

    @Override
    public void setHeaderRefreshing() {
      headerRefreshing = true;
    }

    @Override
    public void setFooterRefreshing() {
      footerRefreshing = true;
    }

    @Override
    public void scrollToPosition(int position) {}

    @Override
    public void notifyItemRangeInserted(int positionStart, int itemCount) {}

    @Override
    public void notifyItemRangeRemoved(int positionStart, int itemCount) {}
  }

  /**
   * Not found.
   */
  public static class NotFoundException extends Exception {
    public NotFoundException() {
      super("Not Found");
    }
  }

  /**
   * Tap to load.
   */
  public static class TapToLoadException extends Exception {
    public TapToLoadException() {
      super("Tap to Load");
    }
  }
}
