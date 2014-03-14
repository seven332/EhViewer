package com.hippo.ehviewer.activity;

import java.io.IOException;
import java.util.ArrayList;

import com.handmark.pulltorefresh.library.LoadingLayoutProxy;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.ListMangaDetail;
import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.dialog.DialogBuilder;
import com.hippo.ehviewer.dialog.SuperDialogUtil;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Crash;
import com.hippo.ehviewer.util.EhClient;
import com.hippo.ehviewer.util.Favourite;
import com.hippo.ehviewer.util.Tag;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.view.AlertButton;
import com.hippo.ehviewer.view.CheckImage;
import com.hippo.ehviewer.view.OlImageView;
import com.hippo.ehviewer.view.TagListView;
import com.hippo.ehviewer.view.TagsAdapter;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.R.color;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
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
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

// TODO check visiblePage is right or not
// TODO Good resource
// TODO http://lofi.e-hentai.org/
// TODO http://exhentai.org/
// TODO add lock to get list

public class MangaListActivity extends SlidingActivity {
    private static String TAG = "MangaListActivity";

    private RelativeLayout mainView;
    private TagListView listMenu;
    private ListView isExhentaiList;
    //private LinearLayout loginMenu;
    private PullToRefreshListView pullListView;
    private ListView listView;
    private View waitView;
    private Button freshButton;
    private View noFoundView;
    private ImageView sadpanda;
    private ViewGroup loginView;
    private ViewGroup loginOverView;
    private TextView usernameText;
    private Button logoutButton;
    private View waitlogoutView;
    
    private TagsAdapter tagsAdapter;
    
    private SlidingMenu mSlidingMenu;

    private String lastCrash;
    private ListUrls lus;
    private GmlAdapter gmlAdapter;
    private ArrayList<String> listMenuTitle = new ArrayList<String>();
    private int mStableItemCount;
    
    private ArrayList<ListMangaDetail> lmdArray = new ArrayList<ListMangaDetail>();

    private int longClickItemIndex;

    private boolean mLayout = false;
    private boolean mListFirst = true;
    private boolean mLoadListOver = false;
    
    private AlertDialog checkLoginDialog;
    private AlertDialog loginDialog;
    private AlertDialog filterDialog;
    private AlertDialog longClickDialog;
    private AlertDialog waitNetworkDialog;
    
    // Modify tag
    private String newTagName = null;
    
    
    // Double click back exit
    private long curBackTime = 0;
    private static final int BACK_PRESSED_INTERVAL = 2000;
    
    //
    private int firstPage = 0;
    private int lastPage = 0;
    
    private int firstIndex = 1;
    private int lastIndex = 1;
    private int visiblePage = 0;
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
    private AlertDialog createCheckLoginDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.wait, null);
        TextView tv = (TextView)view.findViewById(R.id.wait_message);
        tv.setText(R.string.dailog_check_login);
        
        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.wait)
                .setView(view, true).create();
    }

    private AlertDialog createAskLoginDialog() {
        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.dialog_title_login_or_not)
                .setMessage(R.string.dialog_messgae_login_or_not)
                .setPositiveButton(R.string.login,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        loginDialog.show();
                    }
                }).setNegativeButton(R.string.not_login,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        if (mLayout)
                            layoutDrawRight();
                        else
                            layout();
                    }
                }).create();
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
                        checkLoginDialog.show();
                        String username = ((EditText) loginDialog.findViewById(R.id.username)).getText().toString();
                        String password = ((EditText) loginDialog.findViewById(R.id.password)).getText().toString();
                        EhClient.login(username, password, new EhClient.OnLoginListener() {
                            @Override
                            public void onSuccess() {
                                EhClient.checkLogin(new EhClient.OnCheckLoginListener() {
                                    @Override
                                    public void onSuccess() {
                                        checkLoginDialog.dismiss();
                                        Config.loginNow();
                                        Toast.makeText( MangaListActivity.this,
                                                getString(R.string.toast_login_succeeded),
                                                Toast.LENGTH_SHORT).show();
                                        if (mLayout)
                                            layoutDrawRight();
                                        else
                                            layout();
                                        }
                                    @Override
                                    public void onFailure(int errorMessageId) {
                                        checkLoginDialog.dismiss();
                                        Toast.makeText(MangaListActivity.this,
                                                getString(errorMessageId), Toast.LENGTH_SHORT).show();
                                        loginDialog.show();
                                    }
                                });
                            }
                            @Override
                            public void onFailure(int errorMessageId) {
                                checkLoginDialog.dismiss();
                                Toast.makeText(MangaListActivity.this,
                                        getString(errorMessageId),
                                        Toast.LENGTH_SHORT).show();
                                loginDialog.show();
                            }
                        });
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        if (mLayout)
                            layoutDrawRight();
                        else
                            layout();
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
                        MangaListActivity.this.showContent();
                        lus = getLus(filterDialog);
                        refresh();
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
    
    private AlertDialog createWaitNetworkDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.wait, null);
        TextView tv = (TextView)view.findViewById(R.id.wait_message);
        tv.setText(R.string.dailog_check_network);
        
        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.wait)
                .setView(view, true).create();
    }
    

    private AlertDialog createSendCrashDialog() {
        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.dialog_send_crash_title)
                .setMessage(R.string.dialog_send_crash_plain)
                .setPositiveButton(R.string.dialog_send_crash_yes, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        Intent i = new Intent(Intent.ACTION_SENDTO);
                        i.setData(Uri.parse("mailto:ehviewersu@gmail.com"));
                        i.putExtra(Intent.EXTRA_SUBJECT, "I found a bug in EhViewer !");
                        i.putExtra(Intent.EXTRA_TEXT, lastCrash);
                        startActivity(i);
                        lastCrash = null;
                        
                        waitNetworkDialog = createWaitNetworkDialog();
                        waitNetworkDialog.show();
                        EhClient.checkNetwork(new NetworkListener());
                    }
                }).setNegativeButton(R.string.dialog_send_crash_no, new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        lastCrash = null;
                        
                        waitNetworkDialog = createWaitNetworkDialog();
                        waitNetworkDialog.show();
                        EhClient.checkNetwork(new NetworkListener());
                    }
                }).create();
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
    
    private AlertDialog createWarningDialog() {
        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.dailog_waring_title)
                .setMessage(R.string.dailog_waring_plain)
                .setPositiveButton(R.string.dailog_waring_yes,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        Config.allowed();
                        
                        if ((lastCrash = Crash.getLastCrash()) != null) {
                            createSendCrashDialog().show();
                        } else {
                            waitNetworkDialog = createWaitNetworkDialog();
                            waitNetworkDialog.show();
                            EhClient.checkNetwork(new NetworkListener());
                        }
                    }
                }).setNegativeButton(R.string.dailog_waring_no,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        finish();
                    }
                }).create();
    }
    
    private AlertDialog createNetworkErrorDialog(int networkEmsId) {
        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.error).setMessage(networkEmsId)
                .setPositiveButton(R.string.dailog_network_error_yes,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        if (Config.isFirstTime()){
                            Config.firstTime();
                            createAskLoginDialog();
                            createAskLoginDialog().show();
                        } else if (!EhClient.hasLogin() && Config.isLogin()) {
                            checkLogin();
                        } else
                            layout();
                    }
                })
                .setNegativeButton(R.string.dailog_network_error_no,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        finish();
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
                                    && targetPage <= lastPage) { // If targetPage is in range
                                ((AlertButton)v).dialog.dismiss();
                                int position = (targetPage - firstPage) *
                                        lus.getNumPerPage() + 1;
                                listView.setSelectionFromTop(position, -1);
                            } else if (lus.setPage(targetPage)) {
                                ((AlertButton)v).dialog.dismiss();
                                
                                mListFirst = true;
                                mLoadListOver = false;
                                
                                waitView.setVisibility(View.VISIBLE);
                                pullListView.setMode(Mode.DISABLED);
                                freshButton.setVisibility(View.GONE);
                                noFoundView.setVisibility(View.GONE);
                                sadpanda.setVisibility(View.GONE);
                                
                                EhClient.getManagaList(lus.getUrl(),
                                        new MangaListGetPackage(lus.clone(), new Integer[]{lus.getPage(), 1}),
                                        new MangaListGetListener());
                            } else {
                                Toast.makeText(MangaListActivity.this,
                                        getString(R.string.toast_invalid_page),
                                        Toast.LENGTH_SHORT).show();
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
    
    
    private class NetworkListener implements
            EhClient.OnCheckNetworkListener {
        @Override
        public void onSuccess() {
            waitNetworkDialog.dismiss();
            waitNetworkDialog = null;
            if (Config.isFirstTime()){
                Config.firstTime();
                createAskLoginDialog().show();
            } else if (!EhClient.hasLogin() && Config.isLogin()) {
                checkLogin();
            } else
                layout();
        }

        @Override
        public void onFailure(int errorMessageId) {
            waitNetworkDialog.dismiss();
            waitNetworkDialog = null;
            createNetworkErrorDialog(errorMessageId).show();
        }
    }
    
    private class MangaListGetPackage {
        public ListUrls listUrls;
        public Integer[] flag;
        public MangaListGetPackage(ListUrls listUrls, Integer[] flag) {
            this.listUrls = listUrls;
            this.flag = flag;
        }
    }
    
    private class MangaListGetListener implements
            EhClient.OnGetManagaListListener {

        @Override
        public void onSuccess(Object checkFlag, ArrayList<ListMangaDetail> newLmdArray,
                int indexPerPage, int maxPage) {
            MangaListGetPackage getPackage = (MangaListGetPackage)checkFlag;
            if (!getPackage.listUrls.equals(lus))
                return;
            
            pullListView.onRefreshComplete();
            
            // Check no Found view later
            waitView.setVisibility(View.GONE);
            freshButton.setVisibility(View.GONE);
            
            if (maxPage == 0) { // If No hits found
                mLoadListOver = false;
                
                pullListView.setMode(Mode.DISABLED);
                noFoundView.setVisibility(View.VISIBLE);
                sadpanda.setVisibility(View.GONE);
                setTitle(R.string.no_found);
                lmdArray.clear();
                gmlAdapter.notifyDataSetChanged();
            } else if (maxPage == -1) { //panda
                mLoadListOver = false;
                
                pullListView.setMode(Mode.DISABLED);
                noFoundView.setVisibility(View.GONE);
                sadpanda.setVisibility(View.VISIBLE);
                setTitle(R.string.sadpanda);
                lmdArray.clear();
                gmlAdapter.notifyDataSetChanged();
            } else {
                mLoadListOver = true;
                
                pullListView.setMode(Mode.BOTH);
                noFoundView.setVisibility(View.GONE);
                sadpanda.setVisibility(View.GONE);
                // Set indexPerPage and maxPage
                lus.setNumPerPage(indexPerPage);
                lus.setMax(maxPage);
                
                // Check refresh or get more
                Integer[] flag = getPackage.flag;
                int getPageIndex = flag[0];
                boolean setPositon = flag[1] != 0;
                if (getPageIndex == 0 && firstPage == 0) { // Refresh
                    firstPage = 0;
                    lastPage = 0;
                    lmdArray.clear();
                    lmdArray.addAll(newLmdArray);
                    
                    // Get visible page
                    firstIndex = 1;
                    lastIndex = lus.getNumPerPage() + 1;
                    visiblePage = 0;
                    setTitle(String.format(getString(R.string.some_page), visiblePage + 1));
                    
                    gmlAdapter.notifyDataSetChanged();
                    if (setPositon) {
                        int position = (getPageIndex - firstPage) *
                                lus.getNumPerPage() + 1;
                        listView.setSelectionFromTop(position, -1);
                    }
                } else if (getPageIndex == firstPage - 1) { // Get last page
                    firstPage = getPageIndex;
                    lmdArray.addAll(0, newLmdArray);
                    
                    // Get visible page
                    firstIndex += lus.getNumPerPage();
                    lastIndex += lus.getNumPerPage();
                    //setTitle(String.format(getString(R.string.list_page), visiblePage + 1));
                    
                    gmlAdapter.notifyDataSetChanged();
                    if (!setPositon) { // TODO Make it more 
                        int position = lus.getNumPerPage() + 1;
                        listView.setSelectionFromTop(position, -1);
                    } else {
                        int position = (getPageIndex - firstPage) *
                                lus.getNumPerPage() + 1;
                        listView.setSelectionFromTop(position, -1);
                    }
                    
                } else if (getPageIndex == lastPage + 1) { // Get next page
                    lastPage = getPageIndex;
                    lmdArray.addAll(newLmdArray);
                    
                    gmlAdapter.notifyDataSetChanged();
                    if (setPositon) {
                        int position = (getPageIndex - firstPage) *
                                lus.getNumPerPage() + 1;
                        listView.setSelectionFromTop(position, -1);
                    }
                } else if (getPageIndex < firstPage - 1 ||
                        getPageIndex > lastPage + 1){ // Jump somewhere
                    firstPage = getPageIndex;
                    lastPage = getPageIndex;
                    lmdArray.clear();
                    lmdArray.addAll(newLmdArray);
                    
                    // Get visible page
                    firstIndex = 1;
                    lastIndex = lus.getNumPerPage() + 1;
                    visiblePage = getPageIndex;
                    setTitle(String.format(getString(R.string.some_page), visiblePage + 1));
                    
                    Log.d(TAG, (visiblePage + 1) + "");
                    
                    gmlAdapter.notifyDataSetChanged();
                    if (setPositon) {
                        int position = (getPageIndex - firstPage) *
                                lus.getNumPerPage() + 1;
                        listView.setSelectionFromTop(position, -1);
                    }
                }
            }
            // Reset pull string
            setHeaderPullString();
        }

        @Override
        public void onFailure(Object checkFlag, int errorMessageId) {
            MangaListGetPackage getPackage = (MangaListGetPackage)checkFlag;
            if (!getPackage.listUrls.equals(lus))
                return;
            
            pullListView.onRefreshComplete();
            
            // Check pull list view later
            // Check fresh view later

            waitView.setVisibility(View.GONE);
            noFoundView.setVisibility(View.GONE);
            sadpanda.setVisibility(View.GONE);
            
            Integer[] flag = getPackage.flag;
            int getPageIndex = flag[0];
            
            if (getPageIndex == 0 && firstPage == 0) { // Refresh
                mLoadListOver = false;
                // Only show freshButton
                pullListView.setMode(Mode.DISABLED);
                freshButton.setVisibility(View.VISIBLE);
                Toast.makeText(MangaListActivity.this,
                        getString(errorMessageId), Toast.LENGTH_SHORT)
                        .show();
                lmdArray.clear();
                gmlAdapter.notifyDataSetChanged();
            } else {// List is not empty
                // Only show freshButton
                pullListView.setMode(Mode.BOTH);
                freshButton.setVisibility(View.GONE);
                Toast.makeText(
                        MangaListActivity.this,
                        getString(errorMessageId) + " "
                        + getString(R.string.em_retry), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class MangaListListener implements ListView.OnScrollListener {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            
            // First time when list is created
            if (mListFirst && mLoadListOver) {
                // load image
                int getChildCount = view.getChildCount();
                for (int i = 0; i < getChildCount; i++) {
                    View v = ((ViewGroup) view.getChildAt(i)).getChildAt(0);
                    if (v instanceof OlImageView)
                        ((OlImageView)v).loadImage(false);
                }
                mListFirst = false;
            }

            if (lus == null || visibleItemCount == 0)
                return;
            if (lastIndex == 1)
                lastIndex = lus.getNumPerPage() + 1;

            int pageChanged = (firstVisibleItem - firstIndex)
                    / lus.getNumPerPage();
            if (pageChanged == 0)
                pageChanged = (firstVisibleItem + visibleItemCount - lastIndex)
                        / lus.getNumPerPage();
            
            if (pageChanged != 0) {
                visiblePage = visiblePage + pageChanged;
                firstIndex += pageChanged * lus.getNumPerPage();
                lastIndex += pageChanged * lus.getNumPerPage();
                setTitle(String.format(
                        getString(R.string.some_page), visiblePage + 1));
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
                    if (v instanceof OlImageView)
                        ((OlImageView) v).loadImage(false);
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
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.list_item, null);
            
            OlImageView thumb = (OlImageView)convertView.findViewById(R.id.cover);
            if (!lmd.gid.equals(thumb.getKey())) {
                thumb.setUrl(lmd.thumb);
                thumb.setKey(lmd.gid);
                thumb.setCache(Cache.memoryCache, Cache.cpCache);
                thumb.loadFromCache();

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
    
    private void setHeaderPullString() {
        LoadingLayoutProxy ill = (LoadingLayoutProxy)pullListView.getLoadingLayoutProxy();
        if (firstPage == 0)
            ill.setHeaderLabels(getResources().getTextArray(R.array.pull_refresh));
        else
            ill.setHeaderLabels(getResources().getTextArray(R.array.pull_pre));
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int menuWidth = (int)getResources().getDimension(R.dimen.menu_offset);
        mSlidingMenu.setBehindOffset(menuWidth);
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
        checkLoginDialog = createCheckLoginDialog();
        loginDialog = createLoginDialog();
        filterDialog = createFilterDialog();
        longClickDialog = createLongClickDialog();
        
        // Set menu
        mSlidingMenu = getSlidingMenu();
        mSlidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
        mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        mSlidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        mSlidingMenu.setFadeDegree(0.35f);
        int menuWidth = (int)getResources().getDimension(R.dimen.menu_offset);
        mSlidingMenu.setBehindOffset(menuWidth);
        setBehindContentView(R.layout.list_menu_list);
        mSlidingMenu.setSecondaryMenu(R.layout.list_menu_login);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Get View
        mainView = (RelativeLayout) findViewById(R.id.list_main);
        listMenu = (TagListView) findViewById(R.id.list_menu_list);
        isExhentaiList = (ListView)findViewById(R.id.is_exhentai);
        //loginMenu = (LinearLayout) findViewById(R.id.list_menu_login);
        pullListView = (PullToRefreshListView) findViewById(R.id.list_list);
        listView = pullListView.getRefreshableView();
        waitView = (View) findViewById(R.id.list_wait_first);
        freshButton = (Button) findViewById(R.id.list_refresh);
        noFoundView = (View) findViewById(R.id.list_no_found);
        sadpanda = (ImageView) findViewById(R.id.sadpanda);
        loginView = (ViewGroup) findViewById(R.id.drawer_login);
        loginOverView = (ViewGroup) findViewById(R.id.drawer_login_over);
        usernameText = (TextView) findViewById(R.id.text_username);
        logoutButton = (Button) findViewById(R.id.list_button_logout);
        waitlogoutView = (View) findViewById(R.id.list_wait_logout);

        // For colourfy the activity
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.ColourfyScreen(this);
        }

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
                    lus = new ListUrls(ListUrls.ALL_TYPE, null, 0);
                    refresh();
                    showContent();
                } else if (position == 1) { // Favourite
                    Intent intent = new Intent(MangaListActivity.this,
                            FavouriteActivity.class);
                    startActivity(intent);
                    showContent();
                } else if (position == 2) { // filter
                    filterDialog.show();
                } else if (position == 3) { // Download //TODO
                    Intent intent = new Intent(MangaListActivity.this,
                            DownloadActivity.class);
                    startActivity(intent);
                    showContent();
                } else if (position >= mStableItemCount){
                    ListUrls listUrls = Tag.get(listMenuTitle.get(position));
                    if (listUrls != null) {
                        lus = listUrls;
                        refresh();
                    }
                    showContent();
                }
            }
        });
        listMenu.setOnModifyListener(new TagListView.OnModifyListener(){
            @Override
            public void onModify(int position) {
                createModifyTagDialog(position).show();
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
                    lus = new ListUrls(ListUrls.ALL_TYPE, null, 0);
                    refresh();
                    MangaListActivity.this.showContent();
                }
            }
        });
        
        
        // Pull list view
        pullListView.setOnRefreshListener(new OnRefreshListener2<ListView>() {
            @Override
            public void onPullDownToRefresh(
                    PullToRefreshBase<ListView> refreshView) {
                if (firstPage == 0)
                    refresh();
                else {
                    mListFirst = true;
                    mLoadListOver = false;
                    lus.setPage(firstPage - 1);
                    EhClient.getManagaList(lus.getUrl(),
                            new MangaListGetPackage(lus.clone(), new Integer[]{lus.getPage(), 0}),
                            new MangaListGetListener());
                }
            }
            @Override
            public void onPullUpToRefresh(
                    PullToRefreshBase<ListView> refreshView) {
                if (lastPage >= lus.getMax() - 1) {
                    Toast.makeText(MangaListActivity.this, getString(R.string.last_page), Toast.LENGTH_SHORT).show();
                    pullListView.onRefreshComplete();
                } else {
                    mListFirst = true;
                    mLoadListOver = false;
                    lus.setPage(lastPage + 1);
                    EhClient.getManagaList(lus.getUrl(),
                            new MangaListGetPackage(lus.clone(), new Integer[]{lus.getPage(), 0}),
                            new MangaListGetListener());
                }
                    
            }
        });
        
        LoadingLayoutProxy ill = (LoadingLayoutProxy)pullListView.getLoadingLayoutProxy();
        ill.setHeaderLabels(getResources().getTextArray(R.array.pull_refresh));
        ill.setFooterLabels(getResources().getTextArray(R.array.pull_next));
        pullListView.setMode(Mode.DISABLED);
        
        // Listview
        gmlAdapter = new GmlAdapter();
        listView.setAdapter(gmlAdapter);
        listView.setOnScrollListener(new MangaListListener());
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                if (position > 0)
                    position = position-1;
                else
                    position = 0;
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
                if (position > 0)
                    longClickItemIndex = position-1;
                else
                    longClickItemIndex = 0;
                longClickDialog.show();
                return true;
            }
        });
        
        if (!Config.isAllowed()) {
            createWarningDialog().show();
        } else if ((lastCrash = Crash.getLastCrash()) != null) {
            createSendCrashDialog().show();
        } else {
            waitNetworkDialog = createWaitNetworkDialog();
            waitNetworkDialog.show();
            EhClient.checkNetwork(new NetworkListener());
        }
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
                lus = new ListUrls(ListUrls.ALL_TYPE, query);
                refresh();
                return true;
            }
        });
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
            toggle();
            return true;
        case R.id.action_refresh:
            lus.setPage(0);
            refresh();
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
        if (Config.isAutoPageCache()) {
            Cache.pageCache.clear();
        }
    }

    private void layout() { // First time
        mLayout = true;

        setTitle(String.format(getString(R.string.some_page), visiblePage + 1));
        layoutDrawRight();

        waitView.setVisibility(View.VISIBLE);
        pullListView.setMode(Mode.DISABLED);
        freshButton.setVisibility(View.GONE);
        noFoundView.setVisibility(View.GONE);
        sadpanda.setVisibility(View.GONE);

        // get MangeList
        lus.setPage(0);
        EhClient.getManagaList(lus.getUrl(),
                new MangaListGetPackage(lus.clone(), new Integer[]{lus.getPage(), 1}),
                new MangaListGetListener());
    }

    private void layoutDrawRight() {

        if (EhClient.isLogin()) { // If have login
            loginView.setVisibility(View.GONE);
            loginOverView.setVisibility(View.VISIBLE);
            usernameText.setText(EhClient.getUsername());
        } else {
            loginView.setVisibility(View.VISIBLE);
            loginOverView.setVisibility(View.GONE);
        }
    }
    
    private void refresh() {
        mListFirst = true;
        mLoadListOver = false;
        
        firstPage = 0;
        lastPage = 0;
        
        // Only ProgressBar in center
        if (pullListView.isRefreshing()) {
            waitView.setVisibility(View.GONE);
            pullListView.setMode(Mode.BOTH);
        } else {
            waitView.setVisibility(View.VISIBLE);
            pullListView.setMode(Mode.DISABLED);
        }
        freshButton.setVisibility(View.GONE);
        noFoundView.setVisibility(View.GONE);
        sadpanda.setVisibility(View.GONE);

        // Get MangeList
        lus.setPage(0);
        EhClient.getManagaList(lus.getUrl(),
                new MangaListGetPackage(lus.clone(), new Integer[]{lus.getPage(), 1}),
                new MangaListGetListener());
    }
    
    
    // CheckLogin
    private void checkLogin() {
        checkLoginDialog.show();
        EhClient.checkLogin(new EhClient.OnCheckLoginListener() {
            @Override
            public void onSuccess() {
                checkLoginDialog.dismiss();
                Toast.makeText(MangaListActivity.this,
                        getString(R.string.toast_login_succeeded),
                        Toast.LENGTH_SHORT).show();
                if (mLayout)
                    layoutDrawRight();
                else
                    layout();
            }

            @Override
            public void onFailure(int errorMessageId) {
                checkLoginDialog.dismiss();
                Toast.makeText(MangaListActivity.this,
                        getString(errorMessageId),
                        Toast.LENGTH_SHORT).show();
                loginDialog.show();
            }
        });
    }
    
    // *** Button onclick ***//

    public void buttonRefresh(View arg0) {
        refresh();
    }

    public void buttonLogout(View paramView) {
        logoutButton.setVisibility(View.GONE);
        waitlogoutView.setVisibility(View.VISIBLE);
        EhClient.logout(new EhClient.OnLogoutListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MangaListActivity.this,
                        getString(R.string.toast_logout_succeeded),
                        Toast.LENGTH_SHORT).show();
                logoutButton.setVisibility(View.VISIBLE);
                waitlogoutView.setVisibility(View.GONE);
                Config.logoutNow();
                layoutDrawRight();
            }

            @Override
            public void onFailure(int errorMessageId) {
                Toast.makeText(MangaListActivity.this,
                        getString(errorMessageId),
                        Toast.LENGTH_SHORT).show();
                logoutButton.setVisibility(View.VISIBLE);
                waitlogoutView.setVisibility(View.GONE);
            }
        });
    }
    
    public void buttonLogin(View v) {
        loginDialog.show();
    }
    
    // *** Button onclick end ***//
}
