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

package com.hippo.ehviewer.scene.gallerysearch;

/*
 * Created by Hippo on 5/12/2017.
 */

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.GLUrlBuilder;
import com.hippo.ehviewer.view.StatusBarView;
import com.hippo.ehviewer.widget.CategoryTable;

public class GallerySearchView
    extends StatusBarView<GallerySearchContract.Presenter, GallerySearchScene>
    implements GallerySearchContract.View {

  private static final int MODE_NORMAL = 0;

  private static final int TYPE_SEARCH_BAR = 0;
  private static final int TYPE_NORMAL_SEARCH = 1;

  private static final int[] ITEM_COUNT_ARRAY = {
      2,
  };
  private static final int[][] ITEM_TYPE_ARRAY_ARRAY = {
      {TYPE_SEARCH_BAR, TYPE_NORMAL_SEARCH},
  };

  private static final String KEY_SAVED_STATES = "GallerySearchView:saved_states";

  private int mode = MODE_NORMAL;

  /**
   * Recycler can't save states of children. Do it here.
   */
  private SparseArray<Parcelable> savedStates = new SparseArray<>();
  private SearchBarHolder searchBarHolder;
  private NormalSearchHolder normalSearchHolder;

  @NonNull
  @Override
  protected View onCreateStatusBarContent(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.view_gallery_search, parent, false);

    RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
    recyclerView.setAdapter(new Adapter(inflater));

    view.findViewById(R.id.search).setOnClickListener(v -> {
      getPresenter().commitGLUrlBuilder(getGLUrlBuilder());
      getEhvScene().pop();
    });

    return view;
  }

  private GLUrlBuilder getGLUrlBuilder() {
    GLUrlBuilder builder = new GLUrlBuilder();
    if (searchBarHolder != null) {
      builder.setKeyword(searchBarHolder.getKeyword());
    }
    if (normalSearchHolder != null) {
      builder.setCategory(normalSearchHolder.getCategory());
      builder.setLanguage(normalSearchHolder.getLanguage());
    }
    return builder;
  }

  @Override
  protected boolean whetherShowLeftDrawer() {
    return false;
  }

  @Override
  protected void onSaveState(@NonNull Bundle outState) {
    super.onSaveState(outState);
    if (searchBarHolder != null) {
      searchBarHolder.saveState(savedStates);
    }
    if (normalSearchHolder != null) {
      normalSearchHolder.saveState(savedStates);
    }
    outState.putSparseParcelableArray(KEY_SAVED_STATES, savedStates);
  }

  @Override
  protected void onRestoreState(@NonNull Bundle savedViewState) {
    super.onRestoreState(savedViewState);
    SparseArray<Parcelable> savedStates = savedViewState.getSparseParcelableArray(KEY_SAVED_STATES);
    if (savedStates != null) {
      this.savedStates = savedStates;
    }
  }

  private static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view, SparseArray<Parcelable> savedStates) {
      super(view);
      restoreState(savedStates);
    }

    public void saveState(SparseArray<Parcelable> savedStates) {
      itemView.saveHierarchyState(savedStates);
    }

    public void restoreState(SparseArray<Parcelable> savedStates) {
      itemView.restoreHierarchyState(savedStates);
    }
  }

  private class SearchBarHolder extends ViewHolder {

    private FloatingSearchView floatingSearchView;

    private SearchBarHolder(View view, SparseArray<Parcelable> savedStates) {
      super(view, savedStates);
      // Here is a trick to block FloatingSearchView
      view.findViewById(R.id.search_bar_text).setFocusable(false);
      view.findViewById(R.id.search_suggestions_section).setVisibility(View.GONE);

      floatingSearchView = (FloatingSearchView) view.findViewById(R.id.floating_search_view);
      floatingSearchView.setOnHomeActionClickListener(() -> getEhvScene().pop());
    }

    public String getKeyword() {
      return floatingSearchView.getQuery();
    }
  }

  public SearchBarHolder createSearchBarHolder(LayoutInflater inflater, ViewGroup parent,
      SparseArray<Parcelable> savedStates) {
    View view = inflater.inflate(R.layout.gallery_search_item_search_bar, parent, false);
    return new SearchBarHolder(view, savedStates);
  }

  private static class NormalSearchHolder extends ViewHolder {

    private CategoryTable categoryTable;
    private Spinner language;

    public NormalSearchHolder(View view, SparseArray<Parcelable> savedStates) {
      super(view, savedStates);
      categoryTable = (CategoryTable) view.findViewById(R.id.category_table);
      language = (Spinner) view.findViewById(R.id.language);
    }

    public int getCategory() {
      return categoryTable.getCategory();
    }

    public int getLanguage() {
      int position = language.getSelectedItemPosition();
      if (position == 0) {
        return EhUtils.LANG_UNKNOWN;
      } else {
        return position;
      }
    }
  }

  public static NormalSearchHolder createNormalSearchHolder(LayoutInflater inflater,
      ViewGroup parent, SparseArray<Parcelable> savedStates) {
    View view = inflater.inflate(R.layout.gallery_search_item_normal_search, parent, false);
    return new NormalSearchHolder(view, savedStates);
  }

  private class Adapter extends RecyclerView.Adapter<ViewHolder> {

    private LayoutInflater inflater;

    public Adapter(LayoutInflater inflater) {
      this.inflater = inflater;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      switch (viewType) {
        case TYPE_SEARCH_BAR:
          if (searchBarHolder == null) {
            searchBarHolder = createSearchBarHolder(inflater, parent, savedStates);
          }
          return searchBarHolder;
        case TYPE_NORMAL_SEARCH:
          if (normalSearchHolder == null) {
            normalSearchHolder = createNormalSearchHolder(inflater, parent, savedStates);
          }
          return normalSearchHolder;
        default:
          throw new IllegalStateException("Invalid type: " + viewType);
      }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {}

    @Override
    public int getItemViewType(int position) {
      return ITEM_TYPE_ARRAY_ARRAY[mode][position];
    }

    @Override
    public int getItemCount() {
      return ITEM_COUNT_ARRAY[mode];
    }
  }
}
