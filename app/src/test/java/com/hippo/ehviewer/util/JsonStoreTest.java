/*
 * Copyright 2017 Hippo Seven
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

package com.hippo.ehviewer.util;

/*
 * Created by Hippo on 2/27/2017.
 */

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;
import com.hippo.ehviewer.annotation.Since;
import com.hippo.ehviewer.annotation.Until;
import com.hippo.yorozuya.HashCodeUtils;
import com.hippo.yorozuya.ObjectUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import okio.Okio;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JsonStoreTest {

  private Gson gson;
  private TemporaryFolder temp;

  @Before
  public void before() throws IOException {
    gson = new Gson();
    temp = new TemporaryFolder();
    temp.create();
  }

  @Test
  public void testPush() throws IOException {
    TestItemVersion1 item = new TestItemVersion1();
    item.id = 100;
    item.token = "321";

    File file = temp.newFile();
    JsonStore.push(gson, file, item, TestItemVersion1.class);

    assertEquals(
        "{\"version\":1,\"name\":\"JsonStoreTest:TestItem\",\"type\":0,\"value\":{\"id\":100,\"token\":\"321\"}}",
        Okio.buffer(Okio.source(file)).readUtf8()
    );
  }

  @Test
  public void testPushList() throws IOException {
    TestItemVersion1 item1 = new TestItemVersion1();
    item1.id = 100;
    item1.token = "321";

    TestItemVersion1 item2 = new TestItemVersion1();
    item2.id = 200;
    item2.token = "654";

    File file = temp.newFile();
    JsonStore.push(gson, file, new TestItemVersion1[]{item1, item2}, TestItemVersion1.class);

    assertEquals(
        "{\"version\":1,\"name\":\"JsonStoreTest:TestItem\",\"type\":1,\"value\":[{\"id\":100,\"token\":\"321\"},{\"id\":200,\"token\":\"654\"}]}",
        Okio.buffer(Okio.source(file)).readUtf8()
    );
  }

  @Test
  public void testFetch() throws IOException {
    String str = "{\"version\":1,\"name\":\"JsonStoreTest:TestItem\",\"type\":0,\"value\":{\"id\":100,\"token\":\"321\"}}";

    File file = temp.newFile();
    Okio.buffer(Okio.sink(file)).writeUtf8(str).close();

    TestItemVersion1 item = JsonStore.fetch(gson, file, TestItemVersion1.class);
    assertNotNull(item);
    assertEquals(100, item.id);
    assertEquals("321", item.token);
  }

  @Test
  public void testFetchList() throws IOException {
    String str = "{\"version\":1,\"name\":\"JsonStoreTest:TestItem\",\"type\":1,\"value\":[{\"id\":100,\"token\":\"321\"},{\"id\":200,\"token\":\"654\"}]}";

    File file = temp.newFile();
    Okio.buffer(Okio.sink(file)).writeUtf8(str).close();

    List<TestItemVersion1> items = JsonStore.fetchList(gson, file, TestItemVersion1.class);
    assertNotNull(items);
    assertEquals(2, items.size());
    assertEquals(100, items.get(0).id);
    assertEquals("321", items.get(0).token);
    assertEquals(200, items.get(1).id);
    assertEquals("654", items.get(1).token);
  }

  @Test
  public void testOnFetch() throws IOException {
    String str = "{\"version\":1,\"name\":\"JsonStoreTest:TestItem\",\"type\":0,\"value\":{\"id\":100,\"token\":\"321\"}}";

    File file = temp.newFile();
    Okio.buffer(Okio.sink(file)).writeUtf8(str).close();

    TestItemVersion2 item = JsonStore.fetch(gson, file, TestItemVersion2.class);
    assertNotNull(item);
    assertEquals(100, item.id);
    assertArrayEquals(new String[]{"321"}, item.tokens);
  }

  @Test
  public void testPushFetch() throws IOException {
    TestItemVersion1 item = new TestItemVersion1();
    item.id = 100;
    item.token = "321";

    File file = temp.newFile();
    JsonStore.push(gson, file, item, TestItemVersion1.class);

    TestItemVersion1 anotherItem = JsonStore.fetch(gson, file, TestItemVersion1.class);
    assertEquals(item, anotherItem);
  }

  @Test
  public void testPushFetchNull() throws IOException {
    TestItemVersion1 item = null;
    File file = temp.newFile();
    JsonStore.push(gson, file, item, TestItemVersion1.class);
    TestItemVersion1 anotherItem = JsonStore.fetch(gson, file, TestItemVersion1.class);
    assertEquals(item, anotherItem);
  }

  @Test
  public void testPushFetchList() throws IOException {
    TestItemVersion1 item1 = new TestItemVersion1();
    item1.id = 100;
    item1.token = "321";
    TestItemVersion1 item2 = new TestItemVersion1();
    item2.id = 200;
    item2.token = "654";
    TestItemVersion1[] items = new TestItemVersion1[]{item1, null, item2};

    File file = temp.newFile();
    JsonStore.push(gson, file, items, TestItemVersion1.class);

    List<TestItemVersion1> anotherItems = JsonStore.fetchList(gson, file, TestItemVersion1.class);
    assertArrayEquals(items, anotherItems.toArray(new TestItemVersion1[anotherItems.size()]));
  }

  @Test
  public void testPushFetchEmptyList() throws IOException {
    TestItemVersion1[] items = new TestItemVersion1[0];

    File file = temp.newFile();
    JsonStore.push(gson, file, items, TestItemVersion1.class);

    List<TestItemVersion1> anotherItems = JsonStore.fetchList(gson, file, TestItemVersion1.class);
    assertArrayEquals(items, anotherItems.toArray(new TestItemVersion1[anotherItems.size()]));
  }

  @Test
  public void testPushFetchNullList() throws IOException {
    TestItemVersion1[] items = null;

    File file = temp.newFile();
    JsonStore.push(gson, file, items, TestItemVersion1.class);

    List<TestItemVersion1> anotherItems = JsonStore.fetchList(gson, file, TestItemVersion1.class);
    assertEquals(0, anotherItems.size());
  }

  @JsonStore.Info(
      version = 1,
      name = "JsonStoreTest:TestItem"
  )
  public static class TestItemVersion1 implements JsonStore.Item {

    public long id;
    public String token;

    @Override
    public boolean onFetch(int version) {
      return true;
    }

    @Override
    public int hashCode() {
      return HashCodeUtils.hashCode(id, token);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof TestItemVersion1)) {
        return false;
      }

      TestItemVersion1 item = (TestItemVersion1) obj;
      return id == item.id && ObjectUtils.equals(token, item.token);
    }
  }

  @JsonStore.Info(
      version = 2,
      name = "JsonStoreTest:TestItem"
  )
  public static class TestItemVersion2 implements JsonStore.Item {

    public long id;

    @Deprecated
    @Until(1)
    public String token;

    @Since(2)
    public String[] tokens;

    @SuppressWarnings("deprecation")
    @Override
    public boolean onFetch(int version) {
      switch (version) {
        case 1:
          tokens = new String[1];
          tokens[0] = token;
          token = null;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return HashCodeUtils.hashCode(id, Arrays.hashCode(tokens));
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof TestItemVersion2)) {
        return false;
      }

      TestItemVersion2 item = (TestItemVersion2) obj;
      return id == item.id && Arrays.equals(tokens, item.tokens);
    }
  }
}
