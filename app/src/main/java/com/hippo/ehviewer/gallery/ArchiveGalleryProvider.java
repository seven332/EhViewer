/*
 * Copyright 2018 Hippo Seven
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

package com.hippo.ehviewer.gallery;

import android.content.Context;
import android.net.Uri;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.archive.Archive;
import com.hippo.ehviewer.gallery.archive.ArchiveEntry;
import com.hippo.glgallery.GalleryPageView;
import com.hippo.image.Image;
import com.hippo.unifile.UniFile;
import com.hippo.unifile.UniRandomAccessFile;
import com.hippo.util.NaturalComparator;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.thread.PriorityThread;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

public class ArchiveGalleryProvider extends GalleryProvider2 implements Runnable {

  private static final String TAG = ArchiveGalleryProvider.class.getSimpleName();
  private static final AtomicInteger sIdGenerator = new AtomicInteger();

  private final UniFile file;
  private final Stack<Integer> requests = new Stack<>();
  private final AtomicInteger decodingIndex = new AtomicInteger(GalleryPageView.INVALID_INDEX);
  @Nullable
  private Thread bgThread;
  private volatile int size = STATE_WAIT;
  private String error;

  public ArchiveGalleryProvider(Context context, Uri uri) {
    file = UniFile.fromUri(context, uri);
  }

  @Override
  public void start() {
    super.start();

    bgThread = new PriorityThread(this, TAG + '-' + sIdGenerator.incrementAndGet(),
        Process.THREAD_PRIORITY_BACKGROUND);
    bgThread.start();
  }

  @Override
  public void stop() {
    super.stop();

    if (bgThread != null) {
      bgThread.interrupt();
      bgThread = null;
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  protected void onRequest(int index) {
    synchronized (requests) {
      if (!requests.contains(index) && index != decodingIndex.get()) {
        requests.add(index);
        requests.notify();
      }
    }
    notifyPageWait(index);
  }

  @Override
  protected void onForceRequest(int index) {
    onRequest(index);
  }

  @Override
  protected void onCancelRequest(int index) {
    synchronized (requests) {
      requests.remove(Integer.valueOf(index));
    }
  }

  @Override
  public String getError() {
    return error;
  }

  @NonNull
  @Override
  public String getImageFilename(int index) {
    // TODO
    return Integer.toString(index);
  }

  @Override
  public boolean save(int index, @NonNull UniFile file) {
    // TODO
    return false;
  }

  @Nullable
  @Override
  public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
    // TODO
    return null;
  }

  @Override
  public void run() {
    UniRandomAccessFile uraf = null;
    if (file != null) {
      try {
        uraf = file.createRandomAccessFile("r");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (uraf == null) {
      size = STATE_ERROR;
      error = GetText.getString(R.string.error_reading_failed);
      notifyDataChanged();
      return;
    }

    Archive archive = null;
    try {
      archive = Archive.create(uraf);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (archive == null) {
      size = STATE_ERROR;
      error = GetText.getString(R.string.error_invalid_archive);
      notifyDataChanged();
      return;
    }

    List<ArchiveEntry> entries = archive.getArchiveEntries();
    Collections.sort(entries, naturalComparator);

    // Update size and notify changed
    size = entries.size();
    notifyDataChanged();

    while (!Thread.currentThread().isInterrupted()) {
      int index;
      synchronized (requests) {
        if (requests.isEmpty()) {
          try {
            requests.wait();
          } catch (InterruptedException e) {
            // Interrupted
            break;
          }
          continue;
        }
        index = requests.pop();
        decodingIndex.lazySet(index);
      }

      // Check index valid
      if (index < 0 || index >= entries.size()) {
        decodingIndex.lazySet(GalleryPageView.INVALID_INDEX);
        notifyPageFailed(index, GetText.getString(R.string.error_out_of_range));
        continue;
      }

      try {
        InputStream is = entries.get(index).getInputStream();
        Image image = Image.decode(is, true);
        decodingIndex.lazySet(GalleryPageView.INVALID_INDEX);
        if (image != null) {
          notifyPageSucceed(index, image);
        } else {
          notifyPageFailed(index, GetText.getString(R.string.error_decoding_failed));
        }
      } catch (IOException e) {
        decodingIndex.lazySet(GalleryPageView.INVALID_INDEX);
        notifyPageFailed(index, GetText.getString(R.string.error_reading_failed));
      }
    }

    // Clear
    IOUtils.closeQuietly(archive);
  }

  private static Comparator<ArchiveEntry> naturalComparator = new Comparator<ArchiveEntry>() {
    private NaturalComparator comparator = new NaturalComparator();
    @Override
    public int compare(ArchiveEntry o1, ArchiveEntry o2) {
      return comparator.compare(o1.getName(), o2.getName());
    }
  };
}
