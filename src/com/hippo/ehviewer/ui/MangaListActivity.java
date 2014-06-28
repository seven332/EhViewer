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

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UpdateHelper;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.Tag;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.network.Downloader;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.CheckTextView;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;
import com.hippo.ehviewer.widget.PrefixEditText;
import com.hippo.ehviewer.widget.SuperDialogUtil;
import com.hippo.ehviewer.widget.SuperToast;
import com.hippo.ehviewer.widget.TagListView;
import com.hippo.ehviewer.widget.TagsAdapter;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
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

// TODO check visiblePage is right or not
// TODO http://lofi.e-hentai.org/

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
public class MangaListActivity extends AbstractGalleryActivity
        implements View.OnClickListener {
    
    private static final String TAG = "MangaListActivity";
    
    public static final String ACTION_GALLERY_LIST = "com.hippo.ehviewer.intent.action.GALLERY_LIST";
    
    public static final String KEY_MODE = "mode";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_TAG = "tag";
    public static final String KEY_UPLOADER = "uploader";
    
    private EhClient mEhClient;
    private Resources mResources;
    
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
    
    private ListView mList;
    
    private TagsAdapter tagsAdapter;

    private ListUrls lus;
    private ArrayList<String> listMenuTag = new ArrayList<String>();
    
    private Data mData;
    
    private int longClickItemIndex;
    
    private AlertDialog loginDialog;
    private AlertDialog filterDialog;
    private AlertDialog longClickDialog;
    
    // Modify tag
    private String newTagName = null;
    
    // Double click back exit
    private long curBackTime = 0;
    private static final int BACK_PRESSED_INTERVAL = 2000;
    
    private String mTitle;
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
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
                        mEhClient.login(username, password, new EhClient.OnLoginListener() {
                            @Override
                            public void onSuccess() {
                                setUserPanel();
                            }
                            @Override
                            public void onFailure(String eMsg) {
                                setUserPanel();
                                new SuperToast(MangaListActivity.this, eMsg).setIcon(R.drawable.ic_warning).show();
                                if (!MangaListActivity.this.isFinishing())
                                    loginDialog.show();
                            }
                            @Override
                            public void onGetAvatar(final int code) {
                                MangaListActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        switch (code) {
                                        case EhClient.GET_AVATAR_OK:
                                            setUserPanel();
                                            break;
                                        case EhClient.NO_AVATAR:
                                            new SuperToast(MangaListActivity.this, "无头像").show();
                                            break;
                                        case EhClient.GET_AVATAR_ERROR:
                                        default:
                                            new SuperToast(MangaListActivity.this, "获取头像失败", SuperToast.ERROR).show();
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
        final Spinner modeSpinner = new Spinner(this);
        modeSpinner.setAdapter(new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_dropdown_item,
                this.getResources().getTextArray(R.array.mode_list)));
        modeSpinner.setSelection(Config.getMode());
        modeSpinner.setMinimumWidth(Ui.dp2pix(200));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        lp.topMargin = Ui.dp2pix(12);
        lp.bottomMargin = Ui.dp2pix(12);
        return new DialogBuilder(this).setTitle(R.string.mode)
                .setView(modeSpinner, lp)
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int mode = modeSpinner.getSelectedItemPosition();
                        if (mode > 1) {
                            new SuperToast(MangaListActivity.this, R.string.unfinished).show();
                        } else {
                            ((AlertButton)v).dialog.dismiss();
                            Config.setMode(mode);
                        }
                    }
                }).setSimpleNegativeButton().create();
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
    }
    
    private AlertDialog createFilterDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.search, null);
        final View searchNormal = view.findViewById(R.id.search_normal);
        final View searchTag = view.findViewById(R.id.search_tag);
        handleSearchView(view);
        
        return new DialogBuilder(this).setTitle(android.R.string.search_go)
                .setView(view, false)
                .setAction(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (searchNormal.getVisibility() == View.GONE) {
                            searchNormal.setVisibility(View.VISIBLE);
                            searchTag.setVisibility(View.GONE);
                            new SuperToast(MangaListActivity.this, R.string.toast_normal_mode).show();
                        } else {
                            searchNormal.setVisibility(View.GONE);
                            searchTag.setVisibility(View.VISIBLE);
                            new SuperToast(MangaListActivity.this, R.string.toast_tag_mode).show();
                        }
                    }
                }).setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ListUrls backup = lus;
                        lus = getLus(filterDialog);
                        if (refresh(false)) {
                            ((AlertButton)v).dialog.dismiss();
                            showContent();
                            
                            String search = lus.getSearch();
                            String t = null;
                            switch(lus.getMode()) {
                            case ListUrls.NORMAL:
                                if (search == null || search.isEmpty())
                                    t = getString(android.R.string.search_go);
                                else
                                    t = search;
                                break;
                            case ListUrls.UPLOADER:
                                t = search;
                                break;
                            case ListUrls.TAG:
                                t = lus.getTag();
                                break;
                            }
                            mTitle = t;
                            setTitle(mTitle);
                        } else {
                            lus = backup;
                            new SuperToast(MangaListActivity.this, R.string.wait_for_last).show();
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                    }
                }).setNeutralButton(R.string.add, new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        createSetNameDialog(null, null, new OnSetNameListener() {
                            @Override
                            public void onSetVaildName(String newName) {
                                ((AlertButton)v).dialog.dismiss();
                                mData.addTag(new Tag(newName, getLus(filterDialog)));
                                listMenuTag.add(newName);
                                tagsAdapter.addId(newName);
                                tagsAdapter.notifyDataSetChanged();
                            }
                        })
                        .show();
                    }
                }).create();
    }
    
    private ListUrls getLus(AlertDialog dialog) {
        return getLus(dialog.findViewById(R.id.custom));
    }
    
    private ListUrls getLus(View view) {
        
        ListUrls lus;
        
        View search_normal = view.findViewById(R.id.search_normal);
        if (search_normal.getVisibility() == View.GONE) {
            EditText et = (EditText)view.findViewById(R.id.search_tag_text);
            lus = new ListUrls();
            lus.setTag(et.getText().toString());
        } else {
            CheckTextView checkImageDoujinshi = (CheckTextView) view
                    .findViewById(R.id.button_doujinshi);
            CheckTextView checkImageManga = (CheckTextView) view
                    .findViewById(R.id.button_manga);
            CheckTextView checkImageArtistcg = (CheckTextView) view
                    .findViewById(R.id.button_artistcg);
            CheckTextView checkImageGamecg = (CheckTextView) view
                    .findViewById(R.id.button_gamecg);
            CheckTextView checkImageWestern = (CheckTextView) view
                    .findViewById(R.id.button_western);
            CheckTextView checkImageNonH = (CheckTextView) view
                    .findViewById(R.id.button_non_h);
            CheckTextView checkImageImageset = (CheckTextView) view
                    .findViewById(R.id.button_imageset);
            CheckTextView checkImageCosplay = (CheckTextView) view
                    .findViewById(R.id.button_cosplay);
            CheckTextView checkImageAsianporn = (CheckTextView) view
                    .findViewById(R.id.button_asianporn);
            CheckTextView checkImageMisc = (CheckTextView) view
                    .findViewById(R.id.button_misc);

            int type = 0;
            if (!checkImageDoujinshi.isPressed())
                type |= ListUrls.DOUJINSHI;
            if (!checkImageManga.isPressed())
                type |= ListUrls.MANGA;
            if (!checkImageArtistcg.isPressed())
                type |= ListUrls.ARTIST_CG;
            if (!checkImageGamecg.isPressed())
                type |= ListUrls.GAME_CG;
            if (!checkImageWestern.isPressed())
                type |= ListUrls.WESTERN;
            if (!checkImageNonH.isPressed())
                type |= ListUrls.NON_H;
            if (!checkImageImageset.isPressed())
                type |= ListUrls.IMAGE_SET;
            if (!checkImageCosplay.isPressed())
                type |= ListUrls.COSPLAY;
            if (!checkImageAsianporn.isPressed())
                type |= ListUrls.ASIAN_PORN;
            if (!checkImageMisc.isPressed())
                type |= ListUrls.MISC;

            EditText et = (EditText)view.findViewById(R.id.search_text);

            lus = new ListUrls(type, et.getText().toString());
            
            CheckBox uploaderCb = (CheckBox)view.findViewById(R.id.checkbox_uploader);
            if (uploaderCb.isChecked()) {
                lus.setMode(ListUrls.UPLOADER);
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
                            int defaultFavorite = Config.getDefaultFavorite();
                            switch (defaultFavorite) {
                            case -2:
                                Favorite.getAddToFavoriteDialog(MangaListActivity.this, gi).show();
                                break;
                            case -1:
                                ((AppContext)getApplication()).getData().addLocalFavourite(gi);
                                new SuperToast(MangaListActivity.this).setMessage(R.string.toast_add_favourite).show();
                                break;
                            default:
                                ((AppContext)getApplication()).getEhClient().addToFavorite(gi.gid,
                                        gi.token, defaultFavorite, null, new EhClient.OnAddToFavoriteListener() {
                                    @Override
                                    public void onSuccess() {
                                        new SuperToast(MangaListActivity.this).setMessage(R.string.toast_add_favourite).show();
                                    }
                                    @Override
                                    public void onFailure(String eMsg) {
                                        new SuperToast(MangaListActivity.this).setMessage(R.string.failed_to_add).show();
                                    }
                                });
                            }
                            break;
                        case 1:
                            gi = getGalleryInfo(longClickItemIndex);
                            Intent it = new Intent(MangaListActivity.this, DownloadService.class);
                            startService(it);
                            mServiceConn.getService().add(String.valueOf(gi.gid), gi.thumb, 
                                    EhClient.getDetailUrl(gi.gid, gi.token), gi.title);
                            new SuperToast(MangaListActivity.this, R.string.toast_add_download).show();
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
        tv.setText(String.format(getString(R.string.jump_summary), getCurPage() + 1, getMaxPage()));
        tv = (TextView)view.findViewById(R.id.list_jump_to);
        tv.setText(R.string.jump_to);
        final EditText et = (EditText)view.findViewById(R.id.list_jump_edit);
        
        return new DialogBuilder(this).setTitle(R.string.jump)
                .setView(view, true)
                .setPositiveButton(android.R.string.ok,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean error = false;
                        int targetPage = 0;
                        try{
                            targetPage = Integer.parseInt(et.getText().toString()) - 1;
                        } catch(Exception e) {
                            error = true;
                        }
                        
                        if (!error) {
                            error = !jumpTo(targetPage, true);
                        }
                        
                        if (error)
                            new SuperToast(MangaListActivity.this, R.string.toast_invalid_page,
                                    SuperToast.ERROR).show();
                        else
                            ((AlertButton)v).dialog.dismiss();
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
                            new SuperToast(MangaListActivity.this, R.string.tag_name_empty).setIcon(R.drawable.ic_warning).show();
                        else if (listMenuTag.contains(key) && !key.equals(oldStr))
                            new SuperToast(MangaListActivity.this, R.string.tag_name_empty).setIcon(R.drawable.ic_warning).show();
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
                            new SuperToast(MangaListActivity.this, R.string.toast_normal_mode).show();
                        } else {
                            searchNormal.setVisibility(View.GONE);
                            searchTag.setVisibility(View.VISIBLE);
                            new SuperToast(MangaListActivity.this, R.string.toast_tag_mode).show();
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
        
        if (listUrls.getMode() == ListUrls.TAG) {
            searchNormal.setVisibility(View.GONE);
            searchTag.setVisibility(View.VISIBLE);
            
            EditText et = (EditText)view.findViewById(R.id.search_tag_text);
            et.setText(listUrls.getTag());
        } else {
            searchNormal.setVisibility(View.VISIBLE);
            searchTag.setVisibility(View.GONE);
            
            // Normal
            CheckTextView checkImageDoujinshi = (CheckTextView) view
                    .findViewById(R.id.button_doujinshi);
            CheckTextView checkImageManga = (CheckTextView) view
                    .findViewById(R.id.button_manga);
            CheckTextView checkImageArtistcg = (CheckTextView) view
                    .findViewById(R.id.button_artistcg);
            CheckTextView checkImageGamecg = (CheckTextView) view
                    .findViewById(R.id.button_gamecg);
            CheckTextView checkImageWestern = (CheckTextView) view
                    .findViewById(R.id.button_western);
            CheckTextView checkImageNonH = (CheckTextView) view
                    .findViewById(R.id.button_non_h);
            CheckTextView checkImageImageset = (CheckTextView) view
                    .findViewById(R.id.button_imageset);
            CheckTextView checkImageCosplay = (CheckTextView) view
                    .findViewById(R.id.button_cosplay);
            CheckTextView checkImageAsianporn = (CheckTextView) view
                    .findViewById(R.id.button_asianporn);
            CheckTextView checkImageMisc = (CheckTextView) view
                    .findViewById(R.id.button_misc);

            int type = listUrls.getType();
            if ((type & ListUrls.DOUJINSHI) == 0)
                checkImageDoujinshi.setChecked(true);
            else
                checkImageDoujinshi.setChecked(false);
            if ((type & ListUrls.MANGA) == 0)
                checkImageManga.setChecked(true);
            else
                checkImageManga.setChecked(false);
            if ((type & ListUrls.ARTIST_CG) == 0)
                checkImageArtistcg.setChecked(true);
            else
                checkImageArtistcg.setChecked(false);
            if ((type & ListUrls.GAME_CG) == 0)
                checkImageGamecg.setChecked(true);
            else
                checkImageGamecg.setChecked(false);
            if ((type & ListUrls.WESTERN) == 0)
                checkImageWestern.setChecked(true);
            else
                checkImageWestern.setChecked(false);
            if ((type & ListUrls.NON_H) == 0)
                checkImageNonH.setChecked(true);
            else
                checkImageNonH.setChecked(false);
            if ((type & ListUrls.IMAGE_SET) == 0)
                checkImageImageset.setChecked(true);
            else
                checkImageImageset.setChecked(false);
            if ((type & ListUrls.COSPLAY) == 0)
                checkImageCosplay.setChecked(true);
            else
                checkImageCosplay.setChecked(false);
            if ((type & ListUrls.ASIAN_PORN) == 0)
                checkImageAsianporn.setChecked(true);
            else
                checkImageAsianporn.setChecked(false);
            if ((type & ListUrls.MISC) == 0)
                checkImageMisc.setChecked(true);
            else
                checkImageMisc.setChecked(false);
            
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mSlidingMenu.setBehindWidth(
                mResources.getDimensionPixelOffset(R.dimen.menu_offset));
    }
    
    private void handleIntent(Intent intent) { 
        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
           String query = 
                 intent.getStringExtra(SearchManager.QUERY); 
           mSearchView.setQuery(query, true);
        } else if (ACTION_GALLERY_LIST.equals(action)) {
            int mode = intent.getIntExtra(MangaListActivity.KEY_MODE, -1);
            switch(mode) {
            case ListUrls.TAG:
                lus = new ListUrls();
                String tag = intent.getStringExtra(KEY_TAG);
                lus.setTag(tag);
                mTitle = tag;
                setTitle(mTitle);
                refresh(true);
                break;
                
            case ListUrls.UPLOADER:
                String uploader = "uploader:" + intent.getStringExtra(KEY_UPLOADER);
                lus = new ListUrls(ListUrls.ALL_TYPE, uploader);
                lus.setMode(ListUrls.UPLOADER);
                mTitle = uploader;
                setTitle(mTitle);
                refresh(true);
                break;
                
            default:
                // TODO just do somthing
                break;
            }
        } else {
            // TODO just do somthing
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
            mEhClient.logout();
            setUserPanel();
        }
    }
    
    private void showPopularWarningDialog() {
        DialogBuilder db = new DialogBuilder(MangaListActivity.this).
                setCancelable(false).
                setView(R.layout.popular_warning, false);
        db.setTitle(R.string.about_analyics_title);
        ViewGroup vg = db.getCustomLayout();
        final CheckBox cb = (CheckBox)vg.findViewById(R.id.set_default);
        db.setPositiveButton(android.R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AlertButton)v).dialog.dismiss();
                if (cb.isChecked())
                    Config.setPopularWarning(false);
                
                Intent intent = new Intent(MangaListActivity.this,
                        SettingsActivity.class);
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.AboutFragment.class.getName());
                intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                startActivity(intent);
            }
        });
        db.setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AlertButton)v).dialog.dismiss();
                if (cb.isChecked())
                    Config.setPopularWarning(false);
            }
        });
        db.create().show();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
        
        mData = mAppContext.getData();
        mEhClient = mAppContext.getEhClient();
        mResources =getResources();
        
        setBehindContentView(R.layout.list_menu_left);
        setSlidingActionBarEnabled(false);
        mSlidingMenu = getSlidingMenu();
        mSlidingMenu.setSecondaryMenu(R.layout.list_menu_right);
        mSlidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
        mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        mSlidingMenu.setBehindWidth(
                mResources.getDimensionPixelOffset(R.dimen.menu_offset));
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
        Intent it = new Intent(MangaListActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        // Get url
        Intent intent = getIntent();
        int type = intent.getIntExtra("type", ListUrls.ALL_TYPE);
        String search = intent.getStringExtra("search");
        int page = intent.getIntExtra("page", 0);
        lus = new ListUrls(type, search, page);
        
        // Init dialog
        loginDialog = createLoginDialog();
        filterDialog = createFilterDialog();
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
        
        mList = getListView();
        
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
                LayoutInflater li= LayoutInflater.from(MangaListActivity.this);
                Drawable d = mResources.getDrawable(data[position * 2]);
                d.setBounds(0, 0, Ui.dp2pix(36), Ui.dp2pix(36));
                TextView tv = (TextView)li.inflate(R.layout.menu_item, null);
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
                    ListUrls backup = lus;
                    lus = new ListUrls(ListUrls.ALL_TYPE, null, 0);
                    if (refresh(false)) {
                        mTitle = mResources.getString(R.string.homepage);
                        setTitle(mTitle);
                        
                        showContent();
                    } else {
                        new SuperToast(MangaListActivity.this, R.string.wait_for_last).show();
                        lus = backup;
                    }
                    break;
                case 1:
                    createModeDialog().show();
                    break;
                case 2:
                    filterDialog.show();
                    break;
                    
                case 3: // Favourite
                    intent = new Intent(MangaListActivity.this,
                            FavouriteActivity.class);
                    startActivity(intent);
                    break;
                    
                case 4:
                    if (isRefreshing()) {
                        new SuperToast(MangaListActivity.this, R.string.wait_for_last).show();
                    } else {
                        lus = new ListUrls();
                        lus.setMode(ListUrls.POPULAR);
                        mTitle = mResources.getString(R.string.popular);
                        setTitle(mTitle);
                        refresh(true);
                        
                        showContent();
                        
                        // Show dialog
                        if (!Config.getAllowAnalyics() && Config.getPopularWarning())
                            showPopularWarningDialog();
                    }
                    break;
                    
                case 5: // Download
                    intent = new Intent(MangaListActivity.this,
                            DownloadActivity.class);
                    startActivity(intent);
                    break;
                    
                case 6: // Settings
                    intent = new Intent(MangaListActivity.this,
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
        tagListMenu.setClipToPadding(false);
        tagListMenu.setAdapter(tagsAdapter);
        tagListMenu.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        tagListMenu.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                ListUrls backup = lus;
                lus = mData.getTag(position);
                if (lus != null && refresh(false)) {
                    mTitle = listMenuTag.get(position);
                    setTitle(mTitle);
                    showContent();
                } else {
                    new SuperToast(MangaListActivity.this, R.string.wait_for_last).show();
                    lus = backup;
                }
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
        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(MangaListActivity.this,
                        MangaDetailActivity.class);
                GalleryInfo gi = getGalleryInfo(position);
                intent.putExtra("url", EhClient.getDetailUrl(gi.gid, gi.token));
                intent.putExtra(MangaDetailActivity.KEY_G_INFO, gi);
                startActivity(intent);
            }
        });
        mList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                longClickItemIndex = position;
                longClickDialog.show();
                return true;
            }
        });
        
        FswView alignment = (FswView)findViewById(R.id.alignment);
        alignment.addOnFitSystemWindowsListener(new OnFitSystemWindowsListener() {
            @Override
            public void onfitSystemWindows(int paddingLeft, int paddingTop,
                    int paddingRight, int paddingBottom) {
                mUserPanel.setPadding(mUserPanel.getPaddingLeft(), paddingTop,
                        mUserPanel.getPaddingRight(), mUserPanel.getPaddingBottom());
                itemListMenu.setPadding(itemListMenu.getPaddingLeft(), itemListMenu.getPaddingTop(),
                        itemListMenu.getPaddingRight(), paddingBottom);
                tagListMenu.setPadding(tagListMenu.getPaddingLeft(), paddingTop,
                        tagListMenu.getPaddingRight(), paddingBottom);
            }
        });
        
        // Set random color
        int color = Config.getRandomThemeColor() ? Theme.getRandomDeepColor() : Config.getThemeColor();
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
        
        // get MangeList
        mTitle = mResources.getString(R.string.homepage);
        setTitle(mTitle);
        firstTimeRefresh();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                
                ListUrls backup = lus;
                lus = new ListUrls(ListUrls.ALL_TYPE, query);
                if (refresh(false)) {
                    mTitle = t;
                    setTitle(mTitle);
                } else {
                    new SuperToast(MangaListActivity.this, R.string.wait_for_last).show();
                    lus = backup;
                }
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
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
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
                && (plateView = (View)mSearchView.findViewById(plateViewID)) != null) {
            plateView.setBackgroundResource(R.drawable.textfield_searchview);
        }
        
        int plateRightViewID = mResources.getIdentifier("android:id/submit_area", null, null);
        View plateRightView = null;
        if (plateRightViewID > 0
                && (plateRightView = (View)mSearchView.findViewById(plateRightViewID)) != null) {
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
        
        return true;
    }
    
    // Double click back exit
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - curBackTime > BACK_PRESSED_INTERVAL) {
            curBackTime = System.currentTimeMillis();
            new SuperToast(MangaListActivity.this, R.string.exit_tip).show();
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
            if (!refresh(false))
                new SuperToast(MangaListActivity.this, R.string.wait).show();
            return true;
        case R.id.action_jump:
            if (!isRefreshing() && isGetGalleryOk())
                jump();
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
    }
    
    private void checkUpdate() {
        new UpdateHelper((AppContext)getApplication())
        .SetOnCheckUpdateListener(new UpdateHelper.OnCheckUpdateListener() {
            @Override
            public void onSuccess(String version, long size,
                    final String url, final String fileName, String info) {
                String sizeStr = Util.sizeToString(size);
                AlertDialog dialog = SuperDialogUtil.createUpdateDialog(MangaListActivity.this,
                        version, sizeStr, info,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((AlertButton)v).dialog.dismiss();
                                // TODO
                                try {
                                    Downloader downloader = new Downloader(MangaListActivity.this);
                                    downloader.resetData(Config.getDownloadPath(), fileName, url);
                                    downloader.setOnDownloadListener(
                                            new UpdateHelper.UpdateListener(MangaListActivity.this,
                                                    fileName));
                                    new Thread(downloader).start();
                                } catch (MalformedURLException e) {
                                    UpdateHelper.setEnabled(true);
                                }
                            }
                        });
                if (!MangaListActivity.this.isFinishing())
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
        if (mEhClient.isLogin())
            setUserPanel(LOGOUT);
        else
            setUserPanel(LOGIN);
    }
    
    private void setUserPanel(int state) {
        
        switch (state) {
        case LOGIN:
            avatar.setImageBitmap(mEhClient.getAvatar());
            userView.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.GONE);
            waitloginoutView.setVisibility(View.GONE);
            break;
        case LOGOUT:
            avatar.setImageBitmap(mEhClient.getAvatar());
            userView.setText(mEhClient.getDisplayname());
            userView.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
            waitloginoutView.setVisibility(View.GONE);
            break;
        case WAIT:
            avatar.setImageBitmap(mEhClient.getAvatar());
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
        
        if (lus.getMode() == ListUrls.POPULAR) {
            mClient.getPopular(new EhClient.OnGetPopularListener() {
                @Override
                public void onSuccess(List<GalleryInfo> gis, long timeStamp) {
                    listener.onSuccess(taskStamp, gis, gis.size(), gis.size() == 0 ? 0 : 1);
                    // Show update time
                    if (timeStamp != -1 && Config.getShowPopularUpdateTime())
                        new SuperToast(MangaListActivity.this,
                                String.format(getString(R.string.popular_update_time),
                                        mAppContext.getDateFormat().format(timeStamp))).show();
                }
                @Override
                public void onFailure(String eMsg) {
                    listener.onFailure(taskStamp, eMsg);
                }
            });
        } else {
            mClient.getGList(url, null, new EhClient.OnGetGListListener() {
                @Override
                public void onSuccess(Object checkFlag, List<GalleryInfo> lmdArray,
                        int indexPerPage, int maxPage) {
                    listener.onSuccess(taskStamp, lmdArray, indexPerPage, maxPage);
                }
                @Override
                public void onFailure(Object checkFlag, String eMsg) {
                    listener.onFailure(taskStamp, eMsg);
                }
            });
        }
    }
}
