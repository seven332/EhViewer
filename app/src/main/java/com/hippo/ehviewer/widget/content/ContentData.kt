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

package com.hippo.ehviewer.widget.content

import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.INVALID_ID
import com.hippo.ehviewer.util.INVALID_INDEX
import com.hippo.ehviewer.exception.PresetException

/*
 * Created by Hippo on 6/5/2017.
 */

abstract class ContentData<T : Any> : ContentLogic, Iterable<T> {

  /**
   * Mark the entrance to change view.
   *
   * It could be doc, annotation is better.
   */
  @Target(AnnotationTarget.FUNCTION)
  @Retention(AnnotationRetention.SOURCE)
  private annotation class Entrance

  companion object {
    private const val TYPE_RESTORE = 0
    private const val TYPE_GOTO = 1
    private const val TYPE_PREV_PAGE = 2
    private const val TYPE_PREV_PAGE_ADJUST_POSITION = 3
    private const val TYPE_NEXT_PAGE = 4
    private const val TYPE_NEXT_PAGE_ADJUST_POSITION = 5
    private const val TYPE_REFRESH_PAGE = 6

    private const val DEFAULT_FIRST_PAGE_INDEX = 0

    @JvmField val NOT_FOUND_EXCEPTION = PresetException("not found", R.string.error_not_found, 0)
    @JvmField val TAP_TO_LOAD_EXCEPTION = PresetException("tap to Load", R.string.error_tap_to_load, 0)
  }

  private val state: ContentUiState = ContentUiState()

  private var idGenerator = INVALID_ID

  private var requirePage = 0
  private var requireType = 0
  private var requireId = INVALID_ID

  private var restored = false

  private val data: MutableList<T> = mutableListOf()
  /** The min page index **/
  var minPage = DEFAULT_FIRST_PAGE_INDEX
    private set
  /** The max page index + 1  */
  var maxPage = DEFAULT_FIRST_PAGE_INDEX
    private set
  /** The first loaded page index **/
  private var beginPage = DEFAULT_FIRST_PAGE_INDEX
  /** The last loaded page index + 1 **/
  private var endPage = DEFAULT_FIRST_PAGE_INDEX
  /**
   * Store the data divider index
   *
   * For example, the data contain page 3, page 4, page 5,
   * page 3 size is 7, page 4 size is 8, page 5 size is 9,
   * so `dataDivider` contain 7, 15, 24.
   */
  private var dataDivider: MutableList<Int> = mutableListOf()

  /**
   * Whether remove duplicates. If remove, duplicate item
   * in [.setData] will be ignored.
   *
   * Duplicates in the same page are not ignored.
   *
   * `true` in default.
   */
  var removeDuplicates = true

  /**
   * Sets duplicates checking range.
   *
   * `50` in default.
   */
  var duplicatesCheckRange = 50


  override fun attach(ui: ContentUi) {
    state.attach(ui)
  }

  override fun detach() {
    state.detach()
  }

  override fun isRestoring() = state.isRestoring

  override fun iterator() = object : Iterator<T> {
    var index = 0

    override fun next() = data[index++]

    override fun hasNext() = index < data.size
  }

  fun size() = data.size

  fun get(index: Int) = data[index]

  private fun isMinReached() = beginPage <= minPage

  private fun isMaxReached() = endPage >= maxPage

  /**
   * Returns `true` if there is a loading task running.
   */
  fun isLoading() = requireId != INVALID_ID

  /**
   * Gets the page of the position.
   */
  fun getPageForPosition(position: Int): Int {
    if (position < 0) {
      return INVALID_INDEX
    }

    for ((i, divider) in dataDivider.withIndex()) {
      if (position < divider) {
        return beginPage + i
      }
    }

    return INVALID_INDEX
  }

  /**
   * Restores data. `goTo(0)` will be called after failure or success.
   */
  @Entrance
  fun restore() {
    state.stopRefreshing()
    state.showProgressBar()
    requireData(0, TYPE_RESTORE)
  }

  /**
   * Goes to target page. It discards all previous data.
   */
  @Entrance
  fun goTo(page: Int) {
    if (data.isEmpty()) {
      state.stopRefreshing()
      state.showProgressBar()
    } else {
      state.setHeaderRefreshing()
      state.showContent()
    }
    requireData(page, TYPE_GOTO)
  }

  /**
   * It's different from goTo().
   * switchTo() will only scrollToPosition() if the page is in range.
   */
  @Entrance
  fun switchTo(page: Int) {
    if (page in beginPage until endPage) {
      val beginIndex = if (page == beginPage) 0 else dataDivider[page - beginPage - 1]
      state.scrollToPosition(beginIndex)
    } else if (page == endPage) {
      nextPage(true)
    } else if (page == beginPage - 1) {
      prevPage(true)
    } else {
      goTo(page)
    }
  }

  @Entrance
  private fun prevPage(adjustPosition: Boolean) {
    if (data.isEmpty()) {
      state.stopRefreshing()
      state.showProgressBar()
    } else {
      state.setHeaderRefreshing()
      state.showContent()
    }
    requireData(beginPage - 1, if (adjustPosition) TYPE_PREV_PAGE_ADJUST_POSITION else TYPE_PREV_PAGE)
  }

  @Entrance
  private fun nextPage(adjustPosition: Boolean) {
    if (data.isEmpty()) {
      state.stopRefreshing()
      state.showProgressBar()
    } else {
      state.setFooterRefreshing()
      state.showContent()
    }
    requireData(endPage, if (adjustPosition) TYPE_NEXT_PAGE_ADJUST_POSITION else TYPE_NEXT_PAGE)
  }

  override fun onRefreshHeader() {
    if (isMinReached()) {
      goTo(beginPage)
    } else {
      prevPage(false)
    }
  }

  @Entrance
  override fun onRefreshFooter() {
    if (beginPage == endPage) {
      // No data is loaded
      state.stopRefreshing()
    } else if (isMaxReached()) {
      state.setFooterRefreshing()
      requireData(endPage - 1, TYPE_REFRESH_PAGE)
    } else {
      nextPage(false)
    }
  }

  @Entrance
  override fun onReachBottom() {
    if (!isMaxReached()) {
      nextPage(false)
    }
  }

  @Entrance
  override fun onClickTip() {
    if (!isMaxReached()) {
      nextPage(true)
    } else if (!isMinReached()) {
      prevPage(true)
    } else {
      goTo(DEFAULT_FIRST_PAGE_INDEX)
    }
  }

  private fun nextId(): Int {
    var id: Int
    do {
      id = ++idGenerator
    } while (id == INVALID_ID)
    return id
  }

  private fun requireData(page: Int, type: Int) {
    requirePage = page
    requireType = type
    requireId = nextId()

    if (type == TYPE_RESTORE) {
      onRestoreData(requireId)
    } else {
      onRequireData(requireId, page)
    }
  }

  /**
   * Requires data. This method is not blocking.
   * When you get data, call [.setData] or [.setError].
   * If you get data right now, post it.
   */
  protected abstract fun onRequireData(id: Int, page: Int)

  /**
   * Restores data from backed up before. This method is not blocking.
   * When you get data, call [.setData] or [.setError].
   * If you get data right now, post it.
   */
  protected abstract fun onRestoreData(id: Int)

  /**
   * Backs up the data to allow restore later. This method is not blocking.
   */
  protected abstract fun onBackupData(data: List<T>)

  /**
   * Returns `true` if the two items are duplicate.
   *
   * @see .setRemoveDuplicates
   */
  open fun isDuplicate(t1: T, t2: T) = t1 == t2

  /**
   * Got data. Return {@code true} if it affects this {@code ContentData}.
   *
   * Min page index is `0` as default.
   */
  @Entrance
  fun setData(id: Int, list: List<T>, max: Int) = setData(id, list, 0, max)

  /**
   * Got data. Return {@code true} if it affects this {@code ContentData}.
   */
  @Entrance
  fun setData(id: Int, list: List<T>, min: Int, max: Int): Boolean {
    if (requireId == INVALID_ID || id != requireId) return false

    if (min > max) throw IllegalStateException("min > max")

    requireId = INVALID_ID
    restored = requireType == TYPE_RESTORE
    state.stopRefreshing()

    when (requireType) {
      TYPE_RESTORE -> onRestore(list)
      TYPE_GOTO -> onGoTo(list, min, max)
      TYPE_PREV_PAGE -> onPrevPage(list, min, max, false)
      TYPE_PREV_PAGE_ADJUST_POSITION -> onPrevPage(list, min, max, true)
      TYPE_NEXT_PAGE -> onNextPage(list, min, max, false)
      TYPE_NEXT_PAGE_ADJUST_POSITION -> onNextPage(list, min, max, true)
      TYPE_REFRESH_PAGE-> onRefreshPage(list, min, max)
      else -> throw IllegalStateException("Unknown type: " + requireType)
    }

    return true
  }

  @Entrance
  private fun onRestore(list: List<T>) {
    // Update data
    data.clear()
    data.addAll(list)
    state.notifyDataSetChanged()

    // Update dataDivider
    dataDivider.clear()
    dataDivider.add(list.size)

    // Update pages, beginPage, endPage
    // Always assume 1 page
    minPage = 0
    maxPage = 1
    beginPage = 0
    endPage = 1

    // Update UI
    if (data.isEmpty()) {
      state.showProgressBar()
    } else {
      state.showContent()
      state.scrollToPosition(0)
      state.setHeaderRefreshing()
    }

    // Keep loading
    goTo(DEFAULT_FIRST_PAGE_INDEX)
  }

  @Entrance
  private fun onGoTo(d: List<T>, min: Int, max: Int) {
    // Update data
    data.clear()
    data.addAll(d)
    state.notifyDataSetChanged()

    // Update dataDivider
    dataDivider.clear()
    dataDivider.add(d.size)

    // Update pages, beginPage, endPage
    minPage = min
    maxPage = max
    beginPage = requirePage
    endPage = beginPage + 1

    // Update UI
    if (data.isEmpty()) {
      if (isMinReached() && isMaxReached()) {
        state.showTip(NOT_FOUND_EXCEPTION)
      } else {
        state.showTip(TAP_TO_LOAD_EXCEPTION)
      }
    } else {
      state.showContent()
      // Scroll to top
      state.scrollToPosition(0)
    }

    // Backup data
    if (requirePage == min && !d.isEmpty()) {
      onBackupData(d)
    }
  }

  private fun removeDuplicates(d: List<T>, index: Int): List<T> {
    // Don't check all data, just check the data around the index to insert
    return removeDuplicates(d, index - duplicatesCheckRange, index + duplicatesCheckRange)
  }

  // Start and end will be fixed to fit range [0, data.size())
  private fun removeDuplicates(list: List<T>, start: Int, end: Int): List<T> {
    val from = Math.max(0, start)
    val to = Math.min(data.size, end)
    val control = data.subList(from, to)
    return list.filter { it1 -> control.all { it2 -> !isDuplicate(it1, it2) } }
  }

  @Entrance
  private fun onPrevPage(_list: List<T>, min: Int, max: Int, adjustPosition: Boolean) {
    // Remove duplicates
    val list: List<T>
    if (removeDuplicates) {
      list = removeDuplicates(_list, 0)
    } else {
      list = _list
    }

    // Update data
    val size = list.size
    if (size != 0) {
      data.addAll(0, list)
      state.notifyItemRangeInserted(0, size)
    }

    // Update dataDivider
    if (size != 0) {
      var i = 0
      val n = dataDivider.size
      while (i < n) {
        dataDivider[i] = dataDivider[i] + size
        i++
      }
    }
    dataDivider.add(0, size)

    // Update pages, beginPage, endPage
    minPage = min
    maxPage = max
    --beginPage

    // Update UI
    if (data.isEmpty()) {
      if (isMinReached() && isMaxReached()) {
        state.showTip(NOT_FOUND_EXCEPTION)
      } else {
        state.showTip(TAP_TO_LOAD_EXCEPTION)
      }
    } else {
      state.showContent()
      if (adjustPosition) {
        // Scroll to the first position of require page
        state.scrollToPosition(0)
      } else {
        state.scrollUpALittle()
      }
    }
  }

  @Entrance
  private fun onNextPage(_list: List<T>, min: Int, max: Int, adjustPosition: Boolean) {
    // Remove duplicates
    val list: List<T>
    if (removeDuplicates) {
      list = removeDuplicates(_list, data.size)
    } else {
      list = _list
    }

    // Update data
    val oldSize = data.size
    if (!list.isEmpty()) {
      data.addAll(list)
      state.notifyItemRangeInserted(oldSize, list.size)
    }

    // Update dataDivider
    dataDivider.add(data.size)

    // Update pages, beginPage, endPage
    minPage = min
    maxPage = max
    ++endPage

    // Update UI
    if (data.isEmpty()) {
      if (isMinReached() && isMaxReached()) {
        state.showTip(NOT_FOUND_EXCEPTION)
      } else {
        state.showTip(TAP_TO_LOAD_EXCEPTION)
      }
    } else {
      state.showContent()
      if (adjustPosition) {
        if (!list.isEmpty()) {
          // Scroll to the first position of require page
          state.scrollToPosition(oldSize)
        }
      } else {
        state.scrollDownALittle()
      }
    }
  }

  @Entrance
  private fun onRefreshPage(_list: List<T>, min: Int, max: Int) {
    if (requirePage >= endPage || requirePage < beginPage) {
      throw IllegalStateException("TYPE_REFRESH_PAGE requires requirePage in range, "
          + "beginPage=" + beginPage + ", endPage=" + endPage + ", requirePage=" + requirePage)
    }

    val beginIndex = if (requirePage == beginPage) 0 else dataDivider[requirePage - beginPage - 1]
    val oldEndIndex = dataDivider[requirePage - beginPage]

    // Remove duplicates
    val list: List<T>
    if (removeDuplicates) {
      val __list = removeDuplicates(_list, beginIndex - duplicatesCheckRange, beginIndex)
      list = removeDuplicates(__list, oldEndIndex, oldEndIndex + duplicatesCheckRange)
    } else {
      list = _list
    }

    val newEndIndex = beginIndex + list.size

    // Update data
    val oldCount = oldEndIndex - beginIndex
    val newCount = list.size
    val overlapCount = Math.min(oldCount, newCount)
    // Change overlapping data
    if (overlapCount != 0) {
      data.subList(beginIndex, beginIndex + overlapCount).clear()
      data.addAll(beginIndex, list.subList(0, overlapCount))
      state.notifyItemRangeChanged(beginIndex, overlapCount)
    }
    // Remove remaining data
    if (oldCount > overlapCount) {
      data.subList(beginIndex + overlapCount, beginIndex + oldCount).clear()
      state.notifyItemRangeRemoved(beginIndex + overlapCount, oldCount - overlapCount)
    }
    // Add remaining data
    if (newCount > overlapCount) {
      data.addAll(beginIndex + overlapCount, list.subList(overlapCount, newCount))
      state.notifyItemRangeInserted(beginIndex + overlapCount, newCount - overlapCount)
    }

    // Update dataDivider
    if (newEndIndex != oldEndIndex) {
      var i = requirePage - beginPage
      val n = dataDivider.size
      while (i < n) {
        dataDivider[i] = dataDivider[i] - oldEndIndex + newEndIndex
        i++
      }
    }

    // Update pages, beginPage, endPage
    minPage = min
    maxPage = max

    // Update UI
    if (data.isEmpty()) {
      if (isMinReached() && isMaxReached()) {
        state.showTip(NOT_FOUND_EXCEPTION)
      } else {
        state.showTip(TAP_TO_LOAD_EXCEPTION)
      }
    } else {
      state.showContent()
    }
  }

  /**
   * Got exception. Return `true` if it affects this `ContentData`.
   */
  @Entrance
  fun setError(id: Int, e: Throwable): Boolean {
    if (requireId == INVALID_ID || id != requireId) return false

    requireId = INVALID_ID
    state.stopRefreshing()

    if ((requireType == TYPE_GOTO && !restored) || requireType == TYPE_RESTORE || data.isEmpty()) {
      // Clear all data
      if (!data.isEmpty()) {
        data.clear()
        state.notifyDataSetChanged()
      }
      dataDivider.clear()
      minPage = 0
      maxPage = 0
      beginPage = 0
      endPage = 0

      if (requireType == TYPE_RESTORE) {
        state.showProgressBar()
        goTo(0)
      } else {
        state.showTip(e)
      }
    } else {
      // Has some data
      // Only non-interrupting message
      state.showMessage(e)
    }

    return true
  }

  @Entrance
  fun forceError(e: Throwable) {
    requireId = INVALID_ID
    state.stopRefreshing()

    // Clear all data
    if (!data.isEmpty()) {
      data.clear()
      state.notifyDataSetChanged()
    }
    dataDivider.clear()
    minPage = 0
    maxPage = 0
    beginPage = 0
    endPage = 0

    state.showTip(e)
  }

  @Entrance
  fun forceProgress() {
    requireId = INVALID_ID
    state.stopRefreshing()

    // Clear all data
    if (!data.isEmpty()) {
      data.clear()
      state.notifyDataSetChanged()
    }
    dataDivider.clear()
    minPage = 0
    maxPage = 0
    beginPage = 0
    endPage = 0

    state.showProgressBar()
  }
}
