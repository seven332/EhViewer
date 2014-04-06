package com.hippo.ehviewer.activity;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import com.hippo.ehviewer.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.ImageLoadManager;
import com.hippo.ehviewer.ListMangaDetail;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhClient;
import com.hippo.ehviewer.util.Favourite;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.view.AlertButton;
import com.hippo.ehviewer.view.OlImageView;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.LoadImageView;

public class FavouriteActivity extends Activity{
    private static final String TAG = "FavouriteActivity";
    
    private FlAdapter flAdapter;
    private ArrayList<ListMangaDetail> mFavouriteLmd;
    private int longClickItemIndex;
    
    private ImageLoadManager mImageLoadManager;
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
    // List item long click dialog
    private AlertDialog longClickDialog;

    private AlertDialog setLongClickDialog() {
        return new DialogBuilder(this).setTitle(R.string.what_to_do)
                .setItems(R.array.favourite_item_long_click, new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int position, long arg3) {
                        switch (position) {
                        case 0: // Remove favourite item
                            Favourite.remove(longClickItemIndex);
                            flAdapter.notifyDataSetChanged();
                            break;
                        case 1:
                            ListMangaDetail lmd = mFavouriteLmd.get(longClickItemIndex);
                            Intent it = new Intent(FavouriteActivity.this, DownloadService.class);
                            startService(it);
                            mServiceConn.getService().add(lmd.gid, lmd.thumb, 
                                    EhClient.detailHeader + lmd.gid + "/" + lmd.token, lmd.title);
                            Toast.makeText(FavouriteActivity.this,
                                    getString(R.string.toast_add_download),
                                    Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            break;
                        }
                        longClickDialog.cancel();
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                    }
                }).create();
    }
    
    private class FlAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public FlAdapter() {
            mInflater = LayoutInflater.from(FavouriteActivity.this);
        }

        @Override
        public int getCount() {
            return mFavouriteLmd.size();
        }

        @Override
        public Object getItem(int arg0) {
            return mFavouriteLmd.get(arg0);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListMangaDetail lmd= mFavouriteLmd.get(position);
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.list_item, null);
            
            LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.cover);
            if (!lmd.gid.equals(thumb.getKey())) {
                thumb.setLoadInfo(lmd.thumb, lmd.gid);
                mImageLoadManager.add(thumb, true);

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
        
        if (flAdapter != null)
            flAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favourite);
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        // Download service
        Intent it = new Intent(FavouriteActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        longClickDialog = setLongClickDialog();
        
        mImageLoadManager = new ImageLoadManager(getApplicationContext(), Cache.memoryCache, Cache.cpCache);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // For colourfy the activity
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.ColourfyScreen(this);
        }
        
        mFavouriteLmd = Favourite.getFavouriteList();
        
        ListView listView = (ListView)findViewById(R.id.favourite);
        flAdapter = new FlAdapter();
        listView.setAdapter(flAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(FavouriteActivity.this,
                        MangaDetailActivity.class);
                ListMangaDetail lmd = mFavouriteLmd.get(position);
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
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
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
    
    public void buttonListItemCancel(View v) {
        longClickDialog.cancel();
    }
}
