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

import com.github.junrar.Volume;
import com.github.junrar.VolumeManager;
import com.github.junrar.exception.RarException;
import com.github.junrar.io.IReadOnlyAccess;
import com.github.junrar.rarfile.FileHeader;
import com.hippo.unifile.UniRandomAccessFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RarArchive extends Archive {

  private com.github.junrar.Archive file;

  private RarArchive(com.github.junrar.Archive file) {
    this.file = file;
  }

  @Override
  public List<ArchiveEntry> getArchiveEntries() {
    List<ArchiveEntry> result = new ArrayList<>();

    for (FileHeader fileHeader : file.getFileHeaders()) {
      if (isSupportedFilename(fileHeader.getFileNameString().toLowerCase())) {
        result.add(new RarArchiveEntry(file, fileHeader));
      }
    }

    return result;
  }

  @Override
  public void close() throws IOException {
    file.close();
  }

  public static Archive create(UniRandomAccessFile file) throws IOException {
    VolumeManager volumeManager = new UniRandomAccessFileVolumeManager(file);
    com.github.junrar.Archive archive;
    try {
      archive = new com.github.junrar.Archive(volumeManager);
      if (archive.getMainHeader().isSolid()) {
        throw new IOException("Solid archive is not supported");
      }
    } catch (RarException e) {
      throw new IOException("Invalid rar archive", e);
    }
    return new RarArchive(archive);
  }

  public static class RarArchiveEntry extends ArchiveEntry {

    private com.github.junrar.Archive file;
    private FileHeader header;

    private RarArchiveEntry(com.github.junrar.Archive file, FileHeader header) {
      this.file = file;
      this.header = header;
    }

    @Override
    public String getName() {
      return header.getFileNameString();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      try {
        return file.getInputStream(header);
      } catch (RarException e) {
        throw new IOException("Can't getInputStream from rar archive", e);
      }
    }
  }

  private static class UniRandomAccessFileVolumeManager implements VolumeManager {

    private UniRandomAccessFile file;

    private UniRandomAccessFileVolumeManager(UniRandomAccessFile file) {
      this.file = file;
    }

    @Override
    public Volume nextArchive(com.github.junrar.Archive archive, Volume lastVolume) {
      if (lastVolume == null) {
        return new UniRandomAccessFileVolume(archive, file);
      } else {
        return null;
      }
    }
  }

  private static class UniRandomAccessFileVolume implements Volume {

    private com.github.junrar.Archive archive;
    private UniRandomAccessFile file;

    private UniRandomAccessFileVolume(com.github.junrar.Archive archive, UniRandomAccessFile file) {
      this.archive = archive;
      this.file = file;
    }

    @Override
    public IReadOnlyAccess getReadOnlyAccess() {
      return new UniReadOnlyAccessFile(file);
    }

    @Override
    public long getLength() throws IOException {
      return file.length();
    }

    @Override
    public com.github.junrar.Archive getArchive() {
      return archive;
    }
  }

  private static class UniReadOnlyAccessFile implements IReadOnlyAccess {

    private UniRandomAccessFile file;

    private UniReadOnlyAccessFile(UniRandomAccessFile file) {
      this.file = file;
    }

    @Override
    public long getPosition() throws IOException {
      return file.getFilePointer();
    }

    @Override
    public void setPosition(long pos) throws IOException {
      file.seek(pos);
    }

    @Override
    public int read() throws IOException {
      byte[] b = new byte[1];
      if (file.read(b) == -1) {
        return -1;
      } else {
        return b[0];
      }
    }

    @Override
    public int read(byte[] buffer, int off, int count) throws IOException {
      return file.read(buffer, off, count);
    }

    @Override
    public int readFully(byte[] buffer, int count) throws IOException {
      if (count <= 0) {
        return 0;
      }

      int read = 0;
      do {
        int l = read(buffer, read, count - read);
        if (l < 0)
          break;
        read += l;
      } while (read < count);

      if (read == 0) {
        return -1;
      } else {
        return read;
      }
    }

    @Override
    public void close() throws IOException {
      file.close();
    }
  }
}
