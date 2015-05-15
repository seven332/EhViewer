/*
 * Copyright (C) 2014-2015 Hippo Seven
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

package com.hippo.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hippo.ehviewer.R;

public class DrawerListView extends ListView {

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_ACTIVATED_POSITION = "activated_position";

    private Context mContext;
    private DrawerListAdapter mAdapter;

    private int mActivatedPosition = -1;
    private int mDrawableSize;

    public DrawerListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawerListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mAdapter = new DrawerListAdapter();
        setAdapter(mAdapter);
        setDivider(context.getResources().getDrawable(R.drawable.transparent));
        setDividerHeight(0);

        Resources resources = context.getResources();
        mDrawableSize = resources.getDimensionPixelOffset(R.dimen.drawer_list_drawable_size);
    }

    /**
     * Drawable should be 24dp
     */
    public void setData(Drawable[] drawableArray, CharSequence[] titleArray) {
        mAdapter.setData(drawableArray, titleArray);
    }

    public View getViewByPosition(int pos) {
        final int firstListItemPosition = getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + getChildCount() - 1;

        if (pos >= firstListItemPosition && pos <= lastListItemPosition) {
            final int childIndex = pos - firstListItemPosition;
            return getChildAt(childIndex);
        } else {
            return null;
        }
    }

    public void setActivatedPosition(int position) {
        if (mActivatedPosition != position) {
            int oldPosition = mActivatedPosition;
            mActivatedPosition = position;
            View oldView = getViewByPosition(oldPosition);
            View newView = getViewByPosition(position);
            if (oldView != null) {
                oldView.setActivated(false);
            }
            if (newView != null) {
                newView.setActivated(true);
            }
        }
    }

    public int getActivatedPosition() {
        return mActivatedPosition;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        state.putInt(STATE_KEY_ACTIVATED_POSITION, mActivatedPosition);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_SUPER));
            setActivatedPosition(savedState.getInt(STATE_KEY_ACTIVATED_POSITION));
        }
    }

    private class DrawerListAdapter extends BaseAdapter {

        private Drawable[] mDrawableArray;
        private CharSequence[] mTitleArray;

        private void setData(Drawable[] drawableArray, CharSequence[] titleArray) {
            if (drawableArray != null && titleArray != null) {
                if (drawableArray.length != titleArray.length)
                    throw new IllegalStateException("drawableArray.length != titleArray.length");
            } else if (drawableArray == null && titleArray == null) {
                throw new IllegalStateException("drawableArray and titleArray should be all null or not null");
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
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_drawer_list, parent, false);
            }

            // Handle activated
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (position == mActivatedPosition) {
                    convertView.setActivated(true);
                } else {
                    convertView.setActivated(false);
                }
            }

            TextView tv = (TextView) convertView;
            CharSequence t = mTitleArray[position];
            tv.setText(t);
            Drawable d = mDrawableArray[position];
            if (d != null) {
                d.setBounds(0, 0, mDrawableSize, mDrawableSize);
                tv.setCompoundDrawables(d, null, null, null);
            } else {
                tv.setCompoundDrawables(null, null, null, null);
            }

            return convertView;
        }
    }
}
