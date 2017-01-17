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

package com.hippo.ehviewer.database;

/*
 * Created by Hippo on 1/15/2017.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class MSQLiteTest {

  @Test
  public void javaTypeToSQLiteType() {
    assertEquals("INTEGER NOT NULL DEFAULT 0", MSQLite.javaTypeToSQLiteType(boolean.class));
    assertEquals("INTEGER NOT NULL DEFAULT 0", MSQLite.javaTypeToSQLiteType(byte.class));
    assertEquals("INTEGER NOT NULL DEFAULT 0", MSQLite.javaTypeToSQLiteType(short.class));
    assertEquals("INTEGER NOT NULL DEFAULT 0", MSQLite.javaTypeToSQLiteType(int.class));
    assertEquals("INTEGER NOT NULL DEFAULT 0", MSQLite.javaTypeToSQLiteType(long.class));

    assertEquals("REAL NOT NULL DEFAULT 0", MSQLite.javaTypeToSQLiteType(float.class));
    assertEquals("REAL NOT NULL DEFAULT 0", MSQLite.javaTypeToSQLiteType(double.class));

    assertEquals("TEXT", MSQLite.javaTypeToSQLiteType(String.class));

    try {
      assertEquals("MSQLiteTest NOT NULL", MSQLite.javaTypeToSQLiteType(this.getClass()));
      fail();
    } catch (IllegalArgumentException e) {
      // Ignore
    }
  }
}
