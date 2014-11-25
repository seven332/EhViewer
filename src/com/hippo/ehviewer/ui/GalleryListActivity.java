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

package com.hippo.ehviewer.ui;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.etsy.android.grid.StaggeredGridView;
import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.SimpleSuggestionProvider;
import com.hippo.ehviewer.UpdateHelper;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.cardview.CardViewSalon;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.ListUrls;
import com.hippo.ehviewer.data.Tag;
import com.hippo.ehviewer.drawable.FreeMaterialDrawable;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable.Stroke;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.tile.TileSalon;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.DialogUtils;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.util.ViewUtils;
import com.hippo.ehviewer.widget.CategoryTable;
import com.hippo.ehviewer.widget.DrawerListView;
import com.hippo.ehviewer.widget.FitWindowView;
import com.hippo.ehviewer.widget.GalleryListView;
import com.hippo.ehviewer.widget.GalleryListView.OnGetListListener;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.MaterialToast;
import com.hippo.ehviewer.widget.RatingView;
import com.hippo.ehviewer.widget.SlidingDrawerLayout;
import com.hippo.ehviewer.widget.SuggestionHelper;
import com.hippo.ehviewer.widget.SuggestionTextView;
import com.hippo.ehviewer.widget.TagListView;
import com.hippo.ehviewer.widget.TagsAdapter;
import com.hippo.ehviewer.windowsanimate.WindowsAnimate;

/*
 *
 *    Footer 显示产生的 refresh     手动 pull 显示产生的 refresh     按键或其他产生的 refresh
 *            |                                  |                             |
 *            |                                  |                             |
 *            ∨                                 ∨                            ∨
 *     触发 onFootRefresh               触发 onRefreshStart          setHeaderRefresh(true)
 *            |                                  |                             |
 *            |<---------------------------------|-----------------------------|
 *            ∨
 *        set listUrl
 *        set getMode
 *            |
 *            |
 *            ∨
 *          getList
 */

/**
 * Make sure when getting list pulllistview is refreshing
 *
 * @author Hippo
 */
public class GalleryListActivity extends AbsActivity implements View.OnClickListener,
        SearchView.OnQueryTextListener, SearchView.OnFocusChangeListener,
        SlidingDrawerLayout.DrawerListener, EhClient.OnLoginListener,
        GalleryListView.GalleryListViewHelper, FitWindowView.OnFitSystemWindowsListener {

    @SuppressWarnings("unused")
    private static final String TAG = GalleryListActivity.class.getSimpleName();

    private static int RESULT_LOAD_SEARCH_IMAGE = 1;

    public static final String ACTION_GALLERY_LIST = "com.hippo.ehviewer.intent.action.GALLERY_LIST";

    public static final String KEY_MODE = "mode";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_TAG = "tag";
    public static final String KEY_UPLOADER = "uploader";
    public static final String KEY_IMAGE_KEY = "image_key";
    public static final String KEY_IMAGE_URL = "image_url";

    public static final int LIST_MODE_DETAIL = 0x0;
    public static final int LIST_MODE_THUMB = 0x1;

    private AppContext mAppContext;
    private EhClient mClient;
    private Resources mResources;
    private WindowsAnimate mWindowsAnimate;
    private SuggestionHelper mSuggestions;
    private ActionBar mActionBar;
    private int mThemeColor;

    private MaterialIndicatorDrawable mMaterialIndicator;
    private boolean mDirection;

    private SlidingDrawerLayout mDrawerLayout;
    private View mLeftMenu;
    private View mRightMenu;
    private FitWindowView mStandard;
    private GalleryListView mGalleryListView;

    private StaggeredGridView mStaggeredGridView;

    private View mUserPanel;
    private DrawerListView mMenuList;
    private ImageView mAvatar;
    private TextView mUsernameView;
    private Button mLoginButton;
    private Button mRegisterButton;
    private Button mLogoutButton;
    private View mWaitLogView;

    private TagListView mQuickSearchList;
    private View mQuickSearchTip;

    private TextView mSearchImageText;

    private SearchView mSearchView;
    private MenuItem mSearchItem;

    private BaseAdapter mAdapter;

    private TagsAdapter tagsAdapter;

    private ListUrls lus;
    private final ArrayList<String> listMenuTag = new ArrayList<String>();

    private Data mData;

    private int longClickItemIndex;
    private boolean mShowDrawer;

    private AlertDialog loginDialog;
    private AlertDialog mSearchDialog;
    private AlertDialog longClickDialog;

    // Modify tag
    private String newTagName = null;

    // Double click back exit
    private long curBackTime = 0;
    private static final int BACK_PRESSED_INTERVAL = 2000;

    private String mTitle;

    private int mListMode;

    private int mListDetailThumbWidth;
    private int mListDetailThumbHeight;

    // private int mListModeThumbHeight;

    private void toRegister() {
        Uri uri = Uri.parse("http://forums.e-hentai.org/index.php?act=Reg");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    @Override
    public void onSuccess() {
        setUserPanel();
    }

    @Override
    public void onGetAvatar(final int code) {
        GalleryListActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (code) {
                case EhClient.GET_AVATAR_OK:
                    setUserPanel();
                    break;
                case EhClient.NO_AVATAR:
                    MaterialToast.showToast(R.string.no_avatar);
                    break;
                case EhClient.GET_AVATAR_ERROR:
                default:
                    MaterialToast.showToast(R.string.get_avatar_error);
                    break;
                }
            }
        });
    }

    @Override
    public void onFailure(String eMesg) {
        setUserPanel();
        MaterialToast.showToast(eMesg);
        if (!GalleryListActivity.this.isFinishing())
            loginDialog.show();
    }

    @SuppressLint("InflateParams")
    private AlertDialog createLoginDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.login, null);

        return new MaterialAlertDialog.Builder(this).setCancelable(false).setTitle(R.string.login).setView(view, true)
                .setPositiveButton(android.R.string.ok).setNegativeButton(android.R.string.cancel)
                .setNeutralButton(R.string.register).setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        switch (which) {
                        case MaterialAlertDialog.POSITIVE:
                            setUserPanel(WAIT);
                            String username = ((EditText) loginDialog.findViewById(R.id.username)).getText().toString();
                            String password = ((EditText) loginDialog.findViewById(R.id.password)).getText().toString();
                            mClient.login(username, password, GalleryListActivity.this);
                            return true;
                        case MaterialAlertDialog.NEGATIVE:
                            setUserPanel();
                            return true;
                        case MaterialAlertDialog.NEUTRAL:
                            toRegister();
                            return false;
                        default:
                            return false;
                        }
                    }
                }).create();
    }

    @SuppressLint("InflateParams")
    private AlertDialog createModeDialog() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View cv = inflater.inflate(R.layout.select_mode, null);

        final Spinner modeSpinner = (Spinner) cv.findViewById(R.id.mode_list);
        modeSpinner.setSelection(Config.getMode());
        final Spinner apiModeSpinner = (Spinner) cv.findViewById(R.id.api_mode_list);
        apiModeSpinner.setSelection(Config.getApiMode());
        final Spinner lofiResolutionSpinner = (Spinner) cv.findViewById(R.id.lofi_resolution_list);
        lofiResolutionSpinner.setSelection(Config.getLofiResolution() - 1);

        return new MaterialAlertDialog.Builder(this).setTitle(R.string.mode).setView(cv, true)
                .setPositiveButton(android.R.string.ok).setNegativeButton(android.R.string.cancel)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        if (which == MaterialAlertDialog.POSITIVE) {
                            Config.setMode(modeSpinner.getSelectedItemPosition());
                            Config.setApiMode(apiModeSpinner.getSelectedItemPosition());
                            Config.setLofiResolution(lofiResolutionSpinner.getSelectedItemPosition() + 1);
                        }
                        return true;
                    }
                }).create();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_SEARCH_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String ImagePath = cursor.getString(columnIndex);
            cursor.close();

            mSearchImageText.setVisibility(View.VISIBLE);
            mSearchImageText.setText(ImagePath);
        }

    }

    private void handleSearchView(View view) {
        final SuggestionTextView pet = (SuggestionTextView) view.findViewById(R.id.search_text);
        pet.setSuggestionHelper(mSuggestions);
        CheckBox uploaderCb = (CheckBox) view.findViewById(R.id.checkbox_uploader);
        uploaderCb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    pet.setPrefix("uploader:");
                    pet.setSuggestionHelper(null);
                } else {
                    pet.setPrefix(null);
                    pet.setSuggestionHelper(mSuggestions);
                }
            }
        });

        final View advance = view.findViewById(R.id.filter_advance);
        CheckBox cb = (CheckBox) view.findViewById(R.id.checkbox_advance);
        cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    advance.setVisibility(View.VISIBLE);
                else
                    advance.setVisibility(View.GONE);
            }
        });

        final View selectImage = view.findViewById(R.id.select_image);
        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_SEARCH_IMAGE);
            }
        });
    }

    @SuppressLint("InflateParams")
    private AlertDialog createSearchDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.search, null);
        final View searchNormal = view.findViewById(R.id.search_normal);
        final View searchTag = view.findViewById(R.id.search_tag);
        final View searchImage = view.findViewById(R.id.search_image);
        mSearchImageText = (TextView) view.findViewById(R.id.target_image);
        handleSearchView(view);

        return new MaterialAlertDialog.Builder(this).setTitle(android.R.string.search_go).setView(view, true)
                .setActionButton(R.string.mode).setPositiveButton(android.R.string.ok)
                .setNegativeButton(android.R.string.cancel).setNeutralButton(R.string.add)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(final MaterialAlertDialog dialog, int which) {
                        switch (which) {
                        case MaterialAlertDialog.ACTION:
                            if (searchNormal.getVisibility() == View.VISIBLE) {
                                searchNormal.setVisibility(View.GONE);
                                searchTag.setVisibility(View.VISIBLE);
                            } else if (searchTag.getVisibility() == View.VISIBLE) {
                                searchTag.setVisibility(View.GONE);
                                searchImage.setVisibility(View.VISIBLE);
                            } else if (searchImage.getVisibility() == View.VISIBLE) {
                                searchImage.setVisibility(View.GONE);
                                searchNormal.setVisibility(View.VISIBLE);
                            }
                            return false;
                        case MaterialAlertDialog.POSITIVE:
                            ListUrls listUrls = getLus(mSearchDialog);
                            if (listUrls == null)
                                return true;

                            lus = listUrls;
                            mGalleryListView.refresh();
                            mDrawerLayout.closeDrawers();

                            // Get title
                            String search = lus.getSearch();
                            switch (lus.getMode()) {
                            case ListUrls.MODE_IMAGE_SEARCH:
                                mTitle = getString(R.string.image_search);
                                break;
                            case ListUrls.MODE_UPLOADER:
                                mTitle = search;
                                break;
                            case ListUrls.MODE_TAG:
                                mTitle = lus.getTag();
                                break;
                            case ListUrls.MODE_POPULAR:
                                mTitle = getString(R.string.whatshot);
                            case ListUrls.MODE_NORMAL:
                            default:
                                if (search == null || search.isEmpty())
                                    mTitle = getString(android.R.string.search_go);
                                else
                                    mTitle = search;
                                break;
                            }
                            setTitle(mTitle);
                            return true;
                        case MaterialAlertDialog.NEUTRAL:
                            if (searchImage.getVisibility() == View.VISIBLE)
                                return false;
                            createSetNameDialog(null, null, new OnSetNameListener() {
                                @Override
                                public void onSetVaildName(String newName) {
                                    mData.addTag(new Tag(newName, getLus(mSearchDialog)));
                                    listMenuTag.add(newName);
                                    tagsAdapter.addId(newName);
                                    tagsAdapter.notifyDataSetChanged();

                                    dialog.dismiss();
                                }
                            }).show();
                            return false;
                        default:
                            return true;
                        }
                    }
                }).create();
    }

    private ListUrls getLus(AlertDialog dialog) {
        return getLus(dialog.findViewById(R.id.custom));
    }

    private ListUrls getLus(View view) {
        ListUrls lus = null;

        View searchNormal = view.findViewById(R.id.search_normal);
        View searchTag = view.findViewById(R.id.search_tag);
        View searchImage = view.findViewById(R.id.search_image);

        if (searchNormal.getVisibility() == View.VISIBLE) {
            int type;
            CategoryTable ct = (CategoryTable) view.findViewById(R.id.category_table);
            type = ct.getCategory();
            EditText et = (EditText) view.findViewById(R.id.search_text);
            lus = new ListUrls(type, et.getText().toString());

            CheckBox uploaderCb = (CheckBox) view.findViewById(R.id.checkbox_uploader);
            if (uploaderCb.isChecked()) {
                lus.setMode(ListUrls.MODE_UPLOADER);
            }

            CheckBox cb = (CheckBox) view.findViewById(R.id.checkbox_advance);
            if (cb.isChecked()) {
                CheckBox checkImageSname = (CheckBox) view.findViewById(R.id.checkbox_sname);
                CheckBox checkImageStags = (CheckBox) view.findViewById(R.id.checkbox_stags);
                CheckBox checkImageSdesc = (CheckBox) view.findViewById(R.id.checkbox_sdesc);
                CheckBox checkImageStorr = (CheckBox) view.findViewById(R.id.checkbox_storr);
                CheckBox checkImageSto = (CheckBox) view.findViewById(R.id.checkbox_sto);
                CheckBox checkImageSdt1 = (CheckBox) view.findViewById(R.id.checkbox_sdt1);
                CheckBox checkImageSdt2 = (CheckBox) view.findViewById(R.id.checkbox_sdt2);
                CheckBox checkImageSh = (CheckBox) view.findViewById(R.id.checkbox_sh);

                int advType = 0;
                if (checkImageSname.isChecked())
                    advType |= ListUrls.SNAME;
                if (checkImageStags.isChecked())
                    advType |= ListUrls.STAGS;
                if (checkImageSdesc.isChecked())
                    advType |= ListUrls.SDESC;
                if (checkImageStorr.isChecked())
                    advType |= ListUrls.STORR;
                if (checkImageSto.isChecked())
                    advType |= ListUrls.STO;
                if (checkImageSdt1.isChecked())
                    advType |= ListUrls.STD1;
                if (checkImageSdt2.isChecked())
                    advType |= ListUrls.STD2;
                if (checkImageSh.isChecked())
                    advType |= ListUrls.SH;
                CheckBox checkImageSr = (CheckBox) view.findViewById(R.id.checkbox_sr);
                if (checkImageSr.isChecked()) {
                    Spinner spinnerMinRating = (Spinner) view.findViewById(R.id.spinner_min_rating);
                    lus.setAdvance(advType, spinnerMinRating.getSelectedItemPosition() + 2);
                } else
                    lus.setAdvance(advType);
            }

            // For tag search
        } else if (searchTag.getVisibility() == View.VISIBLE) {
            EditText et = (EditText) view.findViewById(R.id.search_tag_text);
            lus = new ListUrls();
            lus.setTag(et.getText().toString());

            // For image search
        } else if (searchImage.getVisibility() == View.VISIBLE) {
            String filePath = (String) mSearchImageText.getText();
            File file = new File(filePath);
            if (!file.exists()) {
                MaterialToast.showToast(R.string.image_not_exist);
            } else {
                if (!file.canRead()) {
                    MaterialToast.showToast(R.string.image_not_readable);
                } else {

                    CheckBox similar = (CheckBox) view.findViewById(R.id.checkboxSimilar);
                    CheckBox covers = (CheckBox) view.findViewById(R.id.checkboxCovers);
                    CheckBox exp = (CheckBox) view.findViewById(R.id.checkboxExp);

                    lus = new ListUrls();
                    lus.setSearchFile(file, (similar.isChecked() ? EhClient.IMAGE_SEARCH_USE_SIMILARITY_SCAN : 0)
                            | (covers.isChecked() ? EhClient.IMAGE_SEARCH_ONLY_SEARCH_COVERS : 0)
                            | (exp.isChecked() ? EhClient.IMAGE_SEARCH_SHOW_EXPUNGED : 0));
                }
            }
        }

        return lus;
    }

    private AlertDialog createLongClickDialog() {
        return new MaterialAlertDialog.Builder(this).setTitle(R.string.what_to_do)
                .setItems(R.array.list_item_long_click, new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int position) {
                        final GalleryInfo gi = mGalleryListView.getGalleryInfo(longClickItemIndex);
                        switch (position) {
                        case 0: // Add favourite item
                            Favorite.addToFavorite(GalleryListActivity.this, gi);
                            break;
                        case 1:
                            Intent it = new Intent(GalleryListActivity.this, DownloadService.class);
                            startService(it);
                            mAppContext.getDownloadServiceConnection().getService().add(gi);
                            MaterialToast.showToast(R.string.toast_add_download);
                            break;
                        }
                        return true;
                    }
                }).setNegativeButton(android.R.string.cancel).create();
    }

    @SuppressLint("InflateParams")
    private AlertDialog createJumpDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.jump, null);
        TextView tv = (TextView) view.findViewById(R.id.list_jump_sum);
        // For lofi, can not get page num, so use Integer.MAX_VALUE
        tv.setText(String.format(getString(R.string.jump_summary), mGalleryListView.getCurPage() + 1,
                mGalleryListView.getPageNum() == Integer.MAX_VALUE ? getString(R.string._unknown) :
                        String.valueOf(mGalleryListView.getPageNum())));
        tv = (TextView) view.findViewById(R.id.list_jump_to);
        tv.setText(R.string.jump_to);
        final EditText et = (EditText) view.findViewById(R.id.list_jump_edit);

        return new MaterialAlertDialog.Builder(this).setTitle(R.string.jump).setView(view, true)
                .setPositiveButton(android.R.string.ok).setNegativeButton(android.R.string.cancel)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        if (which == MaterialAlertDialog.POSITIVE) {
                            int targetPage;
                            try {
                                targetPage = Integer.parseInt(et.getText().toString()) - 1;
                                if (targetPage < 0 || targetPage >= mGalleryListView.getPageNum())
                                    throw new Exception();
                            } catch (Exception e) {
                                MaterialToast.showToast(R.string.toast_invalid_page);
                                return false;
                            }
                            mGalleryListView.jumpTo(targetPage);
                            return true;
                        } else {
                            return true;
                        }
                    }
                }).create();
    }

    private interface OnSetNameListener {
        public void onSetVaildName(String newName);
    }

    /**
     * Create a set name dialog
     *
     * @param hint
     *            Text to set in edittext first
     * @param oldStr
     *            string can be oldstr, even it is in listMenuTitle
     * @param listener
     *            what to do when set right text
     */
    @SuppressLint("InflateParams")
    private AlertDialog createSetNameDialog(final String hint, final String oldStr, final OnSetNameListener listener) {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.set_name, null);
        final EditText et = (EditText) view.findViewById(R.id.set_name_edit);
        if (hint != null)
            et.setText(hint);

        return new MaterialAlertDialog.Builder(this).setTitle(R.string.add_tag).setView(view, true)
                .setPositiveButton(android.R.string.ok).setNegativeButton(android.R.string.cancel)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        if (which == MaterialAlertDialog.POSITIVE) {
                            String key = et.getText().toString();
                            if (key.length() == 0) {
                                MaterialToast.showToast(R.string.invalid_input);
                                return false;
                            } else if (listMenuTag.contains(key) && !key.equals(oldStr)) {
                                MaterialToast.showToast("该名称已存在"); // TODO
                                return false;
                            } else {
                                if (listener != null) {
                                    listener.onSetVaildName(key);
                                }
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }).create();
    }

    @SuppressLint("InflateParams")
    private AlertDialog createModifyTagDialog(final int position) {
        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.search, null);
        final View searchNormal = view.findViewById(R.id.search_normal);
        final View searchTag = view.findViewById(R.id.search_tag);
        final View searchImage = view.findViewById(R.id.search_image);
        handleSearchView(view);

        ListUrls listUrls = mData.getTag(position);
        setFilterView(view, listUrls);

        return new MaterialAlertDialog.Builder(this).setTitle(listMenuTag.get(position)).setView(view, true)
                .setActionButton(R.string.mode).setPositiveButton(android.R.string.ok)
                .setNegativeButton(android.R.string.cancel).setNeutralButton(R.string.tag_change_name)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(final MaterialAlertDialog dialog, int which) {
                        switch (which) {
                        case MaterialAlertDialog.ACTION:
                            if (searchNormal.getVisibility() == View.GONE) {
                                searchNormal.setVisibility(View.VISIBLE);
                                searchTag.setVisibility(View.GONE);
                                searchImage.setVisibility(View.GONE);
                            } else {
                                searchNormal.setVisibility(View.GONE);
                                searchTag.setVisibility(View.VISIBLE);
                                searchImage.setVisibility(View.GONE);
                            }
                            return false;
                        case MaterialAlertDialog.POSITIVE:
                            ListUrls listUrls = getLus(view);
                            if (newTagName != null) {
                                mData.setTag(position, new Tag(newTagName, listUrls));
                                tagsAdapter.set(listMenuTag.get(position), newTagName);
                                listMenuTag.set(position, newTagName);
                                tagsAdapter.notifyDataSetChanged();
                                newTagName = null;
                            } else {
                                mData.setTag(position, new Tag(listMenuTag.get(position), listUrls));
                            }
                            return true;
                        case MaterialAlertDialog.NEUTRAL:
                            String hint = newTagName == null ? listMenuTag.get(position) : newTagName;
                            createSetNameDialog(hint, listMenuTag.get(position), new OnSetNameListener() {
                                @Override
                                public void onSetVaildName(String newName) {
                                    if (newName.equals(listMenuTag.get(position))) { // If
                                                                                     // new
                                                                                     // is
                                                                                     // old
                                                                                     // name
                                        dialog.setTitle(listMenuTag.get(position));
                                    } else {
                                        newTagName = newName;
                                        dialog.setTitle(String.format(getString(R.string.new_tag_name), newTagName));
                                    }
                                }
                            }).show();
                            return false;
                        case MaterialAlertDialog.NEGATIVE:
                        default:
                            return true;
                        }
                    }
                }).create();
    }

    private void setFilterView(View view, ListUrls listUrls) {

        View searchNormal = view.findViewById(R.id.search_normal);
        View searchTag = view.findViewById(R.id.search_tag);

        if (listUrls.getMode() == ListUrls.MODE_TAG) {
            searchNormal.setVisibility(View.GONE);
            searchTag.setVisibility(View.VISIBLE);

            EditText et = (EditText) view.findViewById(R.id.search_tag_text);
            et.setText(listUrls.getTag());
        } else {
            searchNormal.setVisibility(View.VISIBLE);
            searchTag.setVisibility(View.GONE);

            int category = listUrls.getCategory();
            CategoryTable ct = (CategoryTable) view.findViewById(R.id.category_table);
            ct.setCategory(category);

            EditText et = (EditText) view.findViewById(R.id.search_text);
            et.setText(listUrls.getSearch());

            // Advance
            CheckBox cb = (CheckBox) view.findViewById(R.id.checkbox_advance);
            cb.setChecked(listUrls.isAdvance());

            CheckBox checkImageSname = (CheckBox) view.findViewById(R.id.checkbox_sname);
            CheckBox checkImageStags = (CheckBox) view.findViewById(R.id.checkbox_stags);
            CheckBox checkImageSdesc = (CheckBox) view.findViewById(R.id.checkbox_sdesc);
            CheckBox checkImageStorr = (CheckBox) view.findViewById(R.id.checkbox_storr);
            CheckBox checkImageSto = (CheckBox) view.findViewById(R.id.checkbox_sto);
            CheckBox checkImageSdt1 = (CheckBox) view.findViewById(R.id.checkbox_sdt1);
            CheckBox checkImageSdt2 = (CheckBox) view.findViewById(R.id.checkbox_sdt2);
            CheckBox checkImageSh = (CheckBox) view.findViewById(R.id.checkbox_sh);

            int advType = listUrls.getAdvanceType();
            if (advType != -1) {
                if ((advType & ListUrls.SNAME) == 0)
                    checkImageSname.setChecked(false);
                else
                    checkImageSname.setChecked(true);
                if ((advType & ListUrls.STAGS) == 0)
                    checkImageStags.setChecked(false);
                else
                    checkImageStags.setChecked(true);
                if ((advType & ListUrls.SDESC) == 0)
                    checkImageSdesc.setChecked(false);
                else
                    checkImageSdesc.setChecked(true);
                if ((advType & ListUrls.STORR) == 0)
                    checkImageStorr.setChecked(false);
                else
                    checkImageStorr.setChecked(true);
                if ((advType & ListUrls.STO) == 0)
                    checkImageSto.setChecked(false);
                else
                    checkImageSto.setChecked(true);
                if ((advType & ListUrls.STD1) == 0)
                    checkImageSdt1.setChecked(false);
                else
                    checkImageSdt1.setChecked(true);
                if ((advType & ListUrls.STD2) == 0)
                    checkImageSdt2.setChecked(false);
                else
                    checkImageSdt2.setChecked(true);
                if ((advType & ListUrls.SH) == 0)
                    checkImageSh.setChecked(false);
                else
                    checkImageSh.setChecked(true);

                // MinRating
                CheckBox checkImageSr = (CheckBox) view.findViewById(R.id.checkbox_sr);
                if (listUrls.isMinRating())
                    checkImageSr.setChecked(true);
                else
                    checkImageSr.setChecked(false);
                Spinner spinnerMinRating = (Spinner) view.findViewById(R.id.spinner_min_rating);
                int index;
                if (listUrls.getMinRating() == -1)
                    index = 0;
                else
                    index = listUrls.getMinRating() - 2;
                spinnerMinRating.setSelection(index);
            }

            // Show advance if need
            final View advance = view.findViewById(R.id.filter_advance);
            if (listUrls.isAdvance())
                advance.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mListMode = Config.getListMode();
        updateGridView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Get
     *
     * @param intent
     */
    private void handleIntent(Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_SEARCH.equals(action)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (mSearchView != null && query != null)
                mSearchView.setQuery(query, true);
        } else if (ACTION_GALLERY_LIST.equals(action)) {
            int mode = intent.getIntExtra(GalleryListActivity.KEY_MODE, -1);
            switch (mode) {
            case ListUrls.MODE_TAG:
                lus = new ListUrls();
                String tag = intent.getStringExtra(KEY_TAG);
                lus.setTag(tag);
                mTitle = tag;
                setTitle(mTitle);
                break;

            case ListUrls.MODE_UPLOADER:
                String uploader = "uploader:" + intent.getStringExtra(KEY_UPLOADER);
                lus = new ListUrls(ListUrls.ALL_CATEGORT, uploader);
                lus.setMode(ListUrls.MODE_UPLOADER);
                mTitle = uploader;
                setTitle(mTitle);
                break;

            case ListUrls.MODE_IMAGE_SEARCH:
                lus = new ListUrls();
                lus.setSearchImage(intent.getStringExtra(KEY_IMAGE_KEY), intent.getStringExtra(KEY_IMAGE_URL),
                        EhClient.IMAGE_SEARCH_USE_SIMILARITY_SCAN);
                mTitle = getString(R.string.similar_content);
                setTitle(mTitle);
                break;

            case ListUrls.MODE_NORMAL:
                // Target is category
                int category = intent.getIntExtra(KEY_CATEGORY, ListUrls.ALL_CATEGORT);
                lus = new ListUrls(category);
                mTitle = Ui.getCategoryText(category);
                setTitle(mTitle);
                break;

            default:
                // TODO just do somthing
                break;
            }
        } else {
            lus = new ListUrls();
            mTitle = mResources.getString(R.string.homepage);
            setTitle(mTitle);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
        mGalleryListView.refresh();
    }

    @Override
    public void onClick(View v) {
        if (v == mLoginButton) {
            loginDialog.show();
        } else if (v == mRegisterButton) {
            toRegister();
        } else if (v == mLogoutButton) {
            mClient.logout();
            setUserPanel();
        }
    }

    @Override
    public void onDrawerClosed(View view) {
        setTitle(mTitle);
        invalidateOptionsMenu();

        if (view == mLeftMenu)
            mDirection = false;
    }

    @Override
    public void onDrawerOpened(View view) {
        if (view == mLeftMenu)
            setTitle(R.string.app_name);
        invalidateOptionsMenu();

        if (view == mLeftMenu)
            mDirection = true;
    }

    @Override
    public void onDrawerSlide(View view, float f) {
        if (view == mLeftMenu) {
            mMaterialIndicator.setTransformationOffset(
                    MaterialIndicatorDrawable.AnimationState.BURGER_ARROW,
                    mDirection ? 2 - f : f);
        }
    }

    @Override
    public void onDrawerStateChanged(View drawerView, int newState) {
        // Empty
    }

    @Override
    public void onFitSystemWindows(int l, int t, int r, int b) {
        mStaggeredGridView.setPadding(mStaggeredGridView.getPaddingLeft(), t,
                mStaggeredGridView.getPaddingRight(), b);
        ((SlidingDrawerLayout.LayoutParams) mLeftMenu.getLayoutParams()).topMargin = t;
        ((SlidingDrawerLayout.LayoutParams) mRightMenu.getLayoutParams()).topMargin = t;
        mMenuList.setPadding(mMenuList.getPaddingLeft(), mMenuList.getPaddingTop(),
                mMenuList.getPaddingRight(), b);
        mQuickSearchList.setPadding(mQuickSearchList.getPaddingLeft(), mQuickSearchList.getPaddingTop(),
                mQuickSearchList.getPaddingRight(), b);

        Ui.colorStatusBarKK(this, mThemeColor, t - Ui.ACTION_BAR_HEIGHT);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mMaterialIndicator.syncState(savedInstanceState);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMaterialIndicator.onSaveInstanceState(outState);
    }

    @SuppressWarnings("deprecation")
    @Override
    @SuppressLint("InflateParams")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_list);

        mAppContext = (AppContext) getApplication();
        mData = Data.getInstance();
        mClient = EhClient.getInstance();
        mResources = getResources();
        mWindowsAnimate = new WindowsAnimate();
        mWindowsAnimate.init(this);
        mSuggestions = SuggestionHelper.getInstance(GalleryListActivity.this, SimpleSuggestionProvider.AUTHORITY,
                SimpleSuggestionProvider.MODE);

        // Caculate gallery detail height
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.test_gallery_list_detail_thumb, null);
        ((TextView) v.findViewById(R.id.title)).setText("haha\nhaha");
        ((TextView) v.findViewById(R.id.uploader)).setText("haha");
        ((RatingView) v.findViewById(R.id.rate)).setRating(2.3f);
        ((TextView) v.findViewById(R.id.category)).setText("haha");
        ViewUtils.measureView(v);
        mListDetailThumbHeight = Math.max(v.getMeasuredHeight(), Ui.dp2pix(120));
        mListDetailThumbWidth = mListDetailThumbHeight * 2 / 3;

        handleIntent(getIntent());
        // Check show drawer or not
        if (ACTION_GALLERY_LIST.equals(getIntent().getAction()))
            mShowDrawer = false;
        else
            mShowDrawer = true;

        // Init dialog
        loginDialog = createLoginDialog();
        mSearchDialog = createSearchDialog();
        longClickDialog = createLongClickDialog();

        ColorDrawable cd = new ColorDrawable(Color.WHITE);
        cd.setBounds(0, 0, Ui.dp2pix(32), Ui.dp2pix(32));
        this.

        // Menu
        mMaterialIndicator = new MaterialIndicatorDrawable(this, Color.WHITE, Stroke.THIN);
        if (mShowDrawer)
            mMaterialIndicator.setIconState(MaterialIndicatorDrawable.IconState.BURGER);
        else
            mMaterialIndicator.setIconState(MaterialIndicatorDrawable.IconState.ARROW);
        mActionBar = getActionBar();
        Ui.setMaterialIndicator(mActionBar, mMaterialIndicator);

        // Get View
        mDrawerLayout = (SlidingDrawerLayout) findViewById(R.id.drawerlayout);

        mLeftMenu = mDrawerLayout.findViewById(R.id.list_menu_left);
        mRightMenu = mDrawerLayout.findViewById(R.id.list_menu_right);
        mStandard = (FitWindowView) mDrawerLayout.findViewById(R.id.standard);
        mGalleryListView = (GalleryListView) mDrawerLayout.findViewById(R.id.gallery_list);

        mStaggeredGridView = (StaggeredGridView) mGalleryListView.getContentView();

        mUserPanel = mLeftMenu.findViewById(R.id.user_panel);
        mMenuList = (DrawerListView) mLeftMenu.findViewById(R.id.list_menu_item_list);
        mAvatar = (ImageView) mUserPanel.findViewById(R.id.avatar);
        mUsernameView = (TextView) mUserPanel.findViewById(R.id.user);
        mLoginButton = (Button) mUserPanel.findViewById(R.id.login);
        mRegisterButton = (Button) mUserPanel.findViewById(R.id.register);
        mLogoutButton = (Button) mUserPanel.findViewById(R.id.logout);
        mWaitLogView = mUserPanel.findViewById(R.id.wait);

        mQuickSearchList = (TagListView) mRightMenu.findViewById(R.id.list_menu_tag_list);
        mQuickSearchTip = mRightMenu.findViewById(R.id.tip_text);


        mLoginButton.setOnClickListener(this);
        mRegisterButton.setOnClickListener(this);
        mLogoutButton.setOnClickListener(this);

        // Drawer
        mDrawerLayout.setDrawerListener(this);
        if (!mShowDrawer)
            mDrawerLayout.setDrawerLockMode(SlidingDrawerLayout.LOCK_MODE_LOCKED_CLOSED, mLeftMenu);

        // Content
        mGalleryListView.setGalleryListViewHelper(this);
        mStandard.addOnFitSystemWindowsListener(this);
        mGalleryListView.getPullViewGroup().setAgainstToChildPadding(true);

        // leftDrawer
        mUserPanel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mUserPanel.setBackgroundDrawable(new FreeMaterialDrawable());

        final Drawable[] drawableArray = new Drawable[] {
                mResources.getDrawable(R.drawable.ic_drawer_home),
                mResources.getDrawable(R.drawable.ic_drawer_panda),
                mResources.getDrawable(R.drawable.ic_drawer_search),
                mResources.getDrawable(R.drawable.ic_drawer_favorite),
                mResources.getDrawable(R.drawable.ic_drawer_whatshot),
                mResources.getDrawable(R.drawable.ic_drawer_history),
                mResources.getDrawable(R.drawable.ic_drawer_download),
                mResources.getDrawable(R.drawable.ic_drawer_settings),
        };
        final CharSequence[] titleArray = new CharSequence[] {
                mResources.getString(R.string.homepage),
                mResources.getString(R.string.mode),
                mResources.getString(android.R.string.search_go),
                mResources.getString(R.string.favourite),
                mResources.getString(R.string.whatshot),
                mResources.getString(R.string.history),
                mResources.getString(R.string.download),
                mResources.getString(R.string.action_settings)
        };

        // mMenuList.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mMenuList.setClipToPadding(false);
        mMenuList.setData(drawableArray, titleArray);
        mMenuList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch (position) {
                case 0: // Home page
                    lus = new ListUrls(ListUrls.NONE, null, 0);
                    mGalleryListView.refresh();
                    mDrawerLayout.closeDrawers();
                    mTitle = mResources.getString(R.string.homepage);
                    setTitle(mTitle);
                    break;

                case 1: // Mode
                    createModeDialog().show();
                    break;

                case 2: // Search
                    mSearchDialog.show();
                    break;

                case 3: // Favourite
                    intent = new Intent(GalleryListActivity.this, FavouriteActivity.class);
                    startActivity(intent);
                    break;

                case 4: // Popular
                    lus = new ListUrls();
                    lus.setMode(ListUrls.MODE_POPULAR);
                    mTitle = mResources.getString(R.string.whatshot);
                    setTitle(mTitle);
                    mGalleryListView.refresh();
                    mDrawerLayout.closeDrawers();
                    break;

                case 5: // HistoryActivity
                    intent = new Intent(GalleryListActivity.this, HistoryActivity.class);
                    startActivity(intent);
                    break;

                case 6: // Download
                    intent = new Intent(GalleryListActivity.this, DownloadActivity.class);
                    startActivity(intent);
                    break;

                case 7: // Settings
                    intent = new Intent(GalleryListActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    break;
                }
            }
        });

        List<String> keys = mData.getAllTagNames();

        for (int i = 0; i < keys.size(); i++)
            listMenuTag.add(keys.get(i));
        tagsAdapter = new TagsAdapter(this, R.layout.menu_tag, listMenuTag);
        tagsAdapter.setOnDataSetChangedListener(new TagsAdapter.OnDataSetChangedListener() {
            @Override
            public void OnDataSetChanged() {
                if (listMenuTag.size() == 0)
                    mQuickSearchTip.setVisibility(View.VISIBLE);
                else
                    mQuickSearchTip.setVisibility(View.GONE);
            }
        });
        if (listMenuTag.size() == 0)
            mQuickSearchTip.setVisibility(View.VISIBLE);
        else
            mQuickSearchTip.setVisibility(View.GONE);
        mQuickSearchList.setClipToPadding(false);
        mQuickSearchList.setAdapter(tagsAdapter);
        mQuickSearchList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mQuickSearchList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                lus = mData.getTag(position);
                mGalleryListView.refresh();
                mDrawerLayout.closeDrawers();
                mTitle = listMenuTag.get(position);
                setTitle(mTitle);
            }
        });
        mQuickSearchList.setOnModifyListener(new TagListView.OnModifyListener() {
            @Override
            public void onModify(int position) {
                createModifyTagDialog(position).show();
            }
        });
        mQuickSearchList.setOnSwapListener(new TagListView.OnSwapListener() {
            @Override
            public void onSwap(int positionOne, int positionTwo) {
                String temp = listMenuTag.get(positionOne);
                mData.swapTag(positionOne, positionTwo);
                listMenuTag.set(positionOne, listMenuTag.get(positionTwo));
                listMenuTag.set(positionTwo, temp);
                tagsAdapter.notifyDataSetChanged();
            }
        });
        mQuickSearchList.setOnDeleteListener(new TagListView.OnDeleteListener() {
            @Override
            public void onDelete(int position) {
                String removeItem = listMenuTag.remove(position);
                mData.deleteTag(position);
                tagsAdapter.remove(removeItem);
                tagsAdapter.notifyDataSetChanged();
            }
        });

        // Listview
        mAdapter = new ListAdapter(this, mGalleryListView.getGalleryList());
        mStaggeredGridView.setAdapter(mAdapter);
        mStaggeredGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Intent intent = new Intent(GalleryListActivity.this, GalleryDetailActivity.class);
                GalleryInfo gi = mGalleryListView.getGalleryInfo(position);
                intent.putExtra(GalleryDetailActivity.KEY_G_INFO, gi);
                startActivity(intent);
            }
        });
        mStaggeredGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                longClickItemIndex = position;
                longClickDialog.show();
                return true;
            }
        });

        // Set random color
        mThemeColor = Config.getRandomThemeColor() ? Theme.getRandomDarkColor() : Config.getThemeColor();
        mActionBar.setBackgroundDrawable(new ColorDrawable(mThemeColor));
        //mLeftMenu.setBackgroundColor(mThemeColor);
        mRightMenu.setBackgroundColor(mThemeColor);
        Ui.colorStatusBarL(this, mThemeColor);

        // Update user panel
        setUserPanel();

        // Set list mode
        mListMode = Config.getListMode();
        updateGridView();

        // get MangeList
        mGalleryListView.firstTimeRefresh();

        // If not show drawer, just return
        if (!mShowDrawer) {
            return;
        }

        // Check update
        if (Config.isAutoCheckForUpdate())
            checkUpdate();

        String keyFirstTime = "first_time";
        if (Config.getBoolean(keyFirstTime, true)) {
            Config.setBoolean(keyFirstTime, false);

            // Show left menu, Can't invoke showMenu() immediately
            Message m = Message.obtain(null, new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.openDrawer(mLeftMenu);
                }
            });
            AppHandler.getInstance().sendMessageDelayed(m, 500L);
        }

        String keyTcWarning = "traditional_chinese_warning";
        String country = Locale.getDefault().getCountry();
        if (Config.getBoolean(keyTcWarning, true) && (country.equals("HK") || country.equals("TW"))) {
            Config.setBoolean(keyTcWarning, false);
            new MaterialAlertDialog.Builder(this).setTitle("注意")
                    .setMessage("正體中文的翻譯由 OpenCC 自動完成，若有任何錯誤或不妥之處歡迎指出。")
                    .show();
        }
    }

    private void updateGridView() {
        if (mStaggeredGridView != null) {
            switch (mListMode) {
            case LIST_MODE_DETAIL:
                mStaggeredGridView.setColumnCountPortrait(1); // TODO
                mStaggeredGridView.setColumnCountLandscape(2);
                break;
            case LIST_MODE_THUMB:
                mStaggeredGridView.setColumnCountPortrait(Config.getListThumbColumnsPortrait());
                mStaggeredGridView.setColumnCountLandscape(Config.getListThumbColumnsLandscape());
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Store suggestion
        mSuggestions.saveRecentQuery(query, null);
        // collapse search view
        if (mSearchItem != null)
            mSearchItem.collapseActionView();

        String t = null;
        if (query == null || query.isEmpty())
            t = getString(android.R.string.search_go);
        else
            t = query;

        lus = new ListUrls(ListUrls.NONE, query);
        mGalleryListView.refresh();
        mTitle = t;
        setTitle(mTitle);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus && mSearchItem != null)
            mSearchItem.collapseActionView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mDrawerLayout.isDrawerOpen(mRightMenu))
            getMenuInflater().inflate(R.menu.quick_search, menu);
        else if (mDrawerLayout.isDrawerOpen(mLeftMenu))
            return true;
        else
            getMenuInflater().inflate(R.menu.gallery_list, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnQueryTextFocusChangeListener(this);

        // Make search view custom look
        int searchTextID = mResources.getIdentifier("android:id/search_src_text", null, null);
        AutoCompleteTextView searchText = null;
        if (searchTextID > 0 && (searchText = (AutoCompleteTextView) mSearchView.findViewById(searchTextID)) != null) {

            int searchCursorID = mResources.getIdentifier("android:drawable/text_cursor_holo_dark", null, null);
            if (searchCursorID > 0) {
                try {
                    Field f = Class.forName("android.widget.TextView").getDeclaredField("mCursorDrawableRes");
                    f.setAccessible(true);
                    f.setInt(searchText, searchCursorID);
                } catch (Exception e) {
                }
            }

            searchText.setTextColor(Color.WHITE);
            searchText.setHintTextColor(Color.WHITE);

            Drawable searchImage = mResources.getDrawable(R.drawable.ic_action_search);
            if (searchImage != null) {
                SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
                ssb.append(getString(R.string.advance_search_left));
                int textSize = (int) (searchText.getTextSize() * 1.25);
                searchImage.setBounds(0, 0, textSize, textSize);
                ssb.setSpan(new ImageSpan(searchImage), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                searchText.setHint(ssb);
            }
        }

        int plateViewID = mResources.getIdentifier("android:id/search_plate", null, null);
        View plateView = null;
        if (plateViewID > 0 && (plateView = mSearchView.findViewById(plateViewID)) != null) {
            plateView.setBackgroundResource(R.drawable.textfield_searchview);
        }

        int plateRightViewID = mResources.getIdentifier("android:id/submit_area", null, null);
        View plateRightView = null;
        if (plateRightViewID > 0 && (plateRightView = mSearchView.findViewById(plateRightViewID)) != null) {
            plateRightView.setBackgroundResource(R.drawable.textfield_searchview_right);
        }

        int closeViewID = mResources.getIdentifier("android:id/search_close_btn", null, null);
        ImageView closeView = null;
        Drawable closeImage = mResources.getDrawable(R.drawable.ic_clear);
        if (closeViewID > 0 && (closeView = (ImageView) mSearchView.findViewById(closeViewID)) != null
                && closeImage != null) {
            closeView.setImageDrawable(closeImage);
        }

        int voiceViewID = mResources.getIdentifier("android:id/search_voice_btn", null, null);
        ImageView voiceView = null;
        Drawable voiceImage = mResources.getDrawable(R.drawable.ic_voice_search);
        if (voiceViewID > 0 && (voiceView = (ImageView) mSearchView.findViewById(voiceViewID)) != null
                && voiceImage != null) {
            voiceView.setImageDrawable(voiceImage);
        }

        // Hide some menu item
        if (mListMode == LIST_MODE_DETAIL)
            menu.removeItem(R.id.action_detail);
        else if (mListMode == LIST_MODE_THUMB)
            menu.removeItem(R.id.action_thumb);

        return true;
    }

    // Double click back exit
    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mLeftMenu) || mDrawerLayout.isDrawerOpen(mRightMenu)) {
            mDrawerLayout.closeDrawers();
        } else {
            if (!mShowDrawer) {
                finish();
            } else {
                if (System.currentTimeMillis() - curBackTime > BACK_PRESSED_INTERVAL) {
                    curBackTime = System.currentTimeMillis();
                    MaterialToast.showToast(R.string.exit_tip);
                } else
                    finish();
            }
        }
    }

    private void jump() {
        createJumpDialog().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            if (mShowDrawer) {
                if (mDrawerLayout.isDrawerOpen(mLeftMenu))
                    mDrawerLayout.closeDrawers();
                else
                    mDrawerLayout.openDrawer(mLeftMenu);
            } else {
                finish();
            }
            return true;
        case R.id.action_refresh:
            mGalleryListView.refresh();
            return true;
        case R.id.action_jump:
            if (!mGalleryListView.isRefreshing() && mGalleryListView.isGetGalleryOk())
                jump();
            return true;
        case R.id.action_detail:
            mListMode = LIST_MODE_DETAIL;
            Config.setListMode(mListMode);
            updateGridView();
            if (mStaggeredGridView != null)
                mStaggeredGridView.invalidateChildren();
            invalidateOptionsMenu();
            return true;
        case R.id.action_thumb:
            mListMode = LIST_MODE_THUMB;
            Config.setListMode(mListMode);
            updateGridView();
            if (mStaggeredGridView != null)
                mStaggeredGridView.invalidateChildren();
            invalidateOptionsMenu();
            return true;
        case R.id.action_add:
            // TODO
            if (lus != null) {
                View view = LayoutInflater.from(this).inflate(R.layout.set_name, null);
                final EditText et = (EditText) view.findViewById(R.id.set_name_edit);
                new MaterialAlertDialog.Builder(this).setTitle("添加当前状态至快速搜索")
                        .setView(view, true)
                        .setDefaultButton(MaterialAlertDialog.POSITIVE | MaterialAlertDialog.NEGATIVE)
                        .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                            @Override
                            public boolean onClick(MaterialAlertDialog dialog, int which) {
                                if (which == MaterialAlertDialog.POSITIVE) {
                                    String name = et.getText().toString();
                                    if (name == null || name.isEmpty()) {
                                        MaterialToast.showToast(R.string.invalid_input);
                                        return false;
                                    }
                                    if (listMenuTag.contains(name)) {
                                        MaterialToast.showToast("该名称已存在"); // TODO
                                        return false;
                                    }
                                    mData.addTag(new Tag(name, lus));
                                    listMenuTag.add(name);
                                    tagsAdapter.addId(name);
                                    tagsAdapter.notifyDataSetChanged();
                                }
                                return true;
                            }
                        }).show();
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mWindowsAnimate.free();
    }

    private void checkUpdate() {
        new UpdateHelper((AppContext) getApplication()).SetOnCheckUpdateListener(
                new UpdateHelper.OnCheckUpdateListener() {
                    @Override
                    public void onSuccess(String version, long size, final String url, final String fileName,
                            String info) {
                        String sizeStr = Utils.sizeToString(size);
                        AlertDialog dialog = DialogUtils.createUpdateDialog(GalleryListActivity.this, version, sizeStr,
                                info, new MaterialAlertDialog.OnClickListener() {
                                    @Override
                                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                                        if (which == MaterialAlertDialog.POSITIVE) {
                                            HttpHelper hh = new HttpHelper(GalleryListActivity.this);
                                            hh.downloadInThread(url, new File(Config.getDownloadPath()), fileName,
                                                    false, null, new UpdateHelper.UpdateListener(
                                                            GalleryListActivity.this, fileName));
                                        }
                                        return true;
                                    }
                                }).create();
                        if (!GalleryListActivity.this.isFinishing())
                            dialog.show();
                    }

                    @Override
                    public void onNoUpdate() {
                        UpdateHelper.setEnabled(true);
                    }

                    @Override
                    public void onFailure(String eMsg) {
                        UpdateHelper.setEnabled(true);
                    }
                }).autoCheckUpdate();
    }

    private static final int LOGIN = 0x0;
    private static final int LOGOUT = 0x1;
    private static final int WAIT = 0x2;

    private void setUserPanel() {
        if (mClient.isLogin())
            setUserPanel(LOGOUT);
        else
            setUserPanel(LOGIN);
    }

    private void setUserPanel(int state) {

        switch (state) {
        case LOGIN:
            mAvatar.setImageBitmap(mClient.getAvatar());
            mUsernameView.setVisibility(View.INVISIBLE);
            mLoginButton.setVisibility(View.VISIBLE);
            mRegisterButton.setVisibility(View.VISIBLE);
            mLogoutButton.setVisibility(View.INVISIBLE);
            mWaitLogView.setVisibility(View.INVISIBLE);
            break;
        case LOGOUT:
            mAvatar.setImageBitmap(mClient.getAvatar());
            mUsernameView.setText(mClient.getDisplayname());
            mUsernameView.setVisibility(View.VISIBLE);
            mLoginButton.setVisibility(View.INVISIBLE);
            mRegisterButton.setVisibility(View.INVISIBLE);
            mLogoutButton.setVisibility(View.VISIBLE);
            mWaitLogView.setVisibility(View.INVISIBLE);
            break;
        case WAIT:
            mAvatar.setImageBitmap(mClient.getAvatar());
            mUsernameView.setVisibility(View.INVISIBLE);
            mLoginButton.setVisibility(View.INVISIBLE);
            mRegisterButton.setVisibility(View.INVISIBLE);
            mLogoutButton.setVisibility(View.INVISIBLE);
            mWaitLogView.setVisibility(View.VISIBLE);
            break;
        }
    }

    @Override
    public String getTargetUrl(int targetPage) {
        lus.setPage(targetPage);
        return lus.getUrl();
    }

    @Override
    public void doGetGallerys(String url, final long taskStamp, final OnGetListListener listener) {
        if (lus.getMode() == ListUrls.MODE_IMAGE_SEARCH) {
            if (url == null) {
                // No result url
                if (lus.getSearchFile() == null) {
                    ImageLoader.getInstance(GalleryListActivity.this).add(lus.getSearchImageUrl(),
                            lus.getSearchImageKey(), new ImageLoader.OnGetImageListener() {
                                @Override
                                public void onGetImage(String key, Bitmap bmp) {
                                    if (bmp != null) {
                                        mClient.getGListFromImageSearch(bmp, lus.getImageSearchMode(), taskStamp,
                                                new EhClient.OnGetGListFromImageSearchListener() {
                                                    @Override
                                                    public void onSuccess(Object checkFlag, List<GalleryInfo> giList,
                                                            int maxPage, String newUrl) {

                                                        System.out.println(giList.size());

                                                        lus.setSearchResult(newUrl);
                                                        listener.onSuccess(mAdapter, taskStamp, giList, maxPage);
                                                    }

                                                    @Override
                                                    public void onFailure(Object checkFlag, String eMsg) {
                                                        listener.onFailure(mAdapter, taskStamp, eMsg); // TODO
                                                    }
                                                });
                                    } else {
                                        listener.onFailure(mAdapter, taskStamp, "~~~~~~~~~~"); // TODO
                                    }
                                }
                            });
                } else {
                    // File search
                    mClient.getGListFromImageSearch(lus.getSearchFile(), lus.getImageSearchMode(), taskStamp,
                            new EhClient.OnGetGListFromImageSearchListener() {
                                @Override
                                public void onSuccess(Object checkFlag, List<GalleryInfo> giList, int maxPage,
                                        String newUrl) {
                                    lus.setSearchResult(newUrl);
                                    listener.onSuccess(mAdapter, taskStamp, giList, maxPage);
                                }

                                @Override
                                public void onFailure(Object checkFlag, String eMsg) {
                                    listener.onFailure(mAdapter, taskStamp, eMsg);
                                }
                            });
                }
            } else {
                // Get result url
                mClient.getGList(url, null, new EhClient.OnGetGListListener() {
                    @Override
                    public void onSuccess(Object checkFlag, List<GalleryInfo> lmdArray, int pageNum) {
                        listener.onSuccess(mAdapter, taskStamp, lmdArray, pageNum);
                    }

                    @Override
                    public void onFailure(Object checkFlag, String eMsg) {
                        listener.onFailure(mAdapter, taskStamp, eMsg);
                    }
                });
            }
        } else if (lus.getMode() == ListUrls.MODE_POPULAR) {
            mClient.getPopular(new EhClient.OnGetPopularListener() {
                @Override
                public void onSuccess(List<GalleryInfo> gis, long timeStamp) {
                    listener.onSuccess(mAdapter, taskStamp, gis, gis.size() == 0 ? 0 : 1);
                    // Show update time
                    if (timeStamp != -1 && Config.getShowPopularUpdateTime())
                        MaterialToast.showToast(String.format(getString(R.string.popular_update_time),
                                Utils.sDate.format(timeStamp)));
                }

                @Override
                public void onFailure(String eMsg) {
                    listener.onFailure(mAdapter, taskStamp, eMsg);
                }
            });
        } else {
            mClient.getGList(url, null, new EhClient.OnGetGListListener() {
                @Override
                public void onSuccess(Object checkFlag, List<GalleryInfo> lmdArray, int pageNum) {
                    listener.onSuccess(mAdapter, taskStamp, lmdArray, pageNum);
                }

                @Override
                public void onFailure(Object checkFlag, String eMsg) {
                    listener.onFailure(mAdapter, taskStamp, eMsg);
                }
            });
        }
    }

    private class ViewHolder {
        public LoadImageView thumb;
        public TextView category;
        public TextView simpleLanguage;
        public TextView title;
        public TextView uploader;
        public RatingView rate;
        public TextView posted;
    }

    public class ListAdapter extends BaseAdapter {
        private final List<GalleryInfo> mGiList;
        private final ImageLoader mImageLoader;

        public ListAdapter(Context context, List<GalleryInfo> gilist) {
            mGiList = gilist;
            mImageLoader = ImageLoader.getInstance(GalleryListActivity.this);
        }

        @Override
        public int getCount() {
            return mGiList.size();
        }

        @Override
        public Object getItem(int position) {
            return mGiList == null ? 0 : mGiList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GalleryInfo gi = mGiList.get(position);

            if (convertView == null
                    || (mListMode == LIST_MODE_DETAIL && convertView.getId() != R.id.gallery_list_detail)
                    || (mListMode == LIST_MODE_THUMB && convertView.getId() != R.id.gallery_list_thumb)) {
                // Get new view
                ViewHolder viewHolder = new ViewHolder();
                switch (mListMode) {
                case LIST_MODE_DETAIL:
                    convertView = LayoutInflater.from(GalleryListActivity.this).inflate(
                            R.layout.gallery_list_detail_item, parent, false);
                    CardViewSalon.reformWithShadow(convertView, new int[][] {
                            new int[] { android.R.attr.state_pressed }, new int[] { android.R.attr.state_activated },
                            new int[] {} }, new int[] { 0xff84cae4, 0xff33b5e5, 0xFFFAFAFA }, null, false); // TODO
                    viewHolder.thumb = (LoadImageView) convertView.findViewById(R.id.thumb);
                    ViewGroup.LayoutParams lp = viewHolder.thumb.getLayoutParams();
                    lp.width = mListDetailThumbWidth;
                    lp.height = mListDetailThumbHeight;
                    viewHolder.category = (TextView) convertView.findViewById(R.id.category);
                    viewHolder.simpleLanguage = (TextView) convertView.findViewById(R.id.simple_language);
                    viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                    viewHolder.uploader = (TextView) convertView.findViewById(R.id.uploader);
                    viewHolder.rate = (RatingView) convertView.findViewById(R.id.rate);
                    viewHolder.posted = (TextView) convertView.findViewById(R.id.posted);
                    convertView.setTag(viewHolder);
                    break;
                case LIST_MODE_THUMB:
                    convertView = LayoutInflater.from(GalleryListActivity.this).inflate(
                            R.layout.gallery_list_thumb_item, parent, false);
                    TileSalon.reform(convertView, 0xFFFAFAFA); // TODO
                    viewHolder.thumb = (LoadImageView) convertView.findViewById(R.id.thumb);
                    viewHolder.category = (TextView) convertView.findViewById(R.id.category);
                    viewHolder.simpleLanguage = (TextView) convertView.findViewById(R.id.simple_language);
                    convertView.setTag(viewHolder);
                    break;
                }
            }

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            final LoadImageView thumb = viewHolder.thumb;
            if (!String.valueOf(gi.gid).equals(thumb.getKey())) {
                // Set new thumb
                thumb.setImageDrawable(null);
                thumb.setLoadInfo(gi.thumb, String.valueOf(gi.gid));
                mImageLoader.add(gi.thumb, String.valueOf(gi.gid),
                        new LoadImageView.SimpleImageGetListener(thumb).setFixScaleType(true));
            }
            // Set category
            TextView category = viewHolder.category;
            String newText = Ui.getCategoryText(gi.category);
            if (!newText.equals(category.getText())) {
                category.setText(newText);
                category.setBackgroundColor(Ui.getCategoryColor(gi.category));
            }
            // Set simple language
            TextView simpleLanguage = viewHolder.simpleLanguage;
            if (gi.simpleLanguage == null) {
                simpleLanguage.setVisibility(View.GONE);
            } else {
                simpleLanguage.setVisibility(View.VISIBLE);
                simpleLanguage.setText(gi.simpleLanguage);
            }

            // For detail mode
            if (mListMode == LIST_MODE_DETAIL) {
                // Set manga title
                TextView title = viewHolder.title;
                title.setText(gi.title);
                // Set uploder
                TextView uploader = viewHolder.uploader;
                uploader.setText(gi.uploader);
                // Set star
                RatingView rate = viewHolder.rate;
                rate.setRating(gi.rating);
                // set posted
                TextView posted = viewHolder.posted;
                posted.setText(gi.posted);
            }

            return convertView;
        }
    }
}
