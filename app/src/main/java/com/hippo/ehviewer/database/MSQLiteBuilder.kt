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

package com.hippo.ehviewer.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.reflect.KClass

/*
 * Created by Hippo on 6/17/2017.
 */

class MSQLiteBuilder {

  companion object {
    const val COLUMN_ID = "_id"

    val JAVA_TYPE_TO_SQLITE_TYPE = mapOf(
        Boolean::class to "INTEGER NOT NULL DEFAULT 0",
        Byte::class to "INTEGER NOT NULL DEFAULT 0",
        Short::class to "INTEGER NOT NULL DEFAULT 0",
        Int::class to "INTEGER NOT NULL DEFAULT 0",
        Long::class to "INTEGER NOT NULL DEFAULT 0",
        Float::class to "REAL NOT NULL DEFAULT 0",
        Double::class to "REAL NOT NULL DEFAULT 0",
        String::class to "TEXT"
    )

    internal fun javaTypeToSQLiteType(clazz: KClass<*>) = JAVA_TYPE_TO_SQLITE_TYPE[clazz] ?: error("Unknown type: $clazz")
  }

  private var version: Int = 0
  private var commands: MutableList<Command>? = null
  private val commandsMap = mutableMapOf<Int, List<Command>>()

  /**
   * Bump database version.
   */
  fun version(version: Int): MSQLiteBuilder {
    check(this.version < version, { "New version must be bigger than current version. " +
          "current version: ${this.version}, new version: $version." })
    this.version = version
    this.commands = mutableListOf<Command>().also { commandsMap.put(version, it) }
    return this
  }

  /**
   * Creates a table.
   */
  fun createTable(table: String, column: String = COLUMN_ID, clazz: KClass<*> = Int::class) =
      command("CREATE TABLE $table ($column ${javaTypeToSQLiteType(clazz)} PRIMARY KEY);")

  /**
   * Drops a table.
   */
  fun dropTable(table: String) = command("DROP TABLE $table;")

  /**
   * Inserts a column to the table.
   */
  fun insertColumn(table: String, column: String, clazz: KClass<*>) =
      command("ALTER TABLE $table ADD COLUMN $column ${javaTypeToSQLiteType(clazz)};")

  private fun checkState(): MutableList<Command> {
    check(version != 0, { "Call version() first!" })
    return commands ?: error("Call version() first!")
  }

  /**
   * Add a statement command.
   */
  fun command(command: String) = command(StatementCommand(command))

  /**
   * Add a command.
   */
  fun command(command: Command): MSQLiteBuilder = this.also { checkState().add(command) }

  /**
   * Build a SQLiteOpenHelper from it.
   */
  fun build(context: Context, name: String, version: Int): SQLiteOpenHelper =
      MSQLiteOpenHelper(context, name, version, this)

  internal fun getCommands(oldVersion: Int, newVersion: Int): List<Command> {
    val result = mutableListOf<Command>()
    for (i in oldVersion + 1..newVersion) {
      commandsMap[i]?.let { result.addAll(it) }
    }
    return result
  }

  /**
   * One of commands to create or update `SQLiteDatabase`.
   */
  abstract class Command {
    abstract fun run(db: SQLiteDatabase)
  }

  /**
   * A [Command] to execute a statement.
   */
  class StatementCommand(private val statement: String) : Command() {
    override fun run(db: SQLiteDatabase) {
      db.execSQL(statement)
    }
  }
}
