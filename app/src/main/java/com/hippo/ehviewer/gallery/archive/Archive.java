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

import com.hippo.ehviewer.gallery.GalleryProvider2;
import com.hippo.unifile.UniRandomAccessFile;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public abstract class Archive implements Closeable {

  private static final byte[] FILE_SIGNATURE_SEVEN_Z = { 0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C };
  private static final byte[] FILE_SIGNATURE_ZIP = { 0x50, 0x4B };
  private static final byte[] FILE_SIGNATURE_RAR = { 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00 };

  public abstract List<ArchiveEntry> getArchiveEntries();

  public static Archive create(UniRandomAccessFile file) throws IOException {
    byte[] bytes = new byte[8];
    readFully(file, bytes, 8);
    file.seek(0);

    if (equals(bytes, FILE_SIGNATURE_SEVEN_Z, FILE_SIGNATURE_SEVEN_Z.length)) {
      return SevenZArchive.create(file);
    } else if (equals(bytes, FILE_SIGNATURE_ZIP, FILE_SIGNATURE_ZIP.length)) {
      return ZipArchive.create(file);
    } else if (equals(bytes, FILE_SIGNATURE_RAR, FILE_SIGNATURE_RAR.length)) {
      return RarArchive.create(file);
    }

    return null;
  }

  static boolean isSupportedFilename(String name) {
    for (String extension : GalleryProvider2.SUPPORT_IMAGE_EXTENSIONS) {
      if (name.endsWith(extension)) {
        return true;
      }
    }
    return false;
  }

  private static int readFully(UniRandomAccessFile file, byte[] bytes, int count) throws IOException {
    if (count <= 0) {
      return 0;
    }

    int read = 0;
    do {
      int l = file.read(bytes, read, count - read);
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

  private static boolean equals(byte[] b1, byte[] b2, int size) {
    if (b1.length < size || b2.length < size) {
      return false;
    }

    for (int i = 0; i < size; i++) {
      if (b1[i] != b2[i]) {
        return false;
      }
    }

    return true;
  }
}
