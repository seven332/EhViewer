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

import java.util.List;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.cardview.CardViewSalon;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.DownloadInfo;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.windowsanimate.WindowsAnimate;

public class DownloadActivity extends AbsActivity implements AbsListView.OnItemClickListener {

    private WindowsAnimate mWindowsAnimate;
    private int mThemeColor;
    private List<DownloadInfo> mDownloads;

    private ListView mList;
    private ListAdapter mAdapter;

    private final DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(DownloadService.ACTION_UPDATE))
                mAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onOrientationChanged(int paddingTop, int paddingBottom) {
        if (mList != null) {
            mList.setPadding(mList.getPaddingLeft(), paddingTop,
                    mList.getPaddingRight(), paddingBottom);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download);

        // Init windows animate
        mWindowsAnimate = new WindowsAnimate();
        mWindowsAnimate.init(this);

        // Service
        Intent it = new Intent(this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        // Receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_UPDATE);
        registerReceiver(mReceiver, filter);

        ActionBar actionBar = getActionBar();
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mDownloads = Data.getInstance().getAllDownloads();
        for (DownloadInfo di : mDownloads)
            di.selected = false;

        mList = (ListView)findViewById(R.id.download);

        mAdapter = new ListAdapter();

        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        mList.setSelector(new ColorDrawable(Color.TRANSPARENT));

        mThemeColor = Config.getRandomThemeColor() ? Theme.getRandomDarkColor() : Config.getThemeColor();
        int actionBarColor = mThemeColor & 0x00ffffff | 0xdd000000;
        Drawable drawable = new ColorDrawable(actionBarColor);
        actionBar.setBackgroundDrawable(drawable);
        Ui.translucent(this, actionBarColor);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConn);
        mWindowsAnimate.free();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.download, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent it;
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;

        case R.id.action_start:
            it = new Intent(DownloadActivity.this, DownloadService.class);
            startService(it);
            mServiceConn.getService().startAll();
            return true;

        case R.id.action_stop:
            it = new Intent(DownloadActivity.this, DownloadService.class);
            startService(it);
            mServiceConn.getService().stopAll();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        DownloadInfo di = mDownloads.get(position);
        di.selected = !di.selected;
        mAdapter.notifyDataSetChanged();
    }

    public class ListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mDownloads.size();
        }

        @Override
        public Object getItem(int position) {
            return mDownloads.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final DownloadInfo di = mDownloads.get(position);
            final GalleryInfo gi = di.galleryInfo;

            if (convertView == null) {
                convertView = LayoutInflater.from(DownloadActivity.this)
                        .inflate(R.layout.download_list_item, parent, false);
                CardViewSalon.reformWithShadow(((ViewGroup)convertView).getChildAt(0),
                        0xFFFAFAFA, 0, false); // TODO
                mWindowsAnimate.addRippleEffect(((ViewGroup)convertView).getChildAt(0), true);
                ((TextView)convertView.findViewById(R.id.action)).setTextColor(mThemeColor);
            }
            LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.thumb);
            if (!String.valueOf(gi.gid).equals(thumb.getKey())) {
                // Set margin top 8dp if position is 0, otherwise 4dp
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)
                        convertView.findViewById(R.id.card_view).getLayoutParams();
                if (position == 0)
                    lp.topMargin = Ui.dp2pix(8);
                else
                    lp.topMargin = Ui.dp2pix(4);

                // Set new thumb
                thumb.setImageDrawable(null);
                thumb.setLoadInfo(gi.thumb, String.valueOf(gi.gid));
                ImageLoader.getInstance(DownloadActivity.this).add(gi.thumb, String.valueOf(gi.gid),
                        new LoadImageView.SimpleImageGetListener(thumb).setFixScaleType(true));
            }
            TextView title = (TextView)convertView.findViewById(R.id.title);
            title.setText(gi.title);
            View Buttons = convertView.findViewById(R.id.buttons);
            if (di.selected)
                Buttons.setVisibility(View.VISIBLE);
            else
                Buttons.setVisibility(View.GONE);

            ProgressBar pb = (ProgressBar)convertView.findViewById(R.id.progress_bar);
            TextView leftText = (TextView)convertView.findViewById(R.id.text_left);
            TextView rightText = (TextView)convertView.findViewById(R.id.text_right);

            View read = convertView.findViewById(R.id.read);
            TextView action = (TextView)convertView.findViewById(R.id.action);
            View delete1 = convertView.findViewById(R.id.delete1);
            View delete2 = convertView.findViewById(R.id.delete2);
            View detail = convertView.findViewById(R.id.detail);


            read.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DownloadActivity.this,
                            GalleryActivity.class);
                    intent.putExtra(GalleryActivity.KEY_GID, gi.gid);
                    intent.putExtra(GalleryActivity.KEY_TOKEN, gi.token);
                    intent.putExtra(GalleryActivity.KEY_TITLE, gi.title);
                    intent.putExtra(GalleryActivity.KEY_START_INDEX, 0);
                    startActivity(intent);
                }
            });
            delete1.setOnClickListener(new Delete1Action(di));
            delete2.setOnClickListener(new Delete2Action(di));
            detail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DownloadActivity.this,
                            GalleryDetailActivity.class);
                    intent.putExtra(GalleryDetailActivity.KEY_G_INFO, gi);
                    startActivity(intent);
                }
            });

            switch (di.state) {
            case DownloadInfo.STATE_NONE:
                pb.setVisibility(View.INVISIBLE);
                leftText.setText(R.string.not_started);
                rightText.setVisibility(View.GONE);

                action.setText(getString(R.string.start));
                action.setOnClickListener(new StartAction(di));
                break;

            case DownloadInfo.STATE_WAIT:
                pb.setVisibility(View.INVISIBLE);
                leftText.setText(R.string.waiting);
                rightText.setVisibility(View.GONE);

                action.setText(getString(R.string.stop));
                action.setOnClickListener(new StopAction(di));
                break;

            case DownloadInfo.STATE_DOWNLOAD:
                leftText.setText(Utils.sizeToString(di.speed) + "/S");
                if (di.total == -1) {
                    pb.setVisibility(View.VISIBLE);
                    pb.setIndeterminate(true);
                    rightText.setVisibility(View.GONE);
                } else {
                    pb.setVisibility(View.VISIBLE);
                    pb.setIndeterminate(false);
                    pb.setMax(di.total);
                    pb.setProgress(di.download);
                    rightText.setVisibility(View.VISIBLE);
                    rightText.setText(di.download + "/" + di.total);
                }

                action.setText(getString(R.string.stop));
                action.setOnClickListener(new StopAction(di));
                break;

            case DownloadInfo.STATE_FINISH:
                pb.setVisibility(View.INVISIBLE);
                if (di.legacy == 0)
                    leftText.setText(R.string.done);
                else
                    leftText.setText(String.format(getApplication().getString(R.string.legacy_pages), di.legacy));
                rightText.setVisibility(View.GONE);

                action.setText(getString(R.string.start));
                action.setOnClickListener(new StartAction(di));
                break;
            }
            return convertView;
        }
    }

    private class Delete1Action implements View.OnClickListener {
        public DownloadInfo mDownloadInfo;

        public Delete1Action(DownloadInfo di) {
            mDownloadInfo = di;
        }

        @Override
        public void onClick(View v) {
            new DialogBuilder(DownloadActivity.this).setTitle(R.string.attention)
                    .setMessage(R.string.delete).setSimpleNegativeButton()
                    .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((AlertButton)v).dialog.dismiss();
                            Intent it = new Intent(DownloadActivity.this, DownloadService.class);
                            startService(it);
                            mServiceConn.getService().delete(mDownloadInfo);
                        }
                    }).create().show();
        }
    }

    private class Delete2Action implements View.OnClickListener {
        public DownloadInfo mDownloadInfo;

        public Delete2Action(DownloadInfo di) {
            mDownloadInfo = di;
        }

        @Override
        public void onClick(View v) {
            new DialogBuilder(DownloadActivity.this).setTitle(R.string.attention)
                    .setMessage(R.string.delete2).setSimpleNegativeButton()
                    .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((AlertButton)v).dialog.dismiss();
                            Intent it = new Intent(DownloadActivity.this, DownloadService.class);
                            startService(it);
                            mServiceConn.getService().delete(mDownloadInfo);
                            // Delete dir
                            GalleryInfo gi = mDownloadInfo.galleryInfo;
                            Utils.deleteDirInThread(EhUtils.getGalleryDir(gi.gid, gi.title));
                        }
                    }).create().show();
        }
    }

    private class StartAction implements View.OnClickListener {
        public DownloadInfo mDownloadInfo;

        public StartAction(DownloadInfo di) {
            mDownloadInfo = di;
        }

        @Override
        public void onClick(View v) {
            if (mDownloadInfo.state == DownloadInfo.STATE_NONE ||
                    mDownloadInfo.state == DownloadInfo.STATE_FINISH) {
                mDownloadInfo.state = DownloadInfo.STATE_WAIT;

                Intent it = new Intent(DownloadActivity.this, DownloadService.class);
                startService(it);
                mServiceConn.getService().notifyDownloadInfoChanged();
                mAdapter.notifyDataSetChanged();
            }
        }
    }


    private class StopAction implements View.OnClickListener {

        public DownloadInfo mDownloadInfo;

        public StopAction(DownloadInfo di) {
            mDownloadInfo = di;
        }

        @Override
        public void onClick(View v) {
            Intent it = new Intent(DownloadActivity.this, DownloadService.class);
            startService(it);
            mServiceConn.getService().stop(mDownloadInfo);
            mAdapter.notifyDataSetChanged();
        }
    }
}
