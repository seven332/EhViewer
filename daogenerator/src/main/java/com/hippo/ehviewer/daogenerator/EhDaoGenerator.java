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

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.io.File;
import java.io.FileWriter;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class EhDaoGenerator {

    private static final String PACKAGE = "com.hippo.ehviewer.dao";
    private static final String OUT_DIR = "../app/src/main/java-gen";
    private static final String DELETE_DIR = "../app/src/main/java-gen/com/hippo/ehviewer/dao";

    private static final int VERSION = 1;

    private static final String DOWNLOAD_INFO_PATH = "../app/src/main/java-gen/com/hippo/ehviewer/dao/DownloadInfo.java";

    public static void generate() throws Exception {
        Utilities.deleteContents(new File(DELETE_DIR));
        File outDir = new File(OUT_DIR);
        outDir.delete();
        outDir.mkdirs();

        Schema schema = new Schema(VERSION, PACKAGE);
        addGalleryInfo(schema);
        addDownloads(schema);
        addDownloadLabel(schema);
        addDownloadDirname(schema);
        addHistoryInfo(schema);
        addQuickSearch(schema);
        addLocalFavorites(schema);
        new DaoGenerator().generateAll(schema, OUT_DIR);

        adjustDownloads();
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

    private static void addDownloads(Schema schema) {
        Entity entity = schema.addEntity("DownloadInfo");
        entity.setTableName("DOWNLOADS");
        entity.setClassNameDao("DownloadsDao");

        entity.setSuperclass("com.hippo.ehviewer.client.data.GalleryInfo");

        // Gallery Info Data
        entity.addLongProperty("gid").primaryKey().notNull();
        entity.addStringProperty("token");
        entity.addStringProperty("title");
        entity.addStringProperty("titleJpn");
        entity.addStringProperty("thumb");
        entity.addIntProperty("category").notNull();
        entity.addStringProperty("posted");
        entity.addStringProperty("uploader");
        entity.addFloatProperty("rating").notNull();
        entity.addStringProperty("simpleLanguage");
        // Download Info Data
        entity.addIntProperty("state").notNull();
        entity.addIntProperty("legacy").notNull();
        entity.addLongProperty("time").notNull();
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

    private static void addDownloadDirname(Schema schema) {
        Entity entity = schema.addEntity("DownloadDirnameRaw");
        entity.setTableName("DOWNLOAD_DIRNAME");
        entity.setClassNameDao("DownloadDirnameDao");
        entity.addLongProperty("gid").primaryKey();
        entity.addStringProperty("dirname");
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

    private static void addLocalFavorites(Schema schema) {
        Entity entity = schema.addEntity("LocalFavoritesRaw");
        entity.setTableName("LOCAL_FAVORITES");
        entity.setClassNameDao("LocalFavoritesDao");
        entity.addLongProperty("gid").primaryKey();
        entity.addLongProperty("date");
    }

    private static void adjustDownloads() throws Exception {
        JavaClassSource javaClass = Roaster.parse(JavaClassSource.class, new File(DOWNLOAD_INFO_PATH));
        // Remove field from GalleryInfo
        javaClass.removeField(javaClass.getField("gid"));
        javaClass.removeField(javaClass.getField("token"));
        javaClass.removeField(javaClass.getField("title"));
        javaClass.removeField(javaClass.getField("titleJpn"));
        javaClass.removeField(javaClass.getField("thumb"));
        javaClass.removeField(javaClass.getField("category"));
        javaClass.removeField(javaClass.getField("posted"));
        javaClass.removeField(javaClass.getField("uploader"));
        javaClass.removeField(javaClass.getField("rating"));
        // Set all field public
        javaClass.getField("state").setPublic();
        javaClass.getField("legacy").setPublic();
        javaClass.getField("time").setPublic();
        javaClass.getField("label").setPublic();
        // Add Parcelable stuff
        javaClass.addMethod("\t@Override\n" +
                "\tpublic int describeContents() {\n" +
                "\t\treturn 0;\n" +
                "\t}");
        javaClass.addMethod("\t@Override\n" +
                "\tpublic void writeToParcel(Parcel dest, int flags) {\n" +
                "\t\tsuper.writeToParcel(dest, flags);\n" +
                "\t\tdest.writeInt(this.state);\n" +
                "\t\tdest.writeInt(this.legacy);\n" +
                "\t\tdest.writeLong(this.time);\n" +
                "\t\tdest.writeString(this.label);\n" +
                "\t}");
        javaClass.addMethod("\tprotected DownloadInfo(Parcel in) {\n" +
                "\t\tsuper(in);\n" +
                "\t\tthis.state = in.readInt();\n" +
                "\t\tthis.legacy = in.readInt();\n" +
                "\t\tthis.time = in.readLong();\n" +
                "\t\tthis.label = in.readString();\n" +
                "\t}").setConstructor(true);
        javaClass.addField("\tpublic static final Creator<DownloadInfo> CREATOR = new Creator<DownloadInfo>() {\n" +
                "\t\t@Override\n" +
                "\t\tpublic DownloadInfo createFromParcel(Parcel source) {\n" +
                "\t\t\treturn new DownloadInfo(source);\n" +
                "\t\t}\n" +
                "\n" +
                "\t\t@Override\n" +
                "\t\tpublic DownloadInfo[] newArray(int size) {\n" +
                "\t\t\treturn new DownloadInfo[size];\n" +
                "\t\t}\n" +
                "\t};");
        javaClass.addImport("android.os.Parcel");
        // Add download info stuff
        javaClass.addField("public static final int STATE_NONE = 0");
        javaClass.addField("public static final int STATE_WAIT = 1");
        javaClass.addField("public static final int STATE_DOWNLOAD = 2");
        javaClass.addField("public static final int STATE_FINISH = 3");
        javaClass.addField("public static final int STATE_FAILED = 4");
        javaClass.addField("public long speed");
        javaClass.addField("public long remaining");
        javaClass.addField("public int finished");
        javaClass.addField("public int downloaded");
        javaClass.addField("public int total");
        // Add from GalleryInfo constructor
        javaClass.addMethod("\tpublic DownloadInfo(GalleryInfo galleryInfo) {\n" +
                "\t\tthis.gid = galleryInfo.gid;\n" +
                "\t\tthis.token = galleryInfo.token;\n" +
                "\t\tthis.title = galleryInfo.title;\n" +
                "\t\tthis.titleJpn = galleryInfo.titleJpn;\n" +
                "\t\tthis.thumb = galleryInfo.thumb;\n" +
                "\t\tthis.category = galleryInfo.category;\n" +
                "\t\tthis.posted = galleryInfo.posted;\n" +
                "\t\tthis.uploader = galleryInfo.uploader;\n" +
                "\t\tthis.rating = galleryInfo.rating;\n" +
                "\t\tthis.simpleTags = galleryInfo.simpleTags;\n" +
                "\t\tthis.simpleLanguage = galleryInfo.simpleLanguage;\n" +
                "\t}").setConstructor(true);
        javaClass.addImport("com.hippo.ehviewer.client.data.GalleryInfo");

        FileWriter fileWriter = new FileWriter(DOWNLOAD_INFO_PATH);
        fileWriter.write(javaClass.toString());
        fileWriter.close();
    }
}
