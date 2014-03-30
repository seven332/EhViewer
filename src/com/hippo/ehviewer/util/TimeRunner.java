package com.hippo.ehviewer.util;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

public abstract class TimeRunner {
    
    private static final int INTERVAL = 5;
    
    private static final int RUNNING = 0;
    private static final int START = 1;
    private static final int END = 2;
    
    private boolean mRunInThread = true;
    private boolean mListenerInThread = true;
    
    private OnTimeListener mListener;
    
    public interface OnTimeListener {
        public void onStart();
        public void onEnd();
    }
    
    // TODO
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case RUNNING:
                run((Float)msg.obj, msg.arg1);
                break;
            case START:
                mListener.onStart();
                break;
            case END:
                mListener.onEnd();
                break;
            }
        }
    };
    
    private int mDuration = 0;
    private int mDelay = 0;
    
    public void setOnTimerListener(OnTimeListener listener) {
        mListener = listener;
    }
    
    public void setDuration(int duration) {
        mDuration = duration;
    }
    
    public void setRunInThread(boolean runInThread) {
        mRunInThread = runInThread;
    }
    
    public void setListenerInThread(boolean listenerInThread) {
        mListenerInThread = listenerInThread;
    }
    
    public void start() {
        start(0);
    }
    
    public void start(int delay) {
        mDelay = delay;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(mDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mListener != null) {
                    if (mListenerInThread) {
                        mListener.onStart();
                    } else {
                        Message msg = new Message();
                        msg.what = START;
                        mHandler.sendMessage(msg);
                    }
                }
                
                long startTime = System.currentTimeMillis();
                for (int runningTime = 0; runningTime < mDuration; runningTime = (int)(System.currentTimeMillis() - startTime)) {
                    if (mRunInThread) {
                        TimeRunner.this.run(runningTime/(float)mDuration, runningTime);
                    } else {
                        Message msg = new Message();
                        msg.what = RUNNING;
                        msg.obj = runningTime/(float)mDuration;
                        msg.arg1 = runningTime;
                        mHandler.sendMessage(msg);
                    }
                    try {
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
                if (mListener != null) {
                    if (mListenerInThread) {
                        mListener.onEnd();
                    } else {
                        Message msg = new Message();
                        msg.what = END;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }).start();
    }
    
    protected abstract void run(float interpolatedTime, int runningTime);
}
