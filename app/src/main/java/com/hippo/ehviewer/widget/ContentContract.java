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
 * Created by Hippo on 2/10/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.presenter.PresenterInterface;
import com.hippo.ehviewer.view.base.ViewInterface;

public interface ContentContract {

  interface Presenter extends PresenterInterface<View> {

    void onRefreshHeader();

    void onRefreshFooter();

    void onClickTip();

    void goTo(int page);

    void switchTo(int page);

    int size();
  }

  interface View extends ViewInterface {

    void showContent();

    void showTip(Throwable t);

    void showProgressBar();

    void showMessage(Throwable t);

    void stopRefreshing();

    void setHeaderRefreshing();

    void setFooterRefreshing();

    void scrollToPosition(int position);

    void notifyDataSetChanged();

    void notifyItemRangeInserted(int positionStart, int itemCount);

    void notifyItemRangeRemoved(int positionStart, int itemCount);
  }

  abstract class ViewState implements View {

    public abstract void restore(View view);
  }

  interface DataPresenter<T> extends Presenter {

    T get(int index);

    /**
     * Returns a solid copy, which has no reaction.
     */
    DataPresenter<T> solidify();
  }

  abstract class AbsPresenter<T> implements DataPresenter<T>, View {

    @Nullable
    public abstract View getView();

    @NonNull
    public abstract ViewState getViewState();

    @Override
    public void showContent() {
      View view = getView();
      if (view != null) {
        view.showContent();
      }
      getViewState().showContent();
    }

    @Override
    public void showTip(Throwable t) {
      View view = getView();
      if (view != null) {
        view.showTip(t);
      }
      getViewState().showTip(t);
    }

    @Override
    public void showProgressBar() {
      View view = getView();
      if (view != null) {
        view.showProgressBar();
      }
      getViewState().showProgressBar();
    }

    @Override
    public void showMessage(Throwable t) {
      View view = getView();
      if (view != null) {
        view.showMessage(t);
      }
      getViewState().showMessage(t);
    }

    @Override
    public void stopRefreshing() {
      View view = getView();
      if (view != null) {
        view.stopRefreshing();
      }
      getViewState().stopRefreshing();
    }

    @Override
    public void setHeaderRefreshing() {
      View view = getView();
      if (view != null) {
        view.setHeaderRefreshing();
      }
      getViewState().setHeaderRefreshing();
    }

    @Override
    public void setFooterRefreshing() {
      View view = getView();
      if (view != null) {
        view.setFooterRefreshing();
      }
      getViewState().setFooterRefreshing();
    }

    @Override
    public void scrollToPosition(int position) {
      View view = getView();
      if (view != null) {
        view.scrollToPosition(position);
      }
      getViewState().scrollToPosition(position);
    }

    @Override
    public void notifyDataSetChanged() {
      View view = getView();
      if (view != null) {
        view.notifyDataSetChanged();
      }
      getViewState().notifyDataSetChanged();
    }

    @Override
    public void notifyItemRangeInserted(int positionStart, int itemCount) {
      View view = getView();
      if (view != null) {
        view.notifyItemRangeInserted(positionStart, itemCount);
      }
      getViewState().notifyItemRangeInserted(positionStart, itemCount);
    }

    @Override
    public void notifyItemRangeRemoved(int positionStart, int itemCount) {
      View view = getView();
      if (view != null) {
        view.notifyItemRangeRemoved(positionStart, itemCount);
      }
      getViewState().notifyItemRangeRemoved(positionStart, itemCount);
    }
  }

  class Solid<T> implements DataPresenter<T> {

    private T[] array;

    public Solid(T[] array) {
      this.array = array;
    }

    @Override
    public T get(int index) {
      return array[index];
    }

    @Override
    public DataPresenter<T> solidify() {
      return this;
    }

    @Override
    public int size() {
      return array.length;
    }

    @Override
    public void onRefreshHeader() {}

    @Override
    public void onRefreshFooter() {}

    @Override
    public void onClickTip() {}

    @Override
    public void goTo(int page) {}

    @Override
    public void switchTo(int page) {}

    @Override
    public void setView(View view) {}
  }
}
