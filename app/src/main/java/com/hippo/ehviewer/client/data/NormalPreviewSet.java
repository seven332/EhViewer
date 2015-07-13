/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.ehviewer.client.data;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.conaco.BitmapHolder;
import com.hippo.conaco.Conaco;
import com.hippo.conaco.Unikery;
import com.hippo.ehviewer.EhImageKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.widget.SimpleGridLayout;
import com.hippo.yorozuya.Say;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NormalPreviewSet extends PreviewSet {

    private static final String TAG = NormalPreviewSet.class.getSimpleName();

    private List<Row> mRowArray = new ArrayList<>();

    private Set<LoadImageTask> mLoadImageTasks = new HashSet<>();

    @Override
    public int size() {
        int size = 0;
        for (Row row : mRowArray) {
            size += row.itemArray.size();
        }
        return size;
    }

    public void addItem(String imageUrl, int xOffset, int yOffset, int width,
            int height, String url) {
        Row row;
        int size = mRowArray.size();
        if (size == 0 || ! (row = mRowArray.get(size - 1)).imageUrl.equals(imageUrl)) {
            row = new Row(imageUrl);
            mRowArray.add(row);
        }
        row.addItem(xOffset, yOffset, width, height, url);
    }

    @Override
    public void bindView(SimpleGridLayout simpleGridLayout,
            LayoutInflater inflater, Conaco conaco) {
        ensureViewGroup(simpleGridLayout, inflater);

        int i = 0;
        for (Row row : mRowArray) {
            int rowSize = row.itemArray.size();
            ImageView[] imageViews = new ImageView[rowSize];
            for (int j = 0; j < rowSize; j++) {
                ViewGroup viewGroup = (ViewGroup) simpleGridLayout.getChildAt(i);
                ImageView imageView = (ImageView) viewGroup.getChildAt(0);
                imageView.setImageDrawable(null);
                imageViews[j] = imageView;
                ((TextView) viewGroup.getChildAt(1)).setText(Integer.toString(i + getStartIndex() + 1));
                i++;
            }

            LoadImageTask loadImageTask = new LoadImageTask(conaco, row, imageViews);
            mLoadImageTasks.add(loadImageTask);

            String key;
            if (row.imageIndex != -1) {
                key = EhImageKeyFactory.getNormalPreviewKey(getGid(), row.imageIndex);
            } else {
                key = row.imageUrl;
            }
            conaco.load(loadImageTask, key, row.imageUrl);
        }
    }

    @Override
    public void cancelLoadTask(SimpleGridLayout simpleGridLayout, Conaco conaco) {
        // Cancel load task
        for (LoadImageTask loadImageTask : mLoadImageTasks) {
            conaco.cancel(loadImageTask);
        }
        mLoadImageTasks.clear();

        //Cancel long click
        int count = simpleGridLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            ViewGroup viewGroup = (ViewGroup) simpleGridLayout.getChildAt(i);
            View view = viewGroup.getChildAt(0);
            view.setOnLongClickListener(null);
            view.setLongClickable(false);
        }
    }

    class LoadImageTask implements Unikery {

        private int mTaskId = Unikery.INVAILD_ID;

        private Conaco mConaco;
        private Row mRow;
        private ImageView[] mImageViews;

        public LoadImageTask(Conaco conaco, Row row, ImageView[] imageViews) {
            mConaco = conaco;
            mRow = row;
            mImageViews = imageViews;
        }

        @Override
        public void setBitmap(BitmapHolder bitmapHolder, Conaco.Source source) {
            bitmapHolder.obtain();
            Bitmap bitmap = bitmapHolder.getBitmap();
            int maxWidth = bitmap.getWidth();
            int maxHeight = bitmap.getHeight();

            int size = mRow.itemArray.size();
            for(int i = 0; i < size; i++) {
                Item item = mRow.itemArray.get(i);
                if (item.xOffset + item.width > maxWidth) {
                    item.width = maxWidth - item.xOffset;
                    if (item.width <= 0) {
                        // Bad area
                        continue;
                    }
                }
                if (item.yOffset + item.height > maxHeight) {
                    item.height = maxHeight - item.yOffset;
                    if (item.height <= 0) {
                        // Bad area
                        continue;
                    }
                }

                Bitmap previewBitmap = Bitmap.createBitmap(bitmap, item.xOffset,
                        item.yOffset, item.width, item.height);

                if (source != Conaco.Source.MEMORY) {
                    Drawable[] layers = new Drawable[2];
                    layers[0] = new ColorDrawable(Color.TRANSPARENT);
                    layers[1] = new BitmapDrawable(mImageViews[i].getResources(), previewBitmap);
                    TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
                    mImageViews[i].setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(300);
                } else {
                    mImageViews[i].setImageBitmap(previewBitmap);
                }
            }

            bitmapHolder.release();

            mLoadImageTasks.remove(this);
        }

        @Override
        public void setDrawable(Drawable drawable) {
            // Empty
        }

        @Override
        public void setTaskId(int id) {
            mTaskId = id;
        }

        @Override
        public int getTaskId() {
            return mTaskId;
        }

        @Override
        public void onFailure() {
            mLoadImageTasks.remove(this);

            // Add long click to retry
            RetryTask retryTask = new RetryTask(mConaco, mRow, mImageViews);
            for (ImageView imageView : mImageViews) {
                imageView.setImageResource(R.drawable.retry_load_image);
                imageView.setOnLongClickListener(retryTask);
            }
        }

        @Override
        public void onCancel() {
            // Empty
        }
    }

    class RetryTask implements View.OnLongClickListener {

        private Conaco mConaco;
        private Row mRow;
        private ImageView[] mImageViews;

        public RetryTask(Conaco conaco, Row row, ImageView[] imageViews) {
            mConaco = conaco;
            mRow = row;
            mImageViews = imageViews;
        }

        @Override
        public boolean onLongClick(View v) {
            for (ImageView imageView : mImageViews) {
                imageView.setImageDrawable(null);
                imageView.setOnLongClickListener(null);
                imageView.setLongClickable(false);
            }

            LoadImageTask loadImageTask = new LoadImageTask(mConaco, mRow, mImageViews);
            mLoadImageTasks.add(loadImageTask);

            String key;
            if (mRow.imageIndex != -1) {
                key = EhImageKeyFactory.getNormalPreviewKey(getGid(), mRow.imageIndex);
            } else {
                key = mRow.imageUrl;
            }
            mConaco.load(loadImageTask, key, mRow.imageUrl);

            return true;
        }
    }

    class Row {

        public String imageUrl;
        public List<Item> itemArray = new ArrayList<>();
        public int imageIndex;

        public Row(String imageUrl) {
            this.imageUrl = imageUrl;

            int index1 = imageUrl.lastIndexOf('-');
            int index2 = imageUrl.lastIndexOf('.');

            try {
                imageIndex = Integer.parseInt(imageUrl.substring(index1 + 1, index2));
            } catch (Exception e) {
                Say.w(TAG, "Can't get normal preview index", e);
                imageIndex = -1;
            }
        }

        public void addItem(int xOffset, int yOffset, int width, int height,
                String url) {
            itemArray.add(new Item(xOffset, yOffset, width, height, url));
        }
    }

    class Item {

        public int xOffset;
        public int yOffset;
        public int width;
        public int height;
        public String url;

        public Item(int xOffset, int yOffset, int width, int height, String url) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.width = width;
            this.height = height;
            this.url = url;
        }
    }
}
