/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.ehviewer;

import java.io.File;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class EhDaoGenerator {

    private static final String PACKAGE = "com.hippo.ehviewer.dao";
    private static final String OUT_DIR = "./app/src/main/java-gen";

    public static void main(String args[]) throws Exception {
        File outDir = new File(OUT_DIR);
        Utilities.deleteContents(outDir);
        outDir.delete();
        outDir.mkdirs();

        Schema schema = new Schema(1, PACKAGE);
        addGalleryBase(schema);
        addDownloadInfo(schema);
        addSpider(schema);
        new DaoGenerator().generateAll(schema, OUT_DIR);
    }

    public static void addGalleryBase(Schema schema) {
        Entity galleryBase = schema.addEntity("GalleryBaseObj");
        galleryBase.setTableName("GALLERY_BASE");
        galleryBase.addLongProperty("gid").primaryKey();
        galleryBase.addStringProperty("token");
        galleryBase.addStringProperty("title");
        galleryBase.addStringProperty("titleJpn");
        galleryBase.addStringProperty("thumb");
        galleryBase.addIntProperty("category");
        galleryBase.addStringProperty("posted");
        galleryBase.addStringProperty("uploader");
        galleryBase.addFloatProperty("rating");
        galleryBase.addIntProperty("reference");
    }

    public static void addDownloadInfo(Schema schema) {
        Entity downloadInfo = schema.addEntity("DownloadInfoObj");
        downloadInfo.setTableName("DOWNLOAD_INFO");
        downloadInfo.addLongProperty("gid").primaryKey();
        downloadInfo.addIntProperty("state");
    }

    public static void addSpider(Schema schema) {
        Entity spider = schema.addEntity("SpiderObj");
        spider.setTableName("SPIDER");
        spider.addLongProperty("gid").primaryKey();
        spider.addIntProperty("lastIndex");
        spider.addStringProperty("dirname");
        spider.addBooleanProperty("download");
        spider.addLongProperty("time");
    }
}
