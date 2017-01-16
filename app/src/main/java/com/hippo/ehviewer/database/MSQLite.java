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

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of SQLite database create and update commands.
 */
public class MSQLite {

  private static Map<Type, String> JAVA_TYPE_TO_SQLITE_TYPE = new HashMap<>();

  static {
    JAVA_TYPE_TO_SQLITE_TYPE.put(boolean.class, "INTEGER NOT NULL");
    JAVA_TYPE_TO_SQLITE_TYPE.put(byte.class, "INTEGER NOT NULL");
    JAVA_TYPE_TO_SQLITE_TYPE.put(short.class, "INTEGER NOT NULL");
    JAVA_TYPE_TO_SQLITE_TYPE.put(int.class, "INTEGER NOT NULL");
    JAVA_TYPE_TO_SQLITE_TYPE.put(long.class, "INTEGER NOT NULL");
    JAVA_TYPE_TO_SQLITE_TYPE.put(float.class, "REAL NOT NULL");
    JAVA_TYPE_TO_SQLITE_TYPE.put(double.class, "REAL NOT NULL");
    JAVA_TYPE_TO_SQLITE_TYPE.put(String.class, "TEXT");
  }

  @SuppressLint("UseSparseArrays")
  private Map<Integer, List<Command>> commandsMap = new HashMap<>();

  private int version;
  private List<Command> commands;

  // Only MSQLiteOpenHelper should call it
  MSQLite() {}

  /**
   *
   */
  public MSQLite version(int version) {
    if (this.version >= version) {
      throw new IllegalArgumentException("New version must be bigger than current version. "
          + "current version: " + this.version + ", new version: " + version + ".");
    }
    this.version = version;
    this.commands = new ArrayList<>();
    commandsMap.put(this.version, this.commands);
    return this;
  }

  private void checkState() {
    if (version == 0) throw new IllegalStateException("Call version() first!");
    if (commands == null) throw new IllegalStateException("Call version() first!");
  }

  static String javaTypeToSQLiteType(Type type) {
    String dbType = JAVA_TYPE_TO_SQLITE_TYPE.get(type);
    if (dbType == null) throw new IllegalArgumentException("Unknown type. "
        + "Type must be one of " + JAVA_TYPE_TO_SQLITE_TYPE.keySet().toString());
    return dbType;
  }

  /**
   * Creates a table.
   */
  public MSQLite createTable(String table) {
    return command(new StatementCommand(
        "CREATE TABLE " + table + " (_id INTEGER PRIMARY KEY NOT NULL);"));
  }

  /**
   * Drops a table.
   */
  public MSQLite dropTable(String table) {
    return command(new StatementCommand(
        "DROP TABLE " + table + ";"));
  }

  /**
   * Inserts a column to the table.
   */
  public MSQLite insertColumn(String table, String column, Type type) {
    return command(new StatementCommand(
        "ALTER TABLE " + table + " ADD COLUMN " + column + " " + javaTypeToSQLiteType(type) + ";"));
  }

  /**
   * Add a command.
   */
  public MSQLite command(@NonNull Command command) {
    checkState();
    commands.add(command);
    return this;
  }

  @NonNull
  List<Command> getCommands(int oldVersion, int newVersion) {
    List<Command> result = new ArrayList<>();
    for (int i = oldVersion + 1; i <= newVersion; i++) {
      List<Command> commands = commandsMap.get(i);
      if (commands != null) {
        result.addAll(commands);
      }
    }
    return result;
  }

  /**
   * One of commands to create or update {@code SQLiteDatabase}.
   */
  public static abstract class Command {
    public abstract void run(SQLiteDatabase db);
  }

  /**
   * A {@link Command} to execute a statement.
   */
  public static class StatementCommand extends Command {

    private String statement;

    public StatementCommand(@NonNull String statement) {
      this.statement = statement;
    }

    @Override
    public void run(SQLiteDatabase db) {
      db.execSQL(statement);
    }
  }
}
