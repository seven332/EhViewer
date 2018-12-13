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

package com.hippo.ehviewer.gallery.archive;

import com.hippo.unifile.UniRandomAccessFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import okio.Store;
import okio.UniRandomAccessFileStore;
import org.apache.commons.compress.archivers.zip.ZipFile;

public class ZipArchive extends Archive {

  private ZipFile file;

  private ZipArchive(ZipFile file) {
    this.file = file;
  }

  @Override
  public List<ArchiveEntry> getArchiveEntries() {
    List<ArchiveEntry> result = new ArrayList<>();

    Enumeration<org.apache.commons.compress.archivers.zip.ZipArchiveEntry> entries = file.getEntries();
    while (entries.hasMoreElements()) {
      org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry = entries.nextElement();
      if (isSupportedFilename(entry.getName().toLowerCase())) {
        result.add(new ZipArchiveEntry(file, entry));
      }
    }

    return result;
  }

  @Override
  public void close() throws IOException {
    file.close();
  }

  public static Archive create(UniRandomAccessFile file) throws IOException {
    Store store = new UniRandomAccessFileStore(file);
    ZipFile zipFile = new ZipFile(store);
    return new ZipArchive(zipFile);
  }

  public static class ZipArchiveEntry extends ArchiveEntry {

    private ZipFile file;
    private org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry;

    private ZipArchiveEntry(
        ZipFile file,
        org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry
    ) {
      this.file = file;
      this.entry = entry;
    }

    @Override
    public String getName() {
      return entry.getName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return file.getInputStream(entry);
    }
  }
}
