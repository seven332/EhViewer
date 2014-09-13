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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hippo.ehviewer.R;

public class FileExplorerView extends ListView
        implements AdapterView.OnItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = "FileExplorerView";

    private static final File PARENT_DIR = null;
    private static final String PARENT_DIR_NAME = "..";

    private static final DirFileFilter dff = new DirFileFilter();
    private static final FileSort fs = new FileSort();

    private Context mContext;

    private Filter mFilter;
    private static final Filter[] sFilterArray = { Filter.ALL,
        Filter.DIR};

    private File mCurDir;

    private BaseAdapter mAdapter;
    private List<File> mFileList;

    public enum Filter {
        ALL(0),
        DIR(1);

        Filter(int ni) {
            nativeInt = ni;
        }

        final int nativeInt;
    }

    public FileExplorerView(Context context) {
        super(context);
        init(context);
    }

    public FileExplorerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public FileExplorerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        mContext = context;

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.FileExplorerView, defStyle, 0);

        int modeIndex = a.getInt(R.styleable.FileExplorerView_filter, 0);
        if (modeIndex >=0 && modeIndex < sFilterArray.length)
            mFilter = sFilterArray[modeIndex];
        else
            mFilter = Filter.ALL;

        String path = a.getString(R.styleable.FileExplorerView_path);
        if (path != null) {
            mCurDir = new File(path);
            if (!mCurDir.isDirectory())
                mCurDir = null;
        }
        if (mCurDir == null)
            // TODO what if no sdcard
            mCurDir = Environment.getExternalStorageDirectory();

        a.recycle();

        init();
    }

    private void init(Context context) {
        mContext = context;

        mFilter = Filter.ALL;
        mCurDir = Environment.getExternalStorageDirectory();

        init();
    }

    private void init() {
        mFileList = new ArrayList<File>();
        getDirList();
        mAdapter = new FileAdapter();
        setAdapter(mAdapter);
        setOnItemClickListener(this);
    }

    private void getDirList() {

        File[] files = null;
        if (mFilter == Filter.DIR)
            files = mCurDir.listFiles(dff);
        else
            files = mCurDir.listFiles();

        mFileList.clear();
        if (mCurDir.getParent() != null)
            mFileList.add(PARENT_DIR);
        if (files != null) {
            for (File file : files)
                mFileList.add(file);
        }
        // sort
        Collections.sort(mFileList, fs);
    }

    /**
     * Reget dir list and go to top in the list
     */
    public void refresh() {
        getDirList();
        mAdapter.notifyDataSetChanged();
        // Go to top
        setSelection(0);
    }

    /**
     * Set mode
     * @param mode
     */
    public void setMode(Filter mode) {
        mFilter = mode;
        refresh();
    }

    /**
     * @param path The path you want to set for current path
     * @return True if the path is refer to a directory
     */
    public boolean setPath(String path) {
        File dir = new File(path);
        if (!dir.isDirectory())
            return false;

        mCurDir = dir;
        refresh();
        return true;
    }

    /**
     * True if current dir can write
     * @return
     */
    public boolean canWrite() {
        return mCurDir.canWrite();
    }

    /**
     * Get current path
     * @return
     */
    public String getCurPath() {
        return mCurDir.getPath();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        File newFile;
        File file = mFileList.get(position);
        if (file == PARENT_DIR)
            newFile = mCurDir.getParentFile();
        else
            newFile = file;
        mCurDir = newFile;
        refresh();
    }

    class FileAdapter extends BaseAdapter {
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        @Override
        public int getCount() {
            return mFileList.size();
        }

        @Override
        public Object getItem(int position) {
            return mFileList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.file_explorer_item, parent, false);

            TextView tv = (TextView)convertView.findViewById(R.id.text);
            File file = mFileList.get(position);
            String name;
            if (file == PARENT_DIR)
                name = PARENT_DIR_NAME;
            else
                name = file.getName();
            tv.setText(name);

            return convertView;
        }
    }

    static class DirFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }

    static class FileSort implements Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            if (lhs == null)
                return Integer.MIN_VALUE;
            else if (rhs == null)
                return Integer.MAX_VALUE;
            else
                return lhs.getName().compareToIgnoreCase(rhs.getName());
        }
    }
}
