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
import com.hippo.yorozuya.ObjectUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

// TODO Delete some data if the data list is to long

/**
 * Data container for {@link ContentLayout}.
 * It can be keep even after the Activity recreated
 * as long as the data is not relative to the Activity.
 *
 * @param <T> data type
 */
public abstract class ContentData<T> extends ContentContract.AbsPresenter<T> {

  private static final String LOG_TAG = ContentData.class.getSimpleName();

  @SuppressWarnings("ThrowableInstanceNeverThrown")
  private static final Exception NOT_FOUND_EXCEPTION = new NotFoundException();
  @SuppressWarnings("ThrowableInstanceNeverThrown")
  private static final Exception TAP_TO_LOAD_EXCEPTION = new TapToLoadException();

  @IntDef({
      TYPE_RESTORE,
      TYPE_GOTO,
      TYPE_PREV_PAGE,
      TYPE_PREV_PAGE_ADJUST_POSITION,
      TYPE_NEXT_PAGE,
      TYPE_NEXT_PAGE_ADJUST_POSITION,
      TYPE_REFRESH_PAGE,
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface Type {}

  static final int TYPE_RESTORE = 0;
  static final int TYPE_GOTO = 1;
  static final int TYPE_PREV_PAGE = 2;
  static final int TYPE_PREV_PAGE_ADJUST_POSITION = 3;
  static final int TYPE_NEXT_PAGE = 4;
  static final int TYPE_NEXT_PAGE_ADJUST_POSITION = 5;
  static final int TYPE_REFRESH_PAGE = 6;

  private static final long INVALID_ID = -1;

  private long idGenerator = INVALID_ID;

  private int requirePage;
  @Type
  private int requireType;
  private long requireId = INVALID_ID;

  private boolean started;

  private boolean removeDuplicates = false;
  // Duplicates checking left and right range
  private int duplicatesCheckRange = 50;

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
  private ContentContract.View view;
  private ContentViewState state = new ContentViewState();

  @Override
  public void setView(@Nullable ContentContract.View view) {
    this.view = view;
    if (view != null) {
      state.restore(view);
    }
  }

  @Override
  @Nullable
  public ContentContract.View getView() {
    return view;
  }

  @NonNull
  @Override
  public ContentContract.ViewState getViewState() {
    return state;
  }

  /**
   * Return the number of data.
   */
  @Override
  public int size() {
    return data.size();
  }

  /**
   * Return the datum at specified position.
   */
  @Override
  public T get(int index) {
    return data.get(index);
  }

  /**
   * Restores the data from backed up before.
   * {@code goTo(0)} will be called after restoring operation done.
   * <p>
   * Call it before any loading operation.
   */
  public void restore() {
    if (started) {
      Log.e(LOG_TAG, "Only call restore() before any loading operation");
      return;
    }
    requireData(0, TYPE_RESTORE);
  }

  // Return true if min page reached.
  private boolean isMinReached() {
    return beginPage <= minPage;
  }

  // Return true if max page reached.
  @Override
  public boolean isMaxReached() {
    return endPage >= maxPage;
  }

  // Will discard all data
  @Override
  public void goTo(int page) {
    requireData(page, TYPE_GOTO);
  }

  // It's different from goTo()
  // switchTo() will only scrollToPosition() if
  // the page is in range.
  @Override
  public void switchTo(int page) {
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

  @Override
  public void onRefreshHeader() {
    if (isMinReached()) {
      requireData(minPage, TYPE_GOTO);
    } else {
      requireData(beginPage - 1, TYPE_PREV_PAGE);
    }
    state.setHeaderRefreshing();
  }

  @Override
  public void onRefreshFooter() {
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
    state.setFooterRefreshing();
  }

  @Override
  public void onClickTip() {
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
    started = true;
    requirePage = page;
    requireType = type;
    requireId = nextId();

    if (data.isEmpty()) {
      showProgressBar();
    } else {
      showContent();
    }

    if (type == TYPE_RESTORE) {
      onRestoreData(requireId);
    } else {
      onRequireData(requireId, page);
    }
  }

  /**
   * Requires data. This method is not blocking.
   * When you get data, call {@link #setData(long, List, int)}
   * or {@link #setError(long, Throwable)}.
   * If you get data right now, post it.
   */
  protected abstract void onRequireData(long id, int page);

  /**
   * Restores data from backed up before. This method is not blocking.
   * When you get data, call {@link #setData(long, List, int)}
   * or {@link #setError(long, Throwable)}.
   * If you get data right now, post it.
   */
  protected abstract void onRestoreData(long id);

  /**
   * Backs up the data to allow restore later. This method is not blocking.
   */
  protected abstract void onBackupData(List<T> data);

  /**
   * Whether remove duplicates. If remove, duplicate item
   * in {@link #setData(long, List, int, int)} will be ignored.
   * <p>
   * Duplicates in the same page are not ignored.
   *
   * @see #isDuplicate(Object, Object)
   */
  public void setRemoveDuplicates(boolean removeDuplicates) {
    this.removeDuplicates = removeDuplicates;
  }

  /**
   * Sets duplicates checking range.
   *
   * @see #setRemoveDuplicates(boolean)
   */
  public void setDuplicatesCheckRange(int range) {
    this.duplicatesCheckRange = range;
  }

  /**
   * Returns {@code true} if the two items are duplicate.
   *
   * @see #setRemoveDuplicates(boolean)
   */
  protected boolean isDuplicate(@Nullable T t1, @Nullable T t2) {
    return ObjectUtils.equals(t1, t2);
  }

  /**
   * {@code setData(id, d, 0, p)}.
   */
  public boolean setData(long id, List<T> d, int p) {
    return setData(id, d, 0, p);
  }

  /**
   * Got data. Return {@code true} if it affects this {@code ContentData}.
   */
  public boolean setData(long id, List<T> d, int min, int max) {
    if (requireId == INVALID_ID || id != requireId) {
      // Invalid id
      return false;
    }
    // Reset require id
    requireId = INVALID_ID;

    // Reset refresh UI
    stopRefreshing();

    switch (requireType) {
      case TYPE_RESTORE:
        onRestore(d);
        break;
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

    return true;
  }

  private void onRestore(List<T> d) {
    // Update data
    data.clear();
    data.addAll(d);
    notifyDataSetChanged();

    // Update dataDivider
    dataDivider.clear();
    dataDivider.add(d.size());

    // Update pages, beginPage, endPage
    // Always assume 1 page
    minPage = 0;
    maxPage = 1;
    beginPage = 0;
    endPage = 1;

    // Update UI
    if (data.isEmpty()) {
      showProgressBar();
    } else {
      showContent();
      // Scroll to top
      scrollToPosition(0);
    }

    // Continue loading
    setHeaderRefreshing();
    goTo(0);
  }

  private void onGoTo(List<T> d, int min, int max) {
    // Update data
    data.clear();
    data.addAll(d);
    notifyDataSetChanged();

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
        showTip(NOT_FOUND_EXCEPTION);
      } else {
        showTip(TAP_TO_LOAD_EXCEPTION);
      }
    } else {
      showContent();
      // Scroll to top
      scrollToPosition(0);
    }

    // Backup data
    if (requirePage == 0 && !d.isEmpty()) {
      onBackupData(d);
    }
  }

  private List<T> removeDuplicates(List<T> d, int index) {
    // Don't check all data, just check the data around the index to insert
    return removeDuplicates(d, index - duplicatesCheckRange, index + duplicatesCheckRange);
  }

  // Start and end will be fixed to fit range [0, data.size())
  private List<T> removeDuplicates(List<T> d, int start, int end) {
    // Fix start and end
    start = Math.max(0, start);
    end = Math.min(data.size(), end);

    List<T> result = new ArrayList<>(d.size());

    for (T t: d) {
      boolean duplicate = false;
      for (int i = start; i < end; ++i) {
        if (isDuplicate(t, data.get(i))) {
          duplicate = true;
          break;
        }
      }
      if (!duplicate) {
        result.add(t);
      }
    }

    return result;
  }

  private void onPrevPage(List<T> d, int min, int max, boolean adjustPosition) {
    if (requirePage != beginPage - 1) {
      throw new IllegalStateException("TYPE_PREV_PAGE or TYPE_PREV_PAGE_ADJUST_POSITION"
          + " always require the page before begin page, beginPage=" + beginPage + ", requirePage=" + requirePage);
    }

    // Remove duplicates
    if (removeDuplicates) {
      d = removeDuplicates(d, 0);
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
        showTip(NOT_FOUND_EXCEPTION);
      } else {
        showTip(TAP_TO_LOAD_EXCEPTION);
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

    // Remove duplicates
    if (removeDuplicates) {
      d = removeDuplicates(d, data.size());
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
        showTip(NOT_FOUND_EXCEPTION);
      } else {
        showTip(TAP_TO_LOAD_EXCEPTION);
      }
    } else {
      showContent();
      if (adjustPosition) {
        // Scroll to the first position of require page
        scrollToPosition(MathUtils.clamp(oldSize, 0, data.size() - 1));
      } else {
        scrollDownALittle();
      }
    }
  }

  private void onRefreshPage(List<T> d, int min, int max) {
    if (requirePage >= endPage || requirePage < beginPage) {
      throw new IllegalStateException("TYPE_REFRESH_PAGE requires requirePage in range, "
          + "beginPage=" + beginPage + ", endPage=" + endPage + ", requirePage=" + requirePage);
    }

    int beginIndex = (requirePage == beginPage ? 0 : dataDivider.get(requirePage - beginPage - 1));
    int oldEndIndex = dataDivider.get(requirePage - beginPage);

    // Remove duplicates
    if (removeDuplicates) {
      d = removeDuplicates(d, beginIndex - duplicatesCheckRange, beginIndex);
      d = removeDuplicates(d, oldEndIndex, oldEndIndex + duplicatesCheckRange);
    }

    int newEndIndex = beginIndex + d.size();

    // Update data
    int oldCount = oldEndIndex - beginIndex;
    int newCount = d.size();
    int overlapCount = Math.min(oldCount, newCount);
    // Change overlapping data
    if (overlapCount != 0) {
      data.subList(beginIndex, beginIndex + overlapCount).clear();
      data.addAll(beginIndex, d.subList(0, overlapCount));
      notifyItemRangeChanged(beginIndex, overlapCount);
    }
    // Remove remaining data
    if (oldCount > overlapCount) {
      data.subList(beginIndex + overlapCount, beginIndex + oldCount).clear();
      notifyItemRangeRemoved(beginIndex + overlapCount, oldCount - overlapCount);
    }
    // Add remaining data
    if (newCount > overlapCount) {
      data.addAll(beginIndex + overlapCount, d.subList(overlapCount, newCount));
      notifyItemRangeInserted(beginIndex + overlapCount, newCount - overlapCount);
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
        showTip(NOT_FOUND_EXCEPTION);
      } else {
        showTip(TAP_TO_LOAD_EXCEPTION);
      }
    } else {
      showContent();
    }
  }

  /**
   * Got exception. Return {@code true} if it affects this {@code ContentData}.
   */
  public boolean setError(long id, Throwable e) {
    if (requireId == INVALID_ID || id != requireId) {
      // Invalid id
      return false;
    }
    // Reset require id
    requireId = INVALID_ID;

    // Reset refresh UI
    stopRefreshing();

    if (requireType == TYPE_RESTORE) {
      onRestoreError();
    } else {
      onError(e);
    }

    return true;
  }

  private void onRestoreError() {
    // No data at all
    // Reset all
    data.clear();
    dataDivider.clear();
    minPage = 0;
    maxPage = 0;
    beginPage = 0;
    endPage = 0;
    // Show progress bar
    showProgressBar();
    // Continue loading
    setHeaderRefreshing();
    goTo(0);
  }

  private void onError(Throwable e) {
    if (data.isEmpty()) {
      // No data at all
      // Reset all and show tip
      data.clear();
      dataDivider.clear();
      minPage = 0;
      maxPage = 0;
      beginPage = 0;
      endPage = 0;
      showTip(e);
    } else {
      // Has some data
      // Only non-interrupting message
      showMessage(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public ContentContract.DataPresenter<T> solidify() {
    return new ContentContract.Solid<>((T[]) data.toArray());
  }

  private static class ContentViewState extends ContentContract.ViewState {

    private boolean showContent;
    private boolean showProgressBar;
    private Throwable throwable;
    private boolean headerRefreshing;
    private boolean footerRefreshing;

    @Override
    public void restore(ContentContract.View view) {
      if (showContent) {
        view.showContent();
      } else if (throwable != null) {
        view.showTip(throwable);
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
      throwable = null;
      showProgressBar = false;
    }

    @Override
    public void showTip(Throwable t) {
      showContent = false;
      throwable = t;
      showProgressBar = false;
    }

    @Override
    public void showProgressBar() {
      showContent = false;
      throwable = null;
      showProgressBar = true;
    }

    @Override
    public void showMessage(Throwable t) {}

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
    public void scrollDownALittle() {}

    @Override
    public void notifyDataSetChanged() {}

    @Override
    public void notifyItemRangeInserted(int positionStart, int itemCount) {}

    @Override
    public void notifyItemRangeRemoved(int positionStart, int itemCount) {}

    @Override
    public void notifyItemRangeChanged(int positionStart, int itemCount) {}
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
