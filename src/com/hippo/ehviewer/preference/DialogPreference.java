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

package com.hippo.ehviewer.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.ehviewer.app.MaterialAlertDialog;

public abstract class DialogPreference extends android.preference.DialogPreference implements MaterialAlertDialog.OnClickListener {

    private MaterialAlertDialog.Builder mBuilder;

    /** The dialog, if it is showing. */
    private Dialog mDialog;
    /** Which button was clicked. */
    private int mWhichButtonClicked;

    public DialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onClick() {
        if (mDialog != null && mDialog.isShowing()) return;

        showDialog(null);
    }

    @Override
    public boolean onClick(MaterialAlertDialog dialog, int which) {
        mWhichButtonClicked = which;
        return true;
    }

    protected abstract void onPrepareDialogBuilder(MaterialAlertDialog.Builder builder);

    @Override
    protected void showDialog(Bundle state) {
        Context context = getContext();

        mWhichButtonClicked = MaterialAlertDialog.POSITIVE;

        mBuilder = new MaterialAlertDialog.Builder(context)
            .setTitle(getDialogTitle())
            //.setIcon(mDialogIcon) // TODO Not support now
            .setPositiveButton(getPositiveButtonText())
            .setNegativeButton(getNegativeButtonText())
            .setButtonListener(this);


        View contentView = onCreateDialogView();
        if (contentView != null) {
            onBindDialogView(contentView);
            mBuilder.setView(contentView);
        } else {
            mBuilder.setMessage(getDialogMessage());
        }

        onPrepareDialogBuilder(mBuilder);

        // Create the dialog
        final Dialog dialog = mDialog = mBuilder.create();
        onDialogCreate(dialog);
        if (state != null) {
            dialog.onRestoreInstanceState(state);
        }
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    protected void onDialogCreate(Dialog dialog) {

    }

    @Override
    protected abstract void onBindDialogView(View view);

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDialog = null;
        onDialogClosed(mWhichButtonClicked == MaterialAlertDialog.POSITIVE);
    }

    @Override
    public Dialog getDialog() {
        return mDialog;
    }
}
