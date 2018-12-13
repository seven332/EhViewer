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
import java.util.List;
import okio.Store;
import okio.UniRandomAccessFileStore;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

public class SevenZArchive extends Archive {

  private SevenZFile file;

  public SevenZArchive(SevenZFile file) {
    this.file = file;
  }

  @Override
  public List<ArchiveEntry> getArchiveEntries() {
    List<ArchiveEntry> result = new ArrayList<>();

    for (org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry archive : file.getEntries()) {
      if (archive.getSize() != 0
          && !archive.isDirectory()
          && isSupportedFilename(archive.getName().toLowerCase())) {
        result.add(new SevenZArchiveEntry(file, archive));
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
    SevenZFile sevenZFile = new SevenZFile(store);

    // Check whether the archive is solid
    for (org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry entry : sevenZFile.getEntries()) {
      if (entry.getSize() != 0
          && !entry.isDirectory()
          && !entry.isFirstInFolder()) {
        throw new IOException("Solid archive is not supported");
      }
    }

    return new SevenZArchive(sevenZFile);
  }

  public static class SevenZArchiveEntry extends ArchiveEntry {

    private SevenZFile file;
    private org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry entry;

    private SevenZArchiveEntry(
        SevenZFile file,
        org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry entry
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
      file.setEntry(entry);
      return file.getInputStreamForCurrentEntry();
    }
  }
}
