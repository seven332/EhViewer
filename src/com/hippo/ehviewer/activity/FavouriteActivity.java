package com.hippo.ehviewer.activity;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
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

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.ImageGeterManager;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.view.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.LoadImageView;

public class FavouriteActivity extends Activity{
    @SuppressWarnings("unused")
    private static final String TAG = "FavouriteActivity";
    
    private AppContext mAppContext;
    private Data mData;
    
    private FlAdapter flAdapter;
    private List<GalleryInfo> mFavouriteLmd;
    private int longClickItemIndex;
    
    private ImageGeterManager mImageGeterManager;
    
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
                            mData.deleteLocalFavourite(mFavouriteLmd.get(longClickItemIndex).gid);
                            flAdapter.notifyDataSetChanged();
                            break;
                        case 1:
                            GalleryInfo lmd = mFavouriteLmd.get(longClickItemIndex);
                            Intent it = new Intent(FavouriteActivity.this, DownloadService.class);
                            startService(it);
                            mServiceConn.getService().add(String.valueOf(lmd.gid), lmd.thumb, 
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
            GalleryInfo lmd= mFavouriteLmd.get(position);
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.list_item, null);
            
            LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.cover);
            if (!String.valueOf(lmd.gid).equals(thumb.getKey())) {
                thumb.setLoadInfo(lmd.thumb, String.valueOf(lmd.gid));
                mImageGeterManager.add(lmd.thumb, String.valueOf(lmd.gid),
                        new LoadImageView.SimpleImageGetListener(thumb), true);

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
                LinearLayout rate = (LinearLayout) convertView
                        .findViewById(R.id.rate);
                Ui.addStar(rate, lmd.rating);
                
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
        
        mAppContext = (AppContext)getApplication();
        mData = mAppContext.getData();
        mImageGeterManager = mAppContext.getImageGeterManager();
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        // Download service
        Intent it = new Intent(FavouriteActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        longClickDialog = setLongClickDialog();
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // For colourfy the activity
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.ColourfyScreen(this);
        }
        
        mFavouriteLmd = mData.getAllLocalFavourites();
        
        ListView listView = (ListView)findViewById(R.id.favourite);
        flAdapter = new FlAdapter();
        listView.setAdapter(flAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(FavouriteActivity.this,
                        MangaDetailActivity.class);
                GalleryInfo gi = mFavouriteLmd.get(position);
                intent.putExtra("url", EhClient.detailHeader + gi.gid + "/" + gi.token);
                intent.putExtra(MangaDetailActivity.KEY_G_INFO, gi);
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
