package com.hippo.ehviewer;

import java.io.File;
import java.net.MalformedURLException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.hippo.ehviewer.network.Downloader;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhClient;
import com.hippo.ehviewer.util.Future;
import com.hippo.ehviewer.util.FutureListener;
import com.hippo.ehviewer.util.ThreadPool;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.util.ThreadPool.Job;
import com.hippo.ehviewer.util.ThreadPool.JobContext;
import com.hippo.ehviewer.view.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;

public class UpdateHelper {
    private static final String TAG = "UpdateHelper";
    private static final int NOTIFICATION_ID = -1;
    
    public static final String UPDATE_API = "http://ehviewersu.appsp0t.com/API";
    
    // Update host
    private static final int GOOGLE = 0;
    private static final int QINIU = 1;
    
    private Activity mActivity;
    private String updateFileName;
    private OnCheckUpdateListener listener;
    
    
    private int downloadHost = QINIU;
    
    public UpdateHelper(Activity activity) {
        mActivity = activity;
    }
    
    public UpdateHelper SetOnCheckUpdateListener(OnCheckUpdateListener listener) {
        this.listener = listener;
        return this;
    }
    
    public void autoCheckUpdate() {
        if (Config.getUpdateDate() < Util.getDate())
            checkUpdate();
    }
    
    public void checkUpdate() {
        final AppContext appContext = (AppContext)(mActivity.getApplicationContext());
        final HttpHelper hp = new HttpHelper(mActivity.getApplicationContext());
        ThreadPool threadPool = appContext.getNetworkThreadPool();
        threadPool.submit(new Job<String>() {
            @Override
            public String run(JobContext jc) {
                PackageManager pm = appContext.getPackageManager();
                PackageInfo pi = null;
                try {
                    pi = pm.getPackageInfo(appContext.getPackageName(), PackageManager.GET_ACTIVITIES);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                    return "NameNotFound";
                }
                return hp.post(UPDATE_API, "last\n" + pi.versionName);
            }
        }, new FutureListener<String>() {
            @Override
            public void onFutureDone(Future<String> future) {
                String pageContext = future.get();
                if (pageContext != null) {
                    Config.setUpdateDate();
                    String[] items = pageContext.split("\n");
                    if (items.length > 3) {
                        
                        if (listener != null)
                            listener.onSuccess(pageContext);
                        
                        String newVer = items[0];
                        
                        String tempUrl = "";
                        switch (downloadHost) {
                        case GOOGLE:
                            tempUrl = EhClient.UPDATE_URL + items[1];
                            break;
                        case QINIU:
                            tempUrl = EhClient.UPDATE_URI_QINIU + Util.getFileForUrl(items[1]);
                            break;
                        }
                        final String url = tempUrl;
                        final String name = url.substring(url.lastIndexOf('/')+1);
                        updateFileName = name;
                        String size = items[2];
                        String info = items[3];
                        
                        AlertDialog dialog = new DialogBuilder(mActivity).setTitle(R.string.update)
                                .setMessage(String.format(mActivity.getString(R.string.update_message), newVer, size, info))
                                .setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ((AlertButton)v).dialog.dismiss();
                                    }
                                }).setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ((AlertButton)v).dialog.dismiss();
                                        Downloader d = new Downloader();
                                        try {
                                            d.resetData(Config.getDownloadPath(), name, url);
                                            d.setOnDownloadListener(new UpdateListener());
                                            new Thread(d).start();
                                        } catch (MalformedURLException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).create();
                        if (!mActivity.isFinishing())
                            dialog.show();
                    } else {
                        if(pageContext.equals("none")) {
                            if (listener != null)
                                listener.onNoUpdate();
                        } else if(pageContext.equals("error")){
                            if (listener != null)
                                listener.onFailure(appContext.getString(R.string.em_request_error));
                        } else if (pageContext.equals("NameNotFound")){
                            if (listener != null)
                                listener.onFailure("NameNotFound");
                        }else {
                            if (listener != null)
                                listener.onFailure(appContext.getString(R.string.em_host_error));
                        }
                    }
                } else {
                    if (listener != null)
                        listener.onFailure(hp.getEMsg());
                }
            }
        });
    }
    
    public interface OnCheckUpdateListener {
        public void onSuccess(String pageContext);
        public void onNoUpdate();
        public void onFailure(String eMsg);
    }
    
    private class UpdateListener implements Downloader.OnDownloadListener {
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        
        public UpdateListener() {
            mNotifyManager = (NotificationManager)
                    mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(mActivity.getApplicationContext());
            mBuilder.setSmallIcon(R.drawable.ic_launcher);
        }
        
        @Override
        public void onDownloadStart(int totalSize) {
            mBuilder.setContentTitle("正在下载更新") // TODO
                    .setContentText(null)
                    .setProgress(0, 0, true).setOngoing(true);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
        @Override
        public void onDownloadStatusUpdate(
                int downloadSize, int totalSize) {
            mBuilder.setContentTitle("正在下载更新")
                    .setContentText(String.format("%.2f / %.2f KB", downloadSize/1024.0f, totalSize/1024.0f))
                    .setProgress(totalSize, downloadSize, false).setOngoing(true);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            
        }
        @Override
        public void onDownloadOver(boolean ok, int eMesgId) {
            if (ok) {
                mNotifyManager.cancel(NOTIFICATION_ID);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(Config.getDownloadPath(), updateFileName)),
                        "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.getApplicationContext().startActivity(intent);
            } else {
                mBuilder.setContentTitle("下载更新失败")
                        .setContentText(null)
                        .setProgress(0, 0, false).setOngoing(false);
                mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            }
        }
    }
}
