package com.hippo.ehviewer.activity;

import java.util.ArrayList;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.ImageLoadManager;
import com.hippo.ehviewer.ListMangaDetail;
import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UpdateHelper;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhClient;
import com.hippo.ehviewer.util.Favourite;
import com.hippo.ehviewer.util.Tag;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.view.AlertButton;
import com.hippo.ehviewer.view.CheckImage;
import com.hippo.ehviewer.view.GetPaddingRelativeLayout;
import com.hippo.ehviewer.view.TagListView;
import com.hippo.ehviewer.view.TagsAdapter;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.DrawerLayout;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.PullListView;
import com.hippo.ehviewer.widget.SuperDialogUtil;
import com.hippo.ehviewer.widget.PullListView.OnFooterRefreshListener;
import com.hippo.ehviewer.widget.PullListView.OnHeaderRefreshListener;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.Gravity;
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
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// TODO check visiblePage is right or not
// TODO Good resource
// TODO http://lofi.e-hentai.org/
// TODO http://exhentai.org/
// TODO add lock to get list

public class MangaListActivity extends Activity {
    @SuppressWarnings("unused")
    private static String TAG = "MangaListActivity";
    
    private AppContext mAppContext;
    private EhClient mEhClient;
    
    private DrawerLayout drawer;
    private GetPaddingRelativeLayout mainLayout;
    private TagListView listMenu;
    private RelativeLayout loginMenu;
    private ListView isExhentaiList;
    private PullListView pullListView;
    private ListView listView;
    private View waitView;
    private Button freshButton;
    private View noFoundView;
    private ImageView sadpanda;
    private ViewGroup loginView;
    private ViewGroup loginOverView;
    private Button loginButton;
    private Button checkLoginButton;
    private View waitloginView;
    private TextView usernameText;
    private Button logoutButton;
    private View waitlogoutView;
    
    private TagsAdapter tagsAdapter;

    private ListUrls lus;
    private GmlAdapter gmlAdapter;
    private ArrayList<String> listMenuTitle = new ArrayList<String>();
    private int mStableItemCount;
    
    private ArrayList<ListMangaDetail> lmdArray = new ArrayList<ListMangaDetail>();
    
    private ImageLoadManager mImageLoadManager;
    
    
    private int longClickItemIndex;

    private boolean mListFirst = true;
    private boolean mLoadListOver = false;
    
    private AlertDialog loginDialog;
    private AlertDialog filterDialog;
    private AlertDialog longClickDialog;
    
    // Modify tag
    private String newTagName = null;
    
    // Double click back exit
    private long curBackTime = 0;
    private static final int BACK_PRESSED_INTERVAL = 2000;
    
    private long lastGetStamp;
    
    //
    private int firstPage = 0;
    private int lastPage = 0;
    
    private int firstIndex = 0;
    private int lastIndex = 0;
    private int visiblePage = 0;
    
    private String title;
    
    private class RefreshPackage {
        public ListUrls listUrls;
        public String title;
        
        public RefreshPackage(ListUrls listUrls, String title) {
            this.listUrls = listUrls;
            this.title = title;
        }
    }
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
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
                        loginButton.setVisibility(View.GONE);
                        waitloginView.setVisibility(View.VISIBLE);
                        String username = ((EditText) loginDialog.findViewById(R.id.username)).getText().toString();
                        String password = ((EditText) loginDialog.findViewById(R.id.password)).getText().toString();
                        mEhClient.login(username, password, new EhClient.OnLoginListener() {
                            @Override
                            public void onSuccess() {
                                checkLogin(true);
                            }
                            @Override
                            public void onFailure(String eMsg) {
                                loginButton.setVisibility(View.VISIBLE);
                                waitloginView.setVisibility(View.GONE);
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
                        layoutDrawRight();
                    }
                }).create();
    }
    
    private AlertDialog createFilterDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.filter, null);
        
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
        
        return new DialogBuilder(this).setTitle(android.R.string.search_go)
                .setView(view, false)
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        drawer.closeDrawers();
                        ListUrls listUrls = getLus(filterDialog);
                        String search = listUrls.getSearch();
                        String t = null;
                        if (search == null || search.isEmpty())
                            t = getString(android.R.string.search_go);
                        else
                            t = getString(android.R.string.search_go) + " " + search;
                        refresh(listUrls, t);
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
                                if (Tag.add(newName, getLus(filterDialog))) {
                                    listMenuTitle.add(newName);
                                    tagsAdapter.addId(newName);
                                    tagsAdapter.notifyDataSetChanged();
                                }
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
        CheckImage checkImageDoujinshi = (CheckImage) view
                .findViewById(R.id.button_doujinshi);
        CheckImage checkImageManga = (CheckImage) view
                .findViewById(R.id.button_manga);
        CheckImage checkImageArtistcg = (CheckImage) view
                .findViewById(R.id.button_artistcg);
        CheckImage checkImageGamecg = (CheckImage) view
                .findViewById(R.id.button_gamecg);
        CheckImage checkImageWestern = (CheckImage) view
                .findViewById(R.id.button_western);
        CheckImage checkImageNonH = (CheckImage) view
                .findViewById(R.id.button_non_h);
        CheckImage checkImageImageset = (CheckImage) view
                .findViewById(R.id.button_imageset);
        CheckImage checkImageCosplay = (CheckImage) view
                .findViewById(R.id.button_cosplay);
        CheckImage checkImageAsianporn = (CheckImage) view
                .findViewById(R.id.button_asianporn);
        CheckImage checkImageMisc = (CheckImage) view
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

        ListUrls lus = new ListUrls(type, et.getText().toString());
        
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
        return lus;
    }
    
    private AlertDialog createLongClickDialog() {
        return new DialogBuilder(this).setTitle(R.string.what_to_do)
                .setItems(R.array.list_item_long_click,
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int position, long arg3) {
                        ListMangaDetail lmd;
                        switch (position) {
                        case 0: // Add favourite item
                            lmd = lmdArray.get(longClickItemIndex);
                            Favourite.push(lmd);
                            Toast.makeText(MangaListActivity.this,
                                    getString(R.string.toast_add_favourite),
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            lmd = lmdArray.get(longClickItemIndex);
                            Intent it = new Intent(MangaListActivity.this, DownloadService.class);
                            startService(it);
                            mServiceConn.getService().add(lmd.gid, lmd.thumb, 
                                    EhClient.detailHeader + lmd.gid + "/" + lmd.token, lmd.title);
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
                                listView.setSelectionFromTop(position, -1);
                            } else{
                                ListUrls listUrls = lus.clone();
                                if (listUrls.setPage(targetPage)) {
                                    ((AlertButton)v).dialog.dismiss();
                                    mListFirst = true;
                                    mLoadListOver = false;
                                    
                                    waitView.setVisibility(View.GONE);
                                    freshButton.setVisibility(View.GONE);
                                    noFoundView.setVisibility(View.GONE);
                                    sadpanda.setVisibility(View.GONE);
                                    if (pullListView.clickHeaderRefresh(new RefreshPackage(listUrls, title)))
                                        pullListView.setHeaderString(
                                                "下拉加载...",
                                                "释放加载...",
                                                "正在加载...",
                                                "完成加载",
                                                "取消加载"); // TODO
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
                        else if (listMenuTitle.contains(key) && !key.equals(oldStr))
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
        final View view = inflater.inflate(R.layout.filter, null);
        ListUrls listUrls = Tag.get(position - mStableItemCount);
        setFilterView(view, listUrls);
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
        if (cb.isChecked())
            advance.setVisibility(View.VISIBLE);
        
        return new DialogBuilder(this).setTitle(listMenuTitle.get(position))
                .setView(view, false)
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        ListUrls listUrls = getLus(view);
                        if (newTagName != null) {
                            tagsAdapter.set(listMenuTitle.get(position), newTagName);
                            Tag.set(position - mStableItemCount, newTagName, listUrls);
                            listMenuTitle.set(position, newTagName);
                            tagsAdapter.notifyDataSetChanged();
                            
                            newTagName = null;
                        } else
                            Tag.set(position - mStableItemCount, listUrls);
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
                        String hint = newTagName == null ? listMenuTitle.get(position) : newTagName;
                        createSetNameDialog(hint, listMenuTitle.get(position), new OnSetNameListener(){
                            @Override
                            public void onSetVaildName(String newName) {
                                if (newName.equals(listMenuTitle.get(position))) // If new is old name
                                    SuperDialogUtil.setTitle(((AlertButton)v).dialog,
                                            listMenuTitle.get(position));
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
        // Normal
        CheckImage checkImageDoujinshi = (CheckImage) view
                .findViewById(R.id.button_doujinshi);
        CheckImage checkImageManga = (CheckImage) view
                .findViewById(R.id.button_manga);
        CheckImage checkImageArtistcg = (CheckImage) view
                .findViewById(R.id.button_artistcg);
        CheckImage checkImageGamecg = (CheckImage) view
                .findViewById(R.id.button_gamecg);
        CheckImage checkImageWestern = (CheckImage) view
                .findViewById(R.id.button_western);
        CheckImage checkImageNonH = (CheckImage) view
                .findViewById(R.id.button_non_h);
        CheckImage checkImageImageset = (CheckImage) view
                .findViewById(R.id.button_imageset);
        CheckImage checkImageCosplay = (CheckImage) view
                .findViewById(R.id.button_cosplay);
        CheckImage checkImageAsianporn = (CheckImage) view
                .findViewById(R.id.button_asianporn);
        CheckImage checkImageMisc = (CheckImage) view
                .findViewById(R.id.button_misc);

        int type = listUrls.getType();
        if ((type & ListUrls.DOUJINSHI) == 0)
            checkImageDoujinshi.pressed();
        else
            checkImageDoujinshi.unpressed();
        if ((type & ListUrls.MANGA) == 0)
            checkImageManga.pressed();
        else
            checkImageManga.unpressed();
        if ((type & ListUrls.ARTIST_CG) == 0)
            checkImageArtistcg.pressed();
        else
            checkImageArtistcg.unpressed();
        if ((type & ListUrls.GAME_CG) == 0)
            checkImageGamecg.pressed();
        else
            checkImageGamecg.unpressed();
        if ((type & ListUrls.WESTERN) == 0)
            checkImageWestern.pressed();
        else
            checkImageWestern.unpressed();
        if ((type & ListUrls.NON_H) == 0)
            checkImageNonH.pressed();
        else
            checkImageNonH.unpressed();
        if ((type & ListUrls.IMAGE_SET) == 0)
            checkImageImageset.pressed();
        else
            checkImageImageset.unpressed();
        if ((type & ListUrls.COSPLAY) == 0)
            checkImageCosplay.pressed();
        else
            checkImageCosplay.unpressed();
        if ((type & ListUrls.ASIAN_PORN) == 0)
            checkImageAsianporn.pressed();
        else
            checkImageAsianporn.unpressed();
        if ((type & ListUrls.MISC) == 0)
            checkImageMisc.pressed();
        else
            checkImageMisc.unpressed();
        

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
        spinnerMinRating.setSelection(listUrls.getMinRating() - 2);
    }
    
    private class MangaListGetPackage {
        public long stamp;
        public ListUrls targetListUrls;
        public boolean setPosition;
        public String title;
        public MangaListGetPackage(long stamp, ListUrls targetListUrls,
                boolean setPosition, String title) {
            this.stamp = stamp;
            this.targetListUrls = targetListUrls;
            this.setPosition = setPosition;
            this.title = title;
        }
    }
    
    private class MangaListGetListener implements
            EhClient.OnGetMangaListListener {
        @Override
        public void onSuccess(Object checkFlag, ArrayList<ListMangaDetail> newLmdArray,
                int indexPerPage, int maxPage) {
            MangaListGetPackage getPackage = (MangaListGetPackage)checkFlag;
            if (getPackage.stamp != lastGetStamp)
                return;
            lus = getPackage.targetListUrls;
            title = getPackage.title;
            // Check no Found view later
            waitView.setVisibility(View.GONE);
            freshButton.setVisibility(View.GONE);
            
            if (maxPage == 0) { // If No hits found
                mLoadListOver = false;
                
                pullListView.setVisibility(View.GONE);
                noFoundView.setVisibility(View.VISIBLE);
                sadpanda.setVisibility(View.GONE);
                setTitle(R.string.no_found);
                lmdArray.clear();
                gmlAdapter.notifyDataSetChanged();
            } else if (maxPage == -1) { //panda
                mLoadListOver = false;
                
                pullListView.setVisibility(View.GONE);
                noFoundView.setVisibility(View.GONE);
                sadpanda.setVisibility(View.VISIBLE);
                setTitle(R.string.sadpanda);
                lmdArray.clear();
                gmlAdapter.notifyDataSetChanged();
            } else {
                mLoadListOver = true;
                
                pullListView.setVisibility(View.VISIBLE);
                noFoundView.setVisibility(View.GONE);
                sadpanda.setVisibility(View.GONE);
                // Set indexPerPage and maxPage
                lus.setNumPerPage(indexPerPage);
                lus.setMax(maxPage);
                
                // Check refresh or get more
                int getPageIndex = lus.getPage();
                if (getPageIndex == 0 && firstPage == 0) { // Refresh
                    firstPage = 0;
                    lastPage = 0;
                    lmdArray.clear();
                    lmdArray.addAll(newLmdArray);
                    gmlAdapter.notifyDataSetChanged();
                    
                    // Get visible page
                    firstIndex = 0;
                    lastIndex = newLmdArray.size() - 1;
                    visiblePage = 0;
                    setTitle(String.format(getString(R.string.list_title), title, visiblePage + 1));
                    
                    // Go to top
                    listView.setSelection(0);
                } else if (getPageIndex == firstPage - 1) { // Get last page
                    firstPage = getPageIndex;
                    lmdArray.addAll(0, newLmdArray);
                    gmlAdapter.notifyDataSetChanged();
                    
                    if (getPackage.setPosition) {
                        firstIndex = 0;
                        lastIndex = newLmdArray.size()-1;
                        visiblePage = getPageIndex;
                        setTitle(String.format(getString(R.string.list_title), title, visiblePage + 1));
                        // Go to top
                        listView.setSelection(0);
                    } else {
                        firstIndex += newLmdArray.size();
                        lastIndex += newLmdArray.size();
                        // Stay there
                        listView.setSelectionFromTop(listView.getFirstVisiblePosition() + newLmdArray.size(), -1);
                    }
                } else if (getPageIndex == lastPage + 1) { // Get next page
                    lastPage = getPageIndex;
                    lmdArray.addAll(newLmdArray);
                    gmlAdapter.notifyDataSetChanged();
                    
                    if (getPackage.setPosition) { // Go to next page top
                        firstIndex = lmdArray.size() - newLmdArray.size();
                        lastIndex = lmdArray.size() - 1;
                        visiblePage = getPageIndex;
                        setTitle(String.format(getString(R.string.list_title), title, visiblePage + 1));
                        listView.setSelectionFromTop(firstIndex, -1);
                    }
                } else if (getPageIndex < firstPage - 1 ||
                        getPageIndex > lastPage + 1){ // Jump somewhere
                    firstPage = getPageIndex;
                    lastPage = getPageIndex;
                    lmdArray.clear();
                    lmdArray.addAll(newLmdArray);
                    gmlAdapter.notifyDataSetChanged();
                    
                    // Get visible page
                    firstIndex = 0;
                    lastIndex = newLmdArray.size();
                    visiblePage = getPageIndex;
                    setTitle(String.format(getString(R.string.list_title), title, visiblePage + 1));
                    
                    // Go to top
                    listView.setSelection(0);
                }
            }
            // Reset pullListView
            if (pullListView.isHeaderRefreshing())
                pullListView.onHeaderRefreshComplete();
            else if (pullListView.isFooterRefreshing())
                pullListView.onFooterRefreshComplete(true);
            if (lus.getPage() == lus.getMax() - 1)
                pullListView.setActionWhenShow(false);
            
            // Reset pull string
            setHeaderPullString();
        }

        @Override
        public void onFailure(Object checkFlag, String eMsg) {
            MangaListGetPackage getPackage = (MangaListGetPackage)checkFlag;
            if (getPackage.stamp != lastGetStamp)
                return;
            
            // Check pull list view later
            // Check fresh view later
            waitView.setVisibility(View.GONE);
            noFoundView.setVisibility(View.GONE);
            sadpanda.setVisibility(View.GONE);
            
            int getPageIndex = getPackage.targetListUrls.getPage();
            
            if (getPageIndex == 0 && firstPage == 0) { // Refresh
                mLoadListOver = false;
                // Only show freshButton
                pullListView.setVisibility(View.GONE);
                freshButton.setVisibility(View.VISIBLE);
                Toast.makeText(MangaListActivity.this,
                        eMsg, Toast.LENGTH_SHORT)
                        .show();
                lmdArray.clear();
                gmlAdapter.notifyDataSetChanged();
            } else {// List is not empty
                // Only show freshButton
                freshButton.setVisibility(View.GONE);
                Toast.makeText(
                        MangaListActivity.this,
                        eMsg,
                        Toast.LENGTH_SHORT).show();
            }
            
            // Reset pullListView
            if (pullListView.isHeaderRefreshing())
                pullListView.onHeaderRefreshComplete();
            else if (pullListView.isFooterRefreshing())
                pullListView.onFooterRefreshComplete(false);
        }
        
        private void setHeaderPullString() {
            if (firstPage == 0)
                pullListView.setHeaderString(getString(R.string.pull_refresh_pull),
                        getString(R.string.pull_refresh_release),
                        getString(R.string.pull_refresh_doing),
                        getString(R.string.pull_refresh_done),
                        getString(R.string.pull_refresh_cancel));
            else
                pullListView.setHeaderString(getString(R.string.pull_pre_pull),
                        getString(R.string.pull_pre_release),
                        getString(R.string.pull_pre_doing),
                        getString(R.string.pull_pre_done),
                        getString(R.string.pull_pre_cancel));
        }
    }

    private class MangaListListener implements ListView.OnScrollListener {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            pullListView.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            // First time when list is created
            if (mListFirst && mLoadListOver) {
                // load image
                int getChildCount = view.getChildCount();
                for (int i = 0; i < getChildCount; i++) {
                    View v = ((ViewGroup) view.getChildAt(i)).getChildAt(0);
                    if (v instanceof LoadImageView) {
                        LoadImageView liv = (LoadImageView)v;
                        int status = liv.getState();
                        if (status != LoadImageView.LOADING && status != LoadImageView.LOADED)
                            mImageLoadManager.add(liv, true);
                    }
                }
                mListFirst = false;
            }

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
                setTitle(String.format(getString(R.string.list_title), title, visiblePage + 1));
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            // When srcoll over load image in view
            int getChildCount;
            if (scrollState == SCROLL_STATE_IDLE
                    && (getChildCount = view.getChildCount()) != 0) {
                for (int i = 0; i < getChildCount; i++) {
                    View v = ((ViewGroup) view.getChildAt(i)).getChildAt(0);
                    if (v instanceof LoadImageView) {
                        LoadImageView liv = (LoadImageView)v;
                        int status = liv.getState();
                        if (status != LoadImageView.LOADING && status != LoadImageView.LOADED)
                            mImageLoadManager.add(liv, true);
                    }
                }
            }
        }
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
            ListMangaDetail lmd= lmdArray.get(position);
            boolean isNew = false;
            if (convertView == null) {
                isNew = true;
                convertView = mInflater.inflate(R.layout.list_item, null);
            }
            LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.cover);
            if (!lmd.gid.equals(thumb.getKey())) {
                if (!isNew) {
                    // TODO default index = 0, should get it id
                    ((ViewGroup)convertView).removeView(thumb);
                    ViewGroup.LayoutParams lp = thumb.getLayoutParams();
                    thumb = new LoadImageView(MangaListActivity.this);
                    thumb.setId(R.id.cover);
                    ((ViewGroup)convertView).addView(thumb, 0, lp);
                }
                thumb.setLoadInfo(lmd.thumb, lmd.gid);
                mImageLoadManager.add(thumb, false);

                // Set manga name
                TextView name = (TextView) convertView.findViewById(R.id.name);
                name.setText(lmd.title);

                // Set Tpye
                ImageView type = (ImageView) convertView.findViewById(R.id.type);
                Ui.setType(type, lmd.category);

                // Add star
                LinearLayout rate = (LinearLayout) convertView
                        .findViewById(R.id.rate);
                Ui.addStar(rate, lmd.rating);
            }
            return convertView;
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.fixColour(this);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        
        mAppContext = (AppContext)getApplication();
        mEhClient = mAppContext.getEhClient();
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
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
        
        //
        mImageLoadManager = new ImageLoadManager(getApplicationContext(), Cache.memoryCache, Cache.cpCache);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // For colourfy the activity
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.ColourfyScreen(this);
        }
        
        // Get View
        drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        mainLayout = (GetPaddingRelativeLayout)findViewById(R.id.list_main);
        listMenu = (TagListView) findViewById(R.id.list_menu_list);
        loginMenu = (RelativeLayout)findViewById(R.id.list_menu_login);
        isExhentaiList = (ListView)findViewById(R.id.is_exhentai);
        pullListView = ((PullListView)findViewById(R.id.list_list));
        listView = pullListView.getListView();
        waitView = (View) findViewById(R.id.list_wait_first);
        freshButton = (Button) findViewById(R.id.list_refresh);
        noFoundView = (View) findViewById(R.id.list_no_found);
        sadpanda = (ImageView) findViewById(R.id.sadpanda);
        loginView = (ViewGroup) findViewById(R.id.drawer_login);
        loginButton = (Button) findViewById(R.id.button_login);
        checkLoginButton = (Button) findViewById(R.id.button_check_login);
        waitloginView = (View) findViewById(R.id.list_wait_login);
        loginOverView = (ViewGroup) findViewById(R.id.drawer_login_over);
        usernameText = (TextView) findViewById(R.id.text_username);
        logoutButton = (Button) findViewById(R.id.list_button_logout);
        waitlogoutView = (View) findViewById(R.id.list_wait_logout);
        
        // fitsSystemWindows for menu
        mainLayout.setView(listMenu, loginMenu);
        
        // leftDrawer
        String[] menuTitles = getResources().getStringArray(R.array.list_list_title);
        mStableItemCount = menuTitles.length;
        for (int i = 0; i < menuTitles.length; i++)
            listMenuTitle.add(menuTitles[i]);
        ArrayList<String> keys = Tag.getKeyList();
        for (int i = 0; i < keys.size(); i++)
            listMenuTitle.add(keys.get(i));
        tagsAdapter = new TagsAdapter(this, R.layout.menu_item, listMenuTitle);
        listMenu.setAdapter(tagsAdapter);
        listMenu.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listMenu.setStableItemCount(mStableItemCount);
        listMenu.setItemList(listMenuTitle);
        listMenu.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                if (position == 0) { // Home page
                    if (refresh(new ListUrls(ListUrls.ALL_TYPE, null, 0), listMenuTitle.get(position)))
                        drawer.closeDrawers();
                } else if (position == 1) { // Favourite
                    Intent intent = new Intent(MangaListActivity.this,
                            FavouriteActivity.class);
                    startActivity(intent);
                    drawer.closeDrawers();
                } else if (position == 2) { // filter
                    filterDialog.show();
                } else if (position == 3) { // Download
                    Intent intent = new Intent(MangaListActivity.this,
                            DownloadActivity.class);
                    startActivity(intent);
                    drawer.closeDrawers();
                } else if (position >= mStableItemCount){
                    ListUrls listUrls = Tag.get(listMenuTitle.get(position));
                    if (listUrls != null && refresh(listUrls, listMenuTitle.get(position)))
                        drawer.closeDrawers();
                }
            }
        });
        listMenu.setOnModifyListener(new TagListView.OnModifyListener(){
            @Override
            public void onModify(int position) {
                createModifyTagDialog(position).show();
            }
        });
        listMenu.setOnMoveLister(new TagListView.OnMoveLister() {
            @Override
            public void onMoveStart() {
                drawer.setEnabled(false);
            }

            @Override
            public void onMoveOver() {
                drawer.setEnabled(true);
            }
        });
        
        
        // is Exhentai
        final String[] isExhentaiListTitle = getResources().getStringArray(R.array.is_exhentai);
        final BaseAdapter isExhentaiListAdapter =  new BaseAdapter() {
            @Override
            public int getCount() {
                return isExhentaiListTitle.length;
            }

            @Override
            public Object getItem(int paramInt) {
                return isExhentaiListTitle[paramInt];
            }

            @Override
            public long getItemId(int paramInt) {
                return paramInt;
            }

            @Override
            public View getView(int paramInt, View paramView,
                    ViewGroup paramViewGroup) {
                if (paramView == null || !(paramView instanceof TextView)
                        || !isExhentaiListTitle[paramInt].equals((TextView)paramView)) {
                    LayoutInflater inflater = getLayoutInflater();
                    paramView = (TextView)inflater.inflate(R.layout.menu_item, null);
                    ((TextView)paramView).setText(isExhentaiListTitle[paramInt]);
                }
                TextView tv = (TextView)paramView;
                Resources resources = getResources();
                if ((paramInt == 0 && !Config.isExhentai()) ||
                        (paramInt == 1 && Config.isExhentai())) {
                    tv.setTextColor(resources.getColor(android.R.color.black));
                    tv.setBackgroundColor(resources.getColor(android.R.color.white));
                } else {
                    tv.setTextColor(resources.getColor(android.R.color.white));
                    tv.setBackgroundColor(resources.getColor(R.color.blue_dark));
                }
                return tv;
            }
        };
        isExhentaiList.setAdapter(isExhentaiListAdapter);
        isExhentaiList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> paramAdapterView,
                    View paramView, int paramInt, long paramLong) {
                boolean isChanged = false;
                if (paramInt == 0 && Config.isExhentai()) {
                    isChanged = true;
                    EhClient.setHeader(false);
                } else if (paramInt == 1 && !Config.isExhentai()){
                    isChanged = true;
                    EhClient.setHeader(true);
                }
                if (isChanged) {
                    isExhentaiListAdapter.notifyDataSetChanged();
                    refresh(new ListUrls(ListUrls.ALL_TYPE, null, 0), listMenuTitle.get(0));
                    drawer.closeDrawers(); // TODO
                }
            }
        });
        
        
        // Pull list view
        pullListView.setOnRefreshListener(new OnHeaderRefreshListener() {
            @Override
            public void onHeaderRefresh() {
                mListFirst = true;
                mLoadListOver = false;
                ListUrls listUrls = lus.clone();
                if (firstPage == 0)
                    listUrls.setPage(0);
                else
                    listUrls.setPage(firstPage - 1);
                mEhClient.getMangaList(listUrls.getUrl(),
                        new MangaListGetPackage((lastGetStamp = System.currentTimeMillis()), listUrls, false, title),
                        new MangaListGetListener());
            }

            @Override
            public void onHeaderRefresh(Object obj) {
                mListFirst = true;
                mLoadListOver = false;
                RefreshPackage rp = (RefreshPackage)obj;
                mEhClient.getMangaList(rp.listUrls.getUrl(),
                        new MangaListGetPackage((lastGetStamp = System.currentTimeMillis()),
                                rp.listUrls, true, rp.title),
                        new MangaListGetListener());
            }
        });
        
        pullListView.setOnFooterRefreshListener(new OnFooterRefreshListener() {
            @Override
            public void onFooterRefresh() {
                ListUrls listUrls = lus.clone();
                if (listUrls.setPage(lastPage + 1)) {
                    mListFirst = true;
                    mLoadListOver = false;
                    mEhClient.getMangaList(listUrls.getUrl(),
                            new MangaListGetPackage((lastGetStamp = System.currentTimeMillis()), listUrls, false, title),
                            new MangaListGetListener());
                }
                else
                    pullListView.onFooterRefreshComplete(true, false);
            }
        });
        pullListView.setFooterString(getString(R.string.footer_loading),
                getString(R.string.footer_loaded),
                getString(R.string.footer_fail));
        
        // Listview
        gmlAdapter = new GmlAdapter();
        listView.setAdapter(gmlAdapter);
        listView.setOnScrollListener(new MangaListListener());
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(MangaListActivity.this,
                        MangaDetailActivity.class);
                ListMangaDetail lmd = lmdArray.get(position);
                intent.putExtra("url", EhClient.detailHeader + lmd.gid + "/" + lmd.token);
                intent.putExtra("gid", lmd.gid);
                intent.putExtra("token", lmd.token);
                intent.putExtra("archiver_key", lmd.archiver_key);
                intent.putExtra("title", lmd.title);
                intent.putExtra("title_jpn", lmd.title_jpn);
                intent.putExtra("category", lmd.category);
                intent.putExtra("thumb", lmd.thumb);
                intent.putExtra("uploader", lmd.uploader);
                intent.putExtra("posted", lmd.posted);
                intent.putExtra("filecount", lmd.filecount);
                intent.putExtra("filesize", lmd.filesize);
                intent.putExtra("expunged", lmd.expunged);
                intent.putExtra("rating", lmd.rating);
                intent.putExtra("torrentcount", lmd.torrentcount);
                
                startActivity(intent);
            }
        });
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                longClickItemIndex = position;
                longClickDialog.show();
                return true;
            }
        });
        
        setTitle(String.format(getString(R.string.some_page), visiblePage + 1));
        
        pullListView.setVisibility(View.GONE);
        waitView.setVisibility(View.VISIBLE);
        freshButton.setVisibility(View.GONE);
        noFoundView.setVisibility(View.GONE);
        sadpanda.setVisibility(View.GONE);
        layoutDrawRight();
        
        // Check update
        checkUpdate();
        
        // Check login
        layoutDrawRight();
        checkLogin(false);
        
        // get MangeList
        title = listMenuTitle.get(0);
        refresh(lus.clone(), listMenuTitle.get(0));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView =
                (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                String t = null;
                if (query == null || query.isEmpty())
                    t = getString(android.R.string.search_go);
                else
                    t = getString(android.R.string.search_go) + " " + query;
                refresh(new ListUrls(ListUrls.ALL_TYPE, query), t);
                return true;
            }
        });
        
        // Make search view custom look
        int searchTextID = searchView.getContext().
                getResources().getIdentifier("android:id/search_src_text", null, null);
        if (searchTextID > 0) {
            AutoCompleteTextView searchText =
                    (AutoCompleteTextView)searchView.findViewById(searchTextID);
            if (searchText != null) {
                searchText.setTextColor(Color.WHITE);
                searchText.setHintTextColor(Color.WHITE);
            }
        }
        
        int removeImageID = searchView.getContext().
                getResources().getIdentifier("android:id/search_close_btn", null, null);
        if (removeImageID > 0) {
            ImageView removeImage = 
                    (ImageView)searchView.findViewById(removeImageID);
            if (removeImage != null)
                removeImage.setImageResource(R.drawable.ic_action_remove);
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
            if (drawer.isDrawerOpen(Gravity.START))
                drawer.closeDrawers();
            else
                drawer.openDrawer(Gravity.START);
            return true;
        case R.id.action_refresh: // TODO 点击刷新，pullviewheader 不会自动出来
            refresh(lus.clone(), listMenuTitle.get(0));
            return true;
        case R.id.action_jump:
            if (mLoadListOver)
                jump();
            return true;
        case R.id.action_settings:
            Intent intent = new Intent(MangaListActivity.this,
                    SettingsActivity.class);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }
    
    private void checkUpdate() {
        new UpdateHelper(this).autoCheckUpdate();
    }
    
    private void checkLogin(boolean force) {
        if (Config.isLogin() || force) {
            loginButton.setVisibility(View.GONE);
            checkLoginButton.setVisibility(View.GONE);
            waitloginView.setVisibility(View.VISIBLE);
            mEhClient.checkLogin(new EhClient.OnCheckLoginListener() {
                @Override
                public void onSuccess() {
                    Config.loginNow();
                    Toast.makeText(MangaListActivity.this,
                            getString(R.string.toast_login_succeeded),
                            Toast.LENGTH_SHORT).show();
                    layoutDrawRight();
                }

                @Override
                public void onFailure(String eMsg) {
                    loginButton.setVisibility(View.VISIBLE);
                    if (Config.isLogin())
                        checkLoginButton.setVisibility(View.VISIBLE);
                    else
                        checkLoginButton.setVisibility(View.GONE);
                    waitloginView.setVisibility(View.GONE);
                    Toast.makeText(MangaListActivity.this,
                            eMsg,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void layoutDrawRight() {
        if (mEhClient.isLogin()) { // If have login
            loginView.setVisibility(View.GONE);
            loginOverView.setVisibility(View.VISIBLE);
            
            usernameText.setText(mEhClient.getUsername());
            logoutButton.setVisibility(View.VISIBLE);
            waitlogoutView.setVisibility(View.GONE);
        } else {
            loginView.setVisibility(View.VISIBLE);
            loginOverView.setVisibility(View.GONE);
            
            loginButton.setVisibility(View.VISIBLE);
            if (Config.isLogin())
                checkLoginButton.setVisibility(View.VISIBLE);
            else
                checkLoginButton.setVisibility(View.GONE);
            waitloginView.setVisibility(View.GONE);
        }
    }
    
    private boolean refresh(ListUrls listUrls, String title) {
        listUrls.setPage(0);
        boolean re = pullListView.clickHeaderRefresh(new RefreshPackage(listUrls, title));
        if (re) {
            mListFirst = true;
            mLoadListOver = false;
            
            // Make sure callback while think it is refresh
            firstPage = 0;
            lastPage = 0;
            
            if (lmdArray.size() == 0)
                waitView.setVisibility(View.VISIBLE);
            else
                waitView.setVisibility(View.GONE);
            
            freshButton.setVisibility(View.GONE);
            noFoundView.setVisibility(View.GONE);
            sadpanda.setVisibility(View.GONE);
            
            pullListView.setHeaderString(
                    getString(R.string.pull_load_pull),
                    getString(R.string.pull_load_release),
                    getString(R.string.pull_load_doing),
                    getString(R.string.pull_load_done),
                    getString(R.string.pull_load_cancel));
        }
        return re;
    }

    public void buttonRefresh(View arg0) {
        refresh(lus.clone(), listMenuTitle.get(0));
    }

    public void buttonLogout(View paramView) {
        logoutButton.setVisibility(View.GONE);
        waitlogoutView.setVisibility(View.VISIBLE);
        mEhClient.logout(new EhClient.OnLogoutListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MangaListActivity.this,
                        getString(R.string.toast_logout_succeeded),
                        Toast.LENGTH_SHORT).show();
                Config.logoutNow();
                layoutDrawRight();
            }

            @Override
            public void onFailure(String eMsg) {
                Toast.makeText(MangaListActivity.this,
                        eMsg,
                        Toast.LENGTH_SHORT).show();
                logoutButton.setVisibility(View.VISIBLE);
                waitlogoutView.setVisibility(View.GONE);
            }
        });
    }
    
    public void buttonLogin(View v) {
        loginDialog.show();
    }
    
    public void buttonCheckLogin(View v) {
        checkLogin(false);
    }
}
