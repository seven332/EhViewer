/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.util;

import android.support.annotation.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SerialThreadExecutor implements Executor, Runnable {

    private long mKeepAliveTime;
    private BlockingQueue<Runnable> mWorkQueue;
    private ThreadFactory mThreadFactory;

    private transient Thread mThread;

    private final Lock mThreadLock = new ReentrantLock();
    private final Object mWaitLock = new Object();

    public SerialThreadExecutor(long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        mKeepAliveTime = unit.toMillis(keepAliveTime);
        mWorkQueue = workQueue;
        mThreadFactory = threadFactory;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        try {
            mWorkQueue.put(command);
            ensureThread();
        } catch (Exception e) {
            putFailed(command, e);
        }
    }

    private void ensureThread() {
        mThreadLock.lock();
        if (mThread == null) {
            mThread = mThreadFactory.newThread(this);
            mThread.start();
        } else {
            synchronized (mWaitLock) {
                mWaitLock.notifyAll();
            }
        }
        mThreadLock.unlock();
    }

    protected void putFailed(@NonNull Runnable command, @NonNull Exception e) {
    }

    protected void beforeExecute(Thread t, Runnable r) {
    }

    protected void afterExecute(Runnable r, Throwable t) {
    }

    protected void beforeExcute() {

    }

    @Override
    public void run() {
        boolean hasWait = false;

        for (;;) {
            mThreadLock.lock();

            if (mWorkQueue.isEmpty()) {
                if (hasWait) {
                    mThread = null;
                    mThreadLock.unlock();
                    // Have waitted enough time
                    break;
                } else {
                    hasWait = true;
                    synchronized (mWaitLock) {
                        mThreadLock.unlock();
                        try {
                            mWaitLock.wait(mKeepAliveTime);
                        } catch (InterruptedException e) {
                            // Empty
                        }
                        continue;
                    }
                }
            } else {
                mThreadLock.unlock();
            }

            hasWait = false;
            Runnable runnable = mWorkQueue.poll();
            if (runnable != null) {
                beforeExecute(mThread, runnable);
                Throwable throwable = null;
                try {
                    runnable.run();
                } catch (Throwable tr) {
                    throwable = tr;
                }
                afterExecute(runnable, throwable);
            }
        }
    }
}
