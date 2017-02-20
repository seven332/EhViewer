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

package com.hippo.ehviewer.controller.base;

/*
 * Created by Hippo on 2/13/2017.
 */

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.hippo.ehviewer.changehandler.DialogChangeHandler;

/**
 * {@code DialogController} works like {@link android.app.DialogFragment}.
 */
public abstract class DialogController extends RefWatchingController
    implements DialogInterface.OnDismissListener{

  private static final String SAVED_DIALOG_STATE_TAG = "DialogController:savedDialogState";

  private Dialog dialog;
  private boolean dismissByMe;

  /**
   * Shows this DialogController to the router.
   */
  public void show(Router router) {
    router.pushController(
        RouterTransaction.with(this)
            .pushChangeHandler(new DialogChangeHandler())
            .popChangeHandler(new DialogChangeHandler())
    );
  }

  /**
   * Dismisses the dialog if it's shown.
   */
  public void dismiss() {
    if (dialog != null) {
      dialog.dismiss();
    }
  }

  /**
   * Creates a dialog to show.
   * <p>
   * Don't set OnDismissListener,
   * implements {@link #onDismiss(DialogInterface)} instead.
   */
  @NonNull
  public abstract Dialog onCreateDialog();

  @NonNull
  @Override
  protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    dialog = onCreateDialog();
    dialog.setOnDismissListener(dialogInterface -> {
      onDismiss(dialogInterface);
      if (!dismissByMe) {
        getRouter().popController(this);
      } else {
        dismissByMe = false;
      }
      dialog = null;
    });

    // onCreateView() needs a view, just give it a dump view
    return new DumpView(inflater.getContext());
  }

  @Override
  protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    dialog.show();
  }

  @Override
  protected void onDetach(@NonNull View view) {
    super.onDetach(view);
    dismissByMe = true;
    dialog.dismiss();
  }

  @Override
  protected void onRestoreViewState(@NonNull View view, @NonNull Bundle savedViewState) {
    super.onRestoreViewState(view, savedViewState);
    if (dialog != null) {
      Bundle saved = savedViewState.getBundle(SAVED_DIALOG_STATE_TAG);
      if (saved != null) {
        dialog.onRestoreInstanceState(saved);
      }
    }
  }

  @Override
  protected void onSaveViewState(@NonNull View view, @NonNull Bundle outState) {
    super.onSaveViewState(view, outState);
    if (dialog != null) {
      outState.putBundle(SAVED_DIALOG_STATE_TAG, dialog.onSaveInstanceState());
    }
  }

  @Override
  public void onDismiss(DialogInterface dialog) {}

  /**
   * {@code DumpView} represents no view.
   */
  private static final class DumpView extends View {
    public DumpView(Context context) {
      super(context);
    }
  }
}
