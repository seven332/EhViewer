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

package com.hippo.ehviewer.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.hippo.ehviewer.client.data.DownloadInfo;
import com.hippo.ehviewer.client.data.DownloadLabel;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.data.QuickSearch;
import com.hippo.ehviewer.dao.DaoMaster;
import com.hippo.ehviewer.dao.DaoSession;
import com.hippo.ehviewer.dao.DirnameObj;
import com.hippo.ehviewer.dao.DirnameObjDao;
import com.hippo.ehviewer.dao.DownloadInfoObj;
import com.hippo.ehviewer.dao.DownloadInfoObjDao;
import com.hippo.ehviewer.dao.DownloadLabelObj;
import com.hippo.ehviewer.dao.DownloadLabelObjDao;
import com.hippo.ehviewer.dao.GalleryBaseObj;
import com.hippo.ehviewer.dao.GalleryBaseObjDao;
import com.hippo.ehviewer.dao.QuickSearchObj;
import com.hippo.ehviewer.dao.QuickSearchObjDao;

import java.util.ArrayList;
import java.util.List;

public class DBUtils {

    private static DaoSession sDaoSession;

    public static void initialize(Context context) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(
                context.getApplicationContext(), "ehviewer", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        sDaoSession = daoMaster.newSession();
    }

    public static void addGalleryBase(GalleryBase galleryBase) {
        GalleryBaseObjDao galleryBaseObjDao = sDaoSession.getGalleryBaseObjDao();
        GalleryBaseObj galleryBaseObj = galleryBaseObjDao.load((long) galleryBase.gid);
        if (galleryBaseObj == null) {
            // add new item, set refernce 1
            galleryBaseObj = new GalleryBaseObj();
            galleryBaseObj.setGid((long) galleryBase.gid);
            galleryBaseObj.setToken(galleryBase.token);
            galleryBaseObj.setTitle(galleryBase.title);
            galleryBaseObj.setTitleJpn(galleryBase.titleJpn);
            galleryBaseObj.setThumb(galleryBase.thumb);
            galleryBaseObj.setCategory(galleryBase.category);
            galleryBaseObj.setPosted(galleryBase.posted);
            galleryBaseObj.setUploader(galleryBase.uploader);
            galleryBaseObj.setRating(galleryBase.rating);
            galleryBaseObj.setReference(1);
            galleryBaseObjDao.insert(galleryBaseObj);
        } else {
            // already in db, add refernce
            galleryBaseObj.setReference(galleryBaseObj.getReference() + 1);
            galleryBaseObjDao.update(galleryBaseObj);
        }
    }

    public static void removeGalleryBase(GalleryBase galleryBase) {
        GalleryBaseObjDao galleryBaseObjDao = sDaoSession.getGalleryBaseObjDao();
        GalleryBaseObj galleryBaseObj = galleryBaseObjDao.load((long) galleryBase.gid);
        if (galleryBaseObj.getReference() <= 1) {
            // No reference, delete it
            galleryBaseObjDao.deleteByKey((long) galleryBase.gid);
        } else {
            // Still has reference, sub refernce
            galleryBaseObj.setReference(galleryBaseObj.getReference() - 1);
            galleryBaseObjDao.update(galleryBaseObj);
        }
    }

    public static GalleryBase getGalleryBase(int gid) {
        GalleryBaseObjDao dao = sDaoSession.getGalleryBaseObjDao();
        GalleryBaseObj obj = dao.load((long) gid);
        if (obj != null) {
            GalleryBase galleryBase = new GalleryBase();
            galleryBase.gid = (int) (long) obj.getGid();
            galleryBase.token = obj.getToken();
            galleryBase.title = obj.getTitle();
            galleryBase.titleJpn = obj.getTitleJpn();
            galleryBase.thumb = obj.getThumb();
            galleryBase.category = obj.getCategory();
            galleryBase.posted = obj.getPosted();
            galleryBase.uploader = obj.getUploader();
            galleryBase.rating = obj.getRating();
            return galleryBase;
        } else {
            return null;
        }
    }

    public static String getDirname(int gid) {
        DirnameObjDao dirnameObjDao = sDaoSession.getDirnameObjDao();
        DirnameObj dirnameObj = dirnameObjDao.load((long) gid);
        if (dirnameObj != null) {
            // Touch the date
            dirnameObj.setTime(System.currentTimeMillis());
            dirnameObjDao.update(dirnameObj);
            return dirnameObj.getDirname();
        } else {
            return null;
        }
    }

    public static void addDirname(GalleryBase galleryBase, String dirname) {
        addGalleryBase(galleryBase);

        DirnameObj dirnameObj = new DirnameObj();
        dirnameObj.setGid((long) galleryBase.gid);
        dirnameObj.setDirname(dirname);
        dirnameObj.setTime(System.currentTimeMillis());
        sDaoSession.getDirnameObjDao().insert(dirnameObj);
    }

    public static void touchDirname(int gid) {
        DirnameObjDao dirnameObjDao = sDaoSession.getDirnameObjDao();
        DirnameObj dirnameObj = dirnameObjDao.load((long) gid);
        if (dirnameObj != null) {
            dirnameObj.setTime(System.currentTimeMillis());
            dirnameObjDao.update(dirnameObj);
        }
    }

    public static void addQuickSearch(String name, ListUrlBuilder builder) {
        if (TextUtils.isEmpty(name) ||
                (builder.getMode() != ListUrlBuilder.MODE_NORMAL &&
                        builder.getMode() != ListUrlBuilder.MODE_UPLOADER &&
                        builder.getMode() != ListUrlBuilder.MODE_TAG)) {
            return;
        }

        QuickSearchObj quickSearchObj = new QuickSearchObj();
        quickSearchObj.setName(name);
        quickSearchObj.setMode(builder.getMode());
        quickSearchObj.setCategory(builder.getCategory());
        quickSearchObj.setKeyword(builder.getKeyword());
        quickSearchObj.setAdvancedSearch(builder.getAdvanceSearch());
        quickSearchObj.setMinRating(builder.getMinRating());
        quickSearchObj.setTime(System.currentTimeMillis());
        sDaoSession.getQuickSearchObjDao().insert(quickSearchObj);
    }

    public static void moveQuickSearch(long fromId, long toId) {
        QuickSearchObjDao dao = sDaoSession.getQuickSearchObjDao();
        QuickSearchObj from = dao.load(fromId);
        QuickSearchObj to = dao.load(toId);
        long fromTime = from.getTime();
        long toTime = to.getTime();
        from.setTime(toTime);
        to.setTime(fromTime);
        dao.update(from);
        dao.update(to);
    }

    public static void updateQuickSearch(QuickSearch quickSearch) {
        QuickSearchObjDao dao = sDaoSession.getQuickSearchObjDao();
        QuickSearchObj obj = dao.load(quickSearch.id);
        obj.setName(quickSearch.name);
        obj.setMode(quickSearch.mode);
        obj.setCategory(quickSearch.category);
        obj.setKeyword(quickSearch.keyword);
        obj.setAdvancedSearch(quickSearch.advancedSearch);
        obj.setMinRating(quickSearch.minRating);
        dao.update(obj);
    }

    public static void removeQuickSearch(long id) {
        sDaoSession.getQuickSearchObjDao().deleteByKey(id);
    }

    public static List<QuickSearch> getAllQuickSearch() {
        QuickSearchObjDao quickSearchObjDao = sDaoSession.getQuickSearchObjDao();
        List<QuickSearchObj> list = quickSearchObjDao.queryBuilder().orderAsc(QuickSearchObjDao.Properties.Time).list();
        List<QuickSearch> result = new ArrayList<>(list.size());
        for (QuickSearchObj quickSearchObj : list) {
            result.add(QuickSearch.fromQuickSearchObj(quickSearchObj));
        }
        return  result;
    }

    public static List<DownloadInfo> getAllDownloadInfo() {
        DownloadInfoObjDao dao = sDaoSession.getDownloadInfoObjDao();
        List<DownloadInfoObj> list = dao.queryBuilder().orderAsc(DownloadInfoObjDao.Properties.Time).list();
        List<DownloadInfo> result = new ArrayList<>(list.size());
        for (DownloadInfoObj obj : list) {
            result.add(DownloadInfo.fromDownloadInfoObj(obj));
        }
        return  result;
    }

    public static void addDownloadInfo(DownloadInfo downloadInfo) {
        DownloadInfoObj obj = new DownloadInfoObj();
        obj.setGid((long) downloadInfo.galleryBase.gid);
        obj.setLabel(downloadInfo.label);
        obj.setState(downloadInfo.state);
        obj.setLegacy(downloadInfo.legacy);
        obj.setTime(downloadInfo.time);
        sDaoSession.getDownloadInfoObjDao().insert(obj);
        addGalleryBase(downloadInfo.galleryBase);
    }

    public static void updateDownloadInfo(DownloadInfo downloadInfo) {
        DownloadInfoObjDao dao = sDaoSession.getDownloadInfoObjDao();
        DownloadInfoObj obj = dao.load((long) downloadInfo.galleryBase.gid);
        obj.setState(downloadInfo.state);
        obj.setLegacy(downloadInfo.legacy);
        obj.setLabel(downloadInfo.label);
        dao.update(obj);
    }

    public static List<String> getAllDownloadLabel() {
        DownloadLabelObjDao dao = sDaoSession.getDownloadLabelObjDao();
        List<DownloadLabelObj> list = dao.queryBuilder().orderAsc(DownloadLabelObjDao.Properties.Time).list();
        List<String> result = new ArrayList<>(list.size());
        for (DownloadLabelObj obj : list) {
            result.add(obj.getLabel());
        }
        return result;
    }

    public static List<DownloadLabel> getAllDownloadLabelWithId() {
        DownloadLabelObjDao dao = sDaoSession.getDownloadLabelObjDao();
        List<DownloadLabelObj> list = dao.queryBuilder().orderAsc(DownloadLabelObjDao.Properties.Time).list();
        List<DownloadLabel> result = new ArrayList<>(list.size());
        for (DownloadLabelObj obj : list) {
            DownloadLabel tag = new DownloadLabel();
            tag.id = obj.getId();
            tag.label = obj.getLabel();
            tag.time = obj.getTime();
            result.add(tag);
        }
        return result;
    }

    public static void addDownloadLabel(String label) {
        if (TextUtils.isEmpty(label)) {
            return;
        }

        DownloadLabelObj obj = new DownloadLabelObj();
        obj.setLabel(label);
        obj.setTime(System.currentTimeMillis());
        sDaoSession.getDownloadLabelObjDao().insert(obj);
    }

    public static void moveDownloadLabel(long fromId, long toId) {
        DownloadLabelObjDao dao = sDaoSession.getDownloadLabelObjDao();
        DownloadLabelObj from = dao.load(fromId);
        DownloadLabelObj to = dao.load(toId);
        long fromTime = from.getTime();
        long toTime = to.getTime();
        from.setTime(toTime);
        to.setTime(fromTime);
        dao.update(from);
        dao.update(to);
    }

    public static void updateDownloadLabel(DownloadLabel label) {
        DownloadLabelObjDao dao = sDaoSession.getDownloadLabelObjDao();
        DownloadLabelObj obj = dao.load(label.id);
        obj.setId(label.id);
        obj.setLabel(label.label);
        obj.setTime(label.time);
        dao.update(obj);
    }

    public static void removeDownloadLabel(long id) {
        sDaoSession.getDownloadLabelObjDao().deleteByKey(id);
    }

    public static void removeDownloadLabel(String label) {
        DownloadLabelObjDao dao = sDaoSession.getDownloadLabelObjDao();
        List<DownloadLabelObj> list = dao.queryBuilder()
                .where(DownloadLabelObjDao.Properties.Label.eq(label)).list();
        for (DownloadLabelObj obj : list) {
            dao.delete(obj);
        }
    }

    public static boolean containDownloadLabel(String label) {
        return sDaoSession.getDownloadLabelObjDao().queryBuilder()
                .where(DownloadLabelObjDao.Properties.Label.eq(label)).count() != 0;
    }
}
