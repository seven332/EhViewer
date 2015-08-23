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

package com.hippo.ehviewer.ui.scene.preference;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Settings;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.AppbarScene;
import com.hippo.scene.Scene;
import com.hippo.scene.SimpleDialog;
import com.hippo.unifile.UniFile;
import com.hippo.widget.DirExplorer;
import com.hippo.yorozuya.Messenger;
import com.hippo.yorozuya.ViewUtils;

import java.io.File;

public class DirPickScene extends AppbarScene implements View.OnClickListener,
        Scene.ActivityResultListener {

    public static final String ACTION_IMAGE_DOWNLOAD_LOCATION = "com.hippo.ehviewer.dirpick.action.IMAGE_DOWNLOAD_LOCATION";
    public static final String ACTION_ARCHIVE_DOWNLOAD_LOCATION = "com.hippo.ehviewer.dirpick.action.ARCHIVE_DOWNLOAD_LOCATION";

    private String mAction;

    private View mDocument;
    private View mDocumentHelp;
    private View mOk;
    private DirExplorer mDirExplorer;

    private int mDirExplorerOriginalPaddingBottom;

    private File getCurrentFile() {
        UniFile dir = null;
        if (ACTION_IMAGE_DOWNLOAD_LOCATION.equals(mAction)) {
            dir = Settings.getImageDownloadLocation();
        } else if (ACTION_ARCHIVE_DOWNLOAD_LOCATION.equals(mAction)) {
            dir = Settings.getArchiveDownloadLocation();
        }
        if (dir != null) {
            Uri uri = dir.getUri();
            if (UniFile.isFileUri(uri)) {
                return new File(uri.getPath());
            }
        }
        return null;
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setContentView(R.layout.scene_dir_pick);
        setTitle(R.string.dir_pick);
        setIcon(R.drawable.ic_arrow_left_dark_x24);

        Announcer announcer = getAnnouncer();
        if (announcer != null) {
            mAction = announcer.getAction();
        }

        ViewGroup documentGroup = (ViewGroup) findViewById(R.id.document_group);
        mDocument = documentGroup.findViewById(R.id.document);
        mDocumentHelp = documentGroup.findViewById(R.id.document_help);
        mOk = findViewById(R.id.ok);
        mDirExplorer = (DirExplorer) findViewById(R.id.dir_explorer);

        mDirExplorerOriginalPaddingBottom = mDirExplorer.getPaddingBottom();
        mDirExplorer.setHasFixedSize(true);
        mDirExplorer.setClipToPadding(false);
        File file = getCurrentFile();
        if (file != null) {
            mDirExplorer.setCurrentFile(file);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            documentGroup.setVisibility(View.GONE);
        }

        RippleSalon.addRipple(mDocument, false);
        RippleSalon.addRipple(mOk, false);

        mDocument.setOnClickListener(this);
        mDocumentHelp.setOnClickListener(this);
        mOk.setOnClickListener(this);
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        super.onGetFitPaddingBottom(b);

        mDirExplorer.setPadding(mDirExplorer.getPaddingLeft(),
                mDirExplorer.getPaddingTop(), mDirExplorer.getPaddingRight(),
                mDirExplorerOriginalPaddingBottom + b);
    }

    @Override
    public void onIconClick() {
        finish();
    }

    private void save(UniFile uniFile) {
        if (ACTION_IMAGE_DOWNLOAD_LOCATION.equals(mAction)) {
            Settings.putImageDownloadLocation(uniFile);
            Messenger.getInstance().notify(Constants.MESSENGER_ID_IMAGE_DOWNLOAD_LOCATION, uniFile);
        } else if (ACTION_ARCHIVE_DOWNLOAD_LOCATION.equals(mAction)) {
            Settings.putArchiveDownloadLocation(uniFile);
            Messenger.getInstance().notify(Constants.MESSENGER_ID_ARCHIVE_DOWNLOAD_LOCATION, uniFile);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        if (v == mDocument) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, this);
        } else if (v == mDocumentHelp) {
            int[] location = new int[2];
            ViewUtils.getCenterInAncestor(mDocumentHelp, location, getSceneView());
            new SimpleDialog.Builder(getStageActivity()).setTitle(R.string.document)
                    .setMessage(R.string.document_help)
                    .setStartPoint(location[0], location[1])
                    .show(this);
        } else if (v == mOk) {
            UniFile dir = UniFile.fromFile(mDirExplorer.getCurrentFile());
            if (dir == null) {
                Toast.makeText(getStageActivity(), R.string.em_cant_get_folder, Toast.LENGTH_SHORT).show();
            } else if (!dir.canWrite()) {
                Toast.makeText(getStageActivity(), R.string.em_cant_write_folder, Toast.LENGTH_SHORT).show();
            } else {
                save(dir);
                finish();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onGetResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri treeUri = data.getData();
            getStageActivity().getContentResolver().takePersistableUriPermission(
                    treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            UniFile dir = UniFile.fromTreeUri(getStageActivity(), treeUri);
            if (dir != null) {
                save(dir);
                finish();
            } else {
                Toast.makeText(getStageActivity(), R.string.em_cant_parse_document_uri, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
