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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.etsy.android.grid.StaggeredGridView;
import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UpdateHelper;
import com.hippo.ehviewer.cardview.CardViewSalon;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.ListUrls;
import com.hippo.ehviewer.data.Tag;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.network.Downloader;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.tile.TileSalon;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.CategoryTable;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.PrefixEditText;
import com.hippo.ehviewer.widget.RatingView;
import com.hippo.ehviewer.widget.SuperDialogUtil;
import com.hippo.ehviewer.widget.SuperToast;
import com.hippo.ehviewer.widget.TagListView;
import com.hippo.ehviewer.widget.TagsAdapter;
import com.hippo.ehviewer.windowsanimate.WindowsAnimate;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

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
public class GalleryListActivity extends AbstractGalleryActivity
        implements View.OnClickListener {

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

    private EhClient mClient;
    private Resources mResources;
    private WindowsAnimate mWindowsAnimate;

    private SlidingMenu mSlidingMenu;
    private View mMenuLeft;
    private LinearLayout mUserPanel;
    private SearchView mSearchView;
    private ListView itemListMenu;
    private TagListView tagListMenu;

    private ImageView avatar;
    private TextView userView;
    private Button loginButton;
    private Button registerButton;
    private Button logoutButton;
    private View waitloginoutView;
    private View mQuickSearchTip;
    private TextView mSearchImageText;

    private StaggeredGridView mStaggeredGridView;
    private BaseAdapter mAdapter;

    private TagsAdapter tagsAdapter;

    private ListUrls lus;
    private final ArrayList<String> listMenuTag = new ArrayList<String>();

    private Data mData;

    private int longClickItemIndex;

    private AlertDialog loginDialog;
    private AlertDialog mSearchDialog;
    private AlertDialog longClickDialog;

    // Modify tag
    private String newTagName = null;

    // Double click back exit
    private long curBackTime = 0;
    private static final int BACK_PRESSED_INTERVAL = 2000;

    private String mTitle;

    private final DownloadServiceConnection mServiceConn = new DownloadServiceConnection();

    private int mListMode;
    //private int mListModeThumbHeight;

    private void toRegister() {
        Uri uri = Uri.parse("http://forums.e-hentai.org/index.php?act=Reg");
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        startActivity(intent);
    }

    private AlertDialog createLoginDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.login, null);

        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.login)
                .setView(view, false)
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        setUserPanel(WAIT);
                        String username = ((EditText) loginDialog.findViewById(R.id.username)).getText().toString();
                        String password = ((EditText) loginDialog.findViewById(R.id.password)).getText().toString();
                        mClient.login(username, password, new EhClient.OnLoginListener() {
                            @Override
                            public void onSuccess() {
                                setUserPanel();
                            }
                            @Override
                            public void onFailure(String eMsg) {
                                setUserPanel();
                                new SuperToast(eMsg, SuperToast.ERROR).show();
                                if (!GalleryListActivity.this.isFinishing())
                                    loginDialog.show();
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
                                            new SuperToast("无头像").show(); // TODO
                                            break;
                                        case EhClient.GET_AVATAR_ERROR:
                                        default:
                                            new SuperToast("获取头像失败", SuperToast.ERROR).show();  // TODO
                                            break;
                                        }
                                    }
                                });
                            }
                        });
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        setUserPanel();
                    }
                }).setNeutralButton(R.string.register, new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        toRegister();
                    }
                }).create();
    }

    private AlertDialog createModeDialog() {
        DialogBuilder db = new DialogBuilder(this);
        db.setTitle(R.string.mode).setView(R.layout.select_mode, true);
        LinearLayout customLayout = db.getCustomLayout();
        final Spinner modeSpinner = (Spinner)customLayout.findViewById(R.id.mode_list);
        modeSpinner.setSelection(Config.getMode());
        final Spinner apiModeSpinner = (Spinner)customLayout.findViewById(R.id.api_mode_list);
        apiModeSpinner.setSelection(Config.getApiMode());
        return db.setSimpleNegativeButton()
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        Config.setMode(modeSpinner.getSelectedItemPosition());
                        Config.setApiMode(apiModeSpinner.getSelectedItemPosition());
                    }
                }).create();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_SEARCH_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String ImagePath = cursor.getString(columnIndex);
            cursor.close();

            mSearchImageText.setVisibility(View.VISIBLE);
            mSearchImageText.setText(ImagePath);
        }

    }

    private void handleSearchView(View view) {
        final PrefixEditText pet = (PrefixEditText)view.findViewById(R.id.search_text);
        CheckBox uploaderCb = (CheckBox)view.findViewById(R.id.checkbox_uploader);
        uploaderCb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                if (isChecked)
                    pet.setPrefix("uploader:");
                else
                    pet.setPrefix(null);
            }
        });

        final View advance = view.findViewById(R.id.filter_advance);
        CheckBox cb = (CheckBox)view.findViewById(R.id.checkbox_advance);
        cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                if (isChecked)
                    advance.setVisibility(View.VISIBLE);
                else
                    advance.setVisibility(View.GONE);
            }
        });

        final Button selectImage = (Button)view.findViewById(R.id.select_image);
        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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
        mSearchImageText = (TextView)view.findViewById(R.id.target_image);
        handleSearchView(view);

        return new DialogBuilder(this).setTitle(android.R.string.search_go)
                .setView(view, false)
                .setAction(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
                    }
                }).setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ListUrls listUrls = getLus(mSearchDialog);
                        if (listUrls == null)
                            return;

                        ((AlertButton)v).dialog.dismiss();
                        lus = listUrls;
                        refresh();
                        showContent();

                        // Get title
                        String search = lus.getSearch();
                        switch(lus.getMode()) {
                        case ListUrls.MODE_IMAGE_SEARCH:
                            mTitle = "图片搜索"; // TODO
                            break;
                        case ListUrls.MODE_UPLOADER:
                            mTitle = search;
                            break;
                        case ListUrls.MODE_TAG:
                            mTitle = lus.getTag();
                            break;
                        case ListUrls.MODE_POPULAR:
                            mTitle = getString(R.string.popular);
                        case ListUrls.MODE_NORMAL:
                        default:
                            if (search == null || search.isEmpty())
                                mTitle = getString(android.R.string.search_go);
                            else
                                mTitle = search;
                            break;
                        }
                        setTitle(mTitle);
                    }
                }).setSimpleNegativeButton()
                .setNeutralButton(R.string.add, new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if (searchImage.getVisibility() == View.VISIBLE)
                            return;
                        createSetNameDialog(null, null, new OnSetNameListener() {
                            @Override
                            public void onSetVaildName(String newName) {
                                ((AlertButton)v).dialog.dismiss();
                                mData.addTag(new Tag(newName, getLus(mSearchDialog)));
                                listMenuTag.add(newName);
                                tagsAdapter.addId(newName);
                                tagsAdapter.notifyDataSetChanged();
                            }
                        }).show();
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
            CategoryTable ct = (CategoryTable) view
                    .findViewById(R.id.category_table);
            type = ct.getCategory();
            EditText et = (EditText)view.findViewById(R.id.search_text);
            lus = new ListUrls(type, et.getText().toString());

            CheckBox uploaderCb = (CheckBox)view.findViewById(R.id.checkbox_uploader);
            if (uploaderCb.isChecked()) {
                lus.setMode(ListUrls.MODE_UPLOADER);
            }

            CheckBox cb = (CheckBox)view.findViewById(R.id.checkbox_advance);
            if (cb.isChecked()) {
                CheckBox checkImageSname = (CheckBox) view
                        .findViewById(R.id.checkbox_sname);
                CheckBox checkImageStags = (CheckBox) view
                        .findViewById(R.id.checkbox_stags);
                CheckBox checkImageSdesc = (CheckBox) view
                        .findViewById(R.id.checkbox_sdesc);
                CheckBox checkImageStorr = (CheckBox) view
                        .findViewById(R.id.checkbox_storr);
                CheckBox checkImageSto = (CheckBox) view
                        .findViewById(R.id.checkbox_sto);
                CheckBox checkImageSdt1 = (CheckBox) view
                        .findViewById(R.id.checkbox_sdt1);
                CheckBox checkImageSdt2 = (CheckBox) view
                        .findViewById(R.id.checkbox_sdt2);
                CheckBox checkImageSh = (CheckBox) view
                        .findViewById(R.id.checkbox_sh);

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
                CheckBox checkImageSr = (CheckBox) view
                        .findViewById(R.id.checkbox_sr);
                if (checkImageSr.isChecked()) {
                    Spinner spinnerMinRating = (Spinner) view
                            .findViewById(R.id.spinner_min_rating);
                    lus.setAdvance(advType,
                            spinnerMinRating.getSelectedItemPosition() + 2);
                } else
                    lus.setAdvance(advType);
            }

            // For tag search
        } else if (searchTag.getVisibility() == View.VISIBLE) {
            EditText et = (EditText)view.findViewById(R.id.search_tag_text);
            lus = new ListUrls();
            lus.setTag(et.getText().toString());

            // For image search
        } else if (searchImage.getVisibility() == View.VISIBLE) {
            String filePath = (String)mSearchImageText.getText();
            File file = new File(filePath);
            if (!file.exists()) {
                new SuperToast("图片不存在", SuperToast.ERROR).show();
            } else {
                if (!file.canRead()) {
                    new SuperToast("图片不可读", SuperToast.ERROR).show();
                } else {

                    CheckBox similar = (CheckBox)view.findViewById(R.id.checkboxSimilar);
                    CheckBox covers = (CheckBox)view.findViewById(R.id.checkboxCovers);
                    CheckBox exp = (CheckBox)view.findViewById(R.id.checkboxExp);

                    lus = new ListUrls();
                    lus.setSearchFile(file, (similar.isChecked() ? EhClient.IMAGE_SEARCH_USE_SIMILARITY_SCAN : 0) |
                            (covers.isChecked() ? EhClient.IMAGE_SEARCH_ONLY_SEARCH_COVERS : 0) |
                            (exp.isChecked() ? EhClient.IMAGE_SEARCH_SHOW_EXPUNGED : 0));
                }
            }
        }

        return lus;
    }

    private AlertDialog createLongClickDialog() {
        return new DialogBuilder(this).setTitle(R.string.what_to_do)
                .setItems(R.array.list_item_long_click,
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int position, long arg3) {
                        final GalleryInfo gi;
                        switch (position) {
                        case 0: // Add favourite item
                            gi = getGalleryInfo(longClickItemIndex);
                            Favorite.addToFavorite(GalleryListActivity.this, gi);
                            break;
                        case 1:
                            gi = getGalleryInfo(longClickItemIndex);
                            Intent it = new Intent(GalleryListActivity.this, DownloadService.class);
                            startService(it);
                            mServiceConn.getService().add(String.valueOf(gi.gid), gi.thumb,
                                    mClient.getDetailUrl(gi.gid, gi.token), gi.title);
                            new SuperToast(R.string.toast_add_download).show();
                            break;
                        default:
                            break;
                        }
                        longClickDialog.dismiss();
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                    }
                }).create();
    }

    private AlertDialog createJumpDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.jump, null);
        TextView tv = (TextView)view.findViewById(R.id.list_jump_sum);
        // For lofi, can not get page num, so use Integer.MAX_VALUE
        tv.setText(String.format(getString(R.string.jump_summary), getCurPage() + 1,
                getPageNum() == Integer.MAX_VALUE ? "未知" : String.valueOf(getPageNum()))); // TODO
        tv = (TextView)view.findViewById(R.id.list_jump_to);
        tv.setText(R.string.jump_to);
        final EditText et = (EditText)view.findViewById(R.id.list_jump_edit);

        return new DialogBuilder(this).setTitle(R.string.jump)
                .setView(view, true)
                .setPositiveButton(android.R.string.ok,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int targetPage;
                        try{
                            targetPage = Integer.parseInt(et.getText().toString()) - 1;
                            if (targetPage < 0 || targetPage >= getPageNum())
                                throw new Exception();
                        } catch(Exception e) {
                            new SuperToast(R.string.toast_invalid_page, SuperToast.ERROR).show();
                            return;
                        }

                        ((AlertButton)v).dialog.dismiss();
                        jumpTo(targetPage);
                    }
                }).setNegativeButton(android.R.string.cancel,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                    }
                }).create();
    }

    private interface OnSetNameListener {
        public void onSetVaildName(String newName);
    }

    /**
     * Create a set name dialog
     *
     * @param hint Text to set in edittext first
     * @param oldStr string can be oldstr, even it is in listMenuTitle
     * @param listener what to do when set right text
     */
    private AlertDialog createSetNameDialog(final String hint, final String oldStr, final OnSetNameListener listener) {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.set_name, null);
        final EditText et = (EditText)view.findViewById(R.id.set_name_edit);
        if (hint != null)
            et.setText(hint);

        return new DialogBuilder(this).setTitle(R.string.add_tag)
                .setView(view, true).setPositiveButton(android.R.string.ok,
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        String key = et.getText().toString();
                        if (key.length() == 0)
                            new SuperToast(R.string.tag_name_empty, SuperToast.WARNING).show();
                        else if (listMenuTag.contains(key) && !key.equals(oldStr))
                            new SuperToast(R.string.tag_name_empty, SuperToast.WARNING).show();
                        else {
                            ((AlertButton)v).dialog.dismiss();
                            if (listener != null) {
                                listener.onSetVaildName(key);
                            }
                        }
                    }
                }).setNegativeButton(android.R.string.cancel,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                    }
                }).create();
    }

    private AlertDialog createModifyTagDialog(final int position) {
        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.search, null);
        final View searchNormal = view.findViewById(R.id.search_normal);
        final View searchTag = view.findViewById(R.id.search_tag);
        handleSearchView(view);

        ListUrls listUrls = mData.getTag(position);
        setFilterView(view, listUrls);

        return new DialogBuilder(this).setTitle(listMenuTag.get(position))
                .setView(view, false)
                .setAction(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (searchNormal.getVisibility() == View.GONE) {
                            searchNormal.setVisibility(View.VISIBLE);
                            searchTag.setVisibility(View.GONE);
                        } else {
                            searchNormal.setVisibility(View.GONE);
                            searchTag.setVisibility(View.VISIBLE);
                        }
                    }
                }).setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        ListUrls listUrls = getLus(view);
                        if (newTagName != null) {
                            mData.setTag(position, new Tag(newTagName, listUrls));
                            tagsAdapter.set(listMenuTag.get(position), newTagName);
                            listMenuTag.set(position, newTagName);
                            tagsAdapter.notifyDataSetChanged();

                            newTagName = null;
                        } else
                            mData.setTag(position, new Tag(listMenuTag.get(position), listUrls));
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        newTagName = null;
                    }
                }).setNeutralButton(R.string.tag_change_name, new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        String hint = newTagName == null ? listMenuTag.get(position) : newTagName;
                        createSetNameDialog(hint, listMenuTag.get(position), new OnSetNameListener(){
                            @Override
                            public void onSetVaildName(String newName) {
                                if (newName.equals(listMenuTag.get(position))) // If new is old name
                                    SuperDialogUtil.setTitle(((AlertButton)v).dialog,
                                            listMenuTag.get(position));
                                else {
                                    newTagName = newName;
                                    SuperDialogUtil.setTitle(((AlertButton)v).dialog,
                                            String.format(getString(R.string.new_tag_name), newTagName));
                                }
                            }
                        }).show();
                    }
                }).create();
    }

    private void setFilterView(View view, ListUrls listUrls) {

        View searchNormal = view.findViewById(R.id.search_normal);
        View searchTag = view.findViewById(R.id.search_tag);

        if (listUrls.getMode() == ListUrls.MODE_TAG) {
            searchNormal.setVisibility(View.GONE);
            searchTag.setVisibility(View.VISIBLE);

            EditText et = (EditText)view.findViewById(R.id.search_tag_text);
            et.setText(listUrls.getTag());
        } else {
            searchNormal.setVisibility(View.VISIBLE);
            searchTag.setVisibility(View.GONE);

            int category = listUrls.getCategory();
            CategoryTable ct = (CategoryTable) view
                    .findViewById(R.id.category_table);
            ct.setCategory(category);

            EditText et = (EditText)view.findViewById(R.id.search_text);
            et.setText(listUrls.getSearch());

            // Advance
            CheckBox cb = (CheckBox)view.findViewById(R.id.checkbox_advance);
            cb.setChecked(listUrls.isAdvance());

            CheckBox checkImageSname = (CheckBox) view
                    .findViewById(R.id.checkbox_sname);
            CheckBox checkImageStags = (CheckBox) view
                    .findViewById(R.id.checkbox_stags);
            CheckBox checkImageSdesc = (CheckBox) view
                    .findViewById(R.id.checkbox_sdesc);
            CheckBox checkImageStorr = (CheckBox) view
                    .findViewById(R.id.checkbox_storr);
            CheckBox checkImageSto = (CheckBox) view
                    .findViewById(R.id.checkbox_sto);
            CheckBox checkImageSdt1 = (CheckBox) view
                    .findViewById(R.id.checkbox_sdt1);
            CheckBox checkImageSdt2 = (CheckBox) view
                    .findViewById(R.id.checkbox_sdt2);
            CheckBox checkImageSh = (CheckBox) view
                    .findViewById(R.id.checkbox_sh);

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
                CheckBox checkImageSr = (CheckBox) view
                        .findViewById(R.id.checkbox_sr);
                if (listUrls.isMinRating())
                    checkImageSr.setChecked(true);
                else
                    checkImageSr.setChecked(false);
                Spinner spinnerMinRating = (Spinner) view
                        .findViewById(R.id.spinner_min_rating);
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
        mSlidingMenu.setBehindWidth(
                mResources.getDimensionPixelOffset(R.dimen.menu_width));
    }

    /**
     * Get
     * @param intent
     */
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
           String query =
                 intent.getStringExtra(SearchManager.QUERY);
           mSearchView.setQuery(query, true);
        } else if (ACTION_GALLERY_LIST.equals(action)) {
            int mode = intent.getIntExtra(GalleryListActivity.KEY_MODE, -1);
            switch(mode) {
            case ListUrls.MODE_TAG:
                lus = new ListUrls();
                String tag = intent.getStringExtra(KEY_TAG);
                lus.setTag(tag);
                mTitle = tag;
                setTitle(mTitle);
                refresh();
                break;

            case ListUrls.MODE_UPLOADER:
                String uploader = "uploader:" + intent.getStringExtra(KEY_UPLOADER);
                lus = new ListUrls(ListUrls.ALL_CATEGORT, uploader);
                lus.setMode(ListUrls.MODE_UPLOADER);
                mTitle = uploader;
                setTitle(mTitle);
                refresh();
                break;

            case ListUrls.MODE_IMAGE_SEARCH:
                lus = new ListUrls();
                lus.setSearchImage(intent.getStringExtra(KEY_IMAGE_KEY),
                        intent.getStringExtra(KEY_IMAGE_URL),
                        EhClient.IMAGE_SEARCH_USE_SIMILARITY_SCAN | EhClient.IMAGE_SEARCH_SHOW_EXPUNGED); // TODO
                mTitle = "类似内容"; // TODO
                setTitle(mTitle);
                refresh();
                break;

            case ListUrls.MODE_NORMAL:
                // Target is category
                int category = intent.getIntExtra(KEY_CATEGORY, ListUrls.ALL_CATEGORT);
                lus = new ListUrls(category);
                mTitle = Ui.getCategoryText(category);
                setTitle(mTitle);
                refresh();
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
    }

    @Override
    public void onClick(View v) {
        if (v == loginButton) {
            loginDialog.show();
        } else if (v == registerButton) {
            toRegister();
        } else if (v == logoutButton) {
            mClient.logout();
            setUserPanel();
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.gallery_list;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mData = Data.getInstance();
        mClient = EhClient.getInstance();
        mResources = getResources();
        mWindowsAnimate = new WindowsAnimate();
        mWindowsAnimate.init(this);

        handleIntent(getIntent());

        setBehindContentView(R.layout.list_menu_left);
        setSlidingActionBarEnabled(false);
        mSlidingMenu = getSlidingMenu();
        mSlidingMenu.setSecondaryMenu(R.layout.list_menu_right);
        mSlidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
        mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        mSlidingMenu.setBehindWidth(
                mResources.getDimensionPixelOffset(R.dimen.menu_width));
        mSlidingMenu.setShadowDrawable(R.drawable.shadow);
        mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        mSlidingMenu.setSecondaryShadowDrawable(R.drawable.shadow_right);
        mSlidingMenu.setOnOpenedListener(new SlidingMenu.OnOpenedListener() {
            @Override
            public void onOpened() {
                setTitle(R.string.app_name);
                invalidateOptionsMenu();
            }
        });
        mSlidingMenu.setOnClosedListener(new SlidingMenu.OnClosedListener() {
            @Override
            public void onClosed() {
                setTitle(mTitle);
                invalidateOptionsMenu();
            }
        });

        // Download service
        Intent it = new Intent(GalleryListActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);

        // Init dialog
        loginDialog = createLoginDialog();
        mSearchDialog = createSearchDialog();
        longClickDialog = createLongClickDialog();

        // Get View
        mMenuLeft = findViewById(R.id.list_menu_left);
        mUserPanel = (LinearLayout)findViewById(R.id.user_panel);
        itemListMenu = (ListView) findViewById(R.id.list_menu_item_list);
        tagListMenu = (TagListView) findViewById(R.id.list_menu_tag_list);
        avatar = (ImageView)mUserPanel.findViewById(R.id.avatar);
        userView = (TextView)mUserPanel.findViewById(R.id.user);
        loginButton = (Button)mUserPanel.findViewById(R.id.login);
        registerButton = (Button)mUserPanel.findViewById(R.id.register);
        logoutButton = (Button)mUserPanel.findViewById(R.id.logout);
        waitloginoutView = mUserPanel.findViewById(R.id.wait);
        mQuickSearchTip = findViewById(R.id.tip_text);
        mStaggeredGridView = (StaggeredGridView)getContentView();

        loginButton.setOnClickListener(this);
        registerButton.setOnClickListener(this);
        logoutButton.setOnClickListener(this);
        setNoneText(mResources.getString(R.string.no_found));

        // Drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // leftDrawer
        final int[] data = {R.drawable.ic_action_home, R.string.homepage,
                R.drawable.ic_action_panda, R.string.mode,
                R.drawable.ic_action_search, android.R.string.search_go,
                R.drawable.ic_action_favorite, R.string.favourite,
                R.drawable.ic_action_popular, R.string.popular,
                R.drawable.ic_action_download, R.string.download,
                R.drawable.ic_action_settings, R.string.action_settings};

        itemListMenu.setSelector(new ColorDrawable(Color.TRANSPARENT));
        itemListMenu.setClipToPadding(false);
        itemListMenu.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return data.length/2;
            }

            @Override
            public Object getItem(int position) {
                return new int[]{data[position * 2], data[position * 2 + 1]};
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv;
                if (convertView == null) {
                    tv = (TextView)LayoutInflater.from(GalleryListActivity.this).inflate(R.layout.menu_item, parent, false);
                    mWindowsAnimate.addRippleEffect(tv, true);
                } else {
                    tv = (TextView)convertView;
                }
                Drawable d = mResources.getDrawable(data[position * 2]);
                d.setBounds(0, 0, Ui.dp2pix(36), Ui.dp2pix(36));
                tv.setCompoundDrawables(d, null, null, null);
                tv.setCompoundDrawablePadding(Ui.dp2pix(8));
                tv.setText(data[position * 2 + 1]);
                return tv;
            }
        });
        itemListMenu.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Intent intent;
                switch (position) {
                case 0: // Home page
                    lus = new ListUrls(ListUrls.NONE, null, 0);
                    refresh();
                    showContent();
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
                    intent = new Intent(GalleryListActivity.this,
                            FavouriteActivity.class);
                    startActivity(intent);
                    break;

                case 4: // Popular
                    lus = new ListUrls();
                    lus.setMode(ListUrls.MODE_POPULAR);
                    mTitle = mResources.getString(R.string.popular);
                    setTitle(mTitle);
                    refresh();
                    showContent();
                    break;

                case 5: // Download
                    intent = new Intent(GalleryListActivity.this,
                            DownloadActivity.class);
                    startActivity(intent);
                    break;

                case 6: // Settings
                    intent = new Intent(GalleryListActivity.this,
                            SettingsActivity.class);
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
        tagListMenu.setClipToPadding(false);
        tagListMenu.setAdapter(tagsAdapter);
        tagListMenu.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        tagListMenu.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                lus = mData.getTag(position);
                refresh();
                showContent();
                mTitle = listMenuTag.get(position);
                setTitle(mTitle);
            }
        });
        tagListMenu.setOnModifyListener(new TagListView.OnModifyListener(){
            @Override
            public void onModify(int position) {
                createModifyTagDialog(position).show();
            }
        });
        tagListMenu.setOnSwapListener(new TagListView.OnSwapListener() {
            @Override
            public void onSwap(int positionOne, int positionTwo) {
                String temp = listMenuTag.get(positionOne);
                mData.swapTag(positionOne, positionTwo);
                listMenuTag.set(positionOne, listMenuTag.get(positionTwo));
                listMenuTag.set(positionTwo, temp);
                tagsAdapter.notifyDataSetChanged();
            }
        });
        tagListMenu.setOnDeleteListener(new TagListView.OnDeleteListener() {
            @Override
            public void onDelete(int position) {
                String removeItem = listMenuTag.remove(position);
                mData.deleteTag(position);
                tagsAdapter.remove(removeItem);
                tagsAdapter.notifyDataSetChanged();
            }
        });

        // Listview
        mAdapter = new ListAdapter(this, getGalleryList());
        mStaggeredGridView.setAdapter(mAdapter);
        mStaggeredGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(GalleryListActivity.this,
                        GalleryDetailActivity.class);
                GalleryInfo gi = getGalleryInfo(position);
                intent.putExtra("url", mClient.getDetailUrl(gi.gid, gi.token));
                intent.putExtra(GalleryDetailActivity.KEY_G_INFO, gi);
                startActivity(intent);
            }
        });
        mStaggeredGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                longClickItemIndex = position;
                longClickDialog.show();
                return true;
            }
        });

        // Set random color
        int color = Config.getRandomThemeColor() ? Theme.getRandomDarkColor() : Config.getThemeColor();
        color = color & 0x00ffffff | 0xdd000000;
        Drawable drawable = new ColorDrawable(color);
        final ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(drawable);
        Ui.translucent(this, color);
        mMenuLeft.setBackgroundColor(color);
        tagListMenu.setBackgroundColor(color);

        // Check update
        if (Config.isAutoCheckForUpdate())
            checkUpdate();

        // Update user panel
        setUserPanel();

        // Set list mode
        mListMode = Config.getListMode();
        updateGridView();

        // get MangeList
        firstTimeRefresh();

        if (Config.isFirstTime()) {
            Config.firstTime();

            // Show left menu, Can't invoke showMenu() immediately
            Message m = Message.obtain(null, new Runnable() {
                @Override
                public void run() {
                    showMenu();
                }
            });
            AppHandler.getInstance().sendMessageDelayed(m, 500L);

            // Show translate warning
            String country = Locale.getDefault().getCountry();
            if (country.equals("HK") || country.equals("TW")) {
                new DialogBuilder(this).setTitle("注意")
                .setMessage("正體中文的翻譯由 OpenCC 自動完成，若有任何錯誤或不妥之處歡迎指出。")
                .setSimpleNegativeButton().create().show();
            }
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
    public void onOrientationChanged(int paddingTop, int paddingBottom) {
        mUserPanel.setPadding(mUserPanel.getPaddingLeft(), paddingTop,
                mUserPanel.getPaddingRight(), mUserPanel.getPaddingBottom());
        itemListMenu.setPadding(itemListMenu.getPaddingLeft(), itemListMenu.getPaddingTop(),
                itemListMenu.getPaddingRight(), paddingBottom);
        tagListMenu.setPadding(tagListMenu.getPaddingLeft(), paddingTop,
                tagListMenu.getPaddingRight(), paddingBottom);
        mStaggeredGridView.setPadding(mStaggeredGridView.getPaddingLeft(), paddingTop,
                mStaggeredGridView.getPaddingRight(), paddingBottom);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Do not show menu when mSlidingMenu is shown
        if (mSlidingMenu!= null && mSlidingMenu.isMenuShowing())
            return true;

        getMenuInflater().inflate(R.menu.main, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView =
                (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.clearFocus();
                String t = null;
                if (query == null || query.isEmpty())
                    t = getString(android.R.string.search_go);
                else
                    t = query;

                lus = new ListUrls(ListUrls.NONE, query);
                refresh();
                mTitle = t;
                setTitle(mTitle);
                return true;
            }
        });

        // Make search view custom look
        int searchTextID = mResources.getIdentifier("android:id/search_src_text", null, null);
        AutoCompleteTextView searchText = null;
        if (searchTextID > 0
                && (searchText = (AutoCompleteTextView)mSearchView.findViewById(searchTextID)) != null) {

            int searchCursorID = mResources.getIdentifier("android:drawable/text_cursor_holo_dark", null, null);
            if (searchCursorID > 0) {
                try {
                    Field f = Class.forName("android.widget.TextView")
                            .getDeclaredField("mCursorDrawableRes");
                    f.setAccessible(true);
                    f.setInt(searchText, searchCursorID);
                } catch (Exception e) {}
            }

            searchText.setTextColor(Color.WHITE);
            searchText.setHintTextColor(Color.WHITE);

            int searchImageID = mResources.getIdentifier("android:drawable/ic_search", null, null);
            Drawable searchImage = null;
            if (searchImageID > 0
                    && (searchImage = mResources.getDrawable(searchImageID)) != null) {
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
        if (plateViewID > 0
                && (plateView = mSearchView.findViewById(plateViewID)) != null) {
            plateView.setBackgroundResource(R.drawable.textfield_searchview);
        }

        int plateRightViewID = mResources.getIdentifier("android:id/submit_area", null, null);
        View plateRightView = null;
        if (plateRightViewID > 0
                && (plateRightView = mSearchView.findViewById(plateRightViewID)) != null) {
            plateRightView.setBackgroundResource(R.drawable.textfield_searchview_right);
        }

        int closeViewID = mResources.getIdentifier("android:id/search_close_btn", null, null);
        ImageView closeView = null;
        int closeImageID = mResources.getIdentifier("android:drawable/ic_clear", null, null);
        Drawable closeImage = null;
        if (closeViewID > 0
                && (closeView = (ImageView)mSearchView.findViewById(closeViewID)) != null
                && closeImageID > 0
                && (closeImage = mResources.getDrawable(closeImageID)) != null) {
            closeView.setImageDrawable(closeImage);
        }

        int voiceViewID = mResources.getIdentifier("android:id/search_voice_btn", null, null);
        ImageView voiceView = null;
        int voiceImageID = mResources.getIdentifier("android:drawable/ic_voice_search", null, null);
        Drawable voiceImage = null;
        if (voiceViewID > 0
                && (voiceView = (ImageView)mSearchView.findViewById(voiceViewID)) != null
                && voiceImageID > 0
                && (voiceImage = mResources.getDrawable(voiceImageID)) != null) {
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
        if (System.currentTimeMillis() - curBackTime > BACK_PRESSED_INTERVAL) {
            curBackTime = System.currentTimeMillis();
            new SuperToast(R.string.exit_tip).show();
        } else
            finish();
    }

    private void jump() {
        createJumpDialog().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            if (mSlidingMenu.isMenuShowing())
                showContent();
            else
                showMenu();
            return true;
        case R.id.action_refresh:
            refresh();
            return true;
        case R.id.action_jump:
            if (!isRefreshing() && isGetGalleryOk())
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
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);

        /*
        List<GalleryInfo> reads = mData.getAllReads();
        for (GalleryInfo item : reads) {
            File folder = new File(Config.getDownloadPath(),
                    StringEscapeUtils.escapeHtml4(item.title));
            try {
                Util.deleteContents(folder);
                folder.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        mData.deleteAllReads();
        mWindowsAnimate.free();
    }

    private void checkUpdate() {
        new UpdateHelper((AppContext)getApplication())
        .SetOnCheckUpdateListener(new UpdateHelper.OnCheckUpdateListener() {
            @Override
            public void onSuccess(String version, long size,
                    final String url, final String fileName, String info) {
                String sizeStr = Utils.sizeToString(size);
                AlertDialog dialog = SuperDialogUtil.createUpdateDialog(GalleryListActivity.this,
                        version, sizeStr, info,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((AlertButton)v).dialog.dismiss();
                                // TODO
                                try {
                                    Downloader downloader = new Downloader(GalleryListActivity.this);
                                    downloader.resetData(Config.getDownloadPath(), fileName, url);
                                    downloader.setOnDownloadListener(
                                            new UpdateHelper.UpdateListener(GalleryListActivity.this,
                                                    fileName));
                                    new Thread(downloader).start();
                                } catch (MalformedURLException e) {
                                    UpdateHelper.setEnabled(true);
                                }
                            }
                        });
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
            avatar.setImageBitmap(mClient.getAvatar());
            userView.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.GONE);
            waitloginoutView.setVisibility(View.GONE);
            break;
        case LOGOUT:
            avatar.setImageBitmap(mClient.getAvatar());
            userView.setText(mClient.getDisplayname());
            userView.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
            waitloginoutView.setVisibility(View.GONE);
            break;
        case WAIT:
            avatar.setImageBitmap(mClient.getAvatar());
            userView.setVisibility(View.GONE);
            loginButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
            waitloginoutView.setVisibility(View.VISIBLE);
            break;
        }
    }

    @Override
    protected String getTargetUrl(int targetPage) {
        lus.setPage(targetPage);
        return lus.getUrl();
    }

    @Override
    protected void doGetGallerys(String url, final long taskStamp,
            final OnGetListListener listener) {
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
                                            public void onSuccess(
                                                    Object checkFlag,
                                                    List<GalleryInfo> giList,
                                                    int maxPage, String newUrl) {

                                                System.out.println(giList.size());

                                                lus.setSearchResult(newUrl);
                                                listener.onSuccess(mAdapter, taskStamp, giList, maxPage);
                                            }
                                            @Override
                                            public void onFailure(
                                                    Object checkFlag,
                                                    String eMsg) {
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
                        public void onSuccess(Object checkFlag, List<GalleryInfo> giList,
                                int maxPage, String newUrl) {
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
                    public void onSuccess(Object checkFlag, List<GalleryInfo> lmdArray,
                            int pageNum) {
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
                        new SuperToast(String.format(getString(R.string.popular_update_time),
                                AppContext.sFormatter.format(timeStamp))).show();
                }
                @Override
                public void onFailure(String eMsg) {
                    listener.onFailure(mAdapter, taskStamp, eMsg);
                }
            });
        } else {
            mClient.getGList(url, null, new EhClient.OnGetGListListener() {
                @Override
                public void onSuccess(Object checkFlag, List<GalleryInfo> lmdArray,
                        int pageNum) {
                    listener.onSuccess(mAdapter, taskStamp, lmdArray, pageNum);
                }
                @Override
                public void onFailure(Object checkFlag, String eMsg) {
                    listener.onFailure(mAdapter, taskStamp, eMsg);
                }
            });
        }
    }

    public class ListAdapter extends BaseAdapter {
        private final List<GalleryInfo> mGiList;
        private final ImageLoader mImageLoader;

        public ListAdapter(Context context, List<GalleryInfo> gilist) {
            mGiList = gilist;
            mImageLoader =ImageLoader.getInstance(GalleryListActivity.this);
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
            GalleryInfo gi= mGiList.get(position);

            if (convertView == null ||
                    (mListMode == LIST_MODE_DETAIL && convertView.getId() != R.id.gallery_list_detail) ||
                    (mListMode == LIST_MODE_THUMB && convertView.getId() != R.id.gallery_list_thumb)) {
                // Get new view
                switch (mListMode) {
                case LIST_MODE_DETAIL:
                    convertView = LayoutInflater.from(GalleryListActivity.this)
                    .inflate(R.layout.gallery_list_detail_item, parent, false);
                    CardViewSalon.reformWithShadow(convertView, new int[][]{
                                    new int[]{android.R.attr.state_pressed},
                                    new int[]{android.R.attr.state_activated},
                                    new int[]{}},
                                    new int[]{0xff84cae4, 0xff33b5e5, 0xFFFAFAFA}, null, false); // TODO
                    break;
                case LIST_MODE_THUMB:
                    convertView = LayoutInflater.from(GalleryListActivity.this)
                    .inflate(R.layout.gallery_list_thumb_item, parent, false);
                    TileSalon.reform(convertView, 0xFFFAFAFA); // TODO
                    break;
                }
            }

            final LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.thumb);
            if (!String.valueOf(gi.gid).equals(thumb.getKey())) {
                // Set new thumb
                thumb.setImageDrawable(null);
                thumb.setLoadInfo(gi.thumb, String.valueOf(gi.gid));
                mImageLoader.add(gi.thumb, String.valueOf(gi.gid),
                        new LoadImageView.SimpleImageGetListener(thumb).setFixScaleType(true));
            }
            // Set category
            TextView category = (TextView) convertView.findViewById(R.id.category);
            String newText = Ui.getCategoryText(gi.category);
            if (!newText.equals(category.getText())) {
                category.setText(newText);
                category.setBackgroundColor(Ui.getCategoryColor(gi.category));
            }

            // For detail mode
            if (mListMode == LIST_MODE_DETAIL) {
                // Set manga name
                TextView name = (TextView) convertView.findViewById(R.id.name);
                name.setText(gi.title);
                // Set uploder
                TextView uploader = (TextView) convertView.findViewById(R.id.uploader);
                uploader.setText(gi.uploader);

                // Set star
                RatingView rate = (RatingView) convertView
                        .findViewById(R.id.rate);
                rate.setRating(gi.rating);
                // set posted
                TextView posted = (TextView) convertView.findViewById(R.id.posted);
                posted.setText(gi.posted);
                // Set simple language
                TextView simpleLanguage = (TextView) convertView.findViewById(R.id.simple_language);
                if (gi.simpleLanguage == null) {
                    simpleLanguage.setVisibility(View.GONE);
                } else {
                    simpleLanguage.setVisibility(View.VISIBLE);
                    simpleLanguage.setText(gi.simpleLanguage);
                }
            } else if (mListMode == LIST_MODE_THUMB){
                // Set simple language
                TextView simpleLanguage = (TextView) convertView.findViewById(R.id.simple_language);
                if (gi.simpleLanguage == null) {
                    simpleLanguage.setVisibility(View.GONE);
                } else {
                    simpleLanguage.setVisibility(View.VISIBLE);
                    simpleLanguage.setText(gi.simpleLanguage);
                }
            }

            return convertView;
        }
    }
}
