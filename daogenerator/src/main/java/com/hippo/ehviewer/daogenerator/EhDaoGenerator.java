/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.daogenerator;

import java.io.File;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class EhDaoGenerator {

    private static final String PACKAGE = "com.hippo.ehviewer.dao";
    private static final String OUT_DIR = "../app/src/main/java-gen";
    private static final String DELETE_DIR = "../app/src/main/java-gen/com/hippo/ehviewer/dao";

    private static final int VERSION = 1;

    public static void generate() throws Exception {
        Utilities.deleteContents(new File(DELETE_DIR));
        File outDir = new File(OUT_DIR);
        outDir.delete();
        outDir.mkdirs();

        Schema schema = new Schema(VERSION, PACKAGE);
        addGalleryInfo(schema);
        addDownloadInfo(schema);
        addDownloadLabel(schema);
        addHistoryInfo(schema);
        addQuickSearch(schema);
        addLocalFavorite(schema);
        new DaoGenerator().generateAll(schema, OUT_DIR);
    }

    private static void addGalleryInfo(Schema schema) {
        Entity entity = schema.addEntity("GalleryInfoRaw");
        entity.setTableName("GALLERY_INFO");
        entity.setClassNameDao("GalleryInfoDao");
        entity.addLongProperty("gid").primaryKey();
        entity.addStringProperty("token");
        entity.addStringProperty("title");
        entity.addStringProperty("posted");
        entity.addIntProperty("category");
        entity.addStringProperty("thumb");
        entity.addStringProperty("uploader");
        entity.addFloatProperty("rating");
        entity.addIntProperty("reference");
    }

    private static void addDownloadInfo(Schema schema) {
        Entity entity = schema.addEntity("DownloadInfoRaw");
        entity.setTableName("DOWNLOAD_INFO");
        entity.setClassNameDao("DownloadInfoDao");
        entity.addLongProperty("gid").primaryKey();
        entity.addIntProperty("state");
        entity.addIntProperty("legacy");
        entity.addLongProperty("date");
        entity.addStringProperty("label");
    }

    private static void addDownloadLabel(Schema schema) {
        Entity entity = schema.addEntity("DownloadLabelRaw");
        entity.setTableName("DOWNLOAD_LABEL");
        entity.setClassNameDao("DownloadLabelDao");
        entity.addIdProperty();
        entity.addStringProperty("label");
        entity.addLongProperty("time");
    }

    private static void addHistoryInfo(Schema schema) {
        Entity entity = schema.addEntity("HistoryInfoRaw");
        entity.setTableName("HISTORY_INFO");
        entity.setClassNameDao("HistoryInfoDao");
        entity.addLongProperty("gid").primaryKey();
        entity.addIntProperty("mode");
        entity.addLongProperty("date");
    }

    private static void addQuickSearch(Schema schema) {
        Entity entity = schema.addEntity("QuickSearchRaw");
        entity.setTableName("QUICK_SEARCH");
        entity.setClassNameDao("QuickSearchDao");
        entity.addIdProperty();
        entity.addStringProperty("name");
        entity.addIntProperty("mode");
        entity.addIntProperty("category");
        entity.addStringProperty("keyword");
        entity.addIntProperty("advanceSearch");
        entity.addIntProperty("minRating");
        entity.addLongProperty("date");
    }

    private static void addLocalFavorite(Schema schema) {
        Entity entity = schema.addEntity("LocalFavoriteRaw");
        entity.setTableName("LOCAL_FAVORITE");
        entity.setClassNameDao("LocalFavoriteDao");
        entity.addLongProperty("gid").primaryKey();
        entity.addLongProperty("date");
    }
}
