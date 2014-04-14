package com.hippo.ehviewer;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import com.hippo.ehviewer.network.Downloader;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Future;
import com.hippo.ehviewer.util.FutureListener;
import com.hippo.ehviewer.util.ThreadPool;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.util.ThreadPool.Job;
import com.hippo.ehviewer.util.ThreadPool.JobContext;

// TODO return onFailure when downloading update or check update
// TODO add disable to achieve

public class UpdateHelper {
    @SuppressWarnings("unused")
    private static final String TAG = "UpdateHelper";
    private static final int NOTIFICATION_ID = -1;
    
    private static final String UPDATE_API = "http://ehviewersu.appsp0t.com/API";
    private static final String HEADER = "Ehviewer-";
    private static final String FOOTER = ".apk";
    
    
    private AppContext mAppContext;
    private OnCheckUpdateListener mListener;
    
    private static boolean mEnabled = true;
    
    public interface OnCheckUpdateListener {
        public void onSuccess(String version, long size, String url, String fileName, String info);
        public void onNoUpdate();
        public void onFailure(String eMsg);
    }
    
    public UpdateHelper(AppContext appContext) {
        mAppContext = appContext;
    }
    
    public UpdateHelper SetOnCheckUpdateListener(OnCheckUpdateListener listener) {
        this.mListener = listener;
        return this;
    }
    
    public void autoCheckUpdate() {
        if (Config.getUpdateDate() < Util.getDate())
            checkUpdate();
    }
    
    public static void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
    
    
    class CheckUpdatePackage {
        public OnCheckUpdateListener listener;
        public AppContext appContext;
        public String pageContext;
        public HttpHelper hp;
    }
    
    public void checkUpdate() {
        final HttpHelper hp = new HttpHelper(mAppContext);
        ThreadPool threadPool = mAppContext.getNetworkThreadPool();
        threadPool.submit(new Job<String>() {
            @Override
            public String run(JobContext jc) {
                if (!mEnabled) {
                    return "working";
                }
                
                mEnabled = false;
                
                PackageManager pm = mAppContext.getPackageManager();
                PackageInfo pi = null;
                try {
                    pi = pm.getPackageInfo(mAppContext.getPackageName(), PackageManager.GET_ACTIVITIES);
                    
                    JSONObject jo = new JSONObject();
                    jo.put("method", "update");
                    
                    JSONObject detailJO = new JSONObject();
                    detailJO.put("version", pi.versionName);
                    detailJO.put("server", Config.getUpdateServer());
                    
                    jo.put("detail", detailJO);
                    
                    return hp.postJson(UPDATE_API, jo.toString());
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                    return "NameNotFound";
                } catch (JSONException e) {
                    e.printStackTrace();
                    return "JSONException";
                }
            }
        }, new FutureListener<String>() {
            @Override
            public void onFutureDone(Future<String> future) {
                String pageContext = future.get();
                CheckUpdatePackage checkUpdatePackage = new CheckUpdatePackage();
                checkUpdatePackage.appContext = mAppContext;
                checkUpdatePackage.listener = mListener;
                checkUpdatePackage.pageContext = pageContext;
                checkUpdatePackage.hp = hp;
                Message msg = new Message();
                msg.obj = checkUpdatePackage;
                
                mHandler.sendMessage(msg);
            }
        });
    }
    
    // TODO move dialog into listener
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            CheckUpdatePackage checkUpdatePackage = (CheckUpdatePackage)msg.obj;
            OnCheckUpdateListener listener = checkUpdatePackage.listener;
            AppContext appContext = checkUpdatePackage.appContext;
            String pageContext = checkUpdatePackage.pageContext;
            
            if (pageContext != null) {
                Config.setUpdateDate();
                
                if (pageContext.equals("working")) {
                    if (listener != null)
                        listener.onFailure("正在检查更新或者下载更新"); // TODO
                }
                else if (pageContext.equals("NameNotFound")) {
                    if (listener != null)
                        listener.onFailure("NameNotFound"); // TODO
                    
                } else if (pageContext.equals("JSONException")) {
                    if (listener != null)
                        listener.onFailure("JSONException"); // TODO
                    
                } else {
                    try {
                        
                        JSONObject jo = new JSONObject(pageContext);
                        JSONObject updateJO = jo.getJSONObject("update");
                        
                        String version = updateJO.getString("version");
                        
                        if (version.equals("error")) {
                            if (listener != null)
                                listener.onFailure(appContext.getString(R.string.em_invalid_request));
                        } else if (version.equals("none")) {
                            if (listener != null)
                                listener.onNoUpdate();
                        } else {
                            long size = updateJO.getLong("size");
                            String url = updateJO.getString("url");
                            String info = updateJO.getString("info");
                            
                            if (listener != null)
                                listener.onSuccess(version, size, url, HEADER + version + FOOTER, info);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (listener != null)
                            listener.onFailure(appContext.getString(R.string.em_host_error));
                    }
                }
            } else {
                if (listener != null)
                    listener.onFailure(checkUpdatePackage.hp.getEMsg());
            }
        }
    };
    
    public static class UpdateListener implements Downloader.OnDownloadListener {
        private Context mContext;
        private String mFileName;
        
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        
        public UpdateListener(Context context, String fileName) {
            mContext = context;
            mFileName = fileName;
            mNotifyManager = (NotificationManager)
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(mContext);
            mBuilder.setSmallIcon(R.drawable.ic_launcher);
        }
        
        @Override
        public void onDownloadStartConnect() {
            mBuilder.setContentTitle("开始下载更新") // TODO
                    .setContentText(null)
                    .setProgress(0, 0, true).setOngoing(true);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
        
        @Override
        public void onDownloadStartDownload(int totalSize) {
            mBuilder.setContentTitle("正在下载更新")
                    .setContentText(String.format("%.2f / %.2f KB", 0/1024.0f, totalSize/1024.0f))
                    .setProgress(totalSize, 0, false).setOngoing(true);
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
        public void onDownloadOver(int status) {
            if (status == Downloader.COMPLETED) {
                mNotifyManager.cancel(NOTIFICATION_ID);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(Config.getDownloadPath(), mFileName)),
                        "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                mBuilder.setContentTitle("下载更新失败")
                        .setContentText(null)
                        .setProgress(0, 0, false).setOngoing(false);
                mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            }
            
            setEnabled(true);
        }
    }
}
