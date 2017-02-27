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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.ObjectUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@code JsonStore} can push and fetch data to or from file.
 */
public final class JsonStore {
  private JsonStore() {}

  private static final int TYPE_UNKNOWN = -1;
  private static final int TYPE_OBJECT = 0;
  private static final int TYPE_LIST = 1;

  private static final String KEY_VERSION = "version";
  private static final String KEY_NAME = "name";
  private static final String KEY_TYPE = "type";
  private static final String KEY_VALUE = "value";

  private static InfoInternal getInfo(Class<?> clazz) {
    InfoInternal result = new InfoInternal();
    Info info = clazz.getAnnotation(Info.class);
    if (info != null) {
      result.version = info.version();
      result.name = info.name();
    } else {
      result.version = 0;
      result.name = clazz.getName();
    }
    return result;
  }

  /**
   * Saves the {@code t} to the {@code file} in json format converted by the {@code gson}.
   *
   * @see #fetch(Gson, File, Class)
   */
  public static <T> void push(@NonNull Gson gson, @NonNull File file,
      @Nullable T t, @NonNull Class<T> clazz) throws IOException {

    InfoInternal info = getInfo(clazz);

    JsonWriter writer = null;
    try {
      writer = gson.newJsonWriter(new BufferedWriter(new FileWriter(file)));
      writer.beginObject();
      writer.name(KEY_VERSION).value(info.version);
      writer.name(KEY_NAME).value(info.name);
      writer.name(KEY_TYPE).value(TYPE_OBJECT);
      writer.name(KEY_VALUE);
      gson.toJson(gson.toJsonTree(t), writer);
      writer.endObject();
      writer.flush();
    } catch (JsonParseException e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  /**
   * Saves the {@code array} to the {@code file} in json format converted by the {@code gson}.
   *
   * @see #fetchList(Gson, File, Class)
   */
  public static <T> void push(@NonNull Gson gson, @NonNull File file,
      @Nullable T[] array, @NonNull Class<T> clazz) throws IOException {
    push(gson, file, array != null ? Arrays.asList(array) : Collections.emptyList(), clazz);
  }

  /**
   * Saves the {@code iterator} to the {@code file} in json format by the {@code gson}.
   *
   * @see #fetchList(Gson, File, Class)
   */
  public static <T> void push(@NonNull Gson gson, @NonNull File file,
      @Nullable Iterable<T> iterator, @NonNull Class<T> clazz) throws IOException {
    // Avoid null
    if (iterator == null) {
      iterator = Collections.emptyList();
    }

    InfoInternal info = getInfo(clazz);

    JsonWriter writer = null;
    try {
      writer = gson.newJsonWriter(new BufferedWriter(new FileWriter(file)));
      writer.beginObject();
      writer.name(KEY_VERSION).value(info.version);
      writer.name(KEY_NAME).value(info.name);
      writer.name(KEY_TYPE).value(TYPE_LIST);
      writer.name(KEY_VALUE);
      writer.beginArray();
      for (T t: iterator) {
        gson.toJson(gson.toJsonTree(t), writer);
      }
      writer.endArray();
      writer.endObject();
      writer.flush();
    } catch (JsonParseException e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  @NonNull
  private static StorageItem parseStorageItem(Gson gson, File file)
      throws IOException {
    StorageItem item;
    JsonReader reader = null;
    try {
      reader = gson.newJsonReader(new BufferedReader(new FileReader(file)));
      item = gson.fromJson(reader, StorageItem.class);
    } catch (JsonParseException e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
    if (item == null || item.name == null) {
      throw new IOException("Can't parse StorageItem");
    }
    return item;
  }

  /**
   * Parses a object from the {@code file} in json format by the {@code gson}.
   *
   * @see #push(Gson, File, Object, Class)
   */
  @Nullable
  public static <T> T fetch(@NonNull Gson gson, @NonNull File file, @NonNull Class<T> clazz)
      throws IOException {
    StorageItem item = parseStorageItem(gson, file);
    InfoInternal info = getInfo(clazz);

    // Check name
    if (!ObjectUtils.equals(item.name, info.name)) {
      throw new IOException("The name of the object in the file is: " + item.name +
          ", but request object name is: " + info.name);
    }

    // Check type
    if (item.type != TYPE_OBJECT) {
      throw new IOException("The type must be: " + TYPE_OBJECT + ", but it's: " + item.type);
    }

    try {
      T t = gson.fromJson(item.value, clazz);
      if (t instanceof Item) {
        ((Item) t).onFetch(item.version);
      }
      return t;
    } catch (JsonParseException e) {
      throw new IOException(e);
    }
  }

  /**
   * Parses a object list from the {@code file} in json format by the {@code gson}.
   *
   * @see #push(Gson, File, Object[], Class)
   * @see #push(Gson, File, Iterable, Class)
   */
  @NonNull
  public static <T> List<T> fetchList(@NonNull Gson gson, @NonNull File file,
      @NonNull Class<T> clazz) throws IOException {
    StorageItem item = parseStorageItem(gson, file);
    InfoInternal info = getInfo(clazz);

    // Check name
    if (!ObjectUtils.equals(item.name, info.name)) {
      throw new IOException("The name of the object in the file is: " + item.name +
          ", but request object name is: " + info.name);
    }

    // Check type
    if (item.type != TYPE_LIST) {
      throw new IOException("The type must be: " + TYPE_LIST + ", but it's: " + item.type);
    }

    try {
      List<T> list = new ArrayList<>();
      if (item.value instanceof JsonArray) {
        for (JsonElement je: (JsonArray) item.value) {
          T t = gson.fromJson(je, clazz);
          if (t instanceof Item) {
            ((Item) t).onFetch(item.version);
          }
          list.add(t);
        }
      }
      return list;
    } catch (JsonParseException e) {
      throw new IOException(e);
    }
  }

  private final static class StorageItem {

    @SerializedName(KEY_VERSION)
    public int version;

    @SerializedName(KEY_NAME)
    public String name;

    @SerializedName(KEY_TYPE)
    public int type = TYPE_UNKNOWN;

    @SerializedName(KEY_VALUE)
    public JsonElement value;
  }

  /**
   * The item to store in {@code JsonStore}.
   */
  public interface Item {
    /**
     * Converts the item from the {@code version} to the last version.
     */
    void onFetch(int version);
  }

  /**
   * Describes item.
   */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Info {

    /**
     * Returns the last version.
     * <b>
     * The last version must be larger than previous version.
     */
    int version();

    /**
     * Returns an unique name.
     */
    String name();
  }

  private static class InfoInternal {
    public int version;
    public String name;
  }
}
