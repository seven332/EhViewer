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

package com.hippo.ehviewer;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.BgThread;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

// TODO return onFailure when downloading update or check update
// TODO add disable to achieve

public class UpdateHelper implements Runnable {
    @SuppressWarnings("unused")
    private static final String TAG = "UpdateHelper";

    private static final String HEADER = "Ehviewer-";
    private static final String FOOTER = ".apk";


    private final AppContext mAppContext;
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
        if (Config.getUpdateDate() < Utils.getDate())
            checkUpdate();
    }

    public static void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }


    class CheckUpdatePackage {
        public OnCheckUpdateListener listener;
        public String body;
        public String emesg;
    }

    private void send(CheckUpdatePackage _package) {
        AppHandler.getInstance().post(new Respond(_package));
    }

    @Override
    public void run() {
        CheckUpdatePackage _package = new CheckUpdatePackage();
        _package.listener = mListener;
        final HttpHelper hp = new HttpHelper(mAppContext);
        if (!mEnabled) {
            _package.body = "working";
            send(_package);
            return;
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

            _package.body = hp.postJson(EhClient.API_EHVIEWER, jo);
            _package.emesg = hp.getEMsg();
            send(_package);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            _package.body = "NameNotFound";
            send(_package);
        } catch (JSONException e) {
            e.printStackTrace();
            _package.body = "JSONException";
            send(_package);
        }
    }

    public void checkUpdate() {
        new BgThread(this).start();
    }

    private class Respond implements Runnable {

        CheckUpdatePackage mPackage;

        public Respond(CheckUpdatePackage _package) {
            mPackage = _package;
        }

        @Override
        public void run() {
            OnCheckUpdateListener listener = mPackage.listener;
            String body = mPackage.body;
            if (body != null) {
                Config.setUpdateDate();

                if (listener == null)
                    return;

                if (body.equals("working")) {
                    listener.onFailure("正在检查更新或者下载更新"); // TODO
                } else if (body.equals("NameNotFound")) {
                    listener.onFailure("NameNotFound"); // TODO
                } else if (body.equals("JSONException")) {
                    listener.onFailure("JSONException"); // TODO
                } else {
                    try {
                        JSONObject jo = new JSONObject(body);
                        JSONObject updateJO = jo.getJSONObject("update");

                        if (updateJO.has("error")) {
                            listener.onFailure(updateJO.getString("error"));
                        } else {
                            String version = updateJO.getString("version");
                            if (version.equals("none")) {
                                listener.onNoUpdate();
                            } else {
                                long size = updateJO.getLong("size");
                                String url = updateJO.getString("url");
                                String info = updateJO.getString("info");

                                listener.onSuccess(version, size, url, HEADER + version + FOOTER, info);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.onFailure("JSONException");
                    }
                }
            }
        }
    }

    public static class UpdateListener implements HttpHelper.OnDownloadListener {
        private final Context mContext;
        private final String mFileName;

        private final NotificationManager mNotifyManager;

        public UpdateListener(Context context, String fileName) {
            mContext = context;
            mFileName = fileName;
            mNotifyManager = (NotificationManager)
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        @Override
        public void onDownloadStartConnect() {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(mContext)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(mContext.getString(R.string.start_update))
                    .setProgress(0, 0, true)
                    .setOngoing(true)
                    .setTicker(mContext.getString(R.string.start_update));
            mNotifyManager.notify(NotificationId.UPDATE_ID, builder.build());
        }

        @Override
        public void onDownloadStartDownload(int totalSize) {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(mContext)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(mContext.getString(R.string.download_update))
                    .setContentText(String.format("%.2f / %.2f KB", 0/1024.0f, totalSize/1024.0f))
                    .setProgress(totalSize, 0, false)
                    .setOngoing(true);
            mNotifyManager.notify(NotificationId.UPDATE_ID, builder.build());
        }

        @Override
        public void onDownloadStatusUpdate(
                int downloadSize, int totalSize) {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(mContext)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(mContext.getString(R.string.download_update))
                    .setContentText(String.format("%.2f / %.2f KB", downloadSize/1024.0f, totalSize/1024.0f))
                    .setProgress(totalSize, downloadSize, false)
                    .setOngoing(true);
            mNotifyManager.notify(NotificationId.UPDATE_ID, builder.build());
        }

        @Override
        public void onDownloadOver(int status, String eMsg) {
            if (status == HttpHelper.DOWNLOAD_OK_CODE) {
                mNotifyManager.cancel(NotificationId.UPDATE_ID);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(Config.getDownloadPath(), mFileName)),
                        "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(mContext)
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setContentTitle(mContext.getString(R.string.update_failed))
                        .setContentText(eMsg)
                        .setOngoing(false);
                mNotifyManager.notify(NotificationId.UPDATE_ID, builder.build());
            }
            setEnabled(true);
        }

        @Override
        public void onUpdateFilename(String newFilename) {

        }
    }
}
