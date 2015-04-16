package com.hippo.ehviewer.miscellaneous;

import android.content.Context;

import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.BgThread;
import com.hippo.ehviewer.widget.MaterialToast;

import java.io.File;

public class Downloader implements Runnable, HttpHelper.OnDownloadListener {

    private Context mContext;
    private String mUrl;
    private File mDir;
    private String mFilename;

    private boolean mIsRunning = false;

    public Downloader(Context context, String url, File dir, String filename) {
        mContext = context.getApplicationContext();
        mUrl = url;
        mDir = dir;
        mFilename = filename;
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void startDownload() {
        if (mIsRunning) {
            return;
        }

        mIsRunning = true;
        new BgThread(this).start();
        MaterialToast.showToast(R.string.start_saving_torrent);
    }

    public void post(Runnable runnable) {
        AppHandler.getInstance().post(runnable);
    }

    @Override
    public void run() {
        HttpHelper hh = new HttpHelper(mContext);
        final boolean ok = HttpHelper.DOWNLOAD_OK_STR.equals(
                hh.download(mUrl, mDir, mFilename, null, null, this));
        post(new Runnable() {
            @Override
            public void run() {
                MaterialToast.showToast(ok ?
                        String.format(mContext.getString(R.string.save_torrent_successful),
                                mDir.getAbsolutePath() + File.separatorChar + mFilename) :
                        mContext.getString(R.string.save_torrent_failed));
            }
        });
        mIsRunning = false;
    }

    @Override
    public void onDownloadStartConnect() {
        // Empty
    }

    @Override
    public void onDownloadStartDownload(int totalSize) {
        // Empty
    }

    @Override
    public void onDownloadStatusUpdate(int downloadSize, int totalSize) {
        // Empty
    }

    @Override
    public void onDownloadOver(int status, String eMsg) {
        // Empty
    }

    @Override
    public void onUpdateFilename(String newFilename) {
        mFilename = newFilename;
    }
}
