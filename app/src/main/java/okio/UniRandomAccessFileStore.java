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

package okio;

import static okio.Util.checkOffsetAndCount;

import com.hippo.unifile.UniRandomAccessFile;
import java.io.IOException;

public class UniRandomAccessFileStore implements Store {

  private UniRandomAccessFile file;
  private Timeout timeout;

  public UniRandomAccessFileStore(UniRandomAccessFile file) {
    this(file, new Timeout());
  }

  public UniRandomAccessFileStore(UniRandomAccessFile file, Timeout timeout) {
    this.file = file;
    this.timeout = timeout;
  }

  @Override
  public void seek(long position) throws IOException {
    file.seek(position);
  }

  @Override
  public long tell() throws IOException {
    return file.getFilePointer();
  }

  @Override
  public long size() throws IOException {
    return file.length();
  }

  @Override
  public long read(Buffer sink, long byteCount) throws IOException {
    if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
    if (byteCount == 0) return 0;
    timeout.throwIfReached();
    Segment tail = sink.writableSegment(1);
    int maxToCopy = (int) Math.min(byteCount, Segment.SIZE - tail.limit);
    int bytesRead = file.read(tail.data, tail.limit, maxToCopy);
    if (bytesRead == -1) return -1;
    tail.limit += bytesRead;
    sink.size += bytesRead;
    return bytesRead;
  }

  @Override
  public void write(Buffer source, long byteCount) throws IOException {
    checkOffsetAndCount(source.size, 0, byteCount);
    while (byteCount > 0) {
      timeout.throwIfReached();
      Segment head = source.head;
      int toCopy = (int) Math.min(byteCount, head.limit - head.pos);
      file.write(head.data, head.pos, toCopy);

      head.pos += toCopy;
      byteCount -= toCopy;
      source.size -= toCopy;

      if (head.pos == head.limit) {
        source.head = head.pop();
        SegmentPool.recycle(head);
      }
    }
  }

  @Override
  public void flush() { }

  @Override
  public Timeout timeout() {
    return timeout;
  }

  @Override
  public void close() throws IOException {
    file.close();
  }
}
