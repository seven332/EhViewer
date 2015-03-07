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

package com.hippo.ehviewer.util;

import android.os.Process;

public class BgThread extends Thread {

    public BgThread() {
        super();
        setPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    public BgThread(Runnable runnable) {
        super(runnable);
        setPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    public BgThread(Runnable runnable, String threadName) {
        super(runnable, threadName);
        setPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    public BgThread(String threadName) {
        super(threadName);
        setPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    public BgThread(ThreadGroup group, Runnable runnable) {
        super(group, runnable);
        setPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    public BgThread(ThreadGroup group, Runnable runnable, String threadName) {
        super(group, runnable, threadName);
        setPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    public BgThread(ThreadGroup group, String threadName) {
        super(group, threadName);
        setPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    public BgThread(ThreadGroup group, Runnable runnable, String threadName, long stackSize) {
        super(group, runnable, threadName, stackSize);
        setPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }
}
