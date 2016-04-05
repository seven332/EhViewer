package com.hippo.dict;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.DictImportActivity;
import com.hippo.ehviewer.ui.MainActivity;


public class DictNotification {

    private final Context mContext;
    private final NotificationManager mNotifyManager;
    private final NotificationCompat.Builder mBuilder;
    private int mMax = 100;
    private final int mId = 1;

    private final PendingIntent mImportIntent;
    private final PendingIntent mDoneIntent;

    public DictNotification(Context context) {

        mContext = context;
        mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mContext);

        mImportIntent = PendingIntent.getActivity(context, 0, new Intent(context, DictImportActivity.class), 0);
        mDoneIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

        mBuilder.setContentTitle(mContext.getResources().getString(R.string.dict_import))
                .setContentText("0/" + mMax)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(mImportIntent)
                .setOngoing(true);
    }

    public DictImportService.ProcessListener mNotificationListener = new DictImportService.ProcessListener() {
        @Override
        public void process(int progress) {
            DictNotification.this.notify(progress);
        }

        @Override
        public void processTotal(int total) {
            // this may useless
            mMax = total;
        }

        @Override
        public void processComplete() {
            DictNotification.this.notifyDone();
        }
    };

    public void setMax(int max) {
        mMax = max;
        mBuilder.setContentText("0/" + mMax);
    }

    public void setFileName(String fileName) {
        mBuilder.setContentTitle(mContext.getResources().getString(R.string.dict_import) + " " + fileName);
    }

    public void notify(int progress) {
        mBuilder.setContentIntent(mImportIntent);
        mBuilder.setProgress(mMax, progress, false);
        mBuilder.setContentText(progress + "/" + mMax);
        mNotifyManager.notify(mId, mBuilder.build());
    }

    public void notifyDone() {
        mBuilder.setContentIntent(mDoneIntent);
        mBuilder.setOngoing(false);
        mNotifyManager.notify(mId, mBuilder.build());
    }

    public void stopNotify() {
        mNotifyManager.cancel(mId);
    }
}
