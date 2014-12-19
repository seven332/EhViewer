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

package com.hippo.ehviewer.app;

import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.ViewUtils;
import com.hippo.ehviewer.widget.FileExplorerView;

public class DirSelectDialog extends MaterialAlertDialog {

    private final FileExplorerView mFileExplorerView;

    protected DirSelectDialog(Builder builder, FileExplorerView fileExplorerView) {
        super(builder);

        mFileExplorerView = fileExplorerView;
    }

    public static DirSelectDialog create(Builder builder, String path) {
        View view = ViewUtils.inflateDialogView(R.layout.dir_selection, builder.mDarkTheme);
        final FileExplorerView fileExplorerView = (FileExplorerView) view.findViewById(R.id.file_list);
        final TextView warningTextView = (TextView) view.findViewById(R.id.warning);
        final TextView pathTextView = (TextView) view.findViewById(R.id.path);

        if (path != null)
            fileExplorerView.setPath(path);
        fileExplorerView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                fileExplorerView.onItemClick(parent, view, position, id);
                pathTextView.setText(fileExplorerView.getCurPath());
                if (fileExplorerView.canWrite())
                    warningTextView.setVisibility(View.GONE);
                else
                    warningTextView.setVisibility(View.VISIBLE);
            }
        });

        pathTextView.setText(fileExplorerView.getCurPath());
        if (fileExplorerView.canWrite())
            warningTextView.setVisibility(View.GONE);
        else
            warningTextView.setVisibility(View.VISIBLE);

        builder.setView(view, false,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp2pix(360)));

        return new DirSelectDialog(builder, fileExplorerView);
    }

    public String getCurrentPath() {
        return mFileExplorerView.getCurPath();
    }
}
