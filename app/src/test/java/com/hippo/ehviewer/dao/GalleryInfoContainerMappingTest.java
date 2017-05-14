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

package com.hippo.ehviewer.dao;

/*
 * Created by Hippo on 3/4/2017.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.gson.Gson;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryInfoContainer;
import com.hippo.ehviewer.database.MSQLite;
import com.hippo.ehviewer.database.MSQLiteOpenHelper;
import com.hippo.ehviewer.util.DBUtils;
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite;
import com.pushtorefresh.storio.sqlite.queries.Query;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@Config(
    constants = BuildConfig.class,
    sdk = Build.VERSION_CODES.N_MR1
)
@RunWith(RobolectricTestRunner.class)
public class GalleryInfoContainerMappingTest {

  private StorIOSQLite sql;

  @Before
  public void before() throws IOException {
    ShadowLog.stream = System.out;

    TemporaryFolder temp = new TemporaryFolder();
    temp.create();
    File file = temp.newFile();

    EhvApp app = (EhvApp) RuntimeEnvironment.application;
    sql = DefaultStorIOSQLite.builder()
        .sqliteOpenHelper(new TestDBOpenHelper(app, file.getName(), 1))
        .addTypeMapping(TestItem.class, new TestItemMapping(app.getGson()))
        .build();
  }

  @Test
  public void testPutGet() {
    GalleryInfo info = new GalleryInfo();
    info.gid = 100;
    info.token = "abc";
    info.title = "title";
    info.titleJpn = "title_jpn";
    info.cover = "cover";
    info.coverUrl = "cover_url";
    info.coverRatio = 1.0f;
    info.category = EhUtils.CATEGORY_ARTIST_CG;
    info.date = System.currentTimeMillis();
    info.uploader = "uploader";
    info.rating = 3.2f;
    info.language = EhUtils.LANG_DE;
    info.favouriteSlot = 1;
    info.invalid = false;
    info.archiverKey = "abc";
    info.pages = 12;
    info.size = 121212;
    info.torrentCount = 3;
    info.tags = new HashMap<>();
    info.tags.put("group1", Arrays.asList("name1", "name2"));
    info.tags.put("group2", Arrays.asList("name1", "name2", "name3"));

    TestItem item = new TestItem();
    item.info = info;
    item.note = "note";

    sql.put()
        .object(item)
        .prepare()
        .executeAsBlocking();

    TestItem ti = sql.get()
        .object(TestItem.class)
        .withQuery(Query.builder()
            .table(TestItemMapping.TABLE_NAME)
            .where(GalleryInfoContainerMapping.COLUMN_GID + " = ?")
            .whereArgs(info.gid)
            .build()
        )
        .prepare()
        .executeAsBlocking();

    assertNotNull(ti);
    assertNotNull(ti.info);
    assertEquals(info.gid, ti.info.gid);
    assertEquals(info.token, ti.info.token);
    assertEquals(info.title, ti.info.title);
    assertEquals(info.titleJpn, ti.info.titleJpn);
    assertEquals(info.cover, ti.info.cover);
    assertEquals(info.coverUrl, ti.info.coverUrl);
    assertEquals(info.coverRatio, ti.info.coverRatio, 0.0f);
    assertEquals(info.category, ti.info.category);
    assertEquals(info.date, ti.info.date);
    assertEquals(info.uploader, ti.info.uploader);
    assertEquals(info.rating, ti.info.rating, 0.0f);
    assertEquals(info.language, ti.info.language);
    assertEquals(info.favouriteSlot, ti.info.favouriteSlot);
    assertEquals(info.invalid, ti.info.invalid);
    assertEquals(null, ti.info.archiverKey);
    assertEquals(info.pages, ti.info.pages);
    assertEquals(info.size, ti.info.size);
    assertEquals(info.torrentCount, ti.info.torrentCount);
    assertEquals(info.tags, ti.info.tags);
    assertEquals(item.note, ti.note);
  }

  @Test
  public void testPutMerge() {
    GalleryInfo info1 = new GalleryInfo();
    info1.gid = 100;
    info1.token = "abc";
    info1.title = "title";

    TestItem item1 = new TestItem();
    item1.info = info1;
    item1.note = "note";

    sql.put()
        .object(item1)
        .prepare()
        .executeAsBlocking();

    GalleryInfo info2 = new GalleryInfo();
    info2.gid = 100;
    info2.token = "abc";
    info2.title = "title2";

    TestItem item2 = new TestItem();
    item2.info = info2;
    item2.note = "note2";

    sql.put()
        .object(item2)
        .prepare()
        .executeAsBlocking();

    TestItem ti = sql.get()
        .object(TestItem.class)
        .withQuery(Query.builder()
            .table(TestItemMapping.TABLE_NAME)
            .where(GalleryInfoContainerMapping.COLUMN_GID + " = ?")
            .whereArgs(info2.gid)
            .build()
        )
        .prepare()
        .executeAsBlocking();

    assertNotNull(ti);
    assertNotNull(ti.info);
    assertEquals(info2.title, ti.info.title);
    assertEquals(item2.note, ti.note);
  }

  private static void assertReference(StorIOSQLite sql, long gid, int reference) {
    Cursor cursor = sql.lowLevel()
        .query(Query.builder()
            .table(GalleryInfoContainerMapping.TABLE_NAME)
            .where(GalleryInfoContainerMapping.COLUMN_GID + " = ?")
            .whereArgs(gid)
            .build()
        );

    assertTrue(cursor.moveToNext());

    assertEquals(
        reference,
        DBUtils.getInt(cursor, GalleryInfoContainerMapping.COLUMN_REFERENCE, 0)
    );

    cursor.close();
  }

  @Test
  public void testReference() {
    GalleryInfo info1 = new GalleryInfo();
    info1.gid = 100;
    info1.token = "abc";
    info1.title = "title";

    TestItem item1 = new TestItem();
    item1.info = info1;
    item1.note = "note";

    sql.put()
        .object(item1)
        .prepare()
        .executeAsBlocking();

    assertReference(sql, info1.gid, 1);

    GalleryInfo info2 = new GalleryInfo();
    info2.gid = 100;
    info2.token = "abc";
    info2.title = "title2";

    TestItem item2 = new TestItem();
    item2.info = info2;
    item2.note = "note2";

    sql.put()
        .object(item2)
        .prepare()
        .executeAsBlocking();

    assertReference(sql, info1.gid, 1);

    sql.delete()
        .object(item1)
        .prepare()
        .executeAsBlocking();

    try {
      assertReference(sql, info1.gid, 0);
      fail();
    } catch (AssertionError e) {
    }

    TestItem ti = sql.get()
        .object(TestItem.class)
        .withQuery(Query.builder()
            .table(TestItemMapping.TABLE_NAME)
            .where(GalleryInfoContainerMapping.COLUMN_GID + " = ?")
            .whereArgs(info1.gid)
            .build()
        )
        .prepare()
        .executeAsBlocking();

    assertNull(ti);
  }

  private static class TestItem implements GalleryInfoContainer {

    public GalleryInfo info;
    public String note;

    @Nullable
    @Override
    public GalleryInfo getGalleryInfo() {
      return info;
    }

    @Override
    public void setGalleryInfo(GalleryInfo info) {
      this.info = info;
    }
  }

  private static class TestItemMapping extends SQLiteTypeMapping<TestItem> {

    private static final String TABLE_NAME = "test";
    private static final String COLUMN_NOTE = "note";

    protected TestItemMapping(Gson gson) {
      super(
          new TestItemPutResolver(gson, TABLE_NAME),
          new TestItemGetResolver(gson),
          new TestItemDeleteResolver(gson, TABLE_NAME)
      );
    }

    public static void version1(MSQLite ms) {
      ms.createTable(TABLE_NAME, GalleryInfoContainerMapping.COLUMN_GID, long.class)
          .insertColumn(TABLE_NAME, COLUMN_NOTE, String.class);
    }

    private static class TestItemPutResolver
        extends GalleryInfoContainerMapping.UniquePutResolver<TestItem> {

      protected TestItemPutResolver(@NonNull Gson gson, @NonNull String tableName) {
        super(gson, tableName);
      }

      @NonNull
      @Override
      protected ContentValues mapToContentValues(@NonNull TestItem item) {
        ContentValues contentValues = new ContentValues(2);
        contentValues.put(COLUMN_NOTE, item.note);
        return contentValues;
      }
    }

    private static class TestItemGetResolver
        extends GalleryInfoContainerMapping.GetResolver<TestItem> {

      protected TestItemGetResolver(@NonNull Gson gson) {
        super(gson);
      }

      @NonNull
      @Override
      protected TestItem mapFromCursor2(@NonNull Cursor cursor) {
        TestItem item = new TestItem();
        item.note = DBUtils.getString(cursor, COLUMN_NOTE, null);
        return item;
      }
    }

    private static class TestItemDeleteResolver
        extends GalleryInfoContainerMapping.UniqueDeleteResolver<TestItem> {

      protected TestItemDeleteResolver(@NonNull Gson gson, @NonNull String tableName) {
        super(gson, tableName);
      }
    }
  }

  private static class TestDBOpenHelper extends MSQLiteOpenHelper {

    public TestDBOpenHelper(Context context, String name, int version) {
      super(context, name, version);
    }

    @Override
    public void onInit(MSQLite ms) {
      ms.version(1);
      GalleryInfoContainerMapping.galleryInfoTableVersion1(ms);
      TestItemMapping.version1(ms);
    }
  }
}
