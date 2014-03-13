package com.hippo.ehviewer.activity;

import java.util.List;

import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.DownloadInfo;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Download;
import com.hippo.ehviewer.view.OlImageView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadActivity extends Activity {
    
    private static final String TAG = "com.hippo.ehviewer.service.UPDATE";
    private static final String ACTION_UPDATE = "com.hippo.ehviewer.service.UPDATE";
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
    private List<DownloadInfo> mDownloadInfos;
    private DlAdapter mDlAdapter;
    
    private class DlAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        // TODO clean map sometimes
        private SparseArray<Data> map;
        
        class Data {
            public int status;
            public boolean type;
            public int lastStartIndex;
            public float downloadSize;
            
            public Data(int status, boolean type, int lastStartIndex, float downloadSize) {
                this.status = status;
                this.type = type;
                this.lastStartIndex = lastStartIndex;
                this.downloadSize = downloadSize;
            }
        }
        
        public DlAdapter() {
            mInflater = LayoutInflater.from(DownloadActivity.this);
            map = new SparseArray<Data>();
        }
        
        @Override
        public int getCount() {
            return mDownloadInfos.size();
        }

        @Override
        public Object getItem(int arg0) {
            return mDownloadInfos.get(arg0);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DownloadInfo di= mDownloadInfos.get(position);
            Data data;
            int oldId;
            int newId;
            if (convertView != null && (data = map.get((oldId = convertView.getId()))) != null) {
                if (oldId != (newId = Integer.parseInt(di.gid))) {
                    convertView.setId(newId);
                    
                    OlImageView thumb = (OlImageView)convertView
                            .findViewById(R.id.thumb);
                    thumb.setUrl(di.thumb);
                    thumb.setKey(String.valueOf(di.gid));
                    thumb.setCache(Cache.memoryCache, Cache.cpCache);
                    thumb.loadImage(true);
                    
                    TextView title = (TextView)convertView.findViewById(R.id.title);
                    title.setText(di.title);
                }
                if (data.status != di.status) {
                    data.status = di.status;
                    data.type = di.type;
                    data.lastStartIndex = di.lastStartIndex;
                    
                    setDownloadInfo(convertView, di, position);
                } else if (di.status == DownloadInfo.DOWNLOADING &&
                        (data.type != di.type
                        || data.lastStartIndex != di.lastStartIndex
                        || data.downloadSize != di.downloadSize)) {
                    data.type = di.type;
                    data.lastStartIndex = di.lastStartIndex;
                    
                    ProgressBar pb = (ProgressBar)convertView.findViewById(R.id.progressBar);
                    TextView info = (TextView)convertView.findViewById(R.id.info);
                    
                    if (di.type == DownloadInfo.DETAIL_URL) {
                        pb.setIndeterminate(true);
                        info.setText(R.string.downloading);
                    } else {
                        pb.setIndeterminate(false);
                        pb.setMax(di.pageSum);
                        pb.setProgress(di.lastStartIndex-1);
                        StringBuilder sb = new StringBuilder(getString(R.string.downloading));
                        sb.append("  ").append(di.lastStartIndex-1).append(" / ").append(di.pageSum).append("\n");
                        if (di.totalSize != 0)
                            sb.append(String.format("%.2f", di.downloadSize))
                                    .append(" / ").append(String.format("%.2f", di.totalSize)).append(" KB");
                        info.setText(sb.toString());
                    }
                }
                return convertView;
            }
            
            convertView = mInflater.inflate(R.layout.download_item, null);
            
            OlImageView thumb = (OlImageView)convertView
                    .findViewById(R.id.thumb);
            thumb.setUrl(di.thumb);
            thumb.setKey(String.valueOf(di.gid));
            thumb.setCache(Cache.memoryCache, Cache.cpCache);
            thumb.loadImage(true);
            
            TextView title = (TextView)convertView.findViewById(R.id.title);
            title.setText(di.title);
            
            setDownloadInfo(convertView, di, position);
            
            convertView.setId(Integer.parseInt(di.gid));
            map.put(convertView.getId(),
                    new Data(di.status, di.type, di.lastStartIndex, di.downloadSize));
            return convertView;
        }
        
        /**
         * For clickable button, when click change message to "Please wait", then when action over change message
         * 
         * @param view
         * @param di
         * @param position
         */
        private void setDownloadInfo(View view, final DownloadInfo di, final int position) {
            final ProgressBar pb = (ProgressBar)view.findViewById(R.id.progressBar);
            final TextView info = (TextView)view.findViewById(R.id.info);
            final ImageView action = (ImageView)view.findViewById(R.id.action);
            
            if (di.status == DownloadInfo.STOP) {
                pb.setVisibility(View.GONE);
                String meg = getString(R.string.not_started) + (di.type == DownloadInfo.DETAIL_URL ? ""
                        : ("  " + (di.lastStartIndex-1) + " / " + di.pageSum));
                info.setText(meg);
                action.setImageResource(R.drawable.ic_action_start);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pb.setVisibility(View.GONE);
                        info.setText(R.string.wait);
                        action.setClickable(false);
                        
                        Intent it = new Intent(DownloadActivity.this, DownloadService.class);
                        startService(it);
                        mServiceConn.getService().add(Download.get(position));
                    }
                });
            } else if (di.status == DownloadInfo.DOWNLOADING) {
                pb.setVisibility(View.VISIBLE);
                if (di.type == DownloadInfo.DETAIL_URL) {
                    pb.setIndeterminate(true);
                    info.setText(R.string.downloading);
                } else {
                    pb.setIndeterminate(false);
                    pb.setMax(di.pageSum);
                    pb.setProgress(di.lastStartIndex-1);
                    StringBuilder sb = new StringBuilder(getString(R.string.downloading));
                    sb.append("  ").append(di.lastStartIndex-1).append(" / ").append(di.pageSum).append("\n");
                    if (di.totalSize != 0)
                        sb.append(String.format("%.2f", di.downloadSize))
                                .append(" / ").append(String.format("%.2f", di.totalSize)).append(" KB");
                    info.setText(sb.toString());
                }
                action.setImageResource(R.drawable.ic_action_pause);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pb.setVisibility(View.GONE);
                        info.setText(R.string.wait);
                        action.setClickable(false);
                        
                        mServiceConn.getService().cancel(Download.get(position).gid);
                    }
                });
            } else if (di.status == DownloadInfo.WAITING) {
                pb.setVisibility(View.GONE);
                info.setText(R.string.waiting);
                action.setImageResource(R.drawable.ic_action_pause);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pb.setVisibility(View.GONE);
                        info.setText(R.string.wait);
                        action.setClickable(false);
                        
                        mServiceConn.getService().cancel(Download.get(position).gid);
                    }
                });
            } else if (di.status == DownloadInfo.COMPLETED) {
                pb.setVisibility(View.GONE);
                info.setText(R.string.download_successfully);
                action.setImageResource(R.drawable.ic_action_completed);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(DownloadActivity.this,
                                MangaDownloadActivity.class);
                        intent.putExtra("title", di.title);
                        startActivity(intent);
                    }
                });
            } else if (di.status == DownloadInfo.FAILED) {
                pb.setVisibility(View.GONE);
                String meg = getString(R.string.download_unsuccessfully) + (di.type == DownloadInfo.DETAIL_URL ? ""
                        : ("  " + (di.lastStartIndex-1) + " / " + di.pageSum));
                info.setText(meg);
                action.setImageResource(R.drawable.ic_action_start);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pb.setVisibility(View.GONE);
                        info.setText(R.string.wait);
                        action.setClickable(false);
                        
                        Intent it = new Intent(DownloadActivity.this, DownloadService.class);
                        startService(it);
                        mServiceConn.getService().add(Download.get(position));
                    }
                });
            }
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
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConn);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download);
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        // Download service
        Intent it = new Intent(this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE);
        registerReceiver(mReceiver, filter);
        
        // For colourfy the activity
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.ColourfyScreen(this);
        }
        
        mDownloadInfos = Download.getDownloadInfoList();
        
        ListView listView = (ListView)findViewById(R.id.download);
        mDlAdapter = new DlAdapter();
        listView.setAdapter(mDlAdapter);
    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ACTION_UPDATE))
                mDlAdapter.notifyDataSetChanged();
        }
    };
}
