/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.gallery.glrenderer;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InfiniteThreadExecutor implements Executor {

    private long mKeepAliveMillis;
    private BlockingQueue<Runnable> mWorkQueue;
    private ThreadFactory mThreadFactory;
    private volatile int mThreadCount;
    private volatile int mEmptyThreadCount;

    private final Lock mThreadLock = new ReentrantLock();
    private final Object mWaitLock = new Object();

    public InfiniteThreadExecutor(long keepAliveMillis,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        mKeepAliveMillis = keepAliveMillis;
        mWorkQueue = workQueue;
        mThreadFactory = threadFactory;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        mThreadLock.lock();
        try {
            mWorkQueue.add(command);
            if (mEmptyThreadCount > 0) {
                mEmptyThreadCount--;
                synchronized (mWaitLock) {
                    mWaitLock.notify();
                }
            } else {
                mThreadFactory.newThread(new Task()).start();
            }
        } catch (Exception e) {
            // Ignore
        }
        mThreadLock.unlock();
    }

    public int getThreadCount() {
        return mThreadCount;
    }

    public class Task implements Runnable {

        @Override
        public void run() {
            Log.d("TAG", Thread.currentThread().getName() + " start");
            mThreadCount++;

            int loop = 0;
            boolean end = false;
            for (;;) {
                Runnable command = mWorkQueue.poll();
                if (command == null) {
                    mEmptyThreadCount--;
                    end = true;
                }

                if (loop != 0) {
                    mThreadLock.unlock();
                }

                if (end) {
                    break;
                }

                command.run();

                mEmptyThreadCount++;
                synchronized (mWaitLock) {
                    try {
                        mWaitLock.wait(mKeepAliveMillis);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
                mThreadLock.lock();
                loop++;
            }

            mThreadCount--;
            Log.d("TAG", Thread.currentThread().getName() + " end");
        }
    }
}
