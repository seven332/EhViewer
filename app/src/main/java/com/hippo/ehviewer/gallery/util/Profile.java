/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.hippo.ehviewer.gallery.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import java.util.ArrayList;
import java.util.Random;

// The Profile class is used to collect profiling information for a thread. It
// samples stack traces for a thread periodically. enable() and disable() is
// used to enable and disable profiling for the calling thread. The profiling
// information can then be dumped to a file using the dumpToFile() method.
//
// The disableAll() method can be used to disable profiling for all threads and
// can be called in onPause() to ensure all profiling is disabled when an
// activity is paused.
public class Profile {
    @SuppressWarnings("unused")
    private static final String TAG = "Profile";
    private static final int NS_PER_MS = 1000000;

    // This is a watchdog entry for one thread.
    // For every cycleTime period, we dump the stack of the thread.
    private static class WatchEntry {
        Thread thread;

        // Both are in milliseconds
        int cycleTime;
        int wakeTime;

        boolean isHolding;
        ArrayList<String[]> holdingStacks = new ArrayList<>();
    }

    // This is a watchdog thread which dumps stacks of other threads periodically.
    private static Watchdog sWatchdog = new Watchdog();

    private static class Watchdog {
        private ArrayList<WatchEntry> mList = new ArrayList<>();
        private HandlerThread mHandlerThread;
        private Handler mHandler;
        private Runnable mProcessRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (Watchdog.this) {
                    processList();
                }
            }
        };
        private Random mRandom = new Random();
        private ProfileData mProfileData = new ProfileData();

        public Watchdog() {
            mHandlerThread = new HandlerThread("Watchdog Handler",
                    Process.THREAD_PRIORITY_FOREGROUND);
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }

        public synchronized void addWatchEntry(Thread thread, int cycleTime) {
            WatchEntry e = new WatchEntry();
            e.thread = thread;
            e.cycleTime = cycleTime;
            int firstDelay = 1 + mRandom.nextInt(cycleTime);
            e.wakeTime = (int) (System.nanoTime() / NS_PER_MS) + firstDelay;
            mList.add(e);
            processList();
        }

        public synchronized void removeWatchEntry(Thread thread) {
            for (int i = 0; i < mList.size(); i++) {
                if (mList.get(i).thread == thread) {
                    mList.remove(i);
                    break;
                }
            }
            processList();
        }

        public synchronized void removeAllWatchEntries() {
            mList.clear();
            processList();
        }

        private void processList() {
            mHandler.removeCallbacks(mProcessRunnable);
            if (mList.size() == 0) return;

            int currentTime = (int) (System.nanoTime() / NS_PER_MS);
            int nextWakeTime = 0;

            for (WatchEntry entry : mList) {
                if (currentTime > entry.wakeTime) {
                    entry.wakeTime += entry.cycleTime;
                    sampleStack(entry);
                }

                if (entry.wakeTime > nextWakeTime) {
                    nextWakeTime = entry.wakeTime;
                }
            }

            long delay = nextWakeTime - currentTime;
            mHandler.postDelayed(mProcessRunnable, delay);
        }

        private void sampleStack(WatchEntry entry) {
            Thread thread = entry.thread;
            StackTraceElement[] stack = thread.getStackTrace();
            String[] lines = new String[stack.length];
            for (int i = 0; i < stack.length; i++) {
                lines[i] = stack[i].toString();
            }
            if (entry.isHolding) {
                entry.holdingStacks.add(lines);
            } else {
                mProfileData.addSample(lines);
            }
        }

        private WatchEntry findEntry(Thread thread) {
            for (int i = 0; i < mList.size(); i++) {
                WatchEntry entry = mList.get(i);
                if (entry.thread == thread) return entry;
            }
            return null;
        }

        public synchronized void dumpToFile(String filename) {
            mProfileData.dumpToFile(filename);
        }

        public synchronized void reset() {
            mProfileData.reset();
        }

        public synchronized void hold(Thread t) {
            WatchEntry entry = findEntry(t);

            // This can happen if the profiling is disabled (probably from
            // another thread). Same check is applied in commit() and drop()
            // below.
            if (entry == null) return;

            entry.isHolding = true;
        }

        public synchronized void commit(Thread t) {
            WatchEntry entry = findEntry(t);
            if (entry == null) return;
            ArrayList<String[]> stacks = entry.holdingStacks;
            for (int i = 0; i < stacks.size(); i++) {
                mProfileData.addSample(stacks.get(i));
            }
            entry.isHolding = false;
            entry.holdingStacks.clear();
        }

        public synchronized void drop(Thread t) {
            WatchEntry entry = findEntry(t);
            if (entry == null) return;
            entry.isHolding = false;
            entry.holdingStacks.clear();
        }
    }

    // Enable profiling for the calling thread. Periodically (every cycleTimeInMs
    // milliseconds) sample the stack trace of the calling thread.
    public static void enable(int cycleTimeInMs) {
        Thread t = Thread.currentThread();
        sWatchdog.addWatchEntry(t, cycleTimeInMs);
    }

    // Disable profiling for the calling thread.
    public static void disable() {
        sWatchdog.removeWatchEntry(Thread.currentThread());
    }

    // Disable profiling for all threads.
    public static void disableAll() {
        sWatchdog.removeAllWatchEntries();
    }

    // Dump the profiling data to a file.
    public static void dumpToFile(String filename) {
        sWatchdog.dumpToFile(filename);
    }

    // Reset the collected profiling data.
    public static void reset() {
        sWatchdog.reset();
    }

    // Hold the future samples coming from current thread until commit() or
    // drop() is called, and those samples are recorded or ignored as a result.
    // This must called after enable() to be effective.
    public static void hold() {
        sWatchdog.hold(Thread.currentThread());
    }

    public static void commit() {
        sWatchdog.commit(Thread.currentThread());
    }

    public static void drop() {
        sWatchdog.drop(Thread.currentThread());
    }
}
