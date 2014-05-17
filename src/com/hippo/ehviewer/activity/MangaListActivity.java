package com.hippo.ehviewer.activity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.ImageGeterManager;
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
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.CheckTextView;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.FswListView;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.HfListView;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;
import com.hippo.ehviewer.widget.PrefixEditText;
import com.hippo.ehviewer.widget.SuperDialogUtil;
import com.hippo.ehviewer.widget.TagListView;
import com.hippo.ehviewer.widget.TagsAdapter;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
public class MangaListActivity extends SlidingActivity
        implements View.OnClickListener {
    
    private static String TAG = "MangaListActivity";
    
    private static final int REFRESH = 0x0;
    private static final int NEXT_PAGE = 0x1;
    private static final int PRE_PAGE = 0x2;
    private static final int SOMEWHERE = 0x3;
    private static final int SET_POSITION = 0x8000;
    
    private AppContext mAppContext;
    private EhClient mEhClient;
    private Resources mResources;
    private ImageGeterManager mImageGeterManager;
    
    private SlidingMenu mSlidingMenu;
    
    private LinearLayout mUserPanel;
    private SearchView mSearchView;
    private ListView itemListMenu;
    private TagListView tagListMenu;
    private HfListView mHlv;
    private FswListView mMainList;
    private View waitView;
    private Button freshButton;
    private View noFoundView;
    private ImageView sadpanda;
    
    private TextView userView;
    private Button loginButton;
    private Button registerButton;
    private Button logoutButton;
    private View waitloginoutView;
    
    private TagsAdapter tagsAdapter;

    private ListUrls lus;
    private GmlAdapter gmlAdapter;
    private ArrayList<String> listMenuTag = new ArrayList<String>();
    
    private ArrayList<GalleryInfo> lmdArray = new ArrayList<GalleryInfo>();
    
    
    
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
    
    //
    private int firstPage = 0;
    private int lastPage = 0;
    
    private int firstIndex = 0;
    private int lastIndex = 0;
    private int visiblePage = 0;
    
    private String mTitle;
    
    private int mGetMode;
    
    private int mFswPaddingLeft;
    private int mFswPaddingTop;
    private int mFswPaddingRight;
    private int mFswPaddingBottom;
    
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
                                checkLogin(true);
                            }
                            @Override
                            public void onFailure(String eMsg) {
                                setUserPanel();
                                Toast.makeText(MangaListActivity.this,
                                        eMsg,
                                        Toast.LENGTH_SHORT).show();
                                loginDialog.show();
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
    
    private void checkModeWarning(int mode, View warning) {
        if (mode == 1 && !mEhClient.isLogin())
            warning.setVisibility(View.VISIBLE);
        else
            warning.setVisibility(View.GONE);
    }
    
    private AlertDialog createModeDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.mode, null);
        final TextView warning = (TextView)view.findViewById(R.id.warning);
        final Spinner modeSpinner = (Spinner)view.findViewById(R.id.mode);
        int mode = Config.getMode();
        modeSpinner.setSelection(mode);
        checkModeWarning(mode, warning);
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                checkModeWarning(position, warning);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                warning.setVisibility(View.GONE);
            }
        });
        
        return new DialogBuilder(this).setTitle(R.string.mode)
                .setView(view, true)
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int mode = modeSpinner.getSelectedItemPosition();
                        if (mode > 1) {
                            Toast.makeText(MangaListActivity.this,
                                    R.string.unfinished, Toast.LENGTH_SHORT).show();
                        } else {
                            ((AlertButton)v).dialog.dismiss();
                            Config.setMode(mode);
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                    }
                }).create();
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
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.toast_normal_mode), Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            searchNormal.setVisibility(View.GONE);
                            searchTag.setVisibility(View.VISIBLE);
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.toast_tag_mode), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }).setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ListUrls backup = lus;
                        lus = getLus(filterDialog);
                        if (refresh()) {
                            ((AlertButton)v).dialog.dismiss();
                            showContent();
                            
                            String search = lus.getSearch();
                            String t = null;
                            switch(lus.getMode()) {
                            case ListUrls.NORMAL:
                                if (search == null || search.isEmpty())
                                    t = getString(android.R.string.search_go);
                                else
                                    t = getString(android.R.string.search_go) + " " + search;
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
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.wait_for_last),
                                    Toast.LENGTH_SHORT).show();
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
                        GalleryInfo lmd;
                        switch (position) {
                        case 0: // Add favourite item
                            lmd = lmdArray.get(longClickItemIndex);
                            mData.addLocalFavourite(lmd);
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.toast_add_favourite),
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            lmd = lmdArray.get(longClickItemIndex);
                            Intent it = new Intent(MangaListActivity.this, DownloadService.class);
                            startService(it);
                            mServiceConn.getService().add(String.valueOf(lmd.gid), lmd.thumb, 
                                    EhClient.getDetailUrl(lmd.gid, lmd.token), lmd.title);
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.toast_add_download),
                                    Toast.LENGTH_SHORT).show();
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
        tv.setText(String.format(getString(R.string.jump_sum), lus.getMax()));
        tv = (TextView)view.findViewById(R.id.list_jump_to);
        tv.setText(R.string.jump_to);
        final EditText et = (EditText)view.findViewById(R.id.list_jump_edit);
        
        return new DialogBuilder(this).setTitle(R.string.jump)
                .setView(view, true)
                .setPositiveButton(android.R.string.ok,
                new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        try{
                            int targetPage = Integer.parseInt(et.getText().toString()) - 1;
                            if (targetPage >= firstPage
                                    && targetPage <= lastPage) {   // If targetPage is in range
                                ((AlertButton)v).dialog.dismiss(); // Just jump there
                                int position = (targetPage - firstPage) *
                                        lus.getNumPerPage();
                                setMainListPosition(position);
                            } else{
                                if (lus.setPage(targetPage)) {
                                    ((AlertButton)v).dialog.dismiss();
                                    
                                    waitView.setVisibility(View.GONE);
                                    freshButton.setVisibility(View.GONE);
                                    noFoundView.setVisibility(View.GONE);
                                    sadpanda.setVisibility(View.GONE);
                                    mHlv.setRefreshing(true);
                                    
                                    if (targetPage == firstPage - 1)
                                        mGetMode = PRE_PAGE;
                                    else if (targetPage == lastPage + 1)
                                        mGetMode = NEXT_PAGE;
                                    else
                                        mGetMode = SOMEWHERE;
                                    mGetMode |= SET_POSITION;
                                    
                                    getList();
                                } else {
                                    Toast.makeText(MangaListActivity.this,
                                            getString(R.string.toast_invalid_page),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch(Exception e) {
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.toast_invalid_page),
                                    Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.tag_name_empty),
                                    Toast.LENGTH_SHORT).show();
                        else if (listMenuTag.contains(key) && !key.equals(oldStr))
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.tag_name_exist),
                                    Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.toast_normal_mode), Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            searchNormal.setVisibility(View.GONE);
                            searchTag.setVisibility(View.VISIBLE);
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.toast_tag_mode), Toast.LENGTH_SHORT)
                                    .show();
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
    
    private void setMainListPosition(int position) {
        if (position == 0)
            mMainList.setSelectionFromTop(position, mFswPaddingTop);
        else
            mMainList.setSelectionFromTop(position, 0);
    }
    
    private class MangaListGetListener implements
            EhClient.OnGetMangaListListener {
        @Override
        public void onSuccess(Object checkFlag, ArrayList<GalleryInfo> newLmdArray,
                int indexPerPage, int maxPage) {
            
            // Check no Found view later
            waitView.setVisibility(View.GONE);
            freshButton.setVisibility(View.GONE);
            
            if (maxPage == 0) { // If No hits found
                mHlv.setVisibility(View.GONE);
                noFoundView.setVisibility(View.VISIBLE);
                sadpanda.setVisibility(View.GONE);
                
                mTitle = mResources.getString(R.string.no_found);
                setTitle();
                
                lmdArray.clear();
                gmlAdapter.notifyDataSetChanged();
            } else if (maxPage == -1) { //panda
                mHlv.setVisibility(View.GONE);
                noFoundView.setVisibility(View.GONE);
                sadpanda.setVisibility(View.VISIBLE);
                
                mTitle = mResources.getString(R.string.sadpanda);
                setTitle();
                
                lmdArray.clear();
                gmlAdapter.notifyDataSetChanged();
            } else {
                mHlv.setVisibility(View.VISIBLE);
                noFoundView.setVisibility(View.GONE);
                sadpanda.setVisibility(View.GONE);
                
                // Set indexPerPage and maxPage
                lus.setNumPerPage(indexPerPage);
                lus.setMax(maxPage);
                
                // Check refresh or get more
                switch (mGetMode & (~SET_POSITION)) {
                case REFRESH:
                    firstPage = 0;
                    lastPage = 0;
                    lmdArray.clear();
                    lmdArray.addAll(newLmdArray);
                    gmlAdapter.notifyDataSetChanged();
                    
                    // set visible page
                    firstIndex = 0;
                    lastIndex = newLmdArray.size() - 1;
                    visiblePage = 0;
                    
                    // set title
                    setTitle();
                    
                    // set main list positon
                    setMainListPosition(0);
                    break;
                case PRE_PAGE:
                    firstPage -= 1;
                    lmdArray.addAll(0, newLmdArray);
                    gmlAdapter.notifyDataSetChanged();
                    
                    // set position if necessary and set visible page
                    if ((mGetMode & SET_POSITION) != 0) {
                        firstIndex = 0;
                        lastIndex = newLmdArray.size()-1;
                        visiblePage = lus.getPage();
                        
                        setMainListPosition(0);
                        setTitle();
                    } else {
                        firstIndex += newLmdArray.size();
                        lastIndex += newLmdArray.size();
                        
                        setMainListPosition(
                                mMainList.getFirstVisiblePosition() + newLmdArray.size());
                    }
                    break;
                case NEXT_PAGE:
                    lastPage += 1;
                    lmdArray.addAll(newLmdArray);
                    gmlAdapter.notifyDataSetChanged();
                    
                    // set position if necessary and set visible page
                    if ((mGetMode & SET_POSITION) != 0) {
                        firstIndex = lmdArray.size() - newLmdArray.size();
                        lastIndex = lmdArray.size() - 1;
                        visiblePage = lus.getPage();
                        
                        setMainListPosition(firstIndex);
                        setTitle();
                    }
                    break;
                case SOMEWHERE:
                    firstPage = lus.getPage();
                    lastPage = lus.getPage();
                    lmdArray.clear();
                    lmdArray.addAll(newLmdArray);
                    gmlAdapter.notifyDataSetChanged();
                    
                    // Get visible page
                    firstIndex = 0;
                    lastIndex = newLmdArray.size();
                    visiblePage = lus.getPage();
                    
                    setMainListPosition(0);
                    setTitle();
                    break;
                default:
                    break;
                }
            }
            
            mHlv.setAnyRefreshComplete(true);
        }

        @Override
        public void onFailure(Object checkFlag, String eMsg) {
            // Check pull list view later
            // Check fresh view later
            waitView.setVisibility(View.GONE);
            noFoundView.setVisibility(View.GONE);
            sadpanda.setVisibility(View.GONE);
            
            switch (mGetMode & (~SET_POSITION)) {
            case REFRESH:
                mHlv.setVisibility(View.GONE);
                freshButton.setVisibility(View.VISIBLE);
                
                Log.d(TAG, "error");
                
                Toast.makeText(MangaListActivity.this,
                        eMsg, Toast.LENGTH_SHORT)
                        .show();
                lmdArray.clear();
                gmlAdapter.notifyDataSetChanged();
                break;
            default:
                Log.d(TAG, "error " + mGetMode);
                freshButton.setVisibility(View.GONE);
                Toast.makeText(
                        MangaListActivity.this,
                        eMsg,
                        Toast.LENGTH_SHORT).show();
                break;
            }
            mHlv.setAnyRefreshComplete(false);
        }
    }
    
    private class MangaListListener implements ListView.OnScrollListener {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            mHlv.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

            if (lus == null || visibleItemCount < 2)
                return;
            if (lastIndex == 0)
                lastIndex = lus.getNumPerPage() - 1;

            int pageChanged = (firstVisibleItem - firstIndex)
                    / lus.getNumPerPage();
            if (pageChanged == 0)
                pageChanged = (firstVisibleItem + visibleItemCount - lastIndex - 1)
                        / lus.getNumPerPage();
            
            if (pageChanged != 0) {
                visiblePage = visiblePage + pageChanged;
                firstIndex += pageChanged * lus.getNumPerPage();
                lastIndex += pageChanged * lus.getNumPerPage();
                setTitle();
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {}
    }

    private class GmlAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public GmlAdapter() {
            mInflater = LayoutInflater.from(MangaListActivity.this);
        }

        @Override
        public int getCount() {
            return lmdArray.size();
        }

        @Override
        public Object getItem(int arg0) {
            return lmdArray.get(arg0);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GalleryInfo lmd= lmdArray.get(position);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item, null);
            }
            final LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.cover);
            if (!String.valueOf(lmd.gid).equals(thumb.getKey())) {
                
                Bitmap bmp = null;
                if (Cache.memoryCache != null &&
                        (bmp = Cache.memoryCache.get(String.valueOf(lmd.gid))) != null) {
                    thumb.setLoadInfo(lmd.thumb, String.valueOf(lmd.gid));
                    thumb.setImageBitmap(bmp);
                    thumb.setState(LoadImageView.LOADED);
                } else {
                    thumb.setImageDrawable(null);
                    thumb.setLoadInfo(lmd.thumb, String.valueOf(lmd.gid));
                    thumb.setState(LoadImageView.NONE);
                    mImageGeterManager.add(lmd.thumb, String.valueOf(lmd.gid),
                            ImageGeterManager.DISK_CACHE | ImageGeterManager.DOWNLOAD,
                            new LoadImageView.SimpleImageGetListener(thumb));
                }
                
                // Set manga name
                TextView name = (TextView) convertView.findViewById(R.id.name);
                name.setText(lmd.title);
                
                // Set uploder
                TextView uploader = (TextView) convertView.findViewById(R.id.uploader);
                uploader.setText(lmd.uploader);
                
                // Set category
                TextView category = (TextView) convertView.findViewById(R.id.category);
                String newText = Ui.getCategoryText(lmd.category);
                if (!newText.equals(category.getText())) {
                    category.setText(newText);
                    category.setBackgroundColor(Ui.getCategoryColor(lmd.category));
                }
                
                // Add star
                RatingBar rate = (RatingBar) convertView
                        .findViewById(R.id.rate);
                rate.setRating(lmd.rating);
                
                // set posted
                TextView posted = (TextView) convertView.findViewById(R.id.posted);
                posted.setText(lmd.posted);
            }
            return convertView;
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mSlidingMenu.setBehindWidth(
                mResources.getDimensionPixelOffset(R.dimen.menu_offset));
    }
    
    private void handleIntent(Intent intent) { 
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
           String query = 
                 intent.getStringExtra(SearchManager.QUERY); 
           mSearchView.setQuery(query, true);
        } 
     }
    
    @Override
    protected void onResume() {
        super.onResume();
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
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
            setUserPanel(WAIT);
            
            mEhClient.logout(new EhClient.OnLogoutListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MangaListActivity.this,
                            getString(R.string.toast_logout_succeeded),
                            Toast.LENGTH_SHORT).show();
                    Config.logoutNow();
                    setUserPanel();
                }

                @Override
                public void onFailure(String eMsg) {
                    Toast.makeText(MangaListActivity.this,
                            eMsg,
                            Toast.LENGTH_SHORT).show();
                    setUserPanel();
                }
            });
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
        setContentView(R.layout.list);
        
        mAppContext = (AppContext)getApplication();
        mData = mAppContext.getData();
        mEhClient = mAppContext.getEhClient();
        mImageGeterManager = mAppContext.getImageGeterManager();
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
            }
        });
        mSlidingMenu.setOnClosedListener(new SlidingMenu.OnClosedListener() {
            @Override
            public void onClosed() {
                setTitle();
                invalidateOptionsMenu();
            }
        });
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        Ui.translucent(this);
        
        // Download service
        Intent it = new Intent(MangaListActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        // Get url
        Intent intent = getIntent();
        int type = intent.getIntExtra("type", ListUrls.ALL_TYPE);
        String search = intent.getStringExtra("search");
        int page = intent.getIntExtra("page", 0);
        lus = new ListUrls(type, search, page);
        visiblePage = lus.getPage();
        
        // Init dialog
        loginDialog = createLoginDialog();
        filterDialog = createFilterDialog();
        longClickDialog = createLongClickDialog();
        
        
        // Get View
        mUserPanel = (LinearLayout)findViewById(R.id.user_panel);
        itemListMenu = (ListView) findViewById(R.id.list_menu_item_list);
        tagListMenu = (TagListView) findViewById(R.id.list_menu_tag_list);
        mHlv = (HfListView)findViewById(R.id.list_list);
        mMainList = mHlv.getListView();
        waitView = (View) findViewById(R.id.list_wait_first);
        freshButton = (Button) findViewById(R.id.list_refresh);
        noFoundView = (View) findViewById(R.id.list_no_found);
        sadpanda = (ImageView) findViewById(R.id.sadpanda);
        
        userView = (TextView)mUserPanel.findViewById(R.id.user);
        loginButton = (Button)mUserPanel.findViewById(R.id.login);
        registerButton = (Button)mUserPanel.findViewById(R.id.register);
        logoutButton = (Button)mUserPanel.findViewById(R.id.logout);
        waitloginoutView = mUserPanel.findViewById(R.id.wait);
        
        loginButton.setOnClickListener(this);
        registerButton.setOnClickListener(this);
        logoutButton.setOnClickListener(this);
        
        // Drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        
        // leftDrawer
        final int[] data = {R.drawable.ic_action_home, R.string.homepage,
                R.drawable.ic_action_panda, R.string.mode,
                R.drawable.ic_action_search, android.R.string.search_go,
                R.drawable.ic_action_favorite, R.string.favourite,
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
                    if (refresh()) {
                        mTitle = mResources.getString(R.string.homepage);
                        setTitle(mTitle);
                        
                        showContent();
                    } else {
                        Toast.makeText(MangaListActivity.this,
                                getString(R.string.wait_for_last),
                                Toast.LENGTH_SHORT).show();
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
                    showContent();
                    break;
                    
                case 4:
                    showContent();
                    intent = new Intent(MangaListActivity.this,
                            DownloadActivity.class);
                    startActivity(intent);
                    break;
                    
                case 5:
                    showContent();
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
                if (lus != null && refresh()) {
                    mTitle = listMenuTag.get(position);
                    setTitle(mTitle);
                    showContent();
                } else {
                    Toast.makeText(MangaListActivity.this,
                            getString(R.string.wait_for_last),
                            Toast.LENGTH_SHORT).show();
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
        
        // Pull list view
        // Setup ActionBarPullToRefresh
        mHlv.setColorScheme(R.color.refresh_color_1, R.color.refresh_color_2, R.color.refresh_color_3, R.color.refresh_color_4);
        mHlv.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (firstPage != 0) {
                    lus.setPage(firstPage - 1);
                    mGetMode = PRE_PAGE;
                    getList();
                } else {
                    lus.setPage(0);
                    mGetMode = REFRESH;
                    getList();
                }
            }
        });
        mHlv.setOnFooterRefreshListener(new HfListView.OnFooterRefreshListener() {
            @Override
            public boolean onFooterRefresh() {
                if (lus.setPage(lastPage+1)) {
                    mGetMode = NEXT_PAGE;
                    getList();
                    return true;
                } else {
                    return false;
                }
            }
        });
        mHlv.setFooterString(getString(R.string.footer_loading),
                getString(R.string.footer_loaded),
                getString(R.string.footer_fail));
        
        // Listview
        gmlAdapter = new GmlAdapter();
        mMainList.setDivider(null);
        mMainList.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mMainList.setClipToPadding(false);
        mMainList.setAdapter(gmlAdapter);
        mMainList.setOnScrollListener(new MangaListListener());
        mMainList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(MangaListActivity.this,
                        MangaDetailActivity.class);
                GalleryInfo gi = lmdArray.get(position);
                intent.putExtra("url", EhClient.getDetailUrl(gi.gid, gi.token));
                intent.putExtra(MangaDetailActivity.KEY_G_INFO, gi);
                startActivity(intent);
            }
        });
        mMainList.setOnItemLongClickListener(new OnItemLongClickListener() {
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
                mFswPaddingLeft = paddingLeft;
                mFswPaddingTop = paddingTop;
                mFswPaddingRight = paddingRight;
                mFswPaddingBottom = paddingBottom;
                
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mHlv.getLayoutParams();
                lp.topMargin = mFswPaddingTop;
                mMainList.setPadding(mMainList.getPaddingLeft(), mMainList.getPaddingTop(),
                        mMainList.getPaddingRight(), mFswPaddingBottom);
                mUserPanel.setPadding(mUserPanel.getPaddingLeft(), mFswPaddingTop,
                        mUserPanel.getPaddingRight(), mUserPanel.getPaddingBottom());
                itemListMenu.setPadding(itemListMenu.getPaddingLeft(), itemListMenu.getPaddingTop(),
                        itemListMenu.getPaddingRight(), mFswPaddingBottom);
                tagListMenu.setPadding(tagListMenu.getPaddingLeft(), mFswPaddingTop,
                        tagListMenu.getPaddingRight(), mFswPaddingBottom);
            }
        });
        
        
        // Check update
        checkUpdate();
        
        // Check login and update user panel
        checkLogin(false);
        
        // get MangeList
        mTitle = mResources.getString(R.string.homepage);
        setTitle();
        refresh(false);
        
        // set layout
        mHlv.setVisibility(View.GONE);
        waitView.setVisibility(View.VISIBLE);
        freshButton.setVisibility(View.GONE);
        noFoundView.setVisibility(View.GONE);
        sadpanda.setVisibility(View.GONE);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                    t = getString(android.R.string.search_go) + " " + query;
                
                ListUrls backup = lus;
                lus = new ListUrls(ListUrls.ALL_TYPE, query);
                if (refresh()) {
                    mTitle = t;
                    setTitle(mTitle);
                } else {
                    Toast.makeText(MangaListActivity.this,
                            getString(R.string.wait_for_last),
                            Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, getString(R.string.exit_tip), Toast.LENGTH_SHORT).show();
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
            if (!refresh())
                Toast.makeText(MangaListActivity.this,
                        getString(R.string.wait),
                        Toast.LENGTH_SHORT).show();
            return true;
        case R.id.action_jump:
            if (!mHlv.isAnyRefreshing() && isGetOk())
                jump();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public boolean isGetOk() {
        return mHlv.getVisibility() == View.VISIBLE;
    }
    
    public void setTitle() {
        if (isGetOk())
            setTitle(String.format(getString(R.string.list_title), mTitle, visiblePage + 1));
        else
            setTitle(mTitle);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
        
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
        }
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
                        }, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((AlertButton)v).dialog.dismiss();
                                UpdateHelper.setEnabled(true);
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
    
    private void checkLogin(boolean force) {
        
        if ((Config.isLogin() || force)
                && ! mEhClient.isLogin()) {
            setUserPanel(WAIT);
            mEhClient.checkLogin(new EhClient.OnCheckLoginListener() {
                @Override
                public void onSuccess() {
                    Config.loginNow();
                    Toast.makeText(MangaListActivity.this,
                            getString(R.string.toast_login_succeeded),
                            Toast.LENGTH_SHORT).show();
                    setUserPanel();
                }

                @Override
                public void onFailure(String eMsg) {
                    setUserPanel();
                    Toast.makeText(MangaListActivity.this,
                            eMsg,
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            setUserPanel();
        }
        
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
            userView.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.GONE);
            waitloginoutView.setVisibility(View.GONE);
            break;
        case LOGOUT:
            userView.setText(mEhClient.getUsername());
            userView.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
            waitloginoutView.setVisibility(View.GONE);
            break;
        case WAIT:
            userView.setVisibility(View.GONE);
            loginButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
            waitloginoutView.setVisibility(View.VISIBLE);
            break;
        }
    }
    
    private void getList() {
        mEhClient.getMangaList(lus.getUrl(),
                null,
                new MangaListGetListener());
    }
    
    public boolean refresh() {
        return refresh(true);
    }
    
    public boolean refresh(boolean isShowProgress) {
        if (!mHlv.isAnyRefreshing()
                && waitView.getVisibility() != View.VISIBLE) {
            lus.setPage(0);
            if (isShowProgress)
                mHlv.setRefreshing(true);
            mGetMode = REFRESH;
            getList();
            return true;
        } else {
            return false;
        }
    }
    
    public void buttonRefresh(View arg0) {
        refresh(false);
        freshButton.setVisibility(View.GONE);
        waitView.setVisibility(View.VISIBLE);
    }
    
    public void buttonCheckLogin(View v) {
        checkLogin(false);
    }
}
