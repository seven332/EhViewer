/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Ui;

public class DrawerListView extends ListView {

    private Context mContext;
    private DrawerListAdapter mAdapter;

    private static final int DRAWABLE_SIDE = Ui.dp2pix(24);

    public DrawerListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawerListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DrawerListView(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mAdapter = new DrawerListAdapter();
        setAdapter(mAdapter);
    }

    public void setData(Drawable[] drawableArray, CharSequence[] titleArray) {
        mAdapter.setData(drawableArray, titleArray);
    }

    private class DrawerListAdapter extends BaseAdapter {

        private Drawable[] mDrawableArray;
        private CharSequence[] mTitleArray;

        private void setData(Drawable[] drawableArray, CharSequence[] titleArray) {
            if (drawableArray != null && titleArray != null) {
                if (drawableArray.length != titleArray.length)
                    throw new java.lang.IllegalStateException("drawableArray.length != titleArray.length");
            } else if (drawableArray == null && titleArray == null) {
                throw new java.lang.IllegalStateException("drawableArray and titleArray should be all null or not null");
            }
            mDrawableArray = drawableArray;
            mTitleArray = titleArray;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTitleArray == null ? 0 : mTitleArray.length;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(mContext).inflate(R.layout.drawer_list_item, parent, false);

            TextView tv = (TextView) convertView;
            Drawable d = mDrawableArray[position];
            CharSequence t = mTitleArray[position];
            tv.setText(t);
            d.setBounds(0, 0, DRAWABLE_SIDE, DRAWABLE_SIDE);
            tv.setCompoundDrawables(d, null, null, null);

            return convertView;
        }
    }
}
